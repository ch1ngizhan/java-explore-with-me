package ru.practicum.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventWithCountConfirmedRequests {
    private Long eventId;
    private int countConfirmedRequests;

    public EventWithCountConfirmedRequests(Long eventId, Long countConfirmedRequestsLong) {
        this.eventId = eventId;
        this.countConfirmedRequests = (countConfirmedRequestsLong == null) ? 0 : countConfirmedRequestsLong.intValue();
    }
}
