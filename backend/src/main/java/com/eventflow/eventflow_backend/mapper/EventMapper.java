package com.eventflow.eventflow_backend.mapper;

import com.eventflow.eventflow_backend.dto.request.EventRequest;
import com.eventflow.eventflow_backend.dto.response.EventResponse;
import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.User;
import java.time.OffsetDateTime;

import com.eventflow.eventflow_backend.entity.enums.EventStatus;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(config = MapStructConfig.class, imports = {EventStatus.class})
public interface EventMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creator", source = "creator")
    @Mapping(target = "title", expression = "java(request.getTitle().trim())")
    @Mapping(target = "description", expression = "java(normalizeDescription(request.getDescription()))")
    @Mapping(target = "startsAt", source = "request.startsAt")
    @Mapping(target = "address", expression = "java(request.getAddress().trim())")
    @Mapping(target = "lat", source = "request.lat")
    @Mapping(target = "lon", source = "request.lon")
    @Mapping(target = "posterPath", source = "posterPath")
    @Mapping(target = "status", expression = "java(EventStatus.ACTIVE)")
    @Mapping(target = "createdAt", source = "now")
    @Mapping(target = "updatedAt", source = "now")
    Event toEvent(EventRequest request, User creator, String posterPath, OffsetDateTime now);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "posterPath", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "title", expression = "java(request.getTitle().trim())")
    @Mapping(target = "description", expression = "java(normalizeDescription(request.getDescription()))")
    @Mapping(target = "address", expression = "java(request.getAddress().trim())")
    void updateEventFromRequest(EventRequest request, @MappingTarget Event event);

    @Mapping(target = "creatorId", source = "event.creator.id")
    @Mapping(target = "posterUrl", source = "posterUrl")
    @Mapping(target = "status", expression = "java(event.getStatus().name())")
    EventResponse toResponse(Event event, String posterUrl);

    default String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String trimmed = description.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
