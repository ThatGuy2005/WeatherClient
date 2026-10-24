package org.example.idojaras.ui;

import javax.swing.SwingWorker;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * {@link HatterFuttato} based on {@link SwingWorker}: the task runs on a worker thread, and
 * the callbacks run on the event dispatch thread in {@link SwingWorker#done()}.
 */
public final class SwingWorkerFuttato implements HatterFuttato {

    @Override
    public <T> void futtat(Callable<T> feladat, Consumer<T> siker, Consumer<Throwable> hiba) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return feladat.call();
            }

            @Override
            protected void done() {
                T eredmeny;
                try {
                    eredmeny = get();
                } catch (ExecutionException e) {
                    hiba.accept(e.getCause());
                    return;
                } catch (InterruptedException | CancellationException e) {
                    hiba.accept(e);
                    return;
                }
                siker.accept(eredmeny);
            }
        }.execute();
    }
}
