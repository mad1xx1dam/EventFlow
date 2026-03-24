package com.eventflow.eventflow_backend.controller;

import com.eventflow.eventflow_backend.dto.request.CreatePollRequest;
import com.eventflow.eventflow_backend.dto.response.PollResponse;
import com.eventflow.eventflow_backend.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events/{eventId}/polls")
@RequiredArgsConstructor
public class EventPollController {

    private final PollService pollService;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<PollResponse> createPoll(
            @PathVariable Long eventId,
            @Valid @RequestBody CreatePollRequest request
    ) {
        return ResponseEntity.ok(pollService.createPoll(eventId, request));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/active")
    public ResponseEntity<PollResponse> getActivePoll(@PathVariable Long eventId) {
        return ResponseEntity.ok(pollService.getActivePollByEventId(eventId));
    }
}
