package org.example.idojaras.domain;

import java.util.List;
import java.util.Objects;

/**
 * A named location that can be selected in the UI.
 *
 * @param nev display name
 * @param lat latitude in degrees
 * @param lon longitude in degrees
 */
public record Varos(String nev, double lat, double lon) {

    public static final Varos MAROSVASARHELY = new Varos("Târgu Mureș", 46.54, 24.56);
    public static final Varos KOLOZSVAR = new Varos("Cluj-Napoca", 46.77, 23.60);
    public static final Varos BRASSO = new Varos("Brașov", 45.65, 25.61);
    public static final Varos BUKAREST = new Varos("București", 44.43, 26.10);
    public static final Varos BUDAPEST = new Varos("Budapest", 47.50, 19.04);
    public static final Varos BECS = new Varos("Wien", 48.21, 16.37);

    /** The cities offered by the city selector, in display order. */
    public static final List<Varos> ALAPERTELMEZETT_VAROSOK =
            List.of(MAROSVASARHELY, KOLOZSVAR, BRASSO, BUKAREST, BUDAPEST, BECS);

    public Varos {
        Objects.requireNonNull(nev, "nev");
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Latitude out of range: " + lat);
        }
        if (lon < -180 || lon > 180) {
            throw new IllegalArgumentException("Longitude out of range: " + lon);
        }
    }

    /** Used by {@code JComboBox} to render the item. */
    @Override
    public String toString() {
        return nev;
    }
}
