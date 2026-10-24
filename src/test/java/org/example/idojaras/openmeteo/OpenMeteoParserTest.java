package org.example.idojaras.openmeteo;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.idojaras.domain.Idojaras;
import org.example.idojaras.domain.IdojarasKivetel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenMeteoParserTest {

    /** A real response for Târgu Mureș, captured from the live API. */
    static final String ERVENYES_VALASZ = """
            {"latitude":46.54601,"longitude":24.56224,"generationtime_ms":0.04887580871582031,
             "utc_offset_seconds":10800,"timezone":"Europe/Bucharest","timezone_abbreviation":"GMT+3",
             "elevation":330.0,
             "current_units":{"time":"iso8601","interval":"seconds","temperature_2m":"°C","wind_speed_10m":"km/h"},
             "current":{"time":"2026-10-24T13:45","interval":900,"temperature_2m":10.7,"wind_speed_10m":1.0}}
            """;

    private final OpenMeteoParser parser = new OpenMeteoParser();

    @Test
    void ervenyesValaszbolKiolvassaAzAdatokat() throws IdojarasKivetel {
        Idojaras idojaras = parser.feldolgoz(ERVENYES_VALASZ);

        assertEquals(10.7, idojaras.homerseklet());
        assertEquals(1.0, idojaras.szelsebesseg());
        assertEquals(LocalDateTime.of(2026, 10, 24, 13, 45), idojaras.idopont());
    }

    @Test
    void negativHomersekletetEsEgeszSzamotIsElfogad() throws IdojarasKivetel {
        Idojaras idojaras = parser.feldolgoz("""
                {"current":{"time":"2026-01-15T07:00:00","temperature_2m":-12.3,"wind_speed_10m":25}}
                """);

        assertEquals(-12.3, idojaras.homerseklet());
        assertEquals(25.0, idojaras.szelsebesseg());
        assertEquals(LocalDateTime.of(2026, 1, 15, 7, 0), idojaras.idopont());
    }

    static Stream<Arguments> hibasValaszok() {
        return Stream.of(
                Arguments.of("empty body", ""),
                Arguments.of("whitespace only", "   \n"),
                Arguments.of("plain text", "Service Unavailable"),
                Arguments.of("HTML error page", "<html><body>502 Bad Gateway</body></html>"),
                Arguments.of("truncated JSON", "{\"current\":{\"time\":\"2026-10-24T13:45\",\"temperature_2m\":10."),
                Arguments.of("trailing garbage", "{\"current\":{}} oops"),
                Arguments.of("JSON array instead of object", "[1, 2, 3]"),
                Arguments.of("JSON null", "null"),
                Arguments.of("empty object", "{}"),
                Arguments.of("'current' is not an object", "{\"current\":\"sunny\"}"),
                Arguments.of("missing temperature",
                        "{\"current\":{\"time\":\"2026-10-24T13:45\",\"wind_speed_10m\":1.0}}"),
                Arguments.of("temperature is a string",
                        "{\"current\":{\"time\":\"2026-10-24T13:45\",\"temperature_2m\":\"warm\",\"wind_speed_10m\":1.0}}"),
                Arguments.of("missing wind speed",
                        "{\"current\":{\"time\":\"2026-10-24T13:45\",\"temperature_2m\":10.7}}"),
                Arguments.of("wind speed is null",
                        "{\"current\":{\"time\":\"2026-10-24T13:45\",\"temperature_2m\":10.7,\"wind_speed_10m\":null}}"),
                Arguments.of("missing time",
                        "{\"current\":{\"temperature_2m\":10.7,\"wind_speed_10m\":1.0}}"),
                Arguments.of("unparseable time",
                        "{\"current\":{\"time\":\"yesterday\",\"temperature_2m\":10.7,\"wind_speed_10m\":1.0}}"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("hibasValaszok")
    void hibasValaszraIdojarasKiveteltDob(String leiras, String json) {
        IdojarasKivetel kivetel = assertThrows(IdojarasKivetel.class, () -> parser.feldolgoz(json));
        assertTrue(kivetel.getMessage() != null && !kivetel.getMessage().isBlank());
    }

    @Test
    void nullBemenetreIdojarasKiveteltDob() {
        assertThrows(IdojarasKivetel.class, () -> parser.feldolgoz(null));
    }

    @Test
    void szintaktikaiHibanalMegorziAzEredetiKivetelt() {
        IdojarasKivetel kivetel = assertThrows(IdojarasKivetel.class, () -> parser.feldolgoz("{\"current\": "));

        assertInstanceOf(JsonProcessingException.class, kivetel.getCause());
        assertTrue(kivetel.getMessage().startsWith("Malformed JSON"), kivetel.getMessage());
    }

    @Test
    void openMeteoHibavalaszatAzOkkalJelzi() {
        String json = "{\"error\":true,\"reason\":\"Latitude must be in range of -90 to 90°. Given: 146.54.\"}";

        IdojarasKivetel kivetel = assertThrows(IdojarasKivetel.class, () -> parser.feldolgoz(json));

        assertEquals("Open-Meteo error: Latitude must be in range of -90 to 90°. Given: 146.54.", kivetel.getMessage());
        assertEquals("Latitude must be in range of -90 to 90°. Given: 146.54.", parser.hibaOka(json));
    }

    @Test
    void hibaOkaNullHaNincsOk() {
        assertNull(parser.hibaOka(ERVENYES_VALASZ));
        assertNull(parser.hibaOka("not json"));
        assertNull(parser.hibaOka(""));
    }
}
