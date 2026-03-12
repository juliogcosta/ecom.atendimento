package com.ecom.atendimento.adapter.rest;

import com.ecom.atendimento.adapter.dto.*;
import com.ecom.atendimento.domain.aggregate.AtendimentoAggregate;
import com.ecom.atendimento.domain.command.*;
import com.ecom.atendimento.domain.valueobject.*;
import com.ecom.atendimento.infrastructure.config.AggregateType;
import com.ecom.core.cqrs.application.service.AggregateStore;
import com.ecom.core.cqrs.application.service.command.CommandProcessor;
import com.ecom.core.cqrs.domain.Aggregate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 */
@Slf4j
@RestController
@RequestMapping("/api/atendimento")
@RequiredArgsConstructor
@Validated
public class AtendimentoController {

    private final CommandProcessor commandProcessor;
    private final AggregateStore aggregateStore;

    @Value("${db.schema:ecom_ae}")
    private String schemaName;

    /**
     * POST /api/atendimento/solicitar
     * Cria um novo atendimento.
     */
    @PostMapping("/solicitar")
    public ResponseEntity<Map<String, String>> solicitar(@Valid @RequestBody SolicitarRequest request) {
        log.info("Recebendo solicitação de atendimento com protocolo: {}", request.getProtocolo());

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

        // Processa comando
        Aggregate aggregate = commandProcessor.process(command);

        log.info("Atendimento {} solicitado com sucesso", aggregate.getAggregateId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("id", aggregate.getAggregateId().toString()));
    }

    /**
     * PUT /api/atendimento/ajustar
     * Ajusta um atendimento existente.
     */
    @PutMapping("/ajustar")
    public ResponseEntity<Void> ajustar(@Valid @RequestBody AjustarRequest request) {
        log.info("Recebendo ajuste para atendimento: {}", request.getAggregateId());

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

        // Processa comando
        commandProcessor.process(command);

        log.info("Atendimento {} ajustado com sucesso", request.getAggregateId());

        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/atendimento/confirmar
     * Confirma um atendimento.
     */
    @PutMapping("/confirmar")
    public ResponseEntity<Void> confirmar(@Valid @RequestBody SimpleCommandRequest request) {
        log.info("Recebendo confirmação para atendimento: {}", request.getAggregateId());

        ConfirmarCommand command = new ConfirmarCommand(request.getAggregateId());
        commandProcessor.process(command);

        log.info("Atendimento {} confirmado com sucesso", request.getAggregateId());

        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/atendimento/ocorrencia
     * Registra ocorrências durante o atendimento.
     * NOVO: Este endpoint não existe na implementação de referência.
     */
    @PutMapping("/ocorrencia")
    public ResponseEntity<Void> ocorrencia(@Valid @RequestBody OcorrenciaRequest request) {
        log.info("Registrando {} ocorrências para atendimento: {}",
                request.getOcorrencias().size(), request.getAggregateId());

        OcorrenciaCommand command = new OcorrenciaCommand(
                request.getAggregateId(),
                request.getOcorrencias()
        );

        commandProcessor.process(command);

        log.info("Ocorrências registradas com sucesso para atendimento {}", request.getAggregateId());

        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/atendimento/finalizar
     * Finaliza um atendimento.
     */
    @PutMapping("/finalizar")
    public ResponseEntity<Void> finalizar(@Valid @RequestBody SimpleCommandRequest request) {
        log.info("Recebendo finalização para atendimento: {}", request.getAggregateId());

        FinalizarCommand command = new FinalizarCommand(request.getAggregateId());
        commandProcessor.process(command);

        log.info("Atendimento {} finalizado com sucesso", request.getAggregateId());

        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/atendimento/cancelar
     * Cancela um atendimento.
     */
    @PutMapping("/cancelar")
    public ResponseEntity<Void> cancelar(@Valid @RequestBody SimpleCommandRequest request) {
        log.info("Recebendo cancelamento para atendimento: {}", request.getAggregateId());

        CancelarCommand command = new CancelarCommand(request.getAggregateId());
        commandProcessor.process(command);

        log.info("Atendimento {} cancelado com sucesso", request.getAggregateId());

        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/atendimento/{aggregateId}
     * Recupera o estado atual do agregado a partir do Event Store.
     *
     * Este endpoint reconstrói o agregado fazendo replay de todos os eventos
     * persistidos no banco de escrita (Event Store).
     */
    @GetMapping("/{aggregateId}")
    public ResponseEntity<AtendimentoAggregate> getAtendimento(@PathVariable UUID aggregateId) {
        log.info("Recuperando estado do agregado: {}", aggregateId);

        // Lê o agregado do Event Store (reconstrói via replay de eventos)
        // Exceções serão capturadas pelo GlobalExceptionHandler
        Aggregate aggregate = aggregateStore.readAggregate(
                schemaName,
                AggregateType.YC_ECOMIGO_ATENDIMENTO.toString(),
                aggregateId
        );

        AtendimentoAggregate atendimentoAggregate = (AtendimentoAggregate) aggregate;

        log.info("Agregado {} recuperado com sucesso. Versão: {}, Status: {}",
                aggregateId, atendimentoAggregate.getVersion(),
                atendimentoAggregate.getAtendimento().getStatus());

        return ResponseEntity.ok(atendimentoAggregate);
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
