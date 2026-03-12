package com.ecom.atendimento.application.handler;

import com.ecom.atendimento.domain.aggregate.AtendimentoAggregate;
import com.ecom.atendimento.infrastructure.config.AggregateType;
import com.ecom.core.cqrs.application.service.AggregateStore;
import com.ecom.core.cqrs.application.service.event.handler.SyncEventHandler;
import com.ecom.core.cqrs.domain.Aggregate;
import com.ecom.core.cqrs.domain.event.Event;
import com.ecom.core.cqrs.domain.event.EventWithId;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SyncEventHandler para publicar TODOS os eventos do agregado Atendimento
 * no RabbitMQ para atualização da projeção (Read Model).
 *
 * Este handler é executado SÍNCRONAMENTE após a persistência do evento,
 * garantindo que a mensagem seja enviada na mesma transação.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectionEventHandler implements SyncEventHandler {

    private final AggregateStore aggregateStore;
    private final RabbitTemplate rabbitTemplate;

    @Value("${db.schema:ecom_ae}")
    private String schemaName;

    @Value("${spring.rabbitmq.projection.exchange:atendimento.events.exchange}")
    private String projectionExchange;

    @Value("${spring.rabbitmq.projection.routing-key:atendimento.event}")
    private String projectionRoutingKey;

    @Override
    public void handleEvents(@Nonnull List<EventWithId<Event>> events, @Nonnull Aggregate aggregate) {
        log.debug("ProjectionEventHandler processando {} evento(s) para aggregateId: {}",
                events.size(), aggregate.getAggregateId());

        // Processa cada evento
        for (EventWithId<Event> eventWithId : events) {
            try {
                handleSingleEvent(eventWithId, aggregate);
            } catch (Exception e) {
                log.error("Erro ao processar evento {} para projeção: {}",
                        eventWithId.event().getEventType(), e.getMessage(), e);
                throw new RuntimeException("Falha ao publicar evento para projeção", e);
            }
        }
    }

    /**
     * Processa um único evento e publica no RabbitMQ.
     */
    private void handleSingleEvent(EventWithId<Event> eventWithId, Aggregate aggregate) {
        Event event = eventWithId.event();

        log.info("Publicando evento para projeção: aggregateId={}, version={}, eventType={}",
                event.getAggregateId(), event.getVersion(), event.getEventType());

        // Reconstrói o agregado na versão do evento (garante consistência)
        Aggregate aggregateAtVersion = aggregateStore.readAggregate(
                schemaName,
                AggregateType.YC_ECOMIGO_ATENDIMENTO.toString(),
                event.getAggregateId(),
                event.getVersion()
        );

        AtendimentoAggregate atendimentoAggregate = (AtendimentoAggregate) aggregateAtVersion;

        // Constrói payload com estado completo do agregado
        Map<String, Object> payload = buildProjectionPayload(atendimentoAggregate);

        // Publica no RabbitMQ
        publishToProjectionQueue(payload);

        log.info("Evento publicado com sucesso para projeção: aggregateId={}, version={}",
                event.getAggregateId(), event.getVersion());
    }

    /**
     * Constrói payload com estado completo do agregado para projeção.
     */
    private Map<String, Object> buildProjectionPayload(AtendimentoAggregate aggregate) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("aggregateId", aggregate.getAggregateId().toString());
        payload.put("version", aggregate.getVersion());
        payload.put("protocolo", aggregate.getAtendimento().getProtocolo());
        payload.put("status", aggregate.getAtendimento().getStatus().toString());
        payload.put("tipodeocorrencia", aggregate.getAtendimento().getTipodeocorrencia());
        payload.put("descricao", aggregate.getAtendimento().getDescricao());

        // Value Objects
        payload.put("cliente", aggregate.getAtendimento().getCliente());
        payload.put("veiculo", aggregate.getAtendimento().getVeiculo());
        payload.put("servico", aggregate.getAtendimento().getServico());
        payload.put("prestador", aggregate.getAtendimento().getPrestador());
        payload.put("base", aggregate.getAtendimento().getBase());
        payload.put("origem", aggregate.getAtendimento().getOrigem());
        payload.put("destino", aggregate.getAtendimento().getDestino());
        payload.put("items", aggregate.getAtendimento().getItems());

        // Timestamps (converte Timestamp para Long - milliseconds)
        if (aggregate.getAtendimento().getDataHoraSolicitado() != null) {
            payload.put("dataHoraSolicitado", aggregate.getAtendimento().getDataHoraSolicitado().getTime());
        }
        if (aggregate.getAtendimento().getDataHoraAjustado() != null) {
            payload.put("dataHoraAjustado", aggregate.getAtendimento().getDataHoraAjustado().getTime());
        }
        if (aggregate.getAtendimento().getDataHoraConfirmado() != null) {
            payload.put("dataHoraConfirmado", aggregate.getAtendimento().getDataHoraConfirmado().getTime());
        }
        if (aggregate.getAtendimento().getDataHoraOcorrido() != null) {
            payload.put("dataHoraOcorrido", aggregate.getAtendimento().getDataHoraOcorrido().getTime());
        }
        if (aggregate.getAtendimento().getDataHoraFinalizado() != null) {
            payload.put("dataHoraFinalizado", aggregate.getAtendimento().getDataHoraFinalizado().getTime());
        }
        if (aggregate.getAtendimento().getDataHoraCancelado() != null) {
            payload.put("dataHoraCancelado", aggregate.getAtendimento().getDataHoraCancelado().getTime());
        }

        // Ocorrências
        payload.put("ocorrencias", aggregate.getAtendimento().getOcorrencias());

        return payload;
    }

    /**
     * Publica payload no RabbitMQ para fila de projeção.
     *
     * IMPORTANTE: Envia o Map diretamente. O Jackson2JsonMessageConverter configurado
     * no RabbitTemplate fará a serialização automática para JSON.
     */
    private void publishToProjectionQueue(Map<String, Object> payload) {
        // Envia Map diretamente - o messageConverter do RabbitTemplate serializa para JSON
        rabbitTemplate.convertAndSend(projectionExchange, projectionRoutingKey, payload);

        log.debug("Mensagem publicada para exchange '{}' com routing key '{}': {}",
                projectionExchange, projectionRoutingKey, payload);
    }

    @Nonnull
    @Override
    public String getAggregateType() {
        return AggregateType.YC_ECOMIGO_ATENDIMENTO.toString();
    }
}
