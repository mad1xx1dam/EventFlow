package com.eventflow.eventflow_backend.repository;

import java.util.List;
import java.util.Optional;

import com.eventflow.eventflow_backend.entity.Poll;
import com.eventflow.eventflow_backend.entity.enums.PollStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollRepository extends JpaRepository<Poll, Long> {

    List<Poll> findAllByEventIdOrderByStartedAtDesc(Long eventId);

    Optional<Poll> findByEventIdAndStatus(Long eventId, PollStatus status);

    boolean existsByEventIdAndStatus(Long eventId, PollStatus status);
}