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

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final EventGuestRepository eventGuestRepository;
    private final EventRepository eventRepository;
    private final PollRepository pollRepository;

    @Override
    @Transactional(readOnly = true)
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String guestTokenHeader = accessor.getFirstNativeHeader("Guest-Token");

            boolean isAuthenticated = false;

            // 1. Проверка по JWT (для организатора или админа)
            if (authHeader != null && authHeader.startsWith("Bearer ") && destination != null) {
                String token = authHeader.substring(7);
                if (jwtService.isTokenValid(token)) {
                    Long userId = jwtService.extractUserId(token);
                    // предполагаем, что роль зашита в claims токена при генерации
                    String role = jwtService.extractRole(token);

                    if ("ADMIN".equals(role)) {
                        isAuthenticated = true;
                    } else {
                        Long eventId = extractEventIdFromDestination(destination);
                        if (eventId != null) {
                            Event event = eventRepository.findById(eventId).orElse(null);
                            // пускаем, только если юзер из токена является создателем
                            if (event != null && event.getCreator().getId().equals(userId)) {
                                isAuthenticated = true;
                            }
                        }
                    }
                }
            }

            // 2. Проверка по Guest Token (для приглашенных гостей)
            if (!isAuthenticated && guestTokenHeader != null && destination != null) {
                try {
                    UUID guestToken = UUID.fromString(guestTokenHeader);
                    EventGuest guest = eventGuestRepository.findByGuestToken(guestToken).orElse(null);

                    if (guest != null && Boolean.TRUE.equals(guest.getTokenActive())) {
                        Long eventId = extractEventIdFromDestination(destination);
                        if (eventId != null) {
                            isAuthenticated = guest.getEvent().getId().equals(eventId);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Invalid guest token format or ID extraction failed", e);
                }
            }

            if (!isAuthenticated) {
                throw new IllegalArgumentException("Access denied to WebSocket topic");
            }
        }

        return message;
    }

    // логика извлечения ID едина, так как в URL топика всегда зашит либо ID события, либо ID опроса (по которому можно найти событие)
    private Long extractEventIdFromDestination(String destination) {
        if (destination.startsWith("/topic/events/")) {
            String remainder = destination.substring("/topic/events/".length());
            int slashIndex = remainder.indexOf('/');
            String idStr = slashIndex != -1 ? remainder.substring(0, slashIndex) : remainder;
            return Long.parseLong(idStr);
        } else if (destination.startsWith("/topic/polls/")) {
            String remainder = destination.substring("/topic/polls/".length());
            int slashIndex = remainder.indexOf('/');
            String idStr = slashIndex != -1 ? remainder.substring(0, slashIndex) : remainder;
            Long pollId = Long.parseLong(idStr);
            Poll poll = pollRepository.findById(pollId).orElse(null);
            return poll != null ? poll.getEvent().getId() : null;
        }
        return null;
    }
}