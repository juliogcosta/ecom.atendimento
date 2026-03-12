package com.ecom.atendimento.domain.port;

import java.time.Instant;
import java.util.List;

import com.ecom.atendimento.domain.model.HttpRequestMetric;

/**
 * Repositório para persistência de métricas HTTP.
 *
 * Interface de domínio (port) que define operações de persistência
 * para HttpRequestMetric. A implementação (adapter) pode usar JSON,
 * in-memory, banco de dados relacional, ou qualquer outro mecanismo.
 *
 * Padrão Hexagonal Architecture: Domain Port (interface) ← Infrastructure Adapter (implementação).
 */
public interface HttpMetricsRepository {

	/**
	 * Salva uma métrica HTTP.
	 *
	 * @param metric Métrica a ser salva
	 */
	void save(HttpRequestMetric metric);

	/**
	 * Retorna todas as métricas armazenadas.
	 *
	 * @return Lista de todas as métricas (ordenadas por timestamp DESC)
	 */
	List<HttpRequestMetric> findAll();

	/**
	 * Retorna métricas dentro de um intervalo de tempo.
	 *
	 * @param start Timestamp de início (inclusivo)
	 * @param end Timestamp de fim (inclusivo)
	 * @return Lista de métricas no intervalo especificado
	 */
	List<HttpRequestMetric> findByTimeRange(Instant start, Instant end);

	/**
	 * Retorna métricas de um tenant específico.
	 *
	 * @param tenantPid Tenant ID (extraído do header X-Tenant-Id)
	 * @return Lista de métricas do tenant
	 */
	List<HttpRequestMetric> findByTenantPid(String tenantPid);

	/**
	 * Conta quantas métricas têm sucesso ou erro.
	 *
	 * @param success true para contar sucessos (2xx), false para erros (4xx, 5xx)
	 * @return Quantidade de métricas com o status especificado
	 */
	long countBySuccess(Boolean success);

	/**
	 * Limpa todas as métricas armazenadas (apenas em memória).
	 */
	void clear();

	/**
	 * Exporta métricas para arquivo CSV.
	 *
	 * @param filePath Caminho completo do arquivo CSV a ser criado
	 */
	void exportToCsv(String filePath);
}
