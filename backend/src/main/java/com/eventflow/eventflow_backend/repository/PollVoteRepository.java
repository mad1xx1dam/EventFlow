package com.eventflow.eventflow_backend.repository;

import com.eventflow.eventflow_backend.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    List<PollVote> findAllByPollId(Long pollId);

    Optional<PollVote> findByPollIdAndEventGuestId(Long pollId, Long eventGuestId);
}