package com.ecom.atendimento.application.handler;

import com.ecom.atendimento.domain.aggregate.AtendimentoAggregate;
import com.ecom.atendimento.infrastructure.config.AggregateType;
import com.ecom.core.cqrs.application.service.AggregateStore;
import com.ecom.core.cqrs.application.service.event.handler.AsyncEventHandler;
import com.ecom.core.cqrs.domain.Aggregate;
import com.ecom.core.cqrs.domain.event.Event;
import com.ecom.core.cqrs.domain.event.EventWithId;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Event Handler para integração seletiva com sistema Financeiro.
 *
 * IMPORTANTE: De acordo com a especificação, apenas eventos FINALIZADO
 * devem acionar integração com o bounded context financeiro.
 *
 * Todos os outros eventos (solicitado, ajustado, confirmado, ocorrido, cancelado)
 * são processados internamente mas NÃO disparam integração externa.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinanceiroIntegrationEventHandler implements AsyncEventHandler {

    private static final String FINALIZADO_EVENT_TYPE = "YC_ECOMIGO_ATENDIMENTO_FINALIZADO";

    private final AggregateStore aggregateStore;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${db.schema:assistencia_es}")
    private String schemaName;

    @Value("${spring.rabbitmq.financeiro.exchange:financeiro.exchange}")
    private String financeiroExchange;

    @Value("${spring.rabbitmq.financeiro.routing-key:atendimento.finalizado}")
    private String financeiroRoutingKey;

    @Override
    public void handleEvent(EventWithId<Event> eventWithId) {
        Event event = eventWithId.event();
        String eventType = event.getEventType();

        // FILTRO: Apenas eventos FINALIZADO acionam integração
        if (!FINALIZADO_EVENT_TYPE.equals(eventType)) {
            log.debug("Evento {} não aciona integração com financeiro. Evento ignorado.", eventType);
            return;
        }

        log.info("Evento FINALIZADO detectado para atendimento {}. Iniciando integração com financeiro.",
                event.getAggregateId());

        try {
            // Lê o estado completo do agregado
            Aggregate aggregate = aggregateStore.readAggregate(
                    schemaName,
                    AggregateType.YC_ECOMIGO_ATENDIMENTO.toString(),
                    event.getAggregateId(),
                    event.getVersion()
            );

            AtendimentoAggregate atendimentoAggregate = (AtendimentoAggregate) aggregate;

            // Constrói DTO de integração com dados necessários para financeiro
            Map<String, Object> integrationPayload = buildFinanceiroPayload(atendimentoAggregate);

            // Envia para RabbitMQ
            sendToFinanceiro(integrationPayload);

            log.info("Integração com financeiro concluída para atendimento {}", event.getAggregateId());

        } catch (Exception e) {
            log.error("Erro ao processar integração com financeiro para atendimento {}: {}",
                    event.getAggregateId(), e.getMessage(), e);
            throw new RuntimeException("Falha na integração com financeiro", e);
        }
    }

    /**
     * Constrói payload de integração com dados necessários para o sistema financeiro.
     */
    private Map<String, Object> buildFinanceiroPayload(AtendimentoAggregate aggregate) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("aggregateId", aggregate.getAggregateId().toString());
        payload.put("protocolo", aggregate.getAtendimento().getProtocolo());
        payload.put("status", aggregate.getAtendimento().getStatus().toString());
        payload.put("version", aggregate.getVersion());

        // Dados do cliente
        if (aggregate.getAtendimento().getCliente() != null) {
            Map<String, Object> cliente = new HashMap<>();
            cliente.put("id", aggregate.getAtendimento().getCliente().getId());
            cliente.put("nome", aggregate.getAtendimento().getCliente().getNome());
            payload.put("cliente", cliente);
        }

        // Dados do prestador
        if (aggregate.getAtendimento().getPrestador() != null) {
            Map<String, Object> prestador = new HashMap<>();
            prestador.put("id", aggregate.getAtendimento().getPrestador().getId());
            prestador.put("nome", aggregate.getAtendimento().getPrestador().getNome());
            payload.put("prestador", prestador);
        }

        // Itens de cobrança (essenciais para financeiro)
        if (aggregate.getAtendimento().getItems() != null && !aggregate.getAtendimento().getItems().isEmpty()) {
            payload.put("items", aggregate.getAtendimento().getItems().stream()
                    .map(item -> Map.of(
                            "nome", item.getNome(),
                            "quantidade", item.getQuantidade(),
                            "precounitario", item.getPrecounitario(),
                            "valortotal", item.getValorTotal()
                    ))
                    .toList());
        }

        // Timestamps
        payload.put("dataHoraSolicitado", aggregate.getAtendimento().getDataHoraSolicitado());
        payload.put("dataHoraFinalizado", aggregate.getAtendimento().getDataHoraFinalizado());

        // Ocorrências (para justificar possíveis valores extras)
        if (aggregate.getAtendimento().getOcorrencias() != null && !aggregate.getAtendimento().getOcorrencias().isEmpty()) {
            payload.put("ocorrencias", aggregate.getAtendimento().getOcorrencias());
        }

        payload.put("eventType", "ATENDIMENTO_FINALIZADO");
        payload.put("timestamp", System.currentTimeMillis());

        return payload;
    }

    /**
     * Envia payload para RabbitMQ no exchange do financeiro.
     */
    @SneakyThrows
    private void sendToFinanceiro(Map<String, Object> payload) {
        String jsonPayload = objectMapper.writeValueAsString(payload);

        rabbitTemplate.convertAndSend(financeiroExchange, financeiroRoutingKey, jsonPayload);

        log.info("Mensagem enviada para exchange '{}' com routing key '{}': {}",
                financeiroExchange, financeiroRoutingKey, jsonPayload);
    }

    @Nonnull
    @Override
    public String getAggregateType() {
        return AggregateType.YC_ECOMIGO_ATENDIMENTO.toString();
    }
}
