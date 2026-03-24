package com.eventflow.eventflow_backend.repository;

import com.eventflow.eventflow_backend.entity.EventGuest;
import com.eventflow.eventflow_backend.entity.enums.RsvpStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventGuestRepository extends JpaRepository<EventGuest, Long> {

    Optional<EventGuest> findByGuestToken(UUID guestToken);

    Optional<EventGuest> findByEventIdAndGuestEmail(Long eventId, String guestEmail);

    List<EventGuest> findAllByEventIdOrderByInvitedAtAsc(Long eventId);

    List<EventGuest> findAllByRegisteredUserIdOrderByInvitedAtDesc(Long registeredUserId);

    long countByEventIdAndRsvpStatus(Long eventId, RsvpStatus rsvpStatus);

    boolean existsByEventIdAndGuestEmail(Long eventId, String guestEmail);

    long countByRegisteredUserIdAndRsvpStatus(Long registeredUserId, RsvpStatus rsvpStatus);

    List<EventGuest> findAllByRegisteredUserIdAndEvent_StartsAtBetweenOrderByEvent_StartsAtAsc(
            Long registeredUserId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    Optional<EventGuest> findByEventIdAndGuestToken(Long eventId, UUID guestToken);
}