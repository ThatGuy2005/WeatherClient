package org.example.idojaras.openmeteo;

import com.sun.net.httpserver.HttpServer;
import org.example.idojaras.domain.Idojaras;
import org.example.idojaras.domain.IdojarasKivetel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the HTTP client against a local in-process server, so no internet access is needed.
 */
class OpenMeteoKliensTest {

    private HttpServer szerver;
    private volatile String utolsoLekerdezes;

    @BeforeEach
    void szerverInditasa() throws IOException {
        szerver = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        szerver.start();
    }

    @AfterEach
    void szerverLeallitasa() {
        szerver.stop(0);
    }

    private void valaszol(int statusz, String torzs) {
        szerver.createContext("/v1/forecast", csere -> {
            utolsoLekerdezes = csere.getRequestURI().getRawQuery();
            byte[] bajtok = torzs.getBytes(StandardCharsets.UTF_8);
            csere.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            csere.sendResponseHeaders(statusz, bajtok.length == 0 ? -1 : bajtok.length);
            try (OutputStream ki = csere.getResponseBody()) {
                ki.write(bajtok);
            }
        });
    }

    private OpenMeteoKliens kliens() {
        URI alap = URI.create("http://127.0.0.1:" + szerver.getAddress().getPort() + "/v1/forecast");
        return new OpenMeteoKliens(HttpClient.newHttpClient(), alap);
    }

    @Test
    void sikeresValasztFeldolgozEsHelyesParametereketKuld() throws IdojarasKivetel {
        valaszol(200, OpenMeteoParserTest.ERVENYES_VALASZ);

        Idojaras idojaras = kliens().aktualis(46.54, 24.56);

        assertEquals(10.7, idojaras.homerseklet());
        assertEquals("latitude=46.54&longitude=24.56&current=temperature_2m,wind_speed_10m&timezone=auto",
                utolsoLekerdezes);
    }

    @Test
    void hibaStatusznalAzOpenMeteoOkatIsKozli() {
        valaszol(400, "{\"error\":true,\"reason\":\"Latitude must be in range of -90 to 90°. Given: 146.54.\"}");

        IdojarasKivetel kivetel = assertThrows(IdojarasKivetel.class, () -> kliens().aktualis(146.54, 24.56));

        assertEquals("Open-Meteo returned HTTP 400: Latitude must be in range of -90 to 90°. Given: 146.54.",
                kivetel.getMessage());
    }

    @Test
    void szerverhibanalIdojarasKiveteltDob() {
        valaszol(503, "<html>Service Unavailable</html>");

        IdojarasKivetel kivetel = assertThrows(IdojarasKivetel.class, () -> kliens().aktualis(46.54, 24.56));

        assertEquals("Open-Meteo returned HTTP 503.", kivetel.getMessage());
    }

    @Test
    void hibasJsonTorzsnelIdojarasKiveteltDob() {
        valaszol(200, "{\"current\": {\"temperature_2m\": ");

        assertThrows(IdojarasKivetel.class, () -> kliens().aktualis(46.54, 24.56));
    }

    @Test
    void halozatiHibatIdojarasKivetelbeCsomagol() {
        OpenMeteoKliens kliens = kliens();
        szerver.stop(0); // nothing listens on the port any more -> connection refused

        IdojarasKivetel kivetel = assertThrows(IdojarasKivetel.class, () -> kliens.aktualis(46.54, 24.56));

        assertInstanceOf(IOException.class, kivetel.getCause());
        assertTrue(kivetel.getMessage().startsWith("Could not reach Open-Meteo"), kivetel.getMessage());
    }
}
