package com.ecom.atendimento.infrastructure.security;

import com.yc.pr.models.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsible for role-based authorization checks.
 * Validates if authenticated user has required roles to execute commands or access resources.
 *
 * Padrão baseado em ecom.suporte para consistência arquitetural.
 */
@Slf4j
@Service
public class RoleAuthorizationService {

    /**
     * Checks if user has permission to read entity/command.
     * @param user authenticated user from JWT
     * @param allowedRoles roles allowed by entity's/command's toRead() method
     * @throws UnauthorizedException if user doesn't have required role
     */
    public void checkReadAccess(User user, String[] allowedRoles) throws UnauthorizedException {
        checkAccess(user, allowedRoles, "READ");
    }

    /**
     * Checks if user has permission to write/modify entity or execute command.
     * @param user authenticated user from JWT
     * @param allowedRoles roles allowed by entity's/command's toWrite() method
     * @throws UnauthorizedException if user doesn't have required role
     */
    public void checkWriteAccess(User user, String[] allowedRoles) throws UnauthorizedException {
        checkAccess(user, allowedRoles, "WRITE");
    }

    private void checkAccess(User user, String[] allowedRoles, String operation) throws UnauthorizedException {
        log.info("========================================");
        log.info(">>> [AUTH] Iniciando verificação de autorização");
        log.info(">>> [AUTH] Operação: {}", operation);
        log.info(">>> [AUTH] Username: {}", user.getUsername());
        log.info(">>> [AUTH] User ID: {}", user.getId());
        log.info(">>> [AUTH] User tenants: {}", user.getTenants());

        Set<String> userRoles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toSet());

        log.info(">>> [AUTH] User roles (do JWT): {}", userRoles);
        log.info(">>> [AUTH] Roles permitidas: {}", Arrays.toString(allowedRoles));

        // yc.pr adds "ROLE_" prefix to all roles during JWT parsing
        Set<String> requiredRolesWithPrefix = Arrays.stream(allowedRoles)
                .map(role -> "ROLE_" + role)
                .collect(Collectors.toSet());

        log.info(">>> [AUTH] Roles requeridas com prefixo (ROLE_): {}", requiredRolesWithPrefix);

        boolean authorized = requiredRolesWithPrefix.stream()
                .anyMatch(userRoles::contains);

        if (!authorized) {
            log.error(">>> [AUTH] ACESSO NEGADO!");
            log.error(">>> [AUTH] User {} não possui nenhuma das roles requeridas", user.getUsername());
            throw new UnauthorizedException(
                    String.format("Usuário %s não autorizado para operação %s. Roles requeridas: %s",
                            user.getUsername(), operation, Arrays.toString(allowedRoles))
            );
        }

        log.info(">>> [AUTH] ACESSO AUTORIZADO!");
        log.info("========================================");
    }

    /**
     * Custom exception for authorization failures.
     */
    public static class UnauthorizedException extends ResponseStatusException {
        public UnauthorizedException(String reason) {
            super(HttpStatus.FORBIDDEN, reason);
        }
    }
}
