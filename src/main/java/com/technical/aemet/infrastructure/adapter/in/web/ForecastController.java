package com.technical.aemet.infrastructure.adapter.in.web;

import com.technical.aemet.domain.model.Forecast;
import com.technical.aemet.domain.model.TemperatureUnit;
import com.technical.aemet.domain.port.in.GetForecastUseCase;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/forecast")
public class ForecastController {
    private final GetForecastUseCase useCase;

    public ForecastController(GetForecastUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/{municipalityCode}")
    ForecastResponse get(@PathVariable String municipalityCode,
            @RequestParam(defaultValue = "G_CEL") TemperatureUnit unit) {
        var forecast = useCase.get(municipalityCode, unit);
        return ForecastResponse.from(forecast);
    }

    public record ForecastResponse(double averageTemperature, TemperatureUnit temperatureUnit,
            List<com.technical.aemet.domain.model.PrecipitationProbability> precipitationProbability,
            com.technical.aemet.domain.model.DataStatus dataStatus, Instant retrievedAt,
            String municipalityCode, String municipalityName) {
        static ForecastResponse from(Forecast f) {
            return new ForecastResponse(f.averageTemperature(), f.temperatureUnit(), f.precipitationProbability(),
                    f.dataStatus(), f.retrievedAt(), f.municipalityCode(), f.municipalityName());
        }
    }
}
