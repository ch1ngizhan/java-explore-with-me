package ru.practicum.request.service;


import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.dto.RequestStatusUpdateRequest;
import ru.practicum.request.dto.RequestStatusUpdateResult;

import java.util.List;

public interface RequestService {
    List<ParticipationRequestDto> getRequestsByEvent(Long userId, Long eventId);

    RequestStatusUpdateResult updateRequestStatus(Long userId, Long eventId, RequestStatusUpdateRequest request);

    List<ParticipationRequestDto> getRequestsByUserId(Long userId);

    ParticipationRequestDto addRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);
}
