package com.technical.aemet.domain.model;

import java.time.Instant;
import java.util.List;

public record Forecast(
    String municipalityCode,
    String municipalityName,
    double averageTemperature,
    TemperatureUnit temperatureUnit,
    List<PrecipitationProbability> precipitationProbability,
    DataStatus dataStatus,
    Instant retrievedAt
) {
    public Forecast withStatus(DataStatus status) {
        return new Forecast(municipalityCode, municipalityName, averageTemperature, temperatureUnit,
                precipitationProbability, status, retrievedAt);
    }
}
