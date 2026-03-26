package com.eventflow.eventflow_backend.mapper;

import com.eventflow.eventflow_backend.dto.response.PollOptionResponse;
import com.eventflow.eventflow_backend.dto.response.PollResponse;
import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.Poll;
import com.eventflow.eventflow_backend.entity.PollOption;
import com.eventflow.eventflow_backend.entity.User;
import java.time.OffsetDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface PollMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", source = "event")
    @Mapping(target = "createdByUser", source = "createdByUser")
    @Mapping(target = "question", expression = "java(requestQuestion.trim())")
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "startedAt", source = "now")
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    Poll toPoll(
            Event event,
            User createdByUser,
            String requestQuestion,
            OffsetDateTime now
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "poll", source = "poll")
    @Mapping(target = "optionText", expression = "java(optionText.trim())")
    @Mapping(target = "position", source = "position")
    @Mapping(target = "createdAt", source = "now")
    PollOption toPollOption(
            Poll poll,
            String optionText,
            Integer position,
            OffsetDateTime now
    );

    @Mapping(target = "votesCount", source = "votesCount")
    PollOptionResponse toPollOptionResponse(PollOption pollOption, Long votesCount);

    @Mapping(target = "eventId", source = "poll.event.id")
    @Mapping(target = "createdByUserId", source = "poll.createdByUser.id")
    @Mapping(target = "status", expression = "java(poll.getStatus().name())")
    @Mapping(target = "options", source = "options")
    @Mapping(target = "votedByCurrentGuest", ignore = true)
    @Mapping(target = "selectedOptionId", ignore = true)
    PollResponse toPollResponse(Poll poll, List<PollOptionResponse> options);
}