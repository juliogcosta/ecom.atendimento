package com.ecom.atendimento.adapter.rest;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecom.atendimento.application.service.HttpMetricsService;
import com.ecom.atendimento.domain.model.HttpRequestMetric;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST para consulta de métricas HTTP do ecom.atendimento.
 *
 * Endpoints:
 * - GET /metrics/http              → Lista todas as métricas
 * - GET /metrics/http/summary      → Resumo agregado (count, success, error, avg)
 * - GET /metrics/http/tenant/{pid} → Filtrar por tenant
 * - GET /metrics/http/range        → Filtrar por intervalo de tempo
 * - GET /metrics/http/export       → Exportar para CSV
 * - DELETE /metrics/http/clear     → Limpar todas as métricas
 *
 * Padrão baseado em persistence-crs para consistência arquitetural.
 */
@Slf4j
@RestController
@RequestMapping("/metrics/http")
@RequiredArgsConstructor
public class MetricsController {

	private final HttpMetricsService metricsService;

	@Value("${ecom.atendimento.metrics.csv-export-path:./data/ecom-atendimento-http-metrics.csv}")
	private String csvExportPath;

	/**
	 * Lista todas as métricas HTTP.
	 *
	 * GET /metrics/http
	 *
	 * @return Lista de todas as métricas (ordenadas por timestamp DESC)
	 */
	@GetMapping
	public ResponseEntity<List<HttpRequestMetric>> getAllMetrics() {
		try {
			List<HttpRequestMetric> metrics = metricsService.getAllMetrics();
			log.info("📊 [METRICS_CONTROLLER] Retornadas {} métricas", metrics.size());
			return ResponseEntity.ok(metrics);
		} catch (Exception e) {
			log.error("📊 [METRICS_CONTROLLER] Falha ao buscar todas as métricas", e);
			return ResponseEntity.status(500).build();
		}
	}

	/**
	 * Retorna resumo agregado das métricas.
	 *
	 * GET /metrics/http/summary
	 *
	 * Response:
	 * {
	 *   "totalRequests": 48,
	 *   "successCount": 45,
	 *   "errorCount": 3,
	 *   "avgDurationMs": 125.5,
	 *   "minDurationMs": 50,
	 *   "maxDurationMs": 500,
	 *   "totalRequestBytes": 24576,
	 *   "totalResponseBytes": 49152,
	 *   "successRate": 93.75
	 * }
	 *
	 * @return Map com estatísticas agregadas
	 */
	@GetMapping("/summary")
	public ResponseEntity<Map<String, Object>> getMetricsSummary() {
		try {
			Map<String, Object> summary = metricsService.getMetricsSummary();
			log.info("📊 [METRICS_CONTROLLER] Retornado resumo de métricas: totalRequests={}, successRate={}%",
					summary.get("totalRequests"), summary.get("successRate"));
			return ResponseEntity.ok(summary);
		} catch (Exception e) {
			log.error("📊 [METRICS_CONTROLLER] Falha ao gerar resumo de métricas", e);
			return ResponseEntity.status(500).build();
		}
	}

	/**
	 * Retorna métricas de um tenant específico.
	 *
	 * GET /metrics/http/tenant/{tenantPid}
	 *
	 * @param tenantPid Tenant ID
	 * @return Lista de métricas do tenant
	 */
	@GetMapping("/tenant/{tenantPid}")
	public ResponseEntity<List<HttpRequestMetric>> getMetricsByTenant(@PathVariable String tenantPid) {
		try {
			List<HttpRequestMetric> metrics = metricsService.getMetricsByTenant(tenantPid);
			log.info("📊 [METRICS_CONTROLLER] Retornadas {} métricas para tenant: {}", metrics.size(), tenantPid);
			return ResponseEntity.ok(metrics);
		} catch (Exception e) {
			log.error("📊 [METRICS_CONTROLLER] Falha ao buscar métricas do tenant: {}", tenantPid, e);
			return ResponseEntity.status(500).build();
		}
	}

	/**
	 * Retorna métricas dentro de um intervalo de tempo.
	 *
	 * GET /metrics/http/range?start=2026-03-10T22:00:00Z&end=2026-03-10T23:00:00Z
	 *
	 * @param start Timestamp de início (ISO-8601 format)
	 * @param end Timestamp de fim (ISO-8601 format)
	 * @return Lista de métricas no intervalo
	 */
	@GetMapping("/range")
	public ResponseEntity<List<HttpRequestMetric>> getMetricsByTimeRange(
			@RequestParam String start,
			@RequestParam String end) {
		try {
			Instant startInstant = Instant.parse(start);
			Instant endInstant = Instant.parse(end);

			List<HttpRequestMetric> metrics = metricsService.getMetricsByTimeRange(startInstant, endInstant);
			log.info("📊 [METRICS_CONTROLLER] Retornadas {} métricas para intervalo: {} a {}", metrics.size(), start, end);
			return ResponseEntity.ok(metrics);
		} catch (Exception e) {
			log.error("📊 [METRICS_CONTROLLER] Falha ao buscar métricas por intervalo: {} a {}", start, end, e);
			return ResponseEntity.status(400)
					.body(null);
		}
	}

	/**
	 * Exporta métricas para arquivo CSV e retorna o path.
	 *
	 * GET /metrics/http/export
	 *
	 * Response:
	 * {
	 *   "message": "Metrics exported successfully",
	 *   "filePath": "./data/ecom-atendimento-http-metrics.csv",
	 *   "totalMetrics": 48
	 * }
	 *
	 * @return Map com informações do export
	 */
	@GetMapping("/export")
	public ResponseEntity<Map<String, Object>> exportMetrics() {
		try {
			List<HttpRequestMetric> metrics = metricsService.getAllMetrics();
			metricsService.exportToCsv(csvExportPath);

			log.info("📊 [METRICS_CONTROLLER] Exportadas {} métricas para CSV: {}", metrics.size(), csvExportPath);

			return ResponseEntity.ok(Map.of(
					"message", "Metrics exported successfully",
					"filePath", csvExportPath,
					"totalMetrics", metrics.size()
			));
		} catch (Exception e) {
			log.error("📊 [METRICS_CONTROLLER] Falha ao exportar métricas para CSV", e);
			return ResponseEntity.status(500)
					.body(Map.of(
							"error", "Failed to export metrics",
							"details", e.getMessage()
					));
		}
	}

	/**
	 * Limpa todas as métricas armazenadas.
	 *
	 * DELETE /metrics/http/clear
	 *
	 * Response:
	 * {
	 *   "message": "All metrics cleared successfully"
	 * }
	 *
	 * @return Map com mensagem de confirmação
	 */
	@DeleteMapping("/clear")
	public ResponseEntity<Map<String, String>> clearMetrics() {
		try {
			metricsService.clearMetrics();
			log.info("📊 [METRICS_CONTROLLER] Todas as métricas foram limpas via API");

			return ResponseEntity.ok(Map.of("message", "All metrics cleared successfully"));
		} catch (Exception e) {
			log.error("📊 [METRICS_CONTROLLER] Falha ao limpar métricas", e);
			return ResponseEntity.status(500)
					.body(Map.of(
							"error", "Failed to clear metrics",
							"details", e.getMessage()
					));
		}
	}
}
