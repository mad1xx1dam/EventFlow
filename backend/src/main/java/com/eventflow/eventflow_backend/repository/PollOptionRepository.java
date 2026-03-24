package com.eventflow.eventflow_backend.repository;

import com.eventflow.eventflow_backend.entity.PollOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    List<PollOption> findAllByPollIdOrderByPositionAsc(Long pollId);
}