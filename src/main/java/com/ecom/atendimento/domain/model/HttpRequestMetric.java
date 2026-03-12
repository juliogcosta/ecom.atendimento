package com.ecom.atendimento.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Métrica de requisição HTTP capturada nos endpoints de comandos CQRS.
 *
 * Cada instância representa uma requisição recebida em /api/atendimento/{comando},
 * incluindo timing, tamanho de bytes e resultado (sucesso/erro).
 *
 * Padrão baseado em persistence-crs para consistência arquitetural.
 *
 * Usada para análise de performance e comportamento do ecom.atendimento durante
 * execução de comandos (solicitar, ajustar, confirmar, ocorrencia, finalizar, cancelar).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpRequestMetric {

	/**
	 * ID único da métrica (UUID gerado automaticamente).
	 */
	private String id;

	/**
	 * ID de correlação da requisição (pode ser usado para rastrear através de microservices).
	 */
	private String requestId;

	/**
	 * Timestamp da criação da métrica (geralmente igual ao startTime).
	 */
	private Instant timestamp;

	/**
	 * Timestamp do início do processamento da requisição.
	 */
	private Instant startTime;

	/**
	 * Timestamp do fim do processamento da requisição.
	 */
	private Instant endTime;

	/**
	 * Duração total do processamento em milissegundos.
	 * Calculado como: endTime - startTime.
	 */
	private Long durationMs;

	/**
	 * Método HTTP (POST para criar, PUT para atualizar, GET para consultar).
	 */
	private String method;

	/**
	 * Path da requisição (ex: "/api/atendimento/solicitar").
	 */
	private String path;

	/**
	 * Tenant ID extraído do header X-Tenant-Id.
	 */
	private String tenantPid;

	/**
	 * Username extraído do SecurityContext (User.getUsername()).
	 */
	private String username;

	/**
	 * Tamanho do request body em bytes.
	 */
	private Long requestSizeBytes;

	/**
	 * Tamanho do response body em bytes.
	 */
	private Long responseSizeBytes;

	/**
	 * HTTP Status Code retornado (200, 201, 204, 400, 404, 500, etc).
	 */
	private Integer httpStatus;

	/**
	 * Indica se a requisição foi bem-sucedida (status 2xx = true).
	 */
	private Boolean success;

	/**
	 * Tipo da exceção lançada (null se não houve exceção).
	 */
	private String exceptionType;

	/**
	 * Mensagem da exceção lançada (null se não houve exceção).
	 */
	private String exceptionMessage;

	/**
	 * Bounded Context (fixo: "atendimento").
	 */
	private String boundedContext;

	/**
	 * Aggregate (fixo: "Atendimento").
	 */
	private String aggregate;

	/**
	 * Command extraído do path (ex: "solicitar", "ajustar", "confirmar", "ocorrencia", "finalizar", "cancelar").
	 */
	private String command;

	/**
	 * Aggregate ID extraído do request body ou response (UUID do atendimento).
	 */
	private String aggregateId;
}
