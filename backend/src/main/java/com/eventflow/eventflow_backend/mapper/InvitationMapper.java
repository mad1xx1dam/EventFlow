package com.eventflow.eventflow_backend.mapper;

import com.eventflow.eventflow_backend.dto.response.GuestInvitationDetailsResponse;
import com.eventflow.eventflow_backend.dto.response.InvitationResponse;
import com.eventflow.eventflow_backend.entity.EventGuest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapStructConfig.class)
public interface InvitationMapper {

    @Mapping(target = "eventId", source = "eventGuest.event.id")
    @Mapping(target = "registeredUserId", source = "eventGuest.registeredUser.id")
    @Mapping(target = "invitationUrl", source = "invitationUrl")
    InvitationResponse toInvitationResponse(EventGuest eventGuest, String invitationUrl);

    @Mapping(target = "eventId", source = "eventGuest.event.id")
    @Mapping(target = "title", source = "eventGuest.event.title")
    @Mapping(target = "description", source = "eventGuest.event.description")
    @Mapping(target = "startsAt", source = "eventGuest.event.startsAt")
    @Mapping(target = "address", source = "eventGuest.event.address")
    @Mapping(target = "lat", source = "eventGuest.event.lat")
    @Mapping(target = "lon", source = "eventGuest.event.lon")
    @Mapping(target = "posterUrl", source = "posterUrl")
    @Mapping(target = "guestEmail", source = "eventGuest.guestEmail")
    @Mapping(target = "rsvpStatus", source = "eventGuest.rsvpStatus")
    GuestInvitationDetailsResponse toGuestInvitationDetailsResponse(EventGuest eventGuest, String posterUrl);
}
