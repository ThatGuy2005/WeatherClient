package org.example.idojaras;

import org.example.idojaras.domain.Idojaras;
import org.example.idojaras.domain.IdojarasSzolgaltatas;
import org.example.idojaras.domain.Varos;
import org.example.idojaras.mock.MockIdojaras;
import org.example.idojaras.openmeteo.OpenMeteoKliens;
import org.example.idojaras.ui.IdojarasAblak;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Composition root: the only place that knows the concrete {@link IdojarasSzolgaltatas}
 * implementations.
 *
 * <ul>
 *   <li>no flag: real data from Open-Meteo ({@link OpenMeteoKliens})</li>
 *   <li>{@code --mock}: fixed offline data ({@link MockIdojaras})</li>
 *   <li>{@code --mock-error}: offline mock that always fails, to show the error dialog</li>
 * </ul>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        List<String> kapcsolok = List.of(args);
        boolean hibaMod = kapcsolok.contains("--mock-error");
        boolean mockMod = hibaMod || kapcsolok.contains("--mock");

        IdojarasSzolgaltatas szolgaltatas;
        String cim;
        if (mockMod) {
            szolgaltatas = demoMock(hibaMod);
            cim = hibaMod ? "Weather (mock, failing)" : "Weather (mock)";
        } else {
            szolgaltatas = new OpenMeteoKliens();
            cim = "Weather (Open-Meteo)";
        }

        SwingUtilities.invokeLater(() -> {
            rendszerKinezet();
            IdojarasAblak.megnyit(szolgaltatas, Varos.ALAPERTELMEZETT_VAROSOK, cim);
        });
    }

    private static MockIdojaras demoMock(boolean hibaMod) {
        LocalDateTime ido = LocalDateTime.of(2026, 10, 24, 12, 0);
        MockIdojaras mock = new MockIdojaras()
                .beallit(Varos.MAROSVASARHELY, new Idojaras(12.5, 8.3, ido))
                .beallit(Varos.KOLOZSVAR, new Idojaras(10.2, 14.0, ido))
                .beallit(Varos.BRASSO, new Idojaras(7.8, 5.6, ido))
                .beallit(Varos.BUKAREST, new Idojaras(16.4, 11.2, ido))
                .beallit(Varos.BUDAPEST, new Idojaras(14.1, 18.5, ido.minusHours(1)))
                .beallit(Varos.BECS, new Idojaras(11.9, 22.7, ido.minusHours(1)))
                // Makes the disabled Refresh button visible during a "request".
                .setKesleltetes(Duration.ofMillis(800));
        if (hibaMod) {
            mock.hibatDob("Simulated failure: the weather service is unavailable (--mock-error).");
        }
        return mock;
    }

    private static void rendszerKinezet() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            // Keep the default look and feel.
        }
    }
}
