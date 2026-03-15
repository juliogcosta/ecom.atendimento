package com.ecom.atendimento.adapter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * DTO de requisição para ajustar um atendimento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AjustarRequest {

    @NotNull(message = "AggregateId é obrigatório")
    private UUID aggregateId;

    @Size(max = 500, message = "Descrição não pode ter mais de 500 caracteres")
    private String descricao;

    private PrestadorDTO prestador;

    private ServicoDTO servico;

    private EnderecoDTO origem;

    private EnderecoDTO destino;

    private List<ItemDTO> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class PrestadorDTO {
        @NotNull(message = "ID do prestador é obrigatório")
        private Long id;

        @NotBlank(message = "Nome do prestador é obrigatório")
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
        private String doc;
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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class ItemDTO {
        @NotBlank(message = "Nome do item é obrigatório")
        @Size(max = 200, message = "Nome não pode ter mais de 200 caracteres")
        private String nome;

        @NotBlank(message = "Unidade de medida é obrigatória")
        @Size(max = 20, message = "Unidade de medida não pode ter mais de 20 caracteres")
        private String unidadedemedida;

        @NotNull(message = "Preço unitário é obrigatório")
        private Integer precounitario;

        @NotNull(message = "Quantidade é obrigatória")
        private Integer quantidade;

        @Size(max = 500, message = "Observação não pode ter mais de 500 caracteres")
        private String observacao;
    }
}
