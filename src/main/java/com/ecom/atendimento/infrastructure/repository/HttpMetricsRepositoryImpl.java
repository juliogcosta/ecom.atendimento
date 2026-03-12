package com.ecom.atendimento.infrastructure.repository;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.ecom.atendimento.domain.model.HttpRequestMetric;
import com.ecom.atendimento.domain.port.HttpMetricsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementação in-memory do repositório de métricas HTTP.
 *
 * Armazena métricas em ConcurrentLinkedQueue (thread-safe) com limite
 * configurável. Suporta exportação para JSON e CSV.
 *
 * Ativado apenas se: ecom.atendimento.metrics.enabled=true
 */
@Slf4j
@Repository
@ConditionalOnProperty(name = "ecom.atendimento.metrics.enabled", havingValue = "true", matchIfMissing = false)
public class HttpMetricsRepositoryImpl implements HttpMetricsRepository {

	private final ConcurrentLinkedQueue<HttpRequestMetric> metricsStore = new ConcurrentLinkedQueue<>();
	private final ObjectMapper objectMapper;

	@Value("${ecom.atendimento.metrics.json-file-path:./data/ecom-atendimento-http-metrics.json}")
	private String jsonFilePath;

	@Value("${ecom.atendimento.metrics.csv-export-path:./data/ecom-atendimento-http-metrics.csv}")
	private String csvExportPath;

	@Value("${ecom.atendimento.metrics.max-in-memory:10000}")
	private int maxInMemory;

	public HttpMetricsRepositoryImpl() {
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JavaTimeModule());
		this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	@Override
	public void save(HttpRequestMetric metric) {
		// Evitar overflow: remover métricas mais antigas se atingir limite
		if (metricsStore.size() >= maxInMemory) {
			metricsStore.poll(); // Remove a primeira (mais antiga)
			log.debug("📊 [METRICS_REPO] Limite de {} métricas atingido. Removendo métrica mais antiga.", maxInMemory);
		}

		metricsStore.add(metric);
		log.trace("📊 [METRICS_REPO] Métrica salva: {} {} - Status: {} - Duration: {}ms",
				metric.getMethod(), metric.getPath(), metric.getHttpStatus(), metric.getDurationMs());

		// Auto-exportar para JSON periodicamente (a cada 100 métricas)
		if (metricsStore.size() % 100 == 0) {
			try {
				exportToJson();
			} catch (Exception e) {
				log.warn("📊 [METRICS_REPO] Falha ao auto-exportar para JSON: {}", e.getMessage());
			}
		}
	}

	@Override
	public List<HttpRequestMetric> findAll() {
		return new ArrayList<>(metricsStore).stream()
				.sorted(Comparator.comparing(HttpRequestMetric::getTimestamp).reversed())
				.collect(Collectors.toList());
	}

	@Override
	public List<HttpRequestMetric> findByTimeRange(Instant start, Instant end) {
		return metricsStore.stream()
				.filter(m -> m.getTimestamp() != null)
				.filter(m -> !m.getTimestamp().isBefore(start) && !m.getTimestamp().isAfter(end))
				.sorted(Comparator.comparing(HttpRequestMetric::getTimestamp).reversed())
				.collect(Collectors.toList());
	}

	@Override
	public List<HttpRequestMetric> findByTenantPid(String tenantPid) {
		return metricsStore.stream()
				.filter(m -> tenantPid != null && tenantPid.equals(m.getTenantPid()))
				.sorted(Comparator.comparing(HttpRequestMetric::getTimestamp).reversed())
				.collect(Collectors.toList());
	}

	@Override
	public long countBySuccess(Boolean success) {
		return metricsStore.stream()
				.filter(m -> success.equals(m.getSuccess()))
				.count();
	}

	@Override
	public void clear() {
		int size = metricsStore.size();
		metricsStore.clear();
		log.info("📊 [METRICS_REPO] {} métricas removidas da memória", size);
	}

	@Override
	public void exportToCsv(String filePath) {
		try {
			File file = new File(filePath);
			file.getParentFile().mkdirs();

			try (FileWriter writer = new FileWriter(file)) {
				// Header CSV
				writer.append("ID,RequestID,Timestamp,StartTime,EndTime,DurationMs,Method,Path,TenantPid,Username,")
						.append("RequestSizeBytes,ResponseSizeBytes,HttpStatus,Success,ExceptionType,ExceptionMessage,")
						.append("BoundedContext,Aggregate,Command,AggregateId\n");

				// Data rows
				for (HttpRequestMetric metric : metricsStore) {
					writer.append(String.format("%s,%s,%s,%s,%s,%d,%s,%s,%s,%s,%d,%d,%d,%s,%s,%s,%s,%s,%s,%s\n",
							escapeCsv(metric.getId()),
							escapeCsv(metric.getRequestId()),
							escapeCsv(metric.getTimestamp() != null ? metric.getTimestamp().toString() : ""),
							escapeCsv(metric.getStartTime() != null ? metric.getStartTime().toString() : ""),
							escapeCsv(metric.getEndTime() != null ? metric.getEndTime().toString() : ""),
							metric.getDurationMs() != null ? metric.getDurationMs() : 0,
							escapeCsv(metric.getMethod()),
							escapeCsv(metric.getPath()),
							escapeCsv(metric.getTenantPid()),
							escapeCsv(metric.getUsername()),
							metric.getRequestSizeBytes() != null ? metric.getRequestSizeBytes() : 0,
							metric.getResponseSizeBytes() != null ? metric.getResponseSizeBytes() : 0,
							metric.getHttpStatus() != null ? metric.getHttpStatus() : 0,
							metric.getSuccess() != null ? metric.getSuccess().toString() : "false",
							escapeCsv(metric.getExceptionType()),
							escapeCsv(metric.getExceptionMessage()),
							escapeCsv(metric.getBoundedContext()),
							escapeCsv(metric.getAggregate()),
							escapeCsv(metric.getCommand()),
							escapeCsv(metric.getAggregateId())
					));
				}
			}

			log.info("📊 [METRICS_REPO] {} métricas exportadas para CSV: {}", metricsStore.size(), filePath);

		} catch (IOException e) {
			log.error("📊 [METRICS_REPO] Erro ao exportar métricas para CSV: {}", filePath, e);
		}
	}

	/**
	 * Exporta métricas para arquivo JSON (chamado automaticamente a cada 100 métricas).
	 */
	private void exportToJson() {
		try {
			File file = new File(jsonFilePath);
			file.getParentFile().mkdirs();
			objectMapper.writeValue(file, new ArrayList<>(metricsStore));
			log.debug("📊 [METRICS_REPO] {} métricas exportadas para JSON: {}", metricsStore.size(), jsonFilePath);
		} catch (IOException e) {
			log.error("📊 [METRICS_REPO] Erro ao exportar métricas para JSON: {}", jsonFilePath, e);
		}
	}

	/**
	 * Escapa valores para CSV (adiciona aspas se contém vírgula, aspas ou quebra de linha).
	 */
	private String escapeCsv(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}
}
