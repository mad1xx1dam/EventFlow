package com.eventflow.eventflow_backend.repository;

import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findAllByCreatorOrderByStartsAtAsc(User creator);

    List<Event> findAllByCreatorIdOrderByStartsAtAsc(Long creatorId);

    List<Event> findAllByStartsAtBetweenOrderByStartsAtAsc(OffsetDateTime from, OffsetDateTime to);

    List<Event> findAllByCreatorIdAndStartsAtBetweenOrderByStartsAtAsc(
            Long creatorId,
            OffsetDateTime from,
            OffsetDateTime to
    );

    long countByCreatorId(Long creatorId);
}
