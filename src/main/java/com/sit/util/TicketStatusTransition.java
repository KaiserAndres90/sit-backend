package com.sit.util;

import com.sit.exception.BadRequestException;
import com.sit.model.TicketStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Flujo: ASIGNADO → EN_PROGRESO → EN_ESPERA → RESUELTO → CERRADO.
 * ABIERTO → ASIGNADO solo vía PUT /assign. RESUELTO → CERRADO.
 */
public final class TicketStatusTransition {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED = new EnumMap<>(TicketStatus.class);

    static {
        ALLOWED.put(TicketStatus.ASIGNADO, EnumSet.of(TicketStatus.EN_PROGRESO));
        ALLOWED.put(TicketStatus.EN_PROGRESO, EnumSet.of(TicketStatus.EN_ESPERA, TicketStatus.RESUELTO));
        ALLOWED.put(TicketStatus.EN_ESPERA, EnumSet.of(TicketStatus.EN_PROGRESO, TicketStatus.RESUELTO));
        ALLOWED.put(TicketStatus.RESUELTO, EnumSet.of(TicketStatus.CERRADO));
    }

    private TicketStatusTransition() {
    }

    public static void validate(TicketStatus current, TicketStatus next) {
        if (current == next) {
            throw new BadRequestException("El ticket ya se encuentra en el estado solicitado");
        }
        if (current == TicketStatus.ABIERTO || current == TicketStatus.CERRADO) {
            throw new BadRequestException("No se puede cambiar el estado desde " + current
                    + " mediante este endpoint (use asignación si está en ABIERTO)");
        }
        Set<TicketStatus> targets = ALLOWED.get(current);
        if (targets == null || !targets.contains(next)) {
            throw new BadRequestException("Transición no permitida: " + current + " → " + next);
        }
    }
}
