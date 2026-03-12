package com.ecom.atendimento.infrastructure.config;

import com.ecom.atendimento.domain.aggregate.AtendimentoAggregate;
import com.ecom.core.cqrs.domain.Aggregate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum mapeando tipos de agregados para suas classes de implementação.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum AggregateType {

    YC_ECOMIGO_ATENDIMENTO(AtendimentoAggregate.class);

    private final Class<? extends Aggregate> aggregateClass;
}
