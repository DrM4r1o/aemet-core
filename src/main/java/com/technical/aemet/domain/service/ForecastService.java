package com.technical.aemet.domain.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.technical.aemet.domain.model.Forecast;
import com.technical.aemet.domain.model.TemperatureUnit;
import com.technical.aemet.domain.port.in.GetForecastUseCase;
import com.technical.aemet.domain.port.out.ForecastProvider;
import com.technical.aemet.application.exception.InvalidProviderResponseException;
import com.technical.aemet.application.exception.ProviderUnavailableException;
import java.time.LocalDate;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ForecastService implements GetForecastUseCase {
    private final ForecastProvider provider;
    private final Cache<String, Forecast> cache;
    private final Cache<String, Forecast> lastKnown;
    private final Clock clock;

    public ForecastService(ForecastProvider provider,
            @Qualifier("forecastCache") Cache<String, Forecast> cache,
            @Qualifier("forecastFallbackCache") Cache<String, Forecast> lastKnown,
            Clock clock) {
        this.provider = provider;
        this.cache = cache;
        this.lastKnown = lastKnown;
        this.clock = clock;
    }

    @Override
    public Forecast get(String municipalityCode, TemperatureUnit unit) {
        var date = LocalDate.now(clock.withZone(ZoneId.of("Europe/Madrid"))).plusDays(1);
        var key = municipalityCode + ":" + date + ":" + unit;
        var cached = cache.getIfPresent(key);
        if (cached != null)
            return cached.withStatus(com.technical.aemet.domain.model.DataStatus.FRESH);
        try {
            var fresh = provider.getForecast(municipalityCode, date, unit);
            cache.put(key, fresh);
            lastKnown.put(municipalityCode + ":" + unit, fresh);
            return fresh;
        } catch (ProviderUnavailableException error) {
            var stale = lastKnown.getIfPresent(municipalityCode + ":" + unit);
            if (stale != null)
                return stale.withStatus(com.technical.aemet.domain.model.DataStatus.STALE);
            throw error;
        } catch (InvalidProviderResponseException error) {
            throw error;
        } catch (RuntimeException error) {
            var stale = lastKnown.getIfPresent(municipalityCode + ":" + unit);
            if (stale != null)
                return stale.withStatus(com.technical.aemet.domain.model.DataStatus.STALE);
            throw new ProviderUnavailableException("FORECAST_PROVIDER_UNAVAILABLE", error);
        }
    }
}
