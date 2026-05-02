package com.sit.dto;

import com.sit.model.Priority;
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
public class MyTicketSummaryResponse {

    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private TicketStatus status;
    private Priority priority;
    private UserResponse assignedTo;
}
