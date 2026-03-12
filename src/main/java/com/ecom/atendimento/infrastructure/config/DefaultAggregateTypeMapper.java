package com.ecom.atendimento.infrastructure.config;

import com.ecom.core.cqrs.domain.Aggregate;
import com.ecom.core.cqrs.domain.AggregateTypeMapper;
import org.springframework.stereotype.Component;

/**
 * Implementação padrão do AggregateTypeMapper.
 * Mapeia strings de tipo de agregado para classes de agregado.
 */
@Component
public class DefaultAggregateTypeMapper implements AggregateTypeMapper {

    @Override
    public Class<? extends Aggregate> getClassByAggregateType(String aggregateType) {
        return AggregateType.valueOf(aggregateType).getAggregateClass();
    }
}
