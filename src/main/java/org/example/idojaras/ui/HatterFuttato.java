package org.example.idojaras.ui;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Runs a task in the background and hands its outcome back to the UI thread.
 *
 * <p>Production code uses {@link SwingWorkerFuttato}; tests can use a synchronous
 * implementation so the presenter can be tested without threads or a GUI.
 */
public interface HatterFuttato {

    /**
     * Runs {@code feladat} off the UI thread, then calls exactly one of {@code siker} (with the
     * result) or {@code hiba} (with the thrown exception) on the UI thread.
     */
    <T> void futtat(Callable<T> feladat, Consumer<T> siker, Consumer<Throwable> hiba);
}
