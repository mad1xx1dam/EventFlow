package com.eventflow.eventflow_backend.repository;

import com.eventflow.eventflow_backend.entity.Poll;
import com.eventflow.eventflow_backend.entity.enums.PollStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PollRepository extends JpaRepository<Poll, Long> {

    boolean existsByEventIdAndStatus(Long eventId, PollStatus status);

    Optional<Poll> findByEventIdAndStatus(Long eventId, PollStatus status);

    List<Poll> findAllByEventIdOrderByStartedAtDesc(Long eventId);
}