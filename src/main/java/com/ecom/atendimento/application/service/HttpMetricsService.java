package com.ecom.atendimento.application.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.ecom.atendimento.domain.model.HttpRequestMetric;
import com.ecom.atendimento.domain.port.HttpMetricsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Serviço de métricas HTTP para rastreamento de requisições CQRS.
 *
 * Processa métricas de forma ASSÍNCRONA para não adicionar latência
 * às respostas HTTP. O método recordMetricAsync() executa em thread
 * separada via @Async("metricsAsyncExecutor").
 *
 * Padrão baseado em persistence-crs para consistência arquitetural.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HttpMetricsService {

	private final HttpMetricsRepository metricsRepository;

	/**
	 * Registra métrica de requisição HTTP de forma ASSÍNCRONA.
	 *
	 * Este método é executado em thread separada (@Async) para não
	 * bloquear a resposta HTTP ao cliente.
	 *
	 * @param requestId ID de correlação da requisição (UUID)
	 * @param startTime Timestamp de início do processamento
	 * @param endTime Timestamp de fim do processamento
	 * @param method Método HTTP (POST, PUT, GET)
	 * @param path Path da requisição (/api/atendimento/solicitar, etc)
	 * @param tenantPid Tenant ID extraído do header X-Tenant-Id
	 * @param username Username extraído do SecurityContext (User.getUsername())
	 * @param requestSizeBytes Tamanho do request body em bytes
	 * @param responseSizeBytes Tamanho do response body em bytes
	 * @param httpStatus HTTP status code retornado (200, 201, 400, 500, etc)
	 * @param exception Exceção lançada (null se não houve)
	 * @param boundedContext Bounded context (fixo: "atendimento")
	 * @param aggregate Aggregate (fixo: "Atendimento")
	 * @param command Command extraído do path (solicitar, ajustar, confirmar, ocorrencia, finalizar, cancelar)
	 * @param aggregateId Aggregate ID (UUID do atendimento)
	 */
	@Async("metricsAsyncExecutor")
	public void recordMetricAsync(
			String requestId,
			Instant startTime,
			Instant endTime,
			String method,
			String path,
			String tenantPid,
			String username,
			Long requestSizeBytes,
			Long responseSizeBytes,
			Integer httpStatus,
			Exception exception,
			String boundedContext,
			String aggregate,
			String command,
			String aggregateId) {

		try {
			// Calcular duração
			Long durationMs = Duration.between(startTime, endTime).toMillis();

			// Determinar sucesso (2xx status codes)
			Boolean success = httpStatus != null && httpStatus >= 200 && httpStatus < 300;

			// Extrair informações da exceção
			String exceptionType = exception != null ? exception.getClass().getSimpleName() : null;
			String exceptionMessage = exception != null ? exception.getMessage() : null;

			// Construir métrica
			HttpRequestMetric metric = HttpRequestMetric.builder()
					.id(UUID.randomUUID().toString())
					.requestId(requestId)
					.timestamp(startTime)
					.startTime(startTime)
					.endTime(endTime)
					.durationMs(durationMs)
					.method(method)
					.path(path)
					.tenantPid(tenantPid)
					.username(username)
					.requestSizeBytes(requestSizeBytes)
					.responseSizeBytes(responseSizeBytes)
					.httpStatus(httpStatus)
					.success(success)
					.exceptionType(exceptionType)
					.exceptionMessage(exceptionMessage)
					.boundedContext(boundedContext)
					.aggregate(aggregate)
					.command(command)
					.aggregateId(aggregateId)
					.build();

			// Persistir (de forma assíncrona)
			metricsRepository.save(metric);

			log.debug("📊 [METRICS_SERVICE] Métrica registrada: requestId={}, command={}, duration={}ms, status={}, success={}",
					requestId, command, durationMs, httpStatus, success);

		} catch (Exception e) {
			log.error("📊 [METRICS_SERVICE] Falha ao registrar métrica: requestId={}", requestId, e);
		}
	}

	/**
	 * Retorna todas as métricas (ordenadas por timestamp DESC).
	 *
	 * @return Lista de todas as métricas
	 */
	public List<HttpRequestMetric> getAllMetrics() {
		return metricsRepository.findAll();
	}

	/**
	 * Retorna resumo agregado das métricas.
	 *
	 * @return Map com estatísticas (totalRequests, successCount, errorCount, avgDurationMs, etc)
	 */
	public Map<String, Object> getMetricsSummary() {
		List<HttpRequestMetric> allMetrics = metricsRepository.findAll();

		if (allMetrics.isEmpty()) {
			return Map.of(
					"totalRequests", 0,
					"successCount", 0,
					"errorCount", 0,
					"avgDurationMs", 0.0,
					"minDurationMs", 0,
					"maxDurationMs", 0,
					"totalRequestBytes", 0L,
					"totalResponseBytes", 0L,
					"successRate", 0.0
			);
		}

		long totalRequests = allMetrics.size();
		long successCount = metricsRepository.countBySuccess(true);
		long errorCount = metricsRepository.countBySuccess(false);

		double avgDurationMs = allMetrics.stream()
				.filter(m -> m.getDurationMs() != null)
				.mapToLong(HttpRequestMetric::getDurationMs)
				.average()
				.orElse(0.0);

		long minDurationMs = allMetrics.stream()
				.filter(m -> m.getDurationMs() != null)
				.min(Comparator.comparing(HttpRequestMetric::getDurationMs))
				.map(HttpRequestMetric::getDurationMs)
				.orElse(0L);

		long maxDurationMs = allMetrics.stream()
				.filter(m -> m.getDurationMs() != null)
				.max(Comparator.comparing(HttpRequestMetric::getDurationMs))
				.map(HttpRequestMetric::getDurationMs)
				.orElse(0L);

		long totalRequestBytes = allMetrics.stream()
				.filter(m -> m.getRequestSizeBytes() != null)
				.mapToLong(HttpRequestMetric::getRequestSizeBytes)
				.sum();

		long totalResponseBytes = allMetrics.stream()
				.filter(m -> m.getResponseSizeBytes() != null)
				.mapToLong(HttpRequestMetric::getResponseSizeBytes)
				.sum();

		double successRate = totalRequests > 0 ? (successCount * 100.0 / totalRequests) : 0.0;

		Map<String, Object> summary = new HashMap<>();
		summary.put("totalRequests", totalRequests);
		summary.put("successCount", successCount);
		summary.put("errorCount", errorCount);
		summary.put("avgDurationMs", Math.round(avgDurationMs * 100.0) / 100.0);
		summary.put("minDurationMs", minDurationMs);
		summary.put("maxDurationMs", maxDurationMs);
		summary.put("totalRequestBytes", totalRequestBytes);
		summary.put("totalResponseBytes", totalResponseBytes);
		summary.put("successRate", Math.round(successRate * 100.0) / 100.0);

		return summary;
	}

	/**
	 * Retorna métricas de um tenant específico.
	 *
	 * @param tenantPid Tenant ID
	 * @return Lista de métricas do tenant
	 */
	public List<HttpRequestMetric> getMetricsByTenant(String tenantPid) {
		return metricsRepository.findByTenantPid(tenantPid);
	}

	/**
	 * Retorna métricas dentro de um intervalo de tempo.
	 *
	 * @param start Timestamp de início (ISO-8601 format)
	 * @param end Timestamp de fim (ISO-8601 format)
	 * @return Lista de métricas no intervalo
	 */
	public List<HttpRequestMetric> getMetricsByTimeRange(Instant start, Instant end) {
		return metricsRepository.findByTimeRange(start, end);
	}

	/**
	 * Limpa todas as métricas da memória.
	 */
	public void clearMetrics() {
		metricsRepository.clear();
		log.info("📊 [METRICS_SERVICE] Todas as métricas foram limpas");
	}

	/**
	 * Exporta métricas para arquivo CSV.
	 *
	 * @param filePath Caminho completo do arquivo CSV
	 */
	public void exportToCsv(String filePath) {
		metricsRepository.exportToCsv(filePath);
		log.info("📊 [METRICS_SERVICE] Métricas exportadas para CSV: {}", filePath);
	}
}
