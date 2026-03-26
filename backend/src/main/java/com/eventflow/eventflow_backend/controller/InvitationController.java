package com.eventflow.eventflow_backend.controller;

import com.eventflow.eventflow_backend.dto.request.CreateInvitationsRequest;
import com.eventflow.eventflow_backend.dto.request.UpdateRsvpRequest;
import com.eventflow.eventflow_backend.dto.response.GuestInvitationDetailsResponse;
import com.eventflow.eventflow_backend.dto.response.InvitationResponse;
import com.eventflow.eventflow_backend.service.InvitationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events/{eventId}")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/invitations")
    public ResponseEntity<List<InvitationResponse>> createInvitations(
            @PathVariable Long eventId,
            @Valid @RequestBody CreateInvitationsRequest request
    ) {
        return ResponseEntity.ok(invitationService.createInvitations(eventId, request));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping("/invitations/resend")
    public ResponseEntity<List<InvitationResponse>> resendInvitations(@PathVariable Long eventId) {
        return ResponseEntity.ok(invitationService.resendInvitations(eventId));
    }

    @GetMapping("/invite/{guestToken}")
    public ResponseEntity<GuestInvitationDetailsResponse> getGuestInvitation(
            @PathVariable Long eventId,
            @PathVariable UUID guestToken
    ) {
        return ResponseEntity.ok(invitationService.getGuestInvitation(eventId, guestToken));
    }

    @PostMapping("/invite/{guestToken}/rsvp")
    public ResponseEntity<GuestInvitationDetailsResponse> updateRsvp(
            @PathVariable Long eventId,
            @PathVariable UUID guestToken,
            @Valid @RequestBody UpdateRsvpRequest request
    ) {
        return ResponseEntity.ok(invitationService.updateRsvp(eventId, guestToken, request));
    }

    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/guests")
    public ResponseEntity<List<InvitationResponse>> getEventGuests(@PathVariable Long eventId) {
        return ResponseEntity.ok(invitationService.getEventGuests(eventId));
    }
}