package com.ecom.atendimento.adapter.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * DTO padronizado para respostas de erro da API.
 *
 * Segue padrão RFC 7807 (Problem Details for HTTP APIs).
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * Timestamp da ocorrência do erro (ISO-8601).
     */
    private String timestamp;

    /**
     * Código HTTP do erro (400, 404, 500, etc.).
     */
    private Integer status;

    /**
     * Nome do status HTTP (Bad Request, Not Found, etc.).
     */
    private String error;

    /**
     * Mensagem principal do erro (legível para humanos).
     */
    private String message;

    /**
     * Path da requisição que gerou o erro.
     */
    private String path;

    /**
     * Tipo específico da exceção (para debugging).
     */
    private String exceptionType;

    /**
     * Lista de erros de validação (quando aplicável).
     * Exemplo: [{field: "protocolo", message: "não pode ser vazio"}]
     */
    private List<FieldError> fieldErrors;

    /**
     * Informações adicionais contextuais (opcional).
     */
    private Map<String, Object> details;

    /**
     * Representa um erro de validação de campo específico.
     */
    @Data
    @Builder
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    /**
     * Factory method para criar ErrorResponse com informações básicas.
     */
    public static ErrorResponse of(Integer status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
}
