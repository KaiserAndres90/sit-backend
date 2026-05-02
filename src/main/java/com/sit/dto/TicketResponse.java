package com.sit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sit.model.Priority;
import com.sit.model.TicketStatus;
import com.sit.model.TicketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketResponse {

    private Long id;
    private String ticketNumber;
    private String title;
    private String description;
    private TicketType type;
    private Priority priority;
    private TicketStatus status;
    private UserResponse createdBy;
    private UserResponse assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private Long timeToResolve;
    private List<CommentResponse> comments;
}
