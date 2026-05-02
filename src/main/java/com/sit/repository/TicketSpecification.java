package com.sit.repository;

import com.sit.model.Priority;
import com.sit.model.Ticket;
import com.sit.model.TicketStatus;
import com.sit.model.TicketType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TicketSpecification {

    private TicketSpecification() {
    }

    public static Specification<Ticket> withFilters(
            TicketStatus status,
            Priority priority,
            TicketType type,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Long createdById,
            Long assignedToId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), fechaHasta));
            }
            if (createdById != null) {
                predicates.add(cb.equal(root.get("createdBy").get("id"), createdById));
            }
            if (assignedToId != null) {
                predicates.add(cb.equal(root.get("assignedTo").get("id"), assignedToId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
