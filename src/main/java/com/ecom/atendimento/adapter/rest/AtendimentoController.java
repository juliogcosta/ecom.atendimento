package com.ecom.atendimento.adapter.rest;

import com.ecom.atendimento.adapter.dto.*;
import com.ecom.atendimento.domain.aggregate.AtendimentoAggregate;
import com.ecom.atendimento.domain.command.*;
import com.ecom.atendimento.domain.valueobject.*;
import com.ecom.atendimento.infrastructure.config.AggregateType;
import com.ecom.atendimento.infrastructure.security.CommandAuthorizationService;
import com.ecom.atendimento.infrastructure.security.RoleAuthorizationService.UnauthorizedException;
import com.ecom.core.cqrs.application.service.AggregateStore;
import com.ecom.core.cqrs.application.service.command.CommandProcessor;
import com.ecom.core.cqrs.domain.Aggregate;
import com.yc.pr.models.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Controller REST para operações no agregado Atendimento.
 * - Comandos (Write Model): POST/PUT
 * - Consultas (Event Store): GET
 *
 * IMPORTANTE: Todos os endpoints requerem autenticação JWT via headers:
 * - Authorization: Bearer {token}
 * - X-Tenant-Id: {tenant-uuid}
 */
@Slf4j
@RestController
@RequestMapping("/api/atendimento")
@RequiredArgsConstructor
@Validated
public class AtendimentoController extends BaseController {

    private final CommandProcessor commandProcessor;
    private final AggregateStore aggregateStore;
    private final CommandAuthorizationService commandAuthorizationService;

    @Value("${db.schema:ecom_ae}")
    private String schemaName;

    /**
     * POST /api/atendimento/solicitar
     * Cria um novo atendimento.
     */
    @PostMapping(value = "/solicitar", headers = {HttpHeaders.CONTENT_TYPE, "X-Tenant-Id", HttpHeaders.AUTHORIZATION})
    public ResponseEntity<Map<String, String>> solicitar(
            @RequestHeader(name = "Authorization", required = true) String authorization,
            @RequestHeader(name = "X-Tenant-Id", required = true) String tenantId,
            @Valid @RequestBody SolicitarRequest request) {

        // Extrai usuário autenticado do SecurityContext
        final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            // Valida autoridade do usuário para o tenant
            checkTenantIDAuthority(user, tenantId);

            log.info("[Tenant:{}] [User:{}] Recebendo solicitação de atendimento com protocolo: {}",
                tenantId, user.getUsername(), request.getProtocolo());

            // Gera novo UUID para o agregado
            UUID aggregateId = UUID.randomUUID();

            // Converte DTO → Command
            SolicitarCommand command = new SolicitarCommand(
                    aggregateId,
                    request.getProtocolo(),
                    request.getTipodeocorrencia(),
                    toCliente(request.getCliente()),
                    toVeiculo(request.getVeiculo()),
                    toServico(request.getServico()),
                    toEndereco(request.getBase()),
                    toEndereco(request.getOrigem())
            );

            // Valida autorização RBAC antes de processar comando
            commandAuthorizationService.checkCommandAuthorization(command, user);

            // Processa comando
            Aggregate aggregate = commandProcessor.process(command);

            log.info("[Tenant:{}] [User:{}] Atendimento {} solicitado com sucesso",
                tenantId, user.getUsername(), aggregate.getAggregateId());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of("id", aggregate.getAggregateId().toString()));

        } catch (UnauthorizedException e) {
            log.error("[Tenant:{}] [User:{}] Acesso negado ao solicitar atendimento: {}",
                    tenantId, user.getUsername(), e.getReason());
            throw e; // HTTP 403 Forbidden
        } catch (Exception e) {
            if (this.tracePrint) {
                e.printStackTrace();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * PUT /api/atendimento/ajustar
     * Ajusta um atendimento existente.
     */
    @PutMapping(value = "/ajustar", headers = {HttpHeaders.CONTENT_TYPE, "X-Tenant-Id", HttpHeaders.AUTHORIZATION})
    public ResponseEntity<Void> ajustar(
            @RequestHeader(name = "Authorization", required = true) String authorization,
            @RequestHeader(name = "X-Tenant-Id", required = true) String tenantId,
            @Valid @RequestBody AjustarRequest request) {

        final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            checkTenantIDAuthority(user, tenantId);

            log.info("[Tenant:{}] [User:{}] Recebendo ajuste para atendimento: {}",
                tenantId, user.getUsername(), request.getAggregateId());

            // Converte DTO → Command
            AjustarCommand command = new AjustarCommand(
                    request.getAggregateId(),
                    request.getDescricao(),
                    request.getPrestador() != null ? toPrestador(request.getPrestador()) : null,
                    request.getServico() != null ? toServico(request.getServico()) : null,
                    request.getOrigem() != null ? toEndereco(request.getOrigem()) : null,
                    request.getDestino() != null ? toEndereco(request.getDestino()) : null,
                    request.getItems() != null ? request.getItems().stream()
                            .map(this::toItem)
                            .collect(Collectors.toList()) : null
            );

            // Valida autorização RBAC antes de processar comando
            commandAuthorizationService.checkCommandAuthorization(command, user);

            // Processa comando
            commandProcessor.process(command);

            log.info("[Tenant:{}] [User:{}] Atendimento {} ajustado com sucesso",
                tenantId, user.getUsername(), request.getAggregateId());

            return ResponseEntity.ok().build();

        } catch (UnauthorizedException e) {
            log.error("[Tenant:{}] [User:{}] Acesso negado ao ajustar atendimento: {}",
                    tenantId, user.getUsername(), e.getReason());
            throw e; // HTTP 403 Forbidden
        } catch (Exception e) {
            if (this.tracePrint) {
                e.printStackTrace();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * PUT /api/atendimento/confirmar
     * Confirma um atendimento.
     */
    @PutMapping(value = "/confirmar", headers = {HttpHeaders.CONTENT_TYPE, "X-Tenant-Id", HttpHeaders.AUTHORIZATION})
    public ResponseEntity<Void> confirmar(
            @RequestHeader(name = "Authorization", required = true) String authorization,
            @RequestHeader(name = "X-Tenant-Id", required = true) String tenantId,
            @Valid @RequestBody SimpleCommandRequest request) {

        final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            checkTenantIDAuthority(user, tenantId);

            log.info("[Tenant:{}] [User:{}] Recebendo confirmação para atendimento: {}",
                tenantId, user.getUsername(), request.getAggregateId());

            ConfirmarCommand command = new ConfirmarCommand(request.getAggregateId());

            // Valida autorização RBAC antes de processar comando
            commandAuthorizationService.checkCommandAuthorization(command, user);

            commandProcessor.process(command);

            log.info("[Tenant:{}] [User:{}] Atendimento {} confirmado com sucesso",
                tenantId, user.getUsername(), request.getAggregateId());

            return ResponseEntity.ok().build();

        } catch (UnauthorizedException e) {
            log.error("[Tenant:{}] [User:{}] Acesso negado ao confirmar atendimento: {}",
                    tenantId, user.getUsername(), e.getReason());
            throw e; // HTTP 403 Forbidden
        } catch (Exception e) {
            if (this.tracePrint) {
                e.printStackTrace();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * PUT /api/atendimento/ocorrencia
     * Registra ocorrências durante o atendimento.
     * NOVO: Este endpoint não existe na implementação de referência.
     */
    @PutMapping(value = "/ocorrencia", headers = {HttpHeaders.CONTENT_TYPE, "X-Tenant-Id", HttpHeaders.AUTHORIZATION})
    public ResponseEntity<Void> ocorrencia(
            @RequestHeader(name = "Authorization", required = true) String authorization,
            @RequestHeader(name = "X-Tenant-Id", required = true) String tenantId,
            @Valid @RequestBody OcorrenciaRequest request) {

        final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            checkTenantIDAuthority(user, tenantId);

            log.info("[Tenant:{}] [User:{}] Registrando {} ocorrências para atendimento: {}",
                    tenantId, user.getUsername(), request.getOcorrencias().size(), request.getAggregateId());

            OcorrenciaCommand command = new OcorrenciaCommand(
                    request.getAggregateId(),
                    request.getOcorrencias()
            );

            // Valida autorização RBAC antes de processar comando
            commandAuthorizationService.checkCommandAuthorization(command, user);

            commandProcessor.process(command);

            log.info("[Tenant:{}] [User:{}] Ocorrências registradas com sucesso para atendimento {}",
                tenantId, user.getUsername(), request.getAggregateId());

            return ResponseEntity.ok().build();

        } catch (UnauthorizedException e) {
            log.error("[Tenant:{}] [User:{}] Acesso negado ao registrar ocorrências: {}",
                    tenantId, user.getUsername(), e.getReason());
            throw e; // HTTP 403 Forbidden
        } catch (Exception e) {
            if (this.tracePrint) {
                e.printStackTrace();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * PUT /api/atendimento/finalizar
     * Finaliza um atendimento.
     */
    @PutMapping(value = "/finalizar", headers = {HttpHeaders.CONTENT_TYPE, "X-Tenant-Id", HttpHeaders.AUTHORIZATION})
    public ResponseEntity<Void> finalizar(
            @RequestHeader(name = "Authorization", required = true) String authorization,
            @RequestHeader(name = "X-Tenant-Id", required = true) String tenantId,
            @Valid @RequestBody SimpleCommandRequest request) {

        final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            checkTenantIDAuthority(user, tenantId);

            log.info("[Tenant:{}] [User:{}] Recebendo finalização para atendimento: {}",
                tenantId, user.getUsername(), request.getAggregateId());

            FinalizarCommand command = new FinalizarCommand(request.getAggregateId());

            // Valida autorização RBAC antes de processar comando
            commandAuthorizationService.checkCommandAuthorization(command, user);

            commandProcessor.process(command);

            log.info("[Tenant:{}] [User:{}] Atendimento {} finalizado com sucesso",
                tenantId, user.getUsername(), request.getAggregateId());

            return ResponseEntity.ok().build();

        } catch (UnauthorizedException e) {
            log.error("[Tenant:{}] [User:{}] Acesso negado ao finalizar atendimento: {}",
                    tenantId, user.getUsername(), e.getReason());
            throw e; // HTTP 403 Forbidden
        } catch (Exception e) {
            if (this.tracePrint) {
                e.printStackTrace();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * PUT /api/atendimento/cancelar
     * Cancela um atendimento.
     */
    @PutMapping(value = "/cancelar", headers = {HttpHeaders.CONTENT_TYPE, "X-Tenant-Id", HttpHeaders.AUTHORIZATION})
    public ResponseEntity<Void> cancelar(
            @RequestHeader(name = "Authorization", required = true) String authorization,
            @RequestHeader(name = "X-Tenant-Id", required = true) String tenantId,
            @Valid @RequestBody SimpleCommandRequest request) {

        final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            checkTenantIDAuthority(user, tenantId);

            log.info("[Tenant:{}] [User:{}] Recebendo cancelamento para atendimento: {}",
                tenantId, user.getUsername(), request.getAggregateId());

            CancelarCommand command = new CancelarCommand(request.getAggregateId());

            // Valida autorização RBAC antes de processar comando
            commandAuthorizationService.checkCommandAuthorization(command, user);

            commandProcessor.process(command);

            log.info("[Tenant:{}] [User:{}] Atendimento {} cancelado com sucesso",
                tenantId, user.getUsername(), request.getAggregateId());

            return ResponseEntity.ok().build();

        } catch (UnauthorizedException e) {
            log.error("[Tenant:{}] [User:{}] Acesso negado ao cancelar atendimento: {}",
                    tenantId, user.getUsername(), e.getReason());
            throw e; // HTTP 403 Forbidden
        } catch (Exception e) {
            if (this.tracePrint) {
                e.printStackTrace();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * GET /api/atendimento/{aggregateId}
     * Recupera o estado atual do agregado a partir do Event Store.
     *
     * Este endpoint reconstrói o agregado fazendo replay de todos os eventos
     * persistidos no banco de escrita (Event Store).
     */
    @GetMapping(value = "/{aggregateId}", headers = {HttpHeaders.AUTHORIZATION, "X-Tenant-Id"})
    public ResponseEntity<AtendimentoAggregate> getAtendimento(
            @RequestHeader(name = "Authorization", required = true) String authorization,
            @RequestHeader(name = "X-Tenant-Id", required = true) String tenantId,
            @PathVariable UUID aggregateId) {

        final User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        try {
            checkTenantIDAuthority(user, tenantId);

            log.info("[Tenant:{}] [User:{}] Recuperando estado do agregado: {}",
                    tenantId, user.getUsername(), aggregateId);

            // Lê o agregado do Event Store (reconstrói via replay de eventos)
            Aggregate aggregate = aggregateStore.readAggregate(
                    schemaName,
                    AggregateType.YC_ECOMIGO_ATENDIMENTO.toString(),
                    aggregateId
            );

            AtendimentoAggregate atendimentoAggregate = (AtendimentoAggregate) aggregate;

            log.info("[Tenant:{}] [User:{}] Agregado {} recuperado com sucesso. Versão: {}, Status: {}",
                    tenantId, user.getUsername(), aggregateId,
                    atendimentoAggregate.getVersion(),
                    atendimentoAggregate.getAtendimento().getStatus());

            return ResponseEntity.ok(atendimentoAggregate);

        } catch (Exception e) {
            if (this.tracePrint) {
                e.printStackTrace();
            }
            throw new RuntimeException(e);
        }
    }

    // ============================================
    // MÉTODOS DE CONVERSÃO DTO → VALUE OBJECTS
    // ============================================

    private Cliente toCliente(SolicitarRequest.ClienteDTO dto) {
        return new Cliente(
                dto.getId(),
                dto.getNome(),
                new DocFiscal(dto.getDocfiscal().getTipo(), dto.getDocfiscal().getNumero())
        );
    }

    private Veiculo toVeiculo(SolicitarRequest.VeiculoDTO dto) {
        return new Veiculo(dto.getPlaca());
    }

    private Servico toServico(SolicitarRequest.ServicoDTO dto) {
        return new Servico(dto.getId(), dto.getNome());
    }

    private Servico toServico(AjustarRequest.ServicoDTO dto) {
        return new Servico(dto.getId(), dto.getNome());
    }

    private Endereco toEndereco(SolicitarRequest.EnderecoDTO dto) {
        return new Endereco(
                dto.getTipo(),
                dto.getLogradouro(),
                dto.getNumero(),
                dto.getComplemento(),
                dto.getBairro(),
                dto.getCidade(),
                dto.getEstado(),
                dto.getCep()
        );
    }

    private Endereco toEndereco(AjustarRequest.EnderecoDTO dto) {
        return new Endereco(
                dto.getTipo(),
                dto.getLogradouro(),
                dto.getNumero(),
                dto.getComplemento(),
                dto.getBairro(),
                dto.getCidade(),
                dto.getEstado(),
                dto.getCep()
        );
    }

    private Prestador toPrestador(AjustarRequest.PrestadorDTO dto) {
        return new Prestador(
                dto.getId(),
                dto.getNome(),
                new DocFiscal(dto.getDocfiscal().getTipo(), dto.getDocfiscal().getNumero())
        );
    }

    private Item toItem(AjustarRequest.ItemDTO dto) {
        return new Item(
                dto.getNome(),
                dto.getUnidadedemedida(),
                dto.getPrecounitario(),
                dto.getQuantidade(),
                dto.getObservacao()
        );
    }
}
