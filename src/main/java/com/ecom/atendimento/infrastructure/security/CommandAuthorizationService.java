package com.ecom.atendimento.infrastructure.security;

import com.ecom.atendimento.infrastructure.security.RoleAuthorizationService.UnauthorizedException;
import com.yc.pr.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Service específico para validação de autorização a nível de comandos CQRS.
 *
 * Intercepta comandos antes de serem processados pelo CommandProcessor,
 * validando se o User autenticado possui as roles necessárias definidas
 * no método toWrite() de cada comando.
 *
 * Padrão adaptado de ecom.suporte (SecuredRepository) para CQRS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandAuthorizationService {

    private final RoleAuthorizationService roleAuthorizationService;

    /**
     * Valida se User possui autorização para executar o comando.
     *
     * Este método deve ser chamado no Controller ANTES de commandProcessor.process(command).
     *
     * @param command Comando a ser executado (deve implementar Authorizable)
     * @param user User autenticado (extraído do SecurityContext)
     * @throws UnauthorizedException se user não possui role permitida (HTTP 403 Forbidden)
     */
    public void checkCommandAuthorization(Authorizable command, User user) throws UnauthorizedException {
        log.info(">>> [COMMAND_AUTH] ========================================");
        log.info(">>> [COMMAND_AUTH] Validando autorização para comando: {}", command.getClass().getSimpleName());
        log.info(">>> [COMMAND_AUTH] User: {}", user.getUsername());

        // Todos os comandos têm toWrite() (padrão definido na interface Authorizable)
        String[] allowedRoles = command.toWrite();

        log.info(">>> [COMMAND_AUTH] Roles permitidas para comando: {}", Arrays.toString(allowedRoles));

        // Delega para RoleAuthorizationService (mesma lógica do ecom.suporte)
        roleAuthorizationService.checkWriteAccess(user, allowedRoles);

        log.info(">>> [COMMAND_AUTH] ✓ Autorização concedida para comando: {}", command.getClass().getSimpleName());
        log.info(">>> [COMMAND_AUTH] ========================================");
    }
}
