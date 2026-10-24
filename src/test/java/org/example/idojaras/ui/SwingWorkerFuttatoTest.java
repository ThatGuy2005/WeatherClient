package org.example.idojaras.ui;

import org.example.idojaras.domain.IdojarasKivetel;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the threading contract: task off the EDT, callbacks on the EDT. Runs headless. */
class SwingWorkerFuttatoTest {

    private final HatterFuttato futtato = new SwingWorkerFuttato();

    @Test
    void aFeladatHatterszalonAVisszahivasAzEdtnFut() throws InterruptedException {
        AtomicBoolean feladatEdtn = new AtomicBoolean(true);
        AtomicBoolean visszahivasEdtn = new AtomicBoolean(false);
        AtomicReference<Integer> eredmeny = new AtomicReference<>();
        AtomicReference<Throwable> hiba = new AtomicReference<>();
        CountDownLatch kesz = new CountDownLatch(1);

        futtato.futtat(
                () -> {
                    feladatEdtn.set(SwingUtilities.isEventDispatchThread());
                    return 42;
                },
                e -> {
                    visszahivasEdtn.set(SwingUtilities.isEventDispatchThread());
                    eredmeny.set(e);
                    kesz.countDown();
                },
                h -> {
                    hiba.set(h);
                    kesz.countDown();
                });

        assertTrue(kesz.await(5, TimeUnit.SECONDS));
        assertFalse(feladatEdtn.get(), "task must not run on the EDT");
        assertTrue(visszahivasEdtn.get(), "callback must run on the EDT");
        assertEquals(42, eredmeny.get());
        assertNull(hiba.get());
    }

    @Test
    void aFeladatKiveteletKicsomagolvaAdjaAt() throws InterruptedException {
        IdojarasKivetel dobott = new IdojarasKivetel("Service down");
        AtomicReference<Throwable> kapott = new AtomicReference<>();
        AtomicBoolean visszahivasEdtn = new AtomicBoolean(false);
        CountDownLatch kesz = new CountDownLatch(1);

        futtato.<String>futtat(
                () -> {
                    throw dobott;
                },
                e -> kesz.countDown(),
                h -> {
                    visszahivasEdtn.set(SwingUtilities.isEventDispatchThread());
                    kapott.set(h);
                    kesz.countDown();
                });

        assertTrue(kesz.await(5, TimeUnit.SECONDS));
        assertSame(dobott, kapott.get());
        assertTrue(visszahivasEdtn.get());
    }
}
