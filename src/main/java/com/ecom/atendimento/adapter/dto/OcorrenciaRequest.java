package com.ecom.atendimento.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * DTO de requisição para registrar ocorrências durante o atendimento.
 * NOVO: Este comando não existe na implementação de referência.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OcorrenciaRequest {

    @NotNull(message = "AggregateId é obrigatório")
    private UUID aggregateId;

    @NotEmpty(message = "Lista de ocorrências não pode ser vazia")
    private List<String> ocorrencias;
}
