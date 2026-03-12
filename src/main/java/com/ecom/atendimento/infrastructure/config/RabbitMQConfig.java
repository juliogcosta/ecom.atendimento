package com.ecom.atendimento.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do RabbitMQ para integrações:
 * 1. Bounded context Financeiro (eventos de atendimentos finalizados)
 * 2. Projection/Read Model (todos os eventos do agregado Atendimento)
 *
 * Cria exchanges, queues e bindings necessários.
 */
@Configuration
@Slf4j
public class RabbitMQConfig {

    // ========== Configuração Financeiro ==========

    @Value("${spring.rabbitmq.financeiro.exchange:financeiro.exchange}")
    private String financeiroExchange;

    @Value("${spring.rabbitmq.financeiro.queue:atendimento.finalizado.queue}")
    private String financeiroQueue;

    @Value("${spring.rabbitmq.financeiro.routing-key:atendimento.finalizado}")
    private String financeiroRoutingKey;

    // ========== Configuração Projection ==========

    @Value("${spring.rabbitmq.projection.exchange:atendimento.events.exchange}")
    private String projectionExchange;

    @Value("${spring.rabbitmq.projection.queue:atendimento.projection.queue}")
    private String projectionQueue;

    @Value("${spring.rabbitmq.projection.routing-key:atendimento.event}")
    private String projectionRoutingKey;

    /**
     * Exchange para eventos destinados ao sistema financeiro.
     */
    @Bean
    public TopicExchange financeiroExchange() {
        log.info("Criando exchange: {}", financeiroExchange);
        return new TopicExchange(financeiroExchange, true, false);
    }

    /**
     * Queue para eventos de atendimentos finalizados.
     */
    @Bean
    public Queue financeiroQueue() {
        log.info("Criando queue: {}", financeiroQueue);
        return QueueBuilder
                .durable(financeiroQueue)
                .build();
    }

    /**
     * Binding entre exchange e queue usando routing key.
     */
    @Bean
    public Binding financeiroBinding(Queue financeiroQueue, TopicExchange financeiroExchange) {
        log.info("Criando binding: {} -> {} com routing key: {}",
                financeiroExchange.getName(), financeiroQueue.getName(), financeiroRoutingKey);
        return BindingBuilder
                .bind(financeiroQueue)
                .to(financeiroExchange)
                .with(financeiroRoutingKey);
    }

    // ========== Beans de Projection ==========

    /**
     * Exchange para eventos destinados ao Read Model (Projection).
     * Recebe TODOS os eventos do agregado Atendimento.
     */
    @Bean
    public TopicExchange projectionExchange() {
        log.info("Criando exchange de projeção: {}", projectionExchange);
        return new TopicExchange(projectionExchange, true, false);
    }

    /**
     * Queue para consumo pelo microservice ecom.atendimento.projection.
     *
     * NOTA: Esta queue é declarada aqui apenas para garantir que existe no RabbitMQ.
     * O consumer real está no microservice ecom.atendimento.projection.
     */
    @Bean
    public Queue projectionQueue() {
        log.info("Criando queue de projeção: {}", projectionQueue);
        return QueueBuilder
                .durable(projectionQueue)
                .build();
    }

    /**
     * Binding entre exchange de projeção e queue usando routing key.
     */
    @Bean
    public Binding projectionBinding(Queue projectionQueue, TopicExchange projectionExchange) {
        log.info("Criando binding de projeção: {} -> {} com routing key: {}",
                projectionExchange.getName(), projectionQueue.getName(), projectionRoutingKey);
        return BindingBuilder
                .bind(projectionQueue)
                .to(projectionExchange)
                .with(projectionRoutingKey);
    }

    // ========== Beans Comuns ==========

    /**
     * Configura conversão de mensagens para JSON usando Jackson 2.
     *
     * NOTA: Jackson2JsonMessageConverter está deprecated desde Spring AMQP 4.0,
     * mas é mantido aqui pois este microservice usa Spring Boot 3.4.2.
     * A classe substituta JacksonJsonMessageConverter só está disponível no Spring Boot 4.0+.
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configura RabbitTemplate com conversão JSON.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
