package com.eventflow.eventflow_backend.service;

import com.eventflow.eventflow_backend.dto.response.CalendarEventItemResponse;
import com.eventflow.eventflow_backend.dto.response.CalendarResponse;
import com.eventflow.eventflow_backend.entity.Event;
import com.eventflow.eventflow_backend.entity.EventGuest;
import com.eventflow.eventflow_backend.mapper.DashboardMapper;
import com.eventflow.eventflow_backend.repository.EventGuestRepository;
import com.eventflow.eventflow_backend.repository.EventRepository;
import com.eventflow.eventflow_backend.security.CurrentUserService;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final EventRepository eventRepository;
    private final EventGuestRepository eventGuestRepository;
    private final CurrentUserService currentUserService;
    private final DashboardMapper dashboardMapper;

    @Transactional(readOnly = true)
    public CalendarResponse getCalendar(Integer year, Integer month) {
        validateYearMonth(year, month);

        Long currentUserId = currentUserService.getCurrentUserIdOrThrow();

        YearMonth yearMonth = YearMonth.of(year, month);
        OffsetDateTime from = yearMonth.atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime to = yearMonth.plusMonths(1).atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<Event> creatorEvents = eventRepository.findAllByCreatorIdAndStartsAtBetweenOrderByStartsAtAsc(
                currentUserId,
                from,
                to
        );

        List<EventGuest> guestEvents = eventGuestRepository
                .findAllByRegisteredUserIdAndEvent_StartsAtBetweenOrderByEvent_StartsAtAsc(
                        currentUserId,
                        from,
                        to
                );

        List<CalendarEventItemResponse> creatorEventItems = creatorEvents.stream()
                .map(dashboardMapper::toCreatorCalendarItem)
                .toList();

        List<CalendarEventItemResponse> guestEventItems = guestEvents.stream()
                .map(dashboardMapper::toGuestCalendarItem)
                .toList();

        CalendarResponse response = new CalendarResponse();
        response.setYear(year);
        response.setMonth(month);
        response.setCreatorEvents(creatorEventItems);
        response.setGuestEvents(guestEventItems);

        return response;
    }

    private void validateYearMonth(Integer year, Integer month) {
        if (year == null || month == null) {
            throw new IllegalArgumentException("Параметры year и month обязательны");
        }

        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Параметр month должен быть в диапазоне от 1 до 12");
        }

        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Параметр year должен быть в диапазоне от 2000 до 2100");
        }
    }
}
