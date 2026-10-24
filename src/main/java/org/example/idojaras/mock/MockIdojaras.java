package org.example.idojaras.mock;

import org.example.idojaras.domain.Idojaras;
import org.example.idojaras.domain.IdojarasKivetel;
import org.example.idojaras.domain.IdojarasSzolgaltatas;
import org.example.idojaras.domain.Varos;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Offline {@link IdojarasSzolgaltatas} returning configurable, fixed data.
 *
 * <p>Lookup order for {@link #aktualis}: if an error is configured it is thrown; otherwise the
 * value set for the exact coordinates is returned; otherwise the default value; if there is no
 * default either, an {@link IdojarasKivetel} is thrown.
 *
 * <p>Configuration methods return {@code this} so they can be chained. The class is
 * thread-safe, because the UI calls it from a background thread.
 */
public final class MockIdojaras implements IdojarasSzolgaltatas {

    public static final Idojaras ALAPERTELMEZETT =
            new Idojaras(20.0, 10.0, LocalDateTime.of(2026, 1, 1, 12, 0));

    private record Hely(double lat, double lon) {
    }

    private final Map<Hely, Idojaras> helyenkent = new ConcurrentHashMap<>();
    private volatile Idojaras alapertelmezett;
    private volatile String hibauzenet;
    private volatile Duration kesleltetes = Duration.ZERO;
    private final AtomicInteger hivasokSzama = new AtomicInteger();

    /** Returns {@link #ALAPERTELMEZETT} for every location. */
    public MockIdojaras() {
        this(ALAPERTELMEZETT);
    }

    /**
     * @param alapertelmezett returned for locations without a specific value; {@code null}
     *                        makes unknown locations throw {@link IdojarasKivetel}
     */
    public MockIdojaras(Idojaras alapertelmezett) {
        this.alapertelmezett = alapertelmezett;
    }

    /** Sets the value returned for the exact coordinates. */
    public MockIdojaras beallit(double lat, double lon, Idojaras idojaras) {
        helyenkent.put(new Hely(lat, lon), Objects.requireNonNull(idojaras, "idojaras"));
        return this;
    }

    /** Sets the value returned for the city's coordinates. */
    public MockIdojaras beallit(Varos varos, Idojaras idojaras) {
        return beallit(varos.lat(), varos.lon(), idojaras);
    }

    /** Sets the value for locations without a specific value; {@code null} means "throw". */
    public MockIdojaras setAlapertelmezett(Idojaras alapertelmezett) {
        this.alapertelmezett = alapertelmezett;
        return this;
    }

    /** Makes every subsequent call throw an {@link IdojarasKivetel} with the given message. */
    public MockIdojaras hibatDob(String uzenet) {
        this.hibauzenet = Objects.requireNonNull(uzenet, "uzenet");
        return this;
    }

    /** Undoes {@link #hibatDob}: calls return data again. */
    public MockIdojaras hibaKikapcsolasa() {
        this.hibauzenet = null;
        return this;
    }

    /** Simulates network latency, so the UI's busy state can be observed. */
    public MockIdojaras setKesleltetes(Duration kesleltetes) {
        if (kesleltetes.isNegative()) {
            throw new IllegalArgumentException("Delay must not be negative: " + kesleltetes);
        }
        this.kesleltetes = kesleltetes;
        return this;
    }

    /** Number of {@link #aktualis} calls so far, including failed ones. */
    public int getHivasokSzama() {
        return hivasokSzama.get();
    }

    @Override
    public Idojaras aktualis(double lat, double lon) throws IdojarasKivetel {
        hivasokSzama.incrementAndGet();
        var kesleltetes = this.kesleltetes;
        if (!kesleltetes.isZero()) {
            try {
                Thread.sleep(kesleltetes);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IdojarasKivetel("The weather request was interrupted.", e);
            }
        }

        var hibauzenet = this.hibauzenet;
        if (hibauzenet != null) {
            throw new IdojarasKivetel(hibauzenet);
        }

        Idojaras idojaras = helyenkent.getOrDefault(new Hely(lat, lon), alapertelmezett);
        if (idojaras == null) {
            throw new IdojarasKivetel("No mock weather data configured for " + lat + ", " + lon + ".");
        }
        return idojaras;
    }
}
