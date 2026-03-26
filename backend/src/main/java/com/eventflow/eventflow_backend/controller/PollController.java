package com.eventflow.eventflow_backend.controller;

import com.eventflow.eventflow_backend.dto.request.VotePollRequest;
import com.eventflow.eventflow_backend.dto.response.PollResponse;
import com.eventflow.eventflow_backend.service.PollService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @PostMapping("/{pollId}/vote")
    public ResponseEntity<PollResponse> vote(
            @PathVariable Long pollId,
            @RequestParam UUID guestToken,
            @Valid @RequestBody VotePollRequest request
    ) {
        return ResponseEntity.ok(pollService.vote(pollId, guestToken, request));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/{pollId}/close")
    public ResponseEntity<PollResponse> closePoll(@PathVariable Long pollId) {
        return ResponseEntity.ok(pollService.closePoll(pollId));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{pollId}/results")
    public ResponseEntity<PollResponse> getResults(@PathVariable Long pollId) {
        return ResponseEntity.ok(pollService.getPollResults(pollId));
    }
}