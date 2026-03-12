package com.ecom.atendimento.infrastructure.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuração Spring para habilitar processamento assíncrono de métricas.
 *
 * Habilita @Async e configura executor de threads dedicado para
 * processamento de métricas HTTP, evitando sobrecarga no pool
 * principal de threads do Spring.
 *
 * Padrão baseado em persistence-crs para consistência arquitetural.
 */
@Configuration
@EnableAsync
public class MetricsConfig {

	/**
	 * Executor de threads para processamento assíncrono de métricas.
	 *
	 * Configuração:
	 * - Core pool size: 2 threads
	 * - Max pool size: 5 threads
	 * - Queue capacity: 100 tasks
	 * - Thread name prefix: "metrics-async-"
	 * - Rejected execution handler: CallerRunsPolicy (executa na thread do caller se fila cheia)
	 *
	 * Este executor é usado exclusivamente por métodos @Async no
	 * HttpMetricsService, garantindo que o processamento de métricas
	 * não impacte a performance das threads principais do ecom.atendimento.
	 *
	 * @return Executor configurado para métricas
	 */
	@Bean(name = "metricsAsyncExecutor")
	public Executor metricsAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		// Pool de threads
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(5);
		executor.setQueueCapacity(100);

		// Prefixo do nome das threads
		executor.setThreadNamePrefix("metrics-async-");

		// Política de rejeição: CallerRunsPolicy
		// Se a fila estiver cheia, executa na thread do caller (filter)
		executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

		// Aguardar conclusão de tasks ao fazer shutdown
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(30);

		executor.initialize();

		return executor;
	}
}
