package com.technical.aemet.infrastructure.adapter.out.aemet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.technical.aemet.domain.model.Forecast;
import com.technical.aemet.domain.model.Municipality;
import com.technical.aemet.domain.model.PrecipitationProbability;
import com.technical.aemet.domain.model.TemperatureUnit;
import com.technical.aemet.domain.port.out.ForecastProvider;
import com.technical.aemet.domain.port.out.MunicipalityProvider;
import com.technical.aemet.application.exception.InvalidProviderResponseException;
import com.technical.aemet.application.exception.ProviderUnavailableException;
import com.technical.aemet.application.exception.RateLimitException;
import com.technical.aemet.infrastructure.adapter.out.aemet.dto.AemetApiResponse;
import java.time.LocalDate;
import java.time.Instant;
import java.time.Duration;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AemetClientAdapter implements ForecastProvider, MunicipalityProvider {
    private static final Pattern MUNICIPALITY_ID = Pattern.compile("^id(\\d+)$", Pattern.CASE_INSENSITIVE);
    private final RestClient client;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String allowedDataHost;

    public AemetClientAdapter(RestClient.Builder builder, ObjectMapper mapper,
            @Value("${aemet.api-key:}") String apiKey,
            @Value("${aemet.base-url:https://opendata.aemet.es/opendata}") String baseUrl,
            @Value("${aemet.timeout:5s}") Duration timeout) {
        var httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.client = builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.allowedDataHost = URI.create(baseUrl).getHost();
    }

    @Override
    public List<Municipality> loadMunicipalities() {
        try {
            var response = request("/api/maestro/municipios");
            var root = resolveDataUrl(response);
            var result = new ArrayList<Municipality>();
            root.forEach(node -> {
                var code = firstText(node, "id", "codigo", "id_old");
                var name = firstText(node, "nombre", "name");
                var province = firstText(node, "provincia", "province");
                if (code != null && !code.isBlank() && name != null && !name.isBlank()) {
                    result.add(new Municipality(code, name, province == null ? "" : province));
                }
            });
            if (result.isEmpty())
                throw new IllegalStateException("AEMET municipality catalogue is empty");
            return result;
        } catch (HttpClientErrorException.TooManyRequests error) {
            throw new RateLimitException(error);
        } catch (InvalidProviderResponseException error) {
            throw error;
        } catch (Exception error) {
            throw new ProviderUnavailableException("MUNICIPALITY_PROVIDER_UNAVAILABLE", error);
        }
    }

    @Override
    public Forecast getForecast(String municipalityCode, LocalDate date, TemperatureUnit unit) {
        try {
            var aemetCode = numericMunicipalityCode(municipalityCode);
            var response = request("/api/prediccion/especifica/municipio/diaria/" + aemetCode);
            var dataUrl = dataUrl(response, "forecast");
            var forecastRoot = readJson(dataUrl, this::decodeBodyUtf8);
            var city = forecastRoot.isArray() ? forecastRoot.get(0) : forecastRoot;
            var day = findDay(city.path("prediccion").path("dia"), date);
            var temperature = day.path("temperatura");
            var maximum = temperature.path("maxima").asDouble(Double.NaN);
            var minimum = temperature.path("minima").asDouble(Double.NaN);
            var average = (maximum + minimum) / 2;

            if (Double.isNaN(maximum) || Double.isNaN(minimum))
                throw new IllegalStateException("AEMET temperature missing");

            if (unit == TemperatureUnit.G_FAH)
                average = average * 9 / 5 + 32;

            var probabilities = new ArrayList<PrecipitationProbability>();

            day.path("probPrecipitacion").forEach(node -> probabilities.add(
                    new PrecipitationProbability(
                            node.path("value").asInt(0), text(node, "periodo"))));
            return new Forecast(
                    municipalityCode,
                    text(city, "nombre"), average, unit, probabilities,
                    com.technical.aemet.domain.model.DataStatus.FRESH,
                    Instant.now());
        } catch (ProviderUnavailableException error) {
            throw error;
        } catch (HttpClientErrorException.TooManyRequests error) {
            throw new RateLimitException(error);
        } catch (InvalidProviderResponseException error) {
            throw error;
        } catch (Exception error) {
            throw new ProviderUnavailableException("FORECAST_PROVIDER_UNAVAILABLE", error);
        }
    }

    private JsonNode findDay(JsonNode days, LocalDate target) {
        for (JsonNode day : days)
            if (target.toString().equals(text(day, "fecha")))
                return day;
        return days.isArray() && days.size() > 1 ? days.get(1) : days.path(0);
    }

    private String text(JsonNode node, String field) {
        var value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            var value = text(node, field);
            if (value != null && !value.isBlank())
                return value;
        }
        return null;
    }

    private JsonNode resolveDataUrl(AemetApiResponse response) throws Exception {
        var dataUrl = dataUrl(response, "municipality catalogue");
        return readJson(dataUrl, this::decodeBodyIso);
    }

    private JsonNode readJson(String url, Function<byte[], String> decoder) throws Exception {
        try {
            var dataUri = URI.create(url);
            if (!"https".equalsIgnoreCase(dataUri.getScheme())
                    || allowedDataHost == null
                    || !allowedDataHost.equalsIgnoreCase(dataUri.getHost())) {
                throw new IllegalStateException("AEMET returned an invalid data URL");
            }
            var body = client.get().uri(dataUri)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve().body(byte[].class);

            return mapper.readTree(decoder.apply(body));
        } catch (HttpServerErrorException error) {
            throw classifyServerError(error);
        } catch (HttpClientErrorException.TooManyRequests error) {
            throw new RateLimitException(error);
        }
    }

    private String decodeBodyIso(byte[] body) {
        return new String(body, StandardCharsets.ISO_8859_1);
    }

    private String decodeBodyUtf8(byte[] body) {
        return new String(body, StandardCharsets.UTF_8);
    }

    private String dataUrl(AemetApiResponse response, String resource) {
        validateResponse(response);
        if (response == null || !response.hasDataUrl()) {
            var status = response == null ? "unknown" : String.valueOf(response.estado());
            var description = response == null ? "empty response" : response.descripcion();
            throw new IllegalStateException("AEMET returned no data URL for " + resource
                    + " (estado=" + status + ", descripcion=" + description + ")");
        }
        return response.datos();
    }

    private AemetApiResponse request(String uri) {
        try {
            var response = client.get().uri(uri)
                    .header("api_key", apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Accept-Charset", StandardCharsets.UTF_8.name())
                    .retrieve().body(AemetApiResponse.class);
            validateResponse(response);
            return response;
        } catch (HttpServerErrorException error) {
            throw classifyServerError(error);
        }
    }

    private RuntimeException classifyServerError(HttpServerErrorException error) {
        var body = error.getResponseBodyAsString();
        if (body.contains("429") || body.contains("Too Many Requests"))
            return new RateLimitException(error);
        return error;
    }

    private void validateResponse(AemetApiResponse response) {
        if (response != null && response.estado() != null && response.estado() >= 400) {
            throw new InvalidProviderResponseException(response.estado(), response.descripcion());
        }
    }

    private String numericMunicipalityCode(String municipalityCode) {
        if (municipalityCode == null || municipalityCode.isBlank()) {
            throw new IllegalArgumentException("Municipality code is required");
        }
        var matcher = MUNICIPALITY_ID.matcher(municipalityCode.trim());
        return matcher.matches() ? matcher.group(1) : municipalityCode.trim();
    }
}
