package com.ecom.atendimento.domain.aggregate;

import com.ecom.atendimento.domain.command.*;
import com.ecom.core.cqrs.domain.command.Command;
import com.ecom.core.cqrs.domain.command.CommandType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum definindo os tipos de comandos válidos para o agregado Atendimento.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum AtendimentoCommandType implements CommandType {

    SOLICITAR(SolicitarCommand.class),
    AJUSTAR(AjustarCommand.class),
    CONFIRMAR(ConfirmarCommand.class),
    OCORRENCIA(OcorrenciaCommand.class),  // NOVO
    FINALIZAR(FinalizarCommand.class),
    CANCELAR(CancelarCommand.class);

    private final Class<? extends Command> commandClass;
}
