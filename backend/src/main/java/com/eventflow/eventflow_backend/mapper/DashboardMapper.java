package com.eventflow.eventflow_backend.mapper;

import com.eventflow.eventflow_backend.dto.response.CalendarEventItemResponse;
import com.eventflow.eventflow_backend.dto.response.DashboardSummaryResponse;
import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.EventGuest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface DashboardMapper {

    DashboardSummaryResponse toDashboardSummaryResponse(Long createdEventsCount, Long acceptedInvitationsCount);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "title", source = "event.title")
    @Mapping(target = "startsAt", source = "event.startsAt")
    @Mapping(target = "address", source = "event.address")
    @Mapping(target = "colorType", constant = "CREATOR")
    @Mapping(target = "guestToken", ignore = true)
    CalendarEventItemResponse toCreatorCalendarItem(Event event);

    @Mapping(target = "eventId", source = "eventGuest.event.id")
    @Mapping(target = "title", source = "eventGuest.event.title")
    @Mapping(target = "startsAt", source = "eventGuest.event.startsAt")
    @Mapping(target = "address", source = "eventGuest.event.address")
    @Mapping(target = "colorType", constant = "GUEST")
    @Mapping(target = "guestToken", source = "eventGuest.guestToken")
    CalendarEventItemResponse toGuestCalendarItem(EventGuest eventGuest);
}