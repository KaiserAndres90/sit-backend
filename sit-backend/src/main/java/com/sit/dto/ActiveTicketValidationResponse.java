package com.sit.dto;

import com.sit.model.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveTicketValidationResponse {

    private boolean hasActiveTicket;
    private String message;
    private Long ticketId;
    private String title;
    private TicketStatus status;
    private LocalDateTime createdAt;
}
