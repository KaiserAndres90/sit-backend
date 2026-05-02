package com.sit.repository;

import com.sit.model.Comment;
import com.sit.model.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByTicketOrderByCreatedAtAsc(Ticket ticket);
}
