package com.ecom.atendimento.adapter.rest;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;

import com.yc.pr.models.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller base para ecom.atendimento.
 *
 * Fornece métodos utilitários para:
 * - Validação de autoridade de tenant
 * - Transformação de User (yc.pr) para loco3
 * - Extração de logrole e loguser para auditoria
 */
@Slf4j
public abstract class BaseController {

    @Value("${yc.app.log.error.trace.print:false}")
    protected Boolean tracePrint;

    /**
     * Verifica se o usuário tem autoridade para acessar o tenantId informado.
     *
     * @param user     Usuário autenticado
     * @param tenantId Tenant solicitado
     * @throws Exception Se o usuário não tem autoridade (HTTP 403)
     */
    protected void checkTenantIDAuthority(@NotNull User user, @NotNull @NotBlank String tenantId) throws Exception {
        if (!user.getTenants().contains(tenantId)) {
            log.warn("Usuário {} tentou acessar tenant {} sem autorização", user.getUsername(), tenantId);
            throw new Exception(String.valueOf(HttpStatus.FORBIDDEN.value()).concat(":forbidden request."));
        }
    }

    /**
     * Role adapter para br.edu.ufrn.loco3.security.Role
     */
    protected class CRole implements br.edu.ufrn.loco3.security.Role {
        private String name;

        public CRole(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /**
     * User adapter para br.edu.ufrn.loco3.security.User
     */
    protected class CUser implements br.edu.ufrn.loco3.security.User {
        private String username;
        private Set<br.edu.ufrn.loco3.security.Role> cRoles;

        public CUser(String username, Set<br.edu.ufrn.loco3.security.Role> cRoles) {
            this.username = username;
            this.cRoles = cRoles;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public Set<br.edu.ufrn.loco3.security.Role> getRoles() {
            return new HashSet<>(cRoles);
        }
    }

    /**
     * Transforma User (yc.pr) para CUser (loco3).
     *
     * @param user Usuário do yc.pr
     * @return CUser compatível com loco3
     */
    protected CUser transform(User user) {
        final Set<br.edu.ufrn.loco3.security.Role> roles = new HashSet<>();
        user.getRoles().forEach(role -> roles.add(new CRole(role.getName())));
        return new CUser(user.getUsername(), roles);
    }

    /**
     * Extrai logrole do usuário autenticado (primeira role).
     *
     * @param user Usuário autenticado
     * @return Nome da role ou "ANONYMOUS"
     */
    protected String getLogRole(User user) {
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            return user.getRoles().iterator().next().getName();
        }
        return "ANONYMOUS";
    }

    /**
     * Extrai loguser do usuário autenticado.
     *
     * @param user Usuário autenticado
     * @return Username ou "anonymous"
     */
    protected String getLogUser(User user) {
        return user.getUsername() != null ? user.getUsername() : "anonymous";
    }
}
