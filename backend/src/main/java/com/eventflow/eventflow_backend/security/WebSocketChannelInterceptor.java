package com.eventflow.eventflow_backend.security;

import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.EventGuest;
import com.eventflow.eventflow_backend.entity.Poll;
import com.eventflow.eventflow_backend.repository.EventGuestRepository;
import com.eventflow.eventflow_backend.repository.EventRepository;
import com.eventflow.eventflow_backend.repository.PollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private static final String WS_AUTH_TYPE = "wsAuthType";
    private static final String WS_USER_ID = "wsUserId";
    private static final String WS_USER_ROLE = "wsUserRole";
    private static final String WS_GUEST_TOKEN = "wsGuestToken";

    private final JwtService jwtService;
    private final EventGuestRepository eventGuestRepository;
    private final EventRepository eventRepository;
    private final PollRepository pollRepository;

    @Override
    @Transactional(readOnly = true)
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        String guestTokenHeader = accessor.getFirstNativeHeader("Guest-Token");

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes == null) {
            throw new IllegalArgumentException("Не удалось инициализировать WebSocket-сессию");
        }

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (!jwtService.isTokenValid(token)) {
                throw new IllegalArgumentException("Недействительный JWT-токен для WebSocket");
            }

            Long userId = jwtService.extractUserId(token);
            String role = jwtService.extractRole(token);

            sessionAttributes.put(WS_AUTH_TYPE, "JWT");
            sessionAttributes.put(WS_USER_ID, userId);
            sessionAttributes.put(WS_USER_ROLE, role);

            return;
        }

        if (guestTokenHeader != null && !guestTokenHeader.isBlank()) {
            UUID guestToken;

            try {
                guestToken = UUID.fromString(guestTokenHeader);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Некорректный формат guest token");
            }

            EventGuest guest = eventGuestRepository.findByGuestToken(guestToken)
                    .orElseThrow(() -> new IllegalArgumentException("Гостевой токен не найден"));

            if (!Boolean.TRUE.equals(guest.getTokenActive())) {
                throw new IllegalArgumentException("Гостевой токен неактивен");
            }

            sessionAttributes.put(WS_AUTH_TYPE, "GUEST");
            sessionAttributes.put(WS_GUEST_TOKEN, guestToken.toString());

            return;
        }

        throw new IllegalArgumentException("Отсутствуют данные авторизации для WebSocket");
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination == null || destination.isBlank()) {
            throw new IllegalArgumentException("Не указан destination для подписки");
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes == null || sessionAttributes.isEmpty()) {
            throw new IllegalArgumentException("WebSocket-сессия не содержит данных авторизации");
        }

        String authType = asString(sessionAttributes.get(WS_AUTH_TYPE));

        if ("JWT".equals(authType)) {
            validateJwtSubscription(destination, sessionAttributes);
            return;
        }

        if ("GUEST".equals(authType)) {
            validateGuestSubscription(destination, sessionAttributes);
            return;
        }

        throw new IllegalArgumentException("Неизвестный тип авторизации WebSocket");
    }

    private void validateJwtSubscription(String destination, Map<String, Object> sessionAttributes) {
        Long userId = asLong(sessionAttributes.get(WS_USER_ID));
        String role = asString(sessionAttributes.get(WS_USER_ROLE));

        if (userId == null || role == null || role.isBlank()) {
            throw new IllegalArgumentException("В WebSocket-сессии отсутствуют данные пользователя");
        }

        if ("ADMIN".equals(role)) {
            return;
        }

        Long eventId = extractEventIdFromDestination(destination);

        if (eventId == null) {
            throw new IllegalArgumentException("Не удалось определить мероприятие по destination");
        }

        Event event = eventRepository.findById(eventId).orElse(null);

        if (event == null || event.getCreator() == null || !event.getCreator().getId().equals(userId)) {
            throw new IllegalArgumentException("Доступ к WebSocket topic запрещён");
        }
    }

    private void validateGuestSubscription(String destination, Map<String, Object> sessionAttributes) {
        String guestTokenValue = asString(sessionAttributes.get(WS_GUEST_TOKEN));

        if (guestTokenValue == null || guestTokenValue.isBlank()) {
            throw new IllegalArgumentException("В WebSocket-сессии отсутствует guest token");
        }

        UUID guestToken;

        try {
            guestToken = UUID.fromString(guestTokenValue);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Некорректный guest token в WebSocket-сессии");
        }

        EventGuest guest = eventGuestRepository.findByGuestToken(guestToken).orElse(null);

        if (guest == null || !Boolean.TRUE.equals(guest.getTokenActive())) {
            throw new IllegalArgumentException("Гостевой доступ к WebSocket недействителен");
        }

        Long eventId = extractEventIdFromDestination(destination);

        if (eventId == null || guest.getEvent() == null || !guest.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Гостю запрещён доступ к этому WebSocket topic");
        }
    }

    private Long extractEventIdFromDestination(String destination) {
        try {
            if (destination.startsWith("/topic/events/")) {
                String remainder = destination.substring("/topic/events/".length());
                int slashIndex = remainder.indexOf('/');
                String idStr = slashIndex != -1 ? remainder.substring(0, slashIndex) : remainder;
                return Long.parseLong(idStr);
            }

            if (destination.startsWith("/topic/polls/")) {
                String remainder = destination.substring("/topic/polls/".length());
                int slashIndex = remainder.indexOf('/');
                String idStr = slashIndex != -1 ? remainder.substring(0, slashIndex) : remainder;
                Long pollId = Long.parseLong(idStr);

                Poll poll = pollRepository.findById(pollId).orElse(null);
                if (poll != null && poll.getEvent() != null) {
                    return poll.getEvent().getId();
                }
            }

            return null;
        } catch (Exception ex) {
            log.warn("Не удалось извлечь eventId из destination: {}", destination, ex);
            return null;
        }
    }

    private String asString(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private Long asLong(Object value) {
        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Integer intValue) {
            return intValue.longValue();
        }

        if (value instanceof String stringValue) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }
}