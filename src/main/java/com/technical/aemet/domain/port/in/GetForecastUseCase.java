package com.technical.aemet.domain.port.in;

import com.technical.aemet.domain.model.Forecast;
import com.technical.aemet.domain.model.TemperatureUnit;

public interface GetForecastUseCase {
    Forecast get(String municipalityCode, TemperatureUnit unit);
}
