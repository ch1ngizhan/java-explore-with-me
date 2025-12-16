package ru.practicum.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.event.dto.EventWithCountConfirmedRequests;
import ru.practicum.request.dto.ParticipationRequestDto;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    @Query("""
            SELECT NEW ru.practicum.event.dto.EventWithCountConfirmedRequests(r.event.id, COUNT(r))
            FROM Request r
            WHERE r.event.id IN :eventIds AND r.status = ru.practicum.request.model.RequestStatus.CONFIRMED
            GROUP BY r.event.id"""
    )
    List<EventWithCountConfirmedRequests> findCountConfirmedRequestsByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("""
            SELECT r FROM Request r
            LEFT JOIN FETCH r.event
            LEFT JOIN FETCH r.requester
            WHERE r.event.id = :eventId"""
    )
    List<Request> findAllParticipationRequestByEventId(@Param("eventId") Long eventId);

    @Query("""
            SELECT r FROM Request r
            LEFT JOIN FETCH r.event
            LEFT JOIN FETCH r.requester
            WHERE r.id IN :requestIds"""
    )
    List<Request> findAllRequestById(@Param("requestIds") List<Long> requestIds);

    @Query("""
            SELECT NEW ru.practicum.request.dto.ParticipationRequestDto(
                r.id,
                r.created,
                r.event.id,
                r.requester.id,
                r.status)
            FROM Request r
            WHERE r.requester.id = :userId"""
    )
    List<ParticipationRequestDto> findAllRequestsByUserId(@Param("userId") Long userId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long userId);

    int countByEventIdAndStatus(Long id, RequestStatus requestStatus);

    List<Request> findAllByEventIdAndStatus(Long eventId, RequestStatus requestStatus);
}
