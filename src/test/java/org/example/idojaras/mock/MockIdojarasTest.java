package org.example.idojaras.mock;

import org.example.idojaras.domain.Idojaras;
import org.example.idojaras.domain.IdojarasKivetel;
import org.example.idojaras.domain.IdojarasSzolgaltatas;
import org.example.idojaras.domain.Varos;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MockIdojarasTest {

    private static final Idojaras HIDEG = new Idojaras(-5.0, 3.0, LocalDateTime.of(2026, 1, 10, 8, 0));
    private static final Idojaras MELEG = new Idojaras(31.5, 12.0, LocalDateTime.of(2026, 7, 20, 15, 0));

    @Test
    void alapertelmezettErteketAdVissza() throws IdojarasKivetel {
        IdojarasSzolgaltatas szolgaltatas = new MockIdojaras();

        assertEquals(MockIdojaras.ALAPERTELMEZETT, szolgaltatas.aktualis(46.54, 24.56));
    }

    @Test
    void varosonkentBeallitottErteketAdVissza() throws IdojarasKivetel {
        MockIdojaras mock = new MockIdojaras(MELEG).beallit(Varos.MAROSVASARHELY, HIDEG);

        assertSame(HIDEG, mock.aktualis(46.54, 24.56));
        assertSame(MELEG, mock.aktualis(Varos.BUDAPEST.lat(), Varos.BUDAPEST.lon()));
    }

    @Test
    void hibaraAllitvaIdojarasKiveteltDob() {
        MockIdojaras mock = new MockIdojaras().hibatDob("Service down");

        IdojarasKivetel kivetel = assertThrows(IdojarasKivetel.class, () -> mock.aktualis(46.54, 24.56));

        assertEquals("Service down", kivetel.getMessage());
        assertEquals(1, mock.getHivasokSzama());
    }

    @Test
    void hibaKikapcsolasaUtanUjraAdatotAd() throws IdojarasKivetel {
        MockIdojaras mock = new MockIdojaras(MELEG).hibatDob("Service down");
        assertThrows(IdojarasKivetel.class, () -> mock.aktualis(0, 0));

        mock.hibaKikapcsolasa();

        assertSame(MELEG, mock.aktualis(0, 0));
    }

    @Test
    void alapertelmezesNelkulIsmeretlenHelyreKiveteltDob() throws IdojarasKivetel {
        MockIdojaras mock = new MockIdojaras(null).beallit(Varos.KOLOZSVAR, HIDEG);

        assertSame(HIDEG, mock.aktualis(Varos.KOLOZSVAR.lat(), Varos.KOLOZSVAR.lon()));
        assertThrows(IdojarasKivetel.class, () -> mock.aktualis(10, 10));
    }
}
