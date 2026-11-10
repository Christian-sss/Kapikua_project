package com.projects.kapikua_api.cumplimiento.domain.service;

import com.projects.kapikua_api.cumplimiento.domain.model.*;

import com.projects.kapikua_api.shared.Dinero;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasOperacionesInusualesTest {
    static final Dinero TOPE = Dinero.de("3300");
    static final OffsetDateTime AHORA = OffsetDateTime.of(2026, 11, 9, 15, 0, 0, 0, ZoneOffset.ofHours(-5));
    long id = 1;

    OperacionObservada op(String tipo, String monto, OffsetDateTime fecha) {
        return new OperacionObservada(id++, tipo, Dinero.de(monto), fecha);
    }

    List<ReglaAlerta> reglas(OperacionObservada actual, List<OperacionObservada> previas, Map<ReglaAlerta, OffsetDateTime> ultimas) {
        var todas = new ArrayList<>(previas);
        todas.add(actual);
        return ReglasOperacionesInusuales.evaluar(actual, todas, TOPE, ultimas).stream()
                .map(ReglasOperacionesInusuales.AlertaDetectada::regla).toList();
    }

    @Test
    void unaOperacionDel90PorCientoDelTopeEstaCercaDelTope() {
        assertThat(reglas(op("TRANSFERENCIA", "2970", AHORA), List.of(), Map.of())).containsExactly(ReglaAlerta.CERCA_DEL_TOPE);
        assertThat(reglas(op("RECARGA", "2969.99", AHORA), List.of(), Map.of())).isEmpty();
    }

    @Test
    void tresOperacionesEn24HorasQueSuperanElTopeSonFraccionamiento() {
        var previas = List.of(op("RECARGA", "1200", AHORA.minusHours(20)), op("TRANSFERENCIA", "1200", AHORA.minusHours(3)));
        assertThat(reglas(op("PAGO_NEGOCIO", "1200", AHORA), previas, Map.of())).containsExactly(ReglaAlerta.FRACCIONAMIENTO);
    }

    @Test
    void sinTresOperacionesOSinSuperarElTopeNoHayFraccionamiento() {
        assertThat(reglas(op("RECARGA", "2000", AHORA), List.of(op("RECARGA", "2000", AHORA.minusHours(1))), Map.of())).isEmpty();
        var bajo = List.of(op("RECARGA", "1000", AHORA.minusHours(2)), op("RECARGA", "1000", AHORA.minusHours(1)));
        assertThat(reglas(op("RECARGA", "1300", AHORA), bajo, Map.of())).isEmpty();
        var viejas = List.of(op("RECARGA", "2000", AHORA.minusHours(25)), op("RECARGA", "1000", AHORA.minusHours(1)));
        assertThat(reglas(op("RECARGA", "1000", AHORA), viejas, Map.of())).isEmpty();
    }

    @Test
    void elFraccionamientoNoSeRepiteEnLas24HorasSiguientes() {
        var previas = List.of(op("RECARGA", "1200", AHORA.minusHours(5)), op("RECARGA", "1200", AHORA.minusHours(4)));
        assertThat(reglas(op("RECARGA", "1200", AHORA), previas, Map.of(ReglaAlerta.FRACCIONAMIENTO, AHORA.minusHours(2)))).isEmpty();
        assertThat(reglas(op("RECARGA", "1200", AHORA), previas, Map.of(ReglaAlerta.FRACCIONAMIENTO, AHORA.minusHours(25))))
                .containsExactly(ReglaAlerta.FRACCIONAMIENTO);
    }

    @Test
    void cincoEnviosEnDiezMinutosSonTransferenciasSeguidas() {
        var previas = new ArrayList<OperacionObservada>();
        for (int i = 4; i >= 1; i--) previas.add(op(i % 2 == 0 ? "TRANSFERENCIA" : "PAGO_NEGOCIO", "10", AHORA.minusMinutes(2L * i)));
        assertThat(reglas(op("TRANSFERENCIA", "10", AHORA), previas, Map.of())).containsExactly(ReglaAlerta.TRANSFERENCIAS_SEGUIDAS);
        assertThat(reglas(op("TRANSFERENCIA", "10", AHORA), previas,
                Map.of(ReglaAlerta.TRANSFERENCIAS_SEGUIDAS, AHORA.minusMinutes(5)))).isEmpty();
    }

    @Test
    void lasRecargasYLosEnviosViejosNoCuentanComoTransferenciasSeguidas() {
        var previas = List.of(op("TRANSFERENCIA", "10", AHORA.minusMinutes(11)), op("TRANSFERENCIA", "10", AHORA.minusMinutes(6)),
                op("RECARGA", "10", AHORA.minusMinutes(5)), op("TRANSFERENCIA", "10", AHORA.minusMinutes(4)),
                op("TRANSFERENCIA", "10", AHORA.minusMinutes(3)));
        assertThat(reglas(op("TRANSFERENCIA", "10", AHORA), previas, Map.of())).isEmpty();
    }

    @Test
    void unaOperacionPuedeDispararVariasReglas() {
        var previas = List.of(op("TRANSFERENCIA", "100", AHORA.minusHours(2)), op("TRANSFERENCIA", "500", AHORA.minusHours(1)));
        assertThat(reglas(op("TRANSFERENCIA", "3000", AHORA), previas, Map.of()))
                .containsExactlyInAnyOrder(ReglaAlerta.CERCA_DEL_TOPE, ReglaAlerta.FRACCIONAMIENTO);
    }
}
