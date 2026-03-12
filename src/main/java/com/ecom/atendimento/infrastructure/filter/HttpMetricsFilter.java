package com.ecom.atendimento.infrastructure.filter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.ecom.atendimento.application.service.HttpMetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yc.pr.models.User;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Filter para captura de métricas HTTP em endpoints CQRS de Atendimento.
 *
 * Intercepta TODAS as requisições para /api/atendimento/*,
 * captura timing, tamanho de bytes (request/response) e status de sucesso/erro.
 *
 * Processamento é ASSÍNCRONO: após capturar os dados, dispara thread
 * separada para calcular/persistir métricas, retornando response
 * imediatamente ao cliente (sem adicionar latência).
 *
 * Ordem de execução: HIGHEST_PRECEDENCE (executa antes de todos os outros filters).
 *
 * Padrão baseado em persistence-crs para consistência arquitetural.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "ecom.atendimento.metrics.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class HttpMetricsFilter extends OncePerRequestFilter {

	private final HttpMetricsService metricsService;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		// Filtrar apenas /api/atendimento/
		if (!request.getRequestURI().startsWith("/api/atendimento/")) {
			filterChain.doFilter(request, response);
			return;
		}

		// Não capturar métricas de /metrics/* (evitar loop)
		if (request.getRequestURI().startsWith("/metrics/")) {
			filterChain.doFilter(request, response);
			return;
		}

		// Gerar requestId para correlação
		String requestId = UUID.randomUUID().toString();

		// Capturar startTime
		Instant startTime = Instant.now();

		// Wrappers para capturar content (bytes)
		ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, 10240);
		ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

		// Variáveis para captura de dados
		Exception capturedException = null;
		String method = request.getMethod();
		String path = request.getRequestURI();
		String tenantPid = request.getHeader("X-Tenant-Id");
		String username = null;
		String boundedContext = "atendimento"; // Fixo
		String aggregate = "Atendimento";      // Fixo
		String command = null;
		String aggregateId = null;

		// Extrair comando do path
		// Formato esperado: /api/atendimento/{comando}
		// Exemplo: /api/atendimento/solicitar, /api/atendimento/ajustar
		try {
			String[] pathParts = path.split("/");
			if (pathParts.length >= 4) {
				command = pathParts[3]; // "solicitar", "ajustar", "confirmar", "ocorrencia", "finalizar", "cancelar"
			}
		} catch (Exception e) {
			log.debug("📊 [METRICS_FILTER] Falha ao extrair comando do path: {}", path, e);
		}

		try {
			// Executar chain (controller)
			filterChain.doFilter(requestWrapper, responseWrapper);

		} catch (Exception e) {
			capturedException = e;
			throw e;

		} finally {
			// Capturar endTime
			Instant endTime = Instant.now();

			// Capturar bytes
			Long requestSizeBytes = (long) requestWrapper.getContentAsByteArray().length;
			Long responseSizeBytes = (long) responseWrapper.getContentAsByteArray().length;

			// Capturar HTTP status
			Integer httpStatus = responseWrapper.getStatus();

			// Tentar extrair aggregateId do request body
			try {
				byte[] requestBody = requestWrapper.getContentAsByteArray();
				if (requestBody.length > 0) {
					@SuppressWarnings("unchecked")
					Map<String, Object> bodyMap = objectMapper.readValue(requestBody, Map.class);

					// Tentar buscar aggregateId (usado em ajustar, confirmar, ocorrencia, finalizar, cancelar)
					if (bodyMap.containsKey("aggregateId")) {
						aggregateId = String.valueOf(bodyMap.get("aggregateId"));
					}
				}
			} catch (Exception e) {
				log.trace("📊 [METRICS_FILTER] Falha ao extrair aggregateId do request body: {}", e.getMessage());
			}

			// Se aggregateId não foi encontrado no request, tentar buscar no response
			// (usado em solicitar, que retorna {"id": "uuid"})
			if (aggregateId == null) {
				try {
					byte[] responseBody = responseWrapper.getContentAsByteArray();
					if (responseBody.length > 0) {
						@SuppressWarnings("unchecked")
						Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

						// Tentar buscar id no response (solicitar retorna {"id": "uuid"})
						if (responseMap.containsKey("id")) {
							aggregateId = String.valueOf(responseMap.get("id"));
						}
					}
				} catch (Exception e) {
					log.trace("📊 [METRICS_FILTER] Falha ao extrair aggregateId do response body: {}", e.getMessage());
				}
			}

			// Extrair username do SecurityContext (User do yc.pr)
			try {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				if (auth != null && auth.getPrincipal() instanceof User) {
					User user = (User) auth.getPrincipal();
					username = user.getUsername();
				}
			} catch (Exception e) {
				log.trace("📊 [METRICS_FILTER] Falha ao extrair username do SecurityContext: {}", e.getMessage());
			}

			// Copiar content do responseWrapper para o response original (necessário para ContentCachingResponseWrapper)
			responseWrapper.copyBodyToResponse();

			// Disparar processamento ASSÍNCRONO de métricas
			metricsService.recordMetricAsync(
					requestId,
					startTime,
					endTime,
					method,
					path,
					tenantPid,
					username,
					requestSizeBytes,
					responseSizeBytes,
					httpStatus,
					capturedException,
					boundedContext,
					aggregate,
					command,
					aggregateId
			);

			log.debug("📊 [METRICS_FILTER] Captura de métrica disparada (async): requestId={}, command={}, duration={}ms, status={}",
					requestId, command, java.time.Duration.between(startTime, endTime).toMillis(), httpStatus);
		}
	}
}
