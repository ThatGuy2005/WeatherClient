package org.example.idojaras.openmeteo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.example.idojaras.domain.Idojaras;
import org.example.idojaras.domain.IdojarasKivetel;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Turns an Open-Meteo {@code /v1/forecast} response body into an {@link Idojaras}.
 *
 * <p>Kept separate from the HTTP code so it can be tested with fixed JSON strings, without
 * network access. Expected shape (other fields are ignored):
 * <pre>{@code
 * {"current": {"time": "2026-10-24T13:45", "temperature_2m": 10.7, "wind_speed_10m": 1.0}}
 * }</pre>
 */
public final class OpenMeteoParser {

    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    /**
     * @throws IdojarasKivetel if the body is not valid JSON, is an Open-Meteo error response,
     *                         or lacks any of the required fields
     */
    public Idojaras feldolgoz(String json) throws IdojarasKivetel {
        if (json == null || json.isBlank()) {
            throw new IdojarasKivetel("Empty response from Open-Meteo.");
        }

        JsonNode gyoker;
        try {
            gyoker = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IdojarasKivetel("Malformed JSON from Open-Meteo: " + e.getOriginalMessage(), e);
        }
        if (gyoker == null || !gyoker.isObject()) {
            throw new IdojarasKivetel("Unexpected response from Open-Meteo: expected a JSON object.");
        }

        // Open-Meteo reports bad requests as {"error": true, "reason": "..."}.
        if (gyoker.path("error").asBoolean(false)) {
            throw new IdojarasKivetel("Open-Meteo error: " + gyoker.path("reason").asText("unknown reason"));
        }

        JsonNode aktualis = gyoker.get("current");
        if (aktualis == null || !aktualis.isObject()) {
            throw new IdojarasKivetel("Unexpected response from Open-Meteo: missing 'current' object.");
        }

        return new Idojaras(
                szam(aktualis, "temperature_2m"),
                szam(aktualis, "wind_speed_10m"),
                idopont(aktualis));
    }

    /**
     * Extracts the {@code reason} from an Open-Meteo error body such as
     * {@code {"error": true, "reason": "Latitude must be in range of -90 to 90°."}}.
     *
     * @return the reason, or {@code null} if the body is not such an error response
     */
    public String hibaOka(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode ok = mapper.readTree(json).path("reason");
            return ok.isTextual() ? ok.asText() : null;
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private static double szam(JsonNode aktualis, String mezo) throws IdojarasKivetel {
        JsonNode ertek = aktualis.get(mezo);
        if (ertek == null || !ertek.isNumber()) {
            throw new IdojarasKivetel("Unexpected response from Open-Meteo: 'current." + mezo
                    + "' is missing or not a number.");
        }
        return ertek.doubleValue();
    }

    private static LocalDateTime idopont(JsonNode aktualis) throws IdojarasKivetel {
        JsonNode ertek = aktualis.get("time");
        if (ertek == null || !ertek.isTextual()) {
            throw new IdojarasKivetel("Unexpected response from Open-Meteo: 'current.time' is missing.");
        }
        try {
            return LocalDateTime.parse(ertek.asText());
        } catch (DateTimeParseException e) {
            throw new IdojarasKivetel("Unexpected response from Open-Meteo: invalid time '"
                    + ertek.asText() + "'.", e);
        }
    }
}
