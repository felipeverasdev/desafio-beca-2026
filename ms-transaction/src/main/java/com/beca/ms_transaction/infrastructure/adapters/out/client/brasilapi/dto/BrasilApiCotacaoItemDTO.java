package com.beca.ms_transaction.infrastructure.adapters.out.client.brasilapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record BrasilApiCotacaoItemDTO(
        @JsonProperty("cotacao_compra") BigDecimal cotacaoCompra,
        @JsonProperty("cotacao_venda") BigDecimal cotacaoVenda,
        @JsonProperty("data_hora_cotacao") String dataHoraCotacao,
        @JsonProperty("tipo_boletim") String tipoBoletim
) {
}

