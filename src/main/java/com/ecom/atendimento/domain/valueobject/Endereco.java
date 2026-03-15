package com.ecom.atendimento.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Value Object representando um Endereço.
 */
@Getter
@EqualsAndHashCode
@ToString
public class Endereco implements Serializable {

    private static final long serialVersionUID = 8154503746461507571L;
	private final String tipo;         // RESIDENCIAL, COMERCIAL, etc
    private final String logradouro;
    private final String numero;
    private final String complemento;  // Opcional
    private final String bairro;
    private final String cidade;
    private final String estado;       // UF
    private final String cep;          // Sem hífen

    @JsonCreator
    public Endereco(
            @JsonProperty("tipo") String tipo,
            @JsonProperty("logradouro") String logradouro,
            @JsonProperty("numero") String numero,
            @JsonProperty("complemento") String complemento,
            @JsonProperty("bairro") String bairro,
            @JsonProperty("cidade") String cidade,
            @JsonProperty("estado") String estado,
            @JsonProperty("cep") String cep) {

        if (tipo == null || tipo.trim().isEmpty()) {
            throw new IllegalArgumentException("Tipo do endereço não pode ser vazio");
        }
        if (tipo.length() > 20) {
            throw new IllegalArgumentException("Tipo do endereço não pode ter mais de 20 caracteres");
        }
        if (logradouro == null || logradouro.trim().isEmpty()) {
            throw new IllegalArgumentException("Logradouro não pode ser vazio");
        }
        if (logradouro.length() > 200) {
            throw new IllegalArgumentException("Logradouro não pode ter mais de 200 caracteres");
        }
        if (numero == null || numero.trim().isEmpty()) {
            throw new IllegalArgumentException("Número não pode ser vazio");
        }
        if (numero.length() > 10) {
            throw new IllegalArgumentException("Número não pode ter mais de 10 caracteres");
        }
        if (complemento != null && complemento.length() > 100) {
            throw new IllegalArgumentException("Complemento não pode ter mais de 100 caracteres");
        }
        if (bairro == null || bairro.trim().isEmpty()) {
            throw new IllegalArgumentException("Bairro não pode ser vazio");
        }
        if (bairro.length() > 100) {
            throw new IllegalArgumentException("Bairro não pode ter mais de 100 caracteres");
        }
        if (cidade == null || cidade.trim().isEmpty()) {
            throw new IllegalArgumentException("Cidade não pode ser vazia");
        }
        if (cidade.length() > 100) {
            throw new IllegalArgumentException("Cidade não pode ter mais de 100 caracteres");
        }
        if (estado == null || estado.trim().isEmpty()) {
            throw new IllegalArgumentException("Estado não pode ser vazio");
        }
        if (estado.length() != 2) {
            throw new IllegalArgumentException("Estado deve ter exatamente 2 caracteres (UF)");
        }
        if (cep == null || cep.trim().isEmpty()) {
            throw new IllegalArgumentException("CEP não pode ser vazio");
        }

        this.tipo = tipo.trim().toUpperCase();
        this.logradouro = logradouro.trim();
        this.numero = numero.trim();
        this.complemento = complemento != null ? complemento.trim() : null;
        this.bairro = bairro.trim();
        this.cidade = cidade.trim();
        this.estado = estado.trim().toUpperCase();
        this.cep = cep; // Remove formatação
    }
}
