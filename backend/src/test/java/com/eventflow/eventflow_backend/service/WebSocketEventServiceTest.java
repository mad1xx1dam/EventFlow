package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.response.PollLiveEventResponse;
import com.eventflow.eventflow_backend.dto.response.PollResponse;
import com.eventflow.eventflow_backend.dto.response.RsvpCountersResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketEventServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketEventService webSocketEventService;

    @Test
    void sendRsvpCounters_Success() {
        Long eventId = 1L;
        RsvpCountersResponse response = new RsvpCountersResponse();

        webSocketEventService.sendRsvpCounters(eventId, response);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/events/1/rsvp-counters"),
                eq(response)
        );
    }

    @Test
    void sendPollStarted_Success() {
        Long eventId = 1L;
        PollResponse pollResponse = new PollResponse();

        webSocketEventService.sendPollStarted(eventId, pollResponse);

        ArgumentCaptor<PollLiveEventResponse> captor = ArgumentCaptor.forClass(PollLiveEventResponse.class);
        verify(messagingTemplate).convertAndSend(
                eq("/topic/events/1/polls/active"),
                captor.capture()
        );

        PollLiveEventResponse sentResponse = captor.getValue();
        assertEquals("POLL_STARTED", sentResponse.getType());
        assertEquals(pollResponse, sentResponse.getPoll());
    }

    @Test
    void sendPollClosed_Success() {
        Long eventId = 1L;
        Long pollId = 10L;
        PollResponse pollResponse = new PollResponse();

        webSocketEventService.sendPollClosed(eventId, pollId, pollResponse);

        ArgumentCaptor<PollLiveEventResponse> captor = ArgumentCaptor.forClass(PollLiveEventResponse.class);

        // проверяем, что ушло в оба нужных топика
        verify(messagingTemplate).convertAndSend(
                eq("/topic/events/1/polls/active"),
                captor.capture()
        );
        verify(messagingTemplate).convertAndSend(
                eq("/topic/polls/10/results"),
                captor.capture()
        );

        assertEquals("POLL_CLOSED", captor.getAllValues().get(0).getType());
    }
}