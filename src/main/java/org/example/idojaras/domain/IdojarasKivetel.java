package org.example.idojaras.domain;

/**
 * Thrown when weather data cannot be obtained, whatever the underlying cause
 * (network failure, HTTP error, malformed response, ...). Callers only ever see this type,
 * never transport-specific exceptions.
 */
public class IdojarasKivetel extends Exception {

    public IdojarasKivetel(String uzenet) {
        super(uzenet);
    }

    public IdojarasKivetel(String uzenet, Throwable ok) {
        super(uzenet, ok);
    }
}
