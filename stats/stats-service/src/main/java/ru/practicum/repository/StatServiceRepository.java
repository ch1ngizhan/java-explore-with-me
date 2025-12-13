package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.Stat;

import java.time.LocalDateTime;
import java.util.List;

public interface StatServiceRepository extends JpaRepository<Stat, Long> {

    // Все события между датами
    List<Stat> findAllByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Все события между датами, отфильтрованные по списку URI
    List<Stat> findAllByTimestampBetweenAndUriIn(LocalDateTime start, LocalDateTime end, List<String> uris);

    // Все события между датами, отсортированные по uri (для удобства подсчёта)
    List<Stat> findAllByTimestampBetweenOrderByUriAsc(LocalDateTime start, LocalDateTime end);

    // Все события между датами с фильтром URI, отсортированные по uri
    List<Stat> findAllByTimestampBetweenAndUriInOrderByUriAsc(LocalDateTime start, LocalDateTime end, List<String> uris);
}
