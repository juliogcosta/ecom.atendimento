package com.ecom.atendimento.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO genérico para comandos que requerem apenas o aggregateId.
 * Usado para: Confirmar, Finalizar, Cancelar
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleCommandRequest {

    @NotNull(message = "AggregateId é obrigatório")
    private UUID aggregateId;
}
