package org.example.idojaras.domain;

/**
 * Source of current weather data.
 *
 * <p>This is the abstraction the UI and the presenter depend on. It says nothing about
 * where the data comes from (HTTP, a file, fixed test values), so implementations can be
 * swapped without touching the callers.
 */
public interface IdojarasSzolgaltatas {

    /**
     * Returns the current weather at the given coordinates.
     *
     * @param lat latitude in degrees
     * @param lon longitude in degrees
     * @throws IdojarasKivetel if the weather data cannot be obtained
     */
    Idojaras aktualis(double lat, double lon) throws IdojarasKivetel;
}
