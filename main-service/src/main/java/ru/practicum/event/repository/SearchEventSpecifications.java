package ru.practicum.event.repository;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.event.model.Event;
import ru.practicum.event.model.EventState;
import ru.practicum.request.model.Request;
import ru.practicum.request.model.RequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public class SearchEventSpecifications {

    public static Specification<Event> addWhereNull() {
        return (root, query, criteriaBuilder) -> null;
    }

    public static Specification<Event> addWhereUsers(List<Long> userIds) {
        return (root, query, criteriaBuilder) -> {
            if (userIds == null || userIds.isEmpty()) return criteriaBuilder.conjunction();
            return root.get("initiator").get("id").in(userIds);
        };
    }

    public static Specification<Event> addWhereStates(List<EventState> states) {
        return (root, query, criteriaBuilder) -> {
            if (states == null || states.isEmpty()) return criteriaBuilder.conjunction();
            return root.get("state").in(states);
        };
    }

    public static Specification<Event> addWhereCategories(List<Long> categoryIds) {
        return (root, query, criteriaBuilder) -> {
            if (categoryIds == null || categoryIds.isEmpty()) return criteriaBuilder.conjunction();
            return root.get("category").get("id").in(categoryIds);
        };
    }

    public static Specification<Event> addWhereStartsBefore(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) return criteriaBuilder.conjunction();
            return criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), dateTime);
        };
    }

    public static Specification<Event> addWhereEndsAfter(LocalDateTime dateTime) {
        return (root, query, criteriaBuilder) -> {
            if (dateTime == null) return criteriaBuilder.conjunction();
            return criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), dateTime);
        };
    }

    public static Specification<Event> addLikeText(String text) {
        return (root, query, criteriaBuilder) -> {
            if (text == null || text.trim().isEmpty()) return criteriaBuilder.conjunction();
            String searchText = "%" + text.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("annotation")), searchText),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchText)
            );
        };
    }

    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, criteriaBuilder) -> {
            if (paid == null) return criteriaBuilder.conjunction();
            return criteriaBuilder.equal(root.get("paid"), paid);
        };
    }

    public static Specification<Event> addWhereAvailableSlots() {
        return (root, query, criteriaBuilder) -> {
            assert query != null;
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Request> requestRoot = subquery.from(Request.class);

            subquery.select(criteriaBuilder.count(requestRoot));
            subquery.where(
                    criteriaBuilder.and(
                            criteriaBuilder.equal(requestRoot.get("event"), root),
                            criteriaBuilder.equal(requestRoot.get("status"), RequestStatus.CONFIRMED)
                    )
            );

            return criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("participantLimit"), 0),
                    criteriaBuilder.lessThan(subquery, root.get("participantLimit"))
            );
        };
    }
}
