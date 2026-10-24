package org.example.idojaras.ui;

import org.example.idojaras.domain.Idojaras;
import org.example.idojaras.domain.IdojarasKivetel;
import org.example.idojaras.domain.IdojarasSzolgaltatas;
import org.example.idojaras.domain.Varos;
import org.example.idojaras.mock.MockIdojaras;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Presenter tests: the service and the view are Mockito mocks, and background work runs
 * synchronously, so no Swing component or thread is involved.
 */
@ExtendWith(MockitoExtension.class)
class IdojarasPresenterTest {

    private static final Idojaras MINTA = new Idojaras(12.5, 8.3, LocalDateTime.of(2026, 10, 24, 12, 0));

    @Mock
    private IdojarasSzolgaltatas szolgaltatas;
    @Mock
    private IdojarasNezet nezet;

    /** Runs the task immediately on the calling thread. */
    private static final class SzinkronFuttato implements HatterFuttato {
        @Override
        public <T> void futtat(Callable<T> feladat, Consumer<T> siker, Consumer<Throwable> hiba) {
            T eredmeny;
            try {
                eredmeny = feladat.call();
            } catch (Exception e) {
                hiba.accept(e);
                return;
            }
            siker.accept(eredmeny);
        }
    }

    private IdojarasPresenter presenter(IdojarasSzolgaltatas szolgaltatas) {
        return new IdojarasPresenter(szolgaltatas, nezet, new SzinkronFuttato());
    }

    @Test
    void sikerEseteneFrissitiANezetetEsVisszaengedelyeziAGombot() throws IdojarasKivetel {
        when(szolgaltatas.aktualis(46.54, 24.56)).thenReturn(MINTA);

        presenter(szolgaltatas).frissit(Varos.MAROSVASARHELY);

        InOrder sorrend = inOrder(nezet);
        sorrend.verify(nezet).setFrissitesEngedelyezve(false);
        sorrend.verify(nezet).setFrissitesEngedelyezve(true);
        sorrend.verify(nezet).eredmenytMutat("Târgu Mureș", "12.5 °C", "8.3 km/h", "2026-10-24 12:00");
        verify(nezet, never()).hibatMutat(anyString());
        verifyNoMoreInteractions(nezet);
    }

    @Test
    void hibaEseteneHibatJelezEsVisszaengedelyeziAGombot() throws IdojarasKivetel {
        when(szolgaltatas.aktualis(46.54, 24.56))
                .thenThrow(new IdojarasKivetel("Could not reach Open-Meteo: timed out"));

        presenter(szolgaltatas).frissit(Varos.MAROSVASARHELY);

        InOrder sorrend = inOrder(nezet);
        sorrend.verify(nezet).setFrissitesEngedelyezve(false);
        sorrend.verify(nezet).setFrissitesEngedelyezve(true);
        sorrend.verify(nezet).hibatMutat(
                "Could not get the weather for Târgu Mureș.\nCould not reach Open-Meteo: timed out");
        verify(nezet, never()).eredmenytMutat(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void hibaraAllitottMockIdojarassalIsHibatJelez() {
        MockIdojaras hibasSzolgaltatas = new MockIdojaras().hibatDob("Service down");

        presenter(hibasSzolgaltatas).frissit(Varos.BUDAPEST);

        verify(nezet).hibatMutat("Could not get the weather for Budapest.\nService down");
        verify(nezet).setFrissitesEngedelyezve(true);
        verify(nezet, never()).eredmenytMutat(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void varatlanKivetelEseteneIsHibatJelez() throws IdojarasKivetel {
        when(szolgaltatas.aktualis(46.54, 24.56)).thenThrow(new IllegalStateException("bug"));

        presenter(szolgaltatas).frissit(Varos.MAROSVASARHELY);

        verify(nezet).hibatMutat(contains("Unexpected error"));
        verify(nezet).setFrissitesEngedelyezve(true);
    }

    @Test
    void varosNelkulNemHivjaASzolgaltatast() {
        presenter(szolgaltatas).frissit(null);

        verify(nezet).hibatMutat("Please select a city.");
        verify(nezet, never()).setFrissitesEngedelyezve(anyBoolean());
        verifyNoInteractions(szolgaltatas);
    }

    @Captor
    private ArgumentCaptor<Callable<Idojaras>> feladat;
    @Captor
    private ArgumentCaptor<Consumer<Idojaras>> siker;
    @Captor
    private ArgumentCaptor<Consumer<Throwable>> hiba;

    @Test
    void aSzolgaltatastCsakAHatterfeladatbanHivja(@Mock HatterFuttato futtato) throws Exception {
        when(szolgaltatas.aktualis(46.54, 24.56)).thenReturn(MINTA);
        new IdojarasPresenter(szolgaltatas, nezet, futtato).frissit(Varos.MAROSVASARHELY);

        // While the "request" is pending: button disabled, service not yet called.
        verify(futtato).futtat(feladat.capture(), siker.capture(), hiba.capture());
        verify(nezet).setFrissitesEngedelyezve(false);
        verify(nezet, never()).setFrissitesEngedelyezve(true);
        verifyNoInteractions(szolgaltatas);

        // The background task is what calls the service.
        Idojaras eredmeny = feladat.getValue().call();
        assertSame(MINTA, eredmeny);
        verify(szolgaltatas).aktualis(46.54, 24.56);

        // Delivering the result updates the view.
        siker.getValue().accept(eredmeny);
        verify(nezet).setFrissitesEngedelyezve(true);
        verify(nezet).eredmenytMutat("Târgu Mureș", "12.5 °C", "8.3 km/h", "2026-10-24 12:00");
    }
}
