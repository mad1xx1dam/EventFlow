package com.eventflow.eventflow_backend.entity;

import com.eventflow.eventflow_backend.entity.enums.RsvpStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "event_guests",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_event_guests_event_email", columnNames = {"event_id", "guest_email"})
        }
)
@Getter
@Setter
@NoArgsConstructor
public class EventGuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_user_id")
    private User registeredUser;

    @Column(name = "guest_email", nullable = false, length = 255)
    private String guestEmail;

    @Column(name = "guest_token", nullable = false, unique = true)
    private UUID guestToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "rsvp_status", nullable = false, length = 32)
    private RsvpStatus rsvpStatus;

    @Column(name = "token_active", nullable = false)
    private Boolean tokenActive;

    @Column(name = "invited_at", nullable = false)
    private OffsetDateTime invitedAt;

    @Column(name = "responded_at")
    private OffsetDateTime respondedAt;
}