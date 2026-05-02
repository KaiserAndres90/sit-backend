package com.sit.repository;

import com.sit.model.StatusHistory;
import com.sit.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {

    List<StatusHistory> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}
