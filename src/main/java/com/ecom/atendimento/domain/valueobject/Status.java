package com.ecom.atendimento.domain.valueobject;

/**
 * Enum representando os estados possíveis de um Atendimento.
 *
 * Máquina de Estados:
 * null → SOLICITADO → AJUSTADO → CONFIRMADO → OCORRIDO → FINALIZADO
 *          ↓             ↓
 *      CANCELADO    CANCELADO
 */
public enum Status {
    /**
     * Atendimento foi solicitado pelo cliente
     */
    SOLICITADO,

    /**
     * Atendimento foi ajustado com prestador, destino e/ou itens
     */
    AJUSTADO,

    /**
     * Atendimento foi confirmado e está pronto para execução
     */
    CONFIRMADO,

    /**
     * Ocorrências foram registradas durante a execução
     */
    OCORRIDO,

    /**
     * Atendimento foi finalizado com sucesso
     */
    FINALIZADO,

    /**
     * Atendimento foi cancelado
     */
    CANCELADO
}
