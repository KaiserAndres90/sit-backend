package com.sit.service;

import com.sit.dto.CommentRequest;
import com.sit.dto.CommentResponse;
import com.sit.exception.BadRequestException;
import com.sit.exception.ResourceNotFoundException;
import com.sit.model.Comment;
import com.sit.model.Role;
import com.sit.model.Ticket;
import com.sit.model.User;
import com.sit.repository.CommentRepository;
import com.sit.repository.TicketRepository;
import com.sit.repository.UserRepository;
import com.sit.security.UserPrincipal;
import com.sit.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final TicketService ticketService;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse addComment(Long ticketId, CommentRequest request) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Ticket ticket = ticketRepository.findByIdWithUsers(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        ticketService.assertCanComment(ticket, principal);

        if (Boolean.TRUE.equals(request.getIsInternal())
                && principal.getRole() != Role.AGENTE
                && principal.getRole() != Role.ADMINISTRADOR) {
            throw new BadRequestException("Solo agentes o administradores pueden crear comentarios internos");
        }

        User author = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        LocalDateTime now = LocalDateTime.now();
        Comment comment = Comment.builder()
                .content(request.getContent())
                .isInternal(request.getIsInternal())
                .author(author)
                .ticket(ticket)
                .createdAt(now)
                .attachments(request.getAttachments())
                .build();
        comment = commentRepository.save(comment);

        if (!Boolean.TRUE.equals(comment.getIsInternal())) {
            simulatePublicCommentNotification(ticket, author);
        }

        log.info("Comentario {} en ticket {}", comment.getId(), ticket.getTicketNumber());
        return toResponse(comment);
    }

    private void simulatePublicCommentNotification(Ticket ticket, User author) {
        User creator = ticket.getCreatedBy();
        User assignee = ticket.getAssignedTo();

        if (author.getId().equals(creator.getId())) {
            if (assignee != null) {
                System.out.println("[NOTIFICACIÓN EMAIL] Nuevo comentario público en ticket "
                        + ticket.getTicketNumber() + " para el agente " + assignee.getEmail());
            } else {
                System.out.println("[NOTIFICACIÓN EMAIL] Comentario público en ticket "
                        + ticket.getTicketNumber() + " — sin agente asignado aún");
            }
        } else {
            System.out.println("[NOTIFICACIÓN EMAIL] Nuevo comentario público en ticket "
                    + ticket.getTicketNumber() + " para el usuario " + creator.getEmail());
        }
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listForTicket(Long ticketId) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Ticket ticket = ticketRepository.findByIdWithUsers(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));
        ticketService.assertCanViewTicket(ticket, principal);

        return commentRepository.findByTicketOrderByCreatedAtAsc(ticket).stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsInternal())
                        || principal.getRole() == Role.AGENTE
                        || principal.getRole() == Role.ADMINISTRADOR)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CommentResponse toResponse(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .content(c.getContent())
                .isInternal(c.getIsInternal())
                .author(UserService.toUserResponse(c.getAuthor()))
                .createdAt(c.getCreatedAt())
                .attachments(c.getAttachments())
                .build();
    }
}
