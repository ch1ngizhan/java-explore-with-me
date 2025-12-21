package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.StatResponseDto;
import ru.practicum.model.Stat;

import java.time.LocalDateTime;
import java.util.List;

public interface StatServiceRepository extends JpaRepository<Stat, Long> {

    @Query("""
            SELECT new ru.practicum.StatResponseDto(e.app, e.uri, COUNT(DISTINCT e.ip))
            FROM Stat e
            WHERE e.timestamp BETWEEN :start AND :end
            AND (:uris IS NULL OR e.uri IN :uris)
            GROUP BY e.app, e.uri
            ORDER BY COUNT(DISTINCT e.ip) DESC
            """)
    List<StatResponseDto> getUniqueStats(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("""
            SELECT new ru.practicum.StatResponseDto(e.app, e.uri, COUNT(e))
            FROM Stat e
            WHERE e.timestamp BETWEEN :start AND :end
            AND (:uris IS NULL OR e.uri IN :uris)
            GROUP BY e.app, e.uri
            ORDER BY COUNT(e) DESC
            """)
    List<StatResponseDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris);
}
