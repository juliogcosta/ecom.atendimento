package com.ecom.atendimento.domain.aggregate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.UUID;

import com.ecom.atendimento.domain.command.AjustarCommand;
import com.ecom.atendimento.domain.command.CancelarCommand;
import com.ecom.atendimento.domain.command.ConfirmarCommand;
import com.ecom.atendimento.domain.command.FinalizarCommand;
import com.ecom.atendimento.domain.command.OcorrenciaCommand;
import com.ecom.atendimento.domain.command.SolicitarCommand;
import com.ecom.atendimento.domain.event.AjustadoEvent;
import com.ecom.atendimento.domain.event.CanceladoEvent;
import com.ecom.atendimento.domain.event.ConfirmadoEvent;
import com.ecom.atendimento.domain.event.FinalizadoEvent;
import com.ecom.atendimento.domain.event.OcorridoEvent;
import com.ecom.atendimento.domain.event.SolicitadoEvent;
import com.ecom.atendimento.domain.valueobject.Status;
import com.ecom.core.common.infrastructure.exception.AggregateStateException;
import com.ecom.core.cqrs.domain.Aggregate;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Agregado Atendimento - implementação de CQRS/Event Sourcing.
 * Processa comandos, valida regras de negócio e gera eventos imutáveis.
 */
@Slf4j
@Getter
@ToString(callSuper = true)
public class AtendimentoAggregate extends Aggregate {

    private final Atendimento atendimento = new Atendimento();

    @JsonCreator
    public AtendimentoAggregate(@NonNull UUID aggregateId, int version) {
        super(aggregateId, version);
        super.defCommands(AtendimentoCommandType.values());
    }

    // ============================================
    // PROCESS METHODS (Command → Event)
    // ============================================

    /**
     * Processa comando SOLICITAR (criação do agregado).
     * Transição: null → SOLICITADO
     */
    public void process(SolicitarCommand command) {
        if (!super.isCommandDef(command)) {
            throw new AggregateStateException("Comando SOLICITAR não está definido no design do agregado");
        }

        // Valida que agregado ainda não foi criado
        if (this.atendimento.getStatus() != null) {
            throw new AggregateStateException(
                    "Atendimento já existe com status %s. Não é possível solicitar novamente.",
                    this.atendimento.getStatus());
        }

        // Gera evento
        super.applyChange(new SolicitadoEvent(
                super.aggregateId,
                this.getNextVersion(),
                Status.SOLICITADO,
                command.getProtocolo(),
                command.getTipodeocorrencia(),
                command.getCliente(),
                command.getVeiculo(),
                command.getServico(),
                command.getBase(),
                command.getOrigem()));

        log.info("Atendimento {} solicitado com protocolo {}", super.aggregateId, command.getProtocolo());
    }

    /**
     * Processa comando AJUSTAR.
     * Transição: SOLICITADO ou AJUSTADO → AJUSTADO
     * Pode ser executado múltiplas vezes.
     */
    public void process(AjustarCommand command) {
        if (!super.isCommandDef(command)) {
            throw new AggregateStateException("Comando AJUSTAR não está definido no design do agregado");
        }

        // Valida estado atual
        if (!EnumSet.of(Status.SOLICITADO, Status.AJUSTADO).contains(this.atendimento.getStatus())) {
            throw new AggregateStateException(
                    "Não é possível ajustar atendimento com status %s",
                    this.atendimento.getStatus());
        }

        // Gera evento
        super.applyChange(new AjustadoEvent(
                super.aggregateId,
                this.getNextVersion(),
                Status.AJUSTADO,
                command.getDescricao(),
                command.getPrestador(),
                command.getServico(),
                command.getOrigem(),
                command.getDestino(),
                command.getItems()));

        log.info("Atendimento {} ajustado", super.aggregateId);
    }

    /**
     * Processa comando CONFIRMAR.
     * Transição: SOLICITADO ou AJUSTADO → CONFIRMADO
     */
    public void process(ConfirmarCommand command) {
        if (!super.isCommandDef(command)) {
            throw new AggregateStateException("Comando CONFIRMAR não está definido no design do agregado");
        }

        // Valida que não está já confirmado
        if (this.atendimento.getStatus() == Status.CONFIRMADO) {
            throw new AggregateStateException("Atendimento já está confirmado");
        }

        // Valida que está em estado válido para confirmação
        if (!EnumSet.of(Status.SOLICITADO, Status.AJUSTADO).contains(this.atendimento.getStatus())) {
            throw new AggregateStateException(
                    "Não é possível confirmar atendimento com status %s",
                    this.atendimento.getStatus());
        }

        // Gera evento
        super.applyChange(new ConfirmadoEvent(
                super.aggregateId,
                this.getNextVersion(),
                Status.CONFIRMADO));

        log.info("Atendimento {} confirmado", super.aggregateId);
    }

    /**
     * Processa comando OCORRENCIA (NOVO).
     * Transição: CONFIRMADO ou OCORRIDO → OCORRIDO
     * Pode ser executado múltiplas vezes para registrar múltiplas ocorrências.
     */
    public void process(OcorrenciaCommand command) {
        if (!super.isCommandDef(command)) {
            throw new AggregateStateException("Comando OCORRENCIA não está definido no design do agregado");
        }

        // Valida estado atual
        if (!EnumSet.of(Status.CONFIRMADO, Status.OCORRIDO).contains(this.atendimento.getStatus())) {
            throw new AggregateStateException(
                    "Não é possível registrar ocorrências em atendimento com status %s",
                    this.atendimento.getStatus());
        }

        // Gera evento
        super.applyChange(new OcorridoEvent(
                super.aggregateId,
                this.getNextVersion(),
                Status.OCORRIDO,
                command.getOcorrencias()));

        log.info("Ocorrências registradas para atendimento {}: {} ocorrências",
                super.aggregateId, command.getOcorrencias().size());
    }

    /**
     * Processa comando FINALIZAR.
     * Transição: CONFIRMADO ou OCORRIDO → FINALIZADO
     */
    public void process(FinalizarCommand command) {
        if (!super.isCommandDef(command)) {
            throw new AggregateStateException("Comando FINALIZAR não está definido no design do agregado");
        }

        // Valida estado atual
        if (!EnumSet.of(Status.CONFIRMADO, Status.OCORRIDO).contains(this.atendimento.getStatus())) {
            throw new AggregateStateException(
                    "Não é possível finalizar atendimento com status %s",
                    this.atendimento.getStatus());
        }

        // Gera evento
        super.applyChange(new FinalizadoEvent(
                super.aggregateId,
                this.getNextVersion(),
                Status.FINALIZADO));

        log.info("Atendimento {} finalizado", super.aggregateId);
    }

    /**
     * Processa comando CANCELAR.
     * Transição: SOLICITADO ou AJUSTADO → CANCELADO
     * RESTRIÇÃO: Não pode cancelar após confirmado.
     */
    public void process(CancelarCommand command) {
        if (!super.isCommandDef(command)) {
            throw new AggregateStateException("Comando CANCELAR não está definido no design do agregado");
        }

        // Valida estado atual
        if (!EnumSet.of(Status.SOLICITADO, Status.AJUSTADO).contains(this.atendimento.getStatus())) {
            throw new AggregateStateException(
                    "Não é possível cancelar atendimento com status %s. Cancelamento só é permitido antes da confirmação.",
                    this.atendimento.getStatus());
        }

        // Gera evento
        super.applyChange(new CanceladoEvent(
                super.aggregateId,
                this.getNextVersion(),
                Status.CANCELADO));

        log.info("Atendimento {} cancelado", super.aggregateId);
    }

    // ============================================
    // APPLY METHODS (Event → State)
    // ============================================

    /**
     * Aplica evento SOLICITADO ao estado.
     */
    public void apply(SolicitadoEvent event) {
        this.atendimento.setId(event.getAggregateId());
        this.atendimento.setStatus(event.getStatus());
        this.atendimento.setVersion(event.getVersion());
        this.atendimento.setDataHoraSolicitado(event.getCreatedDate());

        // NOVO: protocolo conforme especificação
        this.atendimento.setProtocolo(event.getProtocolo());

        this.atendimento.setTipodeocorrencia(event.getTipodeocorrencia());
        this.atendimento.setCliente(event.getCliente());
        this.atendimento.setVeiculo(event.getVeiculo());
        this.atendimento.setServico(event.getServico());
        this.atendimento.setBase(event.getBase());
        this.atendimento.setOrigem(event.getOrigem());

        this.version = event.getVersion();
    }

    /**
     * Aplica evento AJUSTADO ao estado.
     */
    public void apply(AjustadoEvent event) {
        this.atendimento.setId(event.getAggregateId());
        this.atendimento.setStatus(event.getStatus());
        this.atendimento.setVersion(event.getVersion());
        this.atendimento.setDataHoraAjustado(event.getCreatedDate());

        // Atualiza apenas campos presentes no evento (podem ser nulos)
        if (event.getDescricao() != null) {
            this.atendimento.setDescricao(event.getDescricao());
        }
        if (event.getPrestador() != null) {
            this.atendimento.setPrestador(event.getPrestador());
        }
        if (event.getServico() != null) {
            this.atendimento.setServico(event.getServico());
        }
        if (event.getOrigem() != null) {
            this.atendimento.setOrigem(event.getOrigem());
        }
        if (event.getDestino() != null) {
            this.atendimento.setDestino(event.getDestino());
        }
        if (event.getItems() != null) {
            this.atendimento.setItems(new ArrayList<>(event.getItems()));
        }

        this.version = event.getVersion();
    }

    /**
     * Aplica evento CONFIRMADO ao estado.
     */
    public void apply(ConfirmadoEvent event) {
        this.atendimento.setId(event.getAggregateId());
        this.atendimento.setStatus(event.getStatus());
        this.atendimento.setVersion(event.getVersion());
        this.atendimento.setDataHoraConfirmado(event.getCreatedDate());

        this.version = event.getVersion();
    }

    /**
     * Aplica evento OCORRIDO ao estado (NOVO).
     * Ocorrências são acumulativas.
     */
    public void apply(OcorridoEvent event) {
        this.atendimento.setId(event.getAggregateId());
        this.atendimento.setStatus(event.getStatus());
        this.atendimento.setVersion(event.getVersion());
        this.atendimento.setDataHoraOcorrido(event.getCreatedDate());

        // Acumula ocorrências (não substitui, adiciona)
        if (this.atendimento.getOcorrencias() == null) {
            this.atendimento.setOcorrencias(new ArrayList<>());
        }
        this.atendimento.getOcorrencias().addAll(event.getOcorrencias());

        this.version = event.getVersion();
    }

    /**
     * Aplica evento FINALIZADO ao estado.
     */
    public void apply(FinalizadoEvent event) {
        this.atendimento.setId(event.getAggregateId());
        this.atendimento.setStatus(event.getStatus());
        this.atendimento.setVersion(event.getVersion());
        this.atendimento.setDataHoraFinalizado(event.getCreatedDate());

        this.version = event.getVersion();
    }

    /**
     * Aplica evento CANCELADO ao estado.
     */
    public void apply(CanceladoEvent event) {
        this.atendimento.setId(event.getAggregateId());
        this.atendimento.setStatus(event.getStatus());
        this.atendimento.setVersion(event.getVersion());
        this.atendimento.setDataHoraCancelado(event.getCreatedDate());

        this.version = event.getVersion();
    }

    @NonNull
    @Override
    public String getAggregateType() {
        return "YC_ECOMIGO_ATENDIMENTO";
    }
}
