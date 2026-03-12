package com.ecom.atendimento.domain.aggregate;

import com.ecom.atendimento.domain.valueobject.*;
import lombok.Data;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Conceito representando o estado atual de um Atendimento.
 * Esta classe é mutável e contém todo o estado reconstitutível do agregado.
 */
@Data
public class Atendimento {

    private UUID id;
    private Status status;
    private Integer version;

    // Campo NOVO conforme especificação
    private String protocolo;

    // Timestamps de cada transição
    private Timestamp dataHoraSolicitado;
    private Timestamp dataHoraAjustado;
    private Timestamp dataHoraConfirmado;
    private Timestamp dataHoraOcorrido;     // NOVO
    private Timestamp dataHoraFinalizado;
    private Timestamp dataHoraCancelado;

    // Dados do atendimento
    private String tipodeocorrencia;
    private String descricao;

    // Participantes
    private Cliente cliente;
    private Veiculo veiculo;
    private Prestador prestador;

    // Serviço
    private Servico servico;

    // Endereços
    private Endereco base;
    private Endereco origem;
    private Endereco destino;

    // Itens de cobrança
    private List<Item> items = new ArrayList<>();

    // Ocorrências registradas durante execução (NOVO)
    private List<String> ocorrencias = new ArrayList<>();

    public Atendimento() {
        // Construtor padrão necessário para desserialização
    }
}
