package com.ecom.atendimento.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Value Object representando um Item de Cobrança.
 */
@Getter
@EqualsAndHashCode
@ToString
public class Item implements Serializable {

    private static final long serialVersionUID = 5026706410982628656L;
	private final String nome;
    private final String unidadedemedida;  // km, hora, unidade, etc
    private final Integer precounitario;   // Em centavos
    private final Integer quantidade;
    private final String observacao;       // Opcional

    @JsonCreator
    public Item(
            @JsonProperty("nome") String nome,
            @JsonProperty("unidadedemedida") String unidadedemedida,
            @JsonProperty("precounitario") Integer precounitario,
            @JsonProperty("quantidade") Integer quantidade,
            @JsonProperty("observacao") String observacao) {

        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do item não pode ser vazio");
        }
        if (nome.length() > 200) {
            throw new IllegalArgumentException("Nome do item não pode ter mais de 200 caracteres");
        }
        if (unidadedemedida == null || unidadedemedida.trim().isEmpty()) {
            throw new IllegalArgumentException("Unidade de medida não pode ser vazia");
        }
        if (unidadedemedida.length() > 20) {
            throw new IllegalArgumentException("Unidade de medida não pode ter mais de 20 caracteres");
        }
        if (precounitario == null || precounitario < 0) {
            throw new IllegalArgumentException("Preço unitário não pode ser negativo");
        }
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero");
        }
        if (observacao != null && observacao.length() > 500) {
            throw new IllegalArgumentException("Observação não pode ter mais de 500 caracteres");
        }

        this.nome = nome.trim();
        this.unidadedemedida = unidadedemedida.trim();
        this.precounitario = precounitario;
        this.quantidade = quantidade;
        this.observacao = observacao != null ? observacao.trim() : null;
    }

    /**
     * Calcula o valor total do item (preço unitário * quantidade).
     * @return Valor total em centavos
     */
    public Integer getValorTotal() {
        return precounitario * quantidade;
    }
}
