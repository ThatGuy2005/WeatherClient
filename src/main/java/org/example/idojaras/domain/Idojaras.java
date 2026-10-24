package org.example.idojaras.domain;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A snapshot of the current weather at one location.
 *
 * @param homerseklet  air temperature 2 m above ground, in °C
 * @param szelsebesseg wind speed 10 m above ground, in km/h
 * @param idopont      local time of the measurement at the location
 */
public record Idojaras(double homerseklet, double szelsebesseg, LocalDateTime idopont) {

    public Idojaras {
        Objects.requireNonNull(idopont, "idopont");
    }
}
