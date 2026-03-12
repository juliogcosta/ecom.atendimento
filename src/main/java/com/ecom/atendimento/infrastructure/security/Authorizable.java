package com.ecom.atendimento.infrastructure.security;

/**
 * Interface que define contrato de autorização RBAC.
 *
 * Implementada por Commands que requerem validação de roles antes da execução.
 * Padrão inspirado no ecom.suporte, onde entidades definem toRead() e toWrite().
 *
 * Em CQRS, Commands são equivalentes a operações de escrita, então geralmente
 * apenas toWrite() é necessário. toRead() está presente para consistência
 * com o padrão e para possíveis queries futuras.
 */
public interface Authorizable {

    /**
     * Retorna as roles permitidas para executar operações de escrita (comandos).
     *
     * @return Array de roles permitidas (ex: ["GERENTE", "ADMINISTRADOR"])
     */
    String[] toWrite();

    /**
     * Retorna as roles permitidas para executar operações de leitura.
     *
     * Em CQRS, isso pode ser usado para queries ou para validar acesso
     * antes de reconstruir agregados via Event Store.
     *
     * @return Array de roles permitidas (ex: ["GERENTE", "ADMINISTRADOR"])
     */
    String[] toRead();
}
