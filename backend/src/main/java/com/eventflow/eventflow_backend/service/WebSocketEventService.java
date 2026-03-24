package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.response.PollLiveEventResponse;
import com.eventflow.eventflow_backend.dto.response.PollResponse;
import com.eventflow.eventflow_backend.dto.response.RsvpCountersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketEventService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendRsvpCounters(Long eventId, RsvpCountersResponse response) {
        messagingTemplate.convertAndSend(
                "/topic/events/" + eventId + "/rsvp-counters",
                response
        );
    }

    public void sendPollStarted(Long eventId, PollResponse pollResponse) {
        PollLiveEventResponse response = new PollLiveEventResponse();
        response.setType("POLL_STARTED");
        response.setPoll(pollResponse);

        messagingTemplate.convertAndSend(
                "/topic/events/" + eventId + "/polls/active",
                response
        );
    }

    public void sendPollUpdated(Long pollId, PollResponse pollResponse) {
        PollLiveEventResponse response = new PollLiveEventResponse();
        response.setType("POLL_UPDATED");
        response.setPoll(pollResponse);

        messagingTemplate.convertAndSend(
                "/topic/polls/" + pollId + "/results",
                response
        );
    }

    public void sendPollClosed(Long eventId, Long pollId, PollResponse pollResponse) {
        PollLiveEventResponse response = new PollLiveEventResponse();
        response.setType("POLL_CLOSED");
        response.setPoll(pollResponse);

        messagingTemplate.convertAndSend(
                "/topic/events/" + eventId + "/polls/active",
                response
        );

        messagingTemplate.convertAndSend(
                "/topic/polls/" + pollId + "/results",
                response
        );
    }
}