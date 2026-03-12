package com.ecom.atendimento.infrastructure.config;

import com.ecom.atendimento.domain.event.*;
import com.ecom.core.cqrs.domain.event.Event;
import com.ecom.core.cqrs.domain.event.EventTypeMapper;
import org.springframework.stereotype.Component;

/**
 * Implementação padrão do EventTypeMapper.
 * Mapeia strings de tipo de evento para classes de evento.
 */
@Component
public class DefaultEventTypeMapper implements EventTypeMapper {

    @Override
    public Class<? extends Event> getClassByEventType(String eventType) {
        return switch (eventType) {
            case "YC_ECOMIGO_ATENDIMENTO_SOLICITADO" -> SolicitadoEvent.class;
            case "YC_ECOMIGO_ATENDIMENTO_AJUSTADO" -> AjustadoEvent.class;
            case "YC_ECOMIGO_ATENDIMENTO_CONFIRMADO" -> ConfirmadoEvent.class;
            case "YC_ECOMIGO_ATENDIMENTO_OCORRIDO" -> OcorridoEvent.class;
            case "YC_ECOMIGO_ATENDIMENTO_FINALIZADO" -> FinalizadoEvent.class;
            case "YC_ECOMIGO_ATENDIMENTO_CANCELADO" -> CanceladoEvent.class;
            default -> throw new IllegalArgumentException("Tipo de evento desconhecido: " + eventType);
        };
    }
}
