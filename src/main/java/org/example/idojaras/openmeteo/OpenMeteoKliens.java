package org.example.idojaras.openmeteo;

import org.example.idojaras.domain.Idojaras;
import org.example.idojaras.domain.IdojarasKivetel;
import org.example.idojaras.domain.IdojarasSzolgaltatas;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * {@link IdojarasSzolgaltatas} backed by the free, keyless Open-Meteo API.
 *
 * <p>All failures (network, timeout, non-200 status, malformed body) are reported as
 * {@link IdojarasKivetel}; no {@code java.net} exception leaks to the caller.
 */
public final class OpenMeteoKliens implements IdojarasSzolgaltatas {

    private static final URI ALAP_URI = URI.create("https://api.open-meteo.com/v1/forecast");
    private static final Duration IDOKORLAT = Duration.ofSeconds(10);

    private final HttpClient http;
    private final URI alapUri;
    private final OpenMeteoParser parser = new OpenMeteoParser();

    public OpenMeteoKliens() {
        this(HttpClient.newBuilder().connectTimeout(IDOKORLAT).build(), ALAP_URI);
    }

    /** For tests: lets them point the client at a local HTTP server. */
    OpenMeteoKliens(HttpClient http, URI alapUri) {
        this.http = http;
        this.alapUri = alapUri;
    }

    @Override
    public Idojaras aktualis(double lat, double lon) throws IdojarasKivetel {
        HttpRequest keres = HttpRequest.newBuilder(keresUri(alapUri, lat, lon))
                .timeout(IDOKORLAT)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> valasz;
        try {
            valasz = http.send(keres, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new IdojarasKivetel("Could not reach Open-Meteo: " + leiras(e), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IdojarasKivetel("The weather request was interrupted.", e);
        }

        if (valasz.statusCode() != 200) {
            String ok = parser.hibaOka(valasz.body());
            throw new IdojarasKivetel("Open-Meteo returned HTTP " + valasz.statusCode()
                    + (ok != null ? ": " + ok : "."));
        }
        return parser.feldolgoz(valasz.body());
    }

    /**
     * Builds the request URL. {@code timezone=auto} makes Open-Meteo report the time in the
     * location's local time zone. {@code Double.toString} is locale-independent, so the
     * decimal separator is always a dot.
     */
    static URI keresUri(URI alapUri, double lat, double lon) {
        return URI.create(alapUri + "?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,wind_speed_10m&timezone=auto");
    }

    private static String leiras(IOException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
