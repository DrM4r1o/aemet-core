package com.technical.aemet.infrastructure.adapter.out.aemet.dto;

public record AemetApiResponse(String descripcion, Integer estado, String datos, String metadatos) {
    public boolean hasDataUrl() {
        return datos != null && !datos.isBlank();
    }
}
