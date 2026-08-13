package com.technical.aemet.domain.port.out;

import com.technical.aemet.domain.model.Forecast;
import com.technical.aemet.domain.model.TemperatureUnit;
import java.time.LocalDate;

public interface ForecastProvider {
    Forecast getForecast(String municipalityCode, LocalDate date, TemperatureUnit unit);
}
