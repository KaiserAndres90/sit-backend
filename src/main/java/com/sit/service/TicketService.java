package com.sit.service;

import com.sit.dto.ActiveTicketValidationResponse;
import com.sit.dto.AssignTicketRequest;
import com.sit.dto.CommentResponse;
import com.sit.dto.MyTicketSummaryResponse;
import com.sit.dto.TicketRequest;
import com.sit.dto.TicketResponse;
import com.sit.dto.TicketStatusUpdateRequest;
import com.sit.dto.UserResponse;
import com.sit.exception.BadRequestException;
import com.sit.exception.ResourceNotFoundException;
import com.sit.model.Comment;
import com.sit.model.Role;
import com.sit.model.StatusHistory;
import com.sit.model.Ticket;
import com.sit.model.TicketStatus;
import com.sit.model.TicketType;
import com.sit.model.User;
import com.sit.repository.CommentRepository;
import com.sit.repository.StatusHistoryRepository;
import com.sit.repository.TicketRepository;
import com.sit.repository.TicketSpecification;
import com.sit.repository.UserRepository;
import com.sit.security.UserPrincipal;
import com.sit.util.SecurityUtils;
import com.sit.util.TicketStatusTransition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final List<TicketStatus> ACTIVE_STATUSES = List.of(
            TicketStatus.ABIERTO,
            TicketStatus.ASIGNADO,
            TicketStatus.EN_PROGRESO,
            TicketStatus.EN_ESPERA);

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public TicketResponse create(TicketRequest request) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        User creator = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        LocalDateTime now = LocalDateTime.now();
        String ticketNumber = generateNextTicketNumber();

        Ticket ticket = Ticket.builder()
                .ticketNumber(ticketNumber)
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .priority(request.getPriority())
                .status(TicketStatus.ABIERTO)
                .createdBy(creator)
                .assignedTo(null)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ticket = ticketRepository.save(ticket);
        log.info("Ticket creado {} por usuario {}", ticketNumber, creator.getEmail());
        return toResponse(ticket, null, principal.getRole());
    }

    public String generateNextTicketNumber() {
        String day = LocalDateTime.now().format(DAY_FORMAT);
        String prefix = "SIT-" + day + "-";
        return ticketRepository.findFirstByTicketNumberStartingWithOrderByTicketNumberDesc(prefix)
                .map(t -> {
                    String suffix = t.getTicketNumber().substring(prefix.length());
                    int next = Integer.parseInt(suffix) + 1;
                    return prefix + String.format("%04d", next);
                })
                .orElse(prefix + "0001");
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> findAllForAgent(Pageable pageable) {
        Role role = SecurityUtils.currentPrincipal().getRole();
        return ticketRepository.findAll(pageable)
                .map(t -> toResponse(fetchTicketWithUsers(t.getId()), null, role));
    }

    @Transactional(readOnly = true)
    public Page<MyTicketSummaryResponse> findMyTickets(Pageable pageable) {
        UserPrincipal p = SecurityUtils.currentPrincipal();
        User me = userRepository.getReferenceById(p.getId());
        return ticketRepository.findByCreatedBy(me, pageable)
                .map(this::toMySummary);
    }

    private MyTicketSummaryResponse toMySummary(Ticket t) {
        return MyTicketSummaryResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .createdAt(t.getCreatedAt())
                .status(t.getStatus())
                .priority(t.getPriority())
                .assignedTo(t.getAssignedTo() != null ? UserService.toUserResponse(t.getAssignedTo()) : null)
                .build();
    }

    @Transactional(readOnly = true)
    public ActiveTicketValidationResponse validateActiveTicket(TicketType type) {
        Long userId = SecurityUtils.currentPrincipal().getId();
        List<Ticket> found = ticketRepository.findActiveByUserAndType(userId, type, ACTIVE_STATUSES);
        if (found.isEmpty()) {
            return ActiveTicketValidationResponse.builder()
                    .hasActiveTicket(false)
                    .message("No tiene tickets activos en esta área")
                    .build();
        }
        Ticket t = found.get(0);
        return ActiveTicketValidationResponse.builder()
                .hasActiveTicket(true)
                .message("Tiene un ticket activo en esta área")
                .ticketId(t.getId())
                .title(t.getTitle())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .build();
    }

    @Transactional
    public TicketResponse updateStatus(Long ticketId, TicketStatusUpdateRequest request) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Ticket ticket = ticketRepository.findByIdWithUsers(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        assertCanChangeStatus(ticket, principal);

        TicketStatus current = ticket.getStatus();
        TicketStatus next = request.getNewStatus();

        if (next == TicketStatus.ASIGNADO || next == TicketStatus.ABIERTO) {
            throw new BadRequestException("Use el endpoint de asignación para pasar a ASIGNADO o mantener flujo desde ABIERTO");
        }

        TicketStatusTransition.validate(current, next);

        if (next == TicketStatus.EN_ESPERA
                && (request.getReason() == null || request.getReason().isBlank())) {
            throw new BadRequestException("El motivo (reason) es obligatorio al pasar a EN_ESPERA");
        }

        LocalDateTime now = LocalDateTime.now();
        ticket.setStatus(next);
        ticket.setUpdatedAt(now);

        if (next == TicketStatus.RESUELTO) {
            ticket.setResolvedAt(now);
            ticket.setTimeToResolve(minutesBetween(ticket.getCreatedAt(), now));
        }
        if (next == TicketStatus.CERRADO) {
            ticket.setClosedAt(now);
            ticket.setTimeToResolve(minutesBetween(ticket.getCreatedAt(), now));
        }

        ticketRepository.save(ticket);

        statusHistoryRepository.save(StatusHistory.builder()
                .ticket(ticket)
                .previousStatus(current)
                .newStatus(next)
                .changedBy(userRepository.getReferenceById(principal.getId()))
                .reason(request.getReason())
                .createdAt(now)
                .build());

        log.info("Ticket {} estado {} → {}", ticket.getTicketNumber(), current, next);
        return toResponse(fetchTicketWithUsers(ticketId), loadComments(ticket), principal.getRole());
    }

    private void assertCanChangeStatus(Ticket ticket, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMINISTRADOR) {
            return;
        }
        if (principal.getRole() != Role.AGENTE) {
            throw new AccessDeniedException("Solo agentes o administradores pueden cambiar el estado");
        }
        if (ticket.getAssignedTo() == null || !ticket.getAssignedTo().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Solo el agente asignado puede cambiar el estado de este ticket");
        }
    }

    @Transactional
    public TicketResponse assign(Long ticketId, AssignTicketRequest request) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Ticket ticket = ticketRepository.findByIdWithUsers(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        User agent = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Agente no encontrado"));

        if (agent.getRole() != Role.AGENTE) {
            throw new BadRequestException("Solo se pueden asignar tickets a usuarios con rol AGENTE");
        }
        if (!Boolean.TRUE.equals(agent.getActive())) {
            throw new BadRequestException("El agente seleccionado está inactivo");
        }

        TicketStatus previous = ticket.getStatus();
        LocalDateTime now = LocalDateTime.now();
        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.ASIGNADO);
        ticket.setUpdatedAt(now);
        ticketRepository.save(ticket);

        statusHistoryRepository.save(StatusHistory.builder()
                .ticket(ticket)
                .previousStatus(previous)
                .newStatus(TicketStatus.ASIGNADO)
                .changedBy(userRepository.getReferenceById(principal.getId()))
                .reason("Asignación de ticket")
                .createdAt(now)
                .build());

        String assignMsg = "[Sistema] Ticket asignado a " + agent.getName() + " (" + agent.getEmail() + ").";
        commentRepository.save(Comment.builder()
                .content(assignMsg)
                .isInternal(true)
                .author(userRepository.getReferenceById(principal.getId()))
                .ticket(ticket)
                .createdAt(now)
                .build());

        log.info("Ticket {} asignado a agente id={}", ticket.getTicketNumber(), agent.getId());
        Ticket fresh = fetchTicketWithUsers(ticketId);
        return toResponse(fresh, loadComments(fresh), principal.getRole());
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> search(
            TicketStatus status,
            com.sit.model.Priority priority,
            TicketType type,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Long createdBy,
            Long assignedTo,
            Pageable pageable) {

        Role role = SecurityUtils.currentPrincipal().getRole();
        Specification<Ticket> spec = TicketSpecification.withFilters(
                status, priority, type, fechaDesde, fechaHasta, createdBy, assignedTo);
        return ticketRepository.findAll(spec, pageable)
                .map(t -> toResponse(fetchTicketWithUsers(t.getId()), null, role));
    }

    @Transactional(readOnly = true)
    public TicketResponse findById(Long id) {
        UserPrincipal principal = SecurityUtils.currentPrincipal();
        Ticket ticket = ticketRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));

        assertCanViewTicket(ticket, principal);
        return toResponse(ticket, loadComments(ticket), principal.getRole());
    }

    public void assertCanViewTicket(Ticket ticket, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMINISTRADOR || principal.getRole() == Role.AGENTE) {
            return;
        }
        boolean creator = ticket.getCreatedBy().getId().equals(principal.getId());
        boolean assignee = ticket.getAssignedTo() != null
                && ticket.getAssignedTo().getId().equals(principal.getId());
        if (!creator && !assignee) {
            throw new AccessDeniedException("No tiene permiso para ver este ticket");
        }
    }

    public void assertCanComment(Ticket ticket, UserPrincipal principal) {
        if (principal.getRole() == Role.ADMINISTRADOR || principal.getRole() == Role.AGENTE) {
            return;
        }
        boolean creator = ticket.getCreatedBy().getId().equals(principal.getId());
        boolean assignee = ticket.getAssignedTo() != null
                && ticket.getAssignedTo().getId().equals(principal.getId());
        if (!creator && !assignee) {
            throw new AccessDeniedException("No puede comentar en este ticket");
        }
    }

    private Ticket fetchTicketWithUsers(Long id) {
        return ticketRepository.findByIdWithUsers(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado"));
    }

    private List<Comment> loadComments(Ticket ticket) {
        return commentRepository.findByTicketOrderByCreatedAtAsc(ticket);
    }

    private TicketResponse toResponse(Ticket ticket, List<Comment> comments, Role viewerRole) {
        List<CommentResponse> commentDtos = null;
        if (comments != null) {
            commentDtos = comments.stream()
                    .filter(c -> !Boolean.TRUE.equals(c.getIsInternal())
                            || viewerRole == Role.AGENTE
                            || viewerRole == Role.ADMINISTRADOR)
                    .map(c -> CommentResponse.builder()
                            .id(c.getId())
                            .content(c.getContent())
                            .isInternal(c.getIsInternal())
                            .author(UserService.toUserResponse(c.getAuthor()))
                            .createdAt(c.getCreatedAt())
                            .attachments(c.getAttachments())
                            .build())
                    .collect(Collectors.toList());
        }

        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .type(ticket.getType())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .createdBy(UserService.toUserResponse(ticket.getCreatedBy()))
                .assignedTo(ticket.getAssignedTo() != null
                        ? UserService.toUserResponse(ticket.getAssignedTo())
                        : null)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .timeToResolve(ticket.getTimeToResolve())
                .comments(commentDtos)
                .build();
    }

    private static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }
}
