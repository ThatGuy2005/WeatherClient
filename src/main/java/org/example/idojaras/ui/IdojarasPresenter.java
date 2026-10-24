package org.example.idojaras.ui;

import org.example.idojaras.domain.Idojaras;
import org.example.idojaras.domain.IdojarasKivetel;
import org.example.idojaras.domain.IdojarasSzolgaltatas;
import org.example.idojaras.domain.Varos;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Mediates between the view and the weather service.
 *
 * <p>Depends only on abstractions ({@link IdojarasSzolgaltatas}, {@link IdojarasNezet},
 * {@link HatterFuttato}), so it can be unit-tested with mocks and no Swing at all.
 */
public final class IdojarasPresenter {

    private static final DateTimeFormatter IDO_FORMATUM = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final IdojarasSzolgaltatas szolgaltatas;
    private final IdojarasNezet nezet;
    private final HatterFuttato futtato;

    public IdojarasPresenter(IdojarasSzolgaltatas szolgaltatas, IdojarasNezet nezet, HatterFuttato futtato) {
        this.szolgaltatas = Objects.requireNonNull(szolgaltatas, "szolgaltatas");
        this.nezet = Objects.requireNonNull(nezet, "nezet");
        this.futtato = Objects.requireNonNull(futtato, "futtato");
    }

    /**
     * Fetches the weather for {@code varos} in the background. The Refresh button is disabled
     * until the request finishes, and re-enabled on both success and failure.
     */
    public void frissit(Varos varos) {
        if (varos == null) {
            nezet.hibatMutat("Please select a city.");
            return;
        }

        nezet.setFrissitesEngedelyezve(false);
        futtato.futtat(
                () -> szolgaltatas.aktualis(varos.lat(), varos.lon()),
                idojaras -> {
                    nezet.setFrissitesEngedelyezve(true);
                    megjelenit(varos, idojaras);
                },
                hiba -> {
                    nezet.setFrissitesEngedelyezve(true);
                    nezet.hibatMutat(hibauzenet(varos, hiba));
                });
    }

    private void megjelenit(Varos varos, Idojaras idojaras) {
        nezet.eredmenytMutat(
                varos.nev(),
                String.format(Locale.ROOT, "%.1f °C", idojaras.homerseklet()),
                String.format(Locale.ROOT, "%.1f km/h", idojaras.szelsebesseg()),
                IDO_FORMATUM.format(idojaras.idopont()));
    }

    private static String hibauzenet(Varos varos, Throwable hiba) {
        String ok = hiba instanceof IdojarasKivetel
                ? hiba.getMessage()
                : "Unexpected error: " + hiba;
        return "Could not get the weather for " + varos.nev() + ".\n" + ok;
    }
}
