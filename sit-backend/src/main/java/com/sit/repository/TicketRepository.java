package com.sit.repository;

import com.sit.model.Ticket;
import com.sit.model.TicketStatus;
import com.sit.model.TicketType;
import com.sit.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @Query("""
            SELECT DISTINCT t FROM Ticket t
            LEFT JOIN FETCH t.createdBy
            LEFT JOIN FETCH t.assignedTo
            WHERE t.id = :id
            """)
    Optional<Ticket> findByIdWithUsers(@Param("id") Long id);

    Optional<Ticket> findFirstByTicketNumberStartingWithOrderByTicketNumberDesc(String prefix);

    @EntityGraph(attributePaths = {"assignedTo"})
    Page<Ticket> findByCreatedBy(User createdBy, Pageable pageable);

    List<Ticket> findByCreatedByIdOrAssignedToId(Long createdById, Long assignedToId);

    long countByCreatedByIdOrAssignedToId(Long createdById, Long assignedToId);

    @Query("""
            SELECT t FROM Ticket t
            WHERE t.createdBy.id = :userId
            AND t.type = :type
            AND t.status IN :activeStatuses
            """)
    List<Ticket> findActiveByUserAndType(
            @Param("userId") Long userId,
            @Param("type") TicketType type,
            @Param("activeStatuses") List<TicketStatus> activeStatuses);
}
