package com.sit.controller;

import com.sit.dto.ActiveTicketValidationResponse;
import com.sit.dto.AssignTicketRequest;
import com.sit.dto.MyTicketSummaryResponse;
import com.sit.dto.TicketRequest;
import com.sit.dto.TicketResponse;
import com.sit.dto.TicketStatusUpdateRequest;
import com.sit.model.Priority;
import com.sit.model.TicketStatus;
import com.sit.model.TicketType;
import com.sit.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private static final Set<String> SORT_WHITELIST = Set.of(
            "createdAt", "title", "status", "priority", "ticketNumber", "updatedAt");

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(request));
    }

    @GetMapping("/agent")
    @PreAuthorize("hasAnyRole('AGENTE','ADMINISTRADOR')")
    public ResponseEntity<Page<TicketResponse>> listForAgent(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sortBy, sortDir));
        return ResponseEntity.ok(ticketService.findAllForAgent(pageable));
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<Page<MyTicketSummaryResponse>> myTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sortBy, sortDir));
        return ResponseEntity.ok(ticketService.findMyTickets(pageable));
    }

    @GetMapping("/validate/active")
    public ResponseEntity<ActiveTicketValidationResponse> validateActive(
            @RequestParam TicketType type) {
        return ResponseEntity.ok(ticketService.validateActiveTicket(type));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TicketResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody TicketStatusUpdateRequest request) {
        return ResponseEntity.ok(ticketService.updateStatus(id, request));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<TicketResponse> assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request) {
        return ResponseEntity.ok(ticketService.assign(id, request));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('AGENTE','ADMINISTRADOR')")
    public ResponseEntity<Page<TicketResponse>> search(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) TicketType type,
            @RequestParam(required = false) LocalDateTime fechaDesde,
            @RequestParam(required = false) LocalDateTime fechaHasta,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sortBy, sortDir));
        return ResponseEntity.ok(ticketService.search(
                status, priority, type, fechaDesde, fechaHasta, createdBy, assignedTo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.findById(id));
    }

    private Sort resolveSort(String sortBy, String sortDir) {
        String field = SORT_WHITELIST.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}
