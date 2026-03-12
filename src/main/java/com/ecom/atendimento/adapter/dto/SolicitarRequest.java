package com.ecom.atendimento.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de requisição para solicitar um novo atendimento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitarRequest {

    @NotBlank(message = "Protocolo é obrigatório")
    @Size(max = 64, message = "Protocolo não pode ter mais de 64 caracteres")
    private String protocolo;

    @NotBlank(message = "Tipo de ocorrência é obrigatório")
    @Size(max = 100, message = "Tipo de ocorrência não pode ter mais de 100 caracteres")
    private String tipodeocorrencia;

    @NotNull(message = "Cliente é obrigatório")
    private ClienteDTO cliente;

    @NotNull(message = "Veículo é obrigatório")
    private VeiculoDTO veiculo;

    @NotNull(message = "Serviço é obrigatório")
    private ServicoDTO servico;

    @NotNull(message = "Base é obrigatória")
    private EnderecoDTO base;

    @NotNull(message = "Origem é obrigatória")
    private EnderecoDTO origem;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class ClienteDTO {
        @NotNull(message = "ID do cliente é obrigatório")
        private Long id;

        @NotBlank(message = "Nome do cliente é obrigatório")
        @Size(max = 200, message = "Nome não pode ter mais de 200 caracteres")
        private String nome;

        @NotNull(message = "Documento fiscal é obrigatório")
        private DocFiscalDTO docfiscal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class DocFiscalDTO {
        @NotBlank(message = "Tipo do documento é obrigatório")
        private String tipo;

        @NotBlank(message = "Número do documento é obrigatório")
        private String numero;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class VeiculoDTO {
        @NotBlank(message = "Placa é obrigatória")
        @Size(max = 10, message = "Placa não pode ter mais de 10 caracteres")
        private String placa;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class ServicoDTO {
        @NotNull(message = "ID do serviço é obrigatório")
        private Long id;

        @NotBlank(message = "Nome do serviço é obrigatório")
        @Size(max = 100, message = "Nome do serviço não pode ter mais de 100 caracteres")
        private String nome;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class EnderecoDTO {
        @NotBlank(message = "Tipo do endereço é obrigatório")
        @Size(max = 20, message = "Tipo não pode ter mais de 20 caracteres")
        private String tipo;

        @NotBlank(message = "Logradouro é obrigatório")
        @Size(max = 200, message = "Logradouro não pode ter mais de 200 caracteres")
        private String logradouro;

        @NotBlank(message = "Número é obrigatório")
        @Size(max = 10, message = "Número não pode ter mais de 10 caracteres")
        private String numero;

        @Size(max = 100, message = "Complemento não pode ter mais de 100 caracteres")
        private String complemento;

        @NotBlank(message = "Bairro é obrigatório")
        @Size(max = 100, message = "Bairro não pode ter mais de 100 caracteres")
        private String bairro;

        @NotBlank(message = "Cidade é obrigatória")
        @Size(max = 100, message = "Cidade não pode ter mais de 100 caracteres")
        private String cidade;

        @NotBlank(message = "Estado é obrigatório")
        @Size(min = 2, max = 2, message = "Estado deve ter 2 caracteres (UF)")
        private String estado;

        @NotBlank(message = "CEP é obrigatório")
        @Size(min = 8, max = 8, message = "CEP deve ter 8 dígitos")
        private String cep;
    }
}
