package com.sit.dto;

import com.sit.model.Priority;
import com.sit.model.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequest {

    @NotBlank
    @Size(min = 10, message = "El título debe tener al menos 10 caracteres")
    private String title;

    @NotBlank
    @Size(min = 20, message = "La descripción debe tener al menos 20 caracteres")
    private String description;

    @NotNull
    private TicketType type;

    @NotNull
    private Priority priority;
}
