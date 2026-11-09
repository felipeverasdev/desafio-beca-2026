package com.beca.ms_transaction.infrastructure.adapters.out.client.brasilapi.dto;

import java.util.List;

public record BrasilApiCotacaoResponseDTO(
        String moeda,
        String data,
        List<BrasilApiCotacaoItemDTO> cotacoes
) {
}

