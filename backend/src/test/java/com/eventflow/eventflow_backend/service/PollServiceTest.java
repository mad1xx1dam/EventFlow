package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.request.CreatePollRequest;
import com.eventflow.eventflow_backend.dto.request.VotePollRequest;
import com.eventflow.eventflow_backend.dto.response.PollResponse;
import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.EventGuest;
import com.eventflow.eventflow_backend.entity.Poll;
import com.eventflow.eventflow_backend.entity.PollOption;
import com.eventflow.eventflow_backend.entity.User;
import com.eventflow.eventflow_backend.entity.enums.PollStatus;
import com.eventflow.eventflow_backend.mapper.PollMapper;
import com.eventflow.eventflow_backend.repository.EventGuestRepository;
import com.eventflow.eventflow_backend.repository.EventRepository;
import com.eventflow.eventflow_backend.repository.PollOptionRepository;
import com.eventflow.eventflow_backend.repository.PollRepository;
import com.eventflow.eventflow_backend.repository.PollVoteRepository;
import com.eventflow.eventflow_backend.security.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollServiceTest {

    @Mock
    private PollRepository pollRepository;
    @Mock
    private PollOptionRepository pollOptionRepository;
    @Mock
    private PollVoteRepository pollVoteRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventGuestRepository eventGuestRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PollRedisService pollRedisService;
    @Mock
    private PollMapper pollMapper;
    @Mock
    private WebSocketEventService webSocketEventService;

    @InjectMocks
    private PollService pollService;

    @Test
    void createPoll_Success() {
        Long eventId = 1L;
        Long currentUserId = 1L;

        CreatePollRequest request = new CreatePollRequest();
        request.setQuestion("Test Question?");
        request.setOptions(List.of("Option 1", "Option 2"));

        Event event = new Event();
        event.setId(eventId);
        User creator = new User();
        creator.setId(currentUserId);
        event.setCreator(creator);

        Poll poll = new Poll();
        poll.setId(10L);

        PollOption option1 = new PollOption();
        option1.setId(101L);
        PollOption option2 = new PollOption();
        option2.setId(102L);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(currentUserService.getCurrentUserIdOrThrow()).thenReturn(currentUserId);
        when(currentUserService.isAdmin()).thenReturn(false);
        when(pollRepository.existsByEventIdAndStatus(eventId, PollStatus.ACTIVE)).thenReturn(false);
        when(currentUserService.getCurrentUserOrThrow()).thenReturn(creator);

        when(pollMapper.toPoll(any(), any(), anyString(), any())).thenReturn(poll);
        when(pollRepository.save(poll)).thenReturn(poll);
        when(pollMapper.toPollOption(any(), anyString(), anyInt(), any()))
                .thenReturn(option1)
                .thenReturn(option2);
        when(pollOptionRepository.save(any(PollOption.class)))
                .thenReturn(option1)
                .thenReturn(option2);

        when(pollRedisService.getCounts(poll.getId())).thenReturn(Map.of());
        when(pollMapper.toPollResponse(any(), any())).thenReturn(new PollResponse());

        PollResponse response = pollService.createPoll(eventId, request);

        assertNotNull(response);
        // проверяем инициализацию опроса в Redis и отправку уведомления в вебсокет
        verify(pollRedisService).initializeActivePoll(eq(10L), anyList());
        verify(webSocketEventService).sendPollStarted(eq(eventId), any(PollResponse.class));
    }

    @Test
    void vote_Success() {
        Long pollId = 10L;
        UUID guestToken = UUID.randomUUID();

        VotePollRequest request = new VotePollRequest();
        request.setPollOptionId(101L);

        Event event = new Event();
        event.setId(1L);

        Poll poll = new Poll();
        poll.setId(pollId);
        poll.setEvent(event);
        poll.setStatus(PollStatus.ACTIVE);

        EventGuest guest = new EventGuest();
        guest.setId(5L);
        guest.setTokenActive(true);

        PollOption option = new PollOption();
        option.setId(101L);
        option.setPoll(poll);

        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(pollRedisService.isPollActive(pollId)).thenReturn(true);
        when(eventGuestRepository.findByEventIdAndGuestToken(event.getId(), guestToken))
                .thenReturn(Optional.of(guest));
        when(pollOptionRepository.findById(101L)).thenReturn(Optional.of(option));

        when(pollOptionRepository.findAllByPollIdOrderByPositionAsc(pollId)).thenReturn(List.of(option));
        when(pollRedisService.getCounts(pollId)).thenReturn(Map.of(101L, 1L));
        when(pollMapper.toPollResponse(any(), any())).thenReturn(new PollResponse());

        PollResponse response = pollService.vote(pollId, guestToken, request);

        assertNotNull(response);
        // проверяем сохранение голоса в Redis
        verify(pollRedisService).saveVote(pollId, guest.getId(), option.getId());
        verify(webSocketEventService).sendPollUpdated(eq(pollId), any(PollResponse.class));
    }

    @Test
    void closePoll_Success() {
        Long pollId = 10L;
        Long currentUserId = 1L;

        Event event = new Event();
        event.setId(1L);
        User creator = new User();
        creator.setId(currentUserId);
        event.setCreator(creator);

        Poll poll = new Poll();
        poll.setId(pollId);
        poll.setEvent(event);
        poll.setStatus(PollStatus.ACTIVE);

        EventGuest guest = new EventGuest();
        guest.setId(5L);

        PollOption option = new PollOption();
        option.setId(101L);

        when(pollRepository.findById(pollId)).thenReturn(Optional.of(poll));
        when(currentUserService.getCurrentUserIdOrThrow()).thenReturn(currentUserId);
        when(currentUserService.isAdmin()).thenReturn(false);

        when(pollRedisService.getChoices(pollId)).thenReturn(Map.of(5L, 101L));
        // мокаем получение существующих голосов перед пакетным сохранением
        when(pollVoteRepository.findAllByPollId(pollId)).thenReturn(List.of());
        when(eventGuestRepository.findById(5L)).thenReturn(Optional.of(guest));
        when(pollOptionRepository.findById(101L)).thenReturn(Optional.of(option));
        when(pollRepository.save(any(Poll.class))).thenReturn(poll);
        when(pollRedisService.getCounts(pollId)).thenReturn(Map.of(101L, 1L));
        when(pollMapper.toPollResponse(any(), any())).thenReturn(new PollResponse());

        PollResponse response = pollService.closePoll(pollId);

        assertNotNull(response);
        assertEquals(PollStatus.CLOSED, poll.getStatus());
        // проверяем пакетное сохранение вместо одиночного
        verify(pollVoteRepository, times(1)).saveAll(anyList());
        verify(pollRedisService).closePoll(pollId);
        verify(pollRedisService).cleanup(pollId);
        verify(webSocketEventService).sendPollClosed(eq(event.getId()), eq(pollId), any(PollResponse.class));
    }
}