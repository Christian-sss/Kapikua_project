package com.projects.kapikua_api.evaluacion.domain.model;

import org.junit.jupiter.api.Test;

import static com.projects.kapikua_api.evaluacion.domain.service.PoliticaCreditoTest.CLIENTE;
import static com.projects.kapikua_api.evaluacion.domain.service.PoliticaCreditoTest.P;
import static org.assertj.core.api.Assertions.assertThat;

class HistorialCrediticioTest {
    private static final ReglasNivel REGLAS = P.reglasNivel();

    @Test
    void subeDelUnoAlDosConUnCreditoPagadoATiempo() {
        var h = HistorialCrediticio.nuevo(CLIENTE);
        h.registrarDesembolso();
        h.registrarAtraso(8, REGLAS);
        h.registrarCreditoPagado(false, REGLAS);
        assertThat(h.getNivel()).isEqualTo(2);
        assertThat(h.esNuevo()).isFalse();
    }

    @Test
    void paraLlegarAlNivelCuatroHacenFaltaDosCreditos() {
        var h = HistorialCrediticio.reconstituir(CLIENTE, 3, 0, 0, false, 0L);
        h.registrarCreditoPagado(false, REGLAS);
        assertThat(h.getNivel()).isEqualTo(3);
        h.registrarDesembolso();
        h.registrarCreditoPagado(false, REGLAS);
        assertThat(h.getNivel()).isEqualTo(4);
        h.registrarCreditoPagado(false, REGLAS);
        assertThat(h.getNivel()).isEqualTo(4);
    }

    @Test
    void conMasDeOchoDiasDeAtrasoNoSube() {
        var h = HistorialCrediticio.reconstituir(CLIENTE, 2, 0, 0, false, 0L);
        h.registrarAtraso(9, REGLAS);
        assertThat(h.getNivel()).isEqualTo(1);
        h.registrarCreditoPagado(false, REGLAS);
        assertThat(h.getNivel()).isEqualTo(1);
    }

    @Test
    void bajaUnSoloNivelPorCreditoAunqueElAtrasoSigaCadaDia() {
        var h = HistorialCrediticio.reconstituir(CLIENTE, 4, 1, 0, false, 0L);
        for (int dia = 1; dia <= 20; dia++) h.registrarAtraso(dia, REGLAS);
        assertThat(h.getNivel()).isEqualTo(3);
        assertThat(h.getCreditosPagadosEnNivel()).isZero();
        assertThat(h.isBloqueadoInterno()).isFalse();
    }

    @Test
    void desdeElDiaTreintaYUnoQuedaBloqueado() {
        var h = HistorialCrediticio.reconstituir(CLIENTE, 2, 0, 0, false, 0L);
        h.registrarAtraso(30, REGLAS);
        assertThat(h.isBloqueadoInterno()).isFalse();
        h.registrarAtraso(31, REGLAS);
        assertThat(h.isBloqueadoInterno()).isTrue();
    }

    @Test
    void siPagaElAvalElDeudorQuedaBloqueado() {
        var h = HistorialCrediticio.reconstituir(CLIENTE, 1, 0, 15, false, 0L);
        h.registrarCreditoPagado(true, REGLAS);
        assertThat(h.isBloqueadoInterno()).isTrue();
        assertThat(h.getNivel()).isEqualTo(1);
    }

    @Test
    void elDesembolsoReiniciaElAtrasoDelCreditoEnCurso() {
        var h = HistorialCrediticio.reconstituir(CLIENTE, 2, 0, 12, false, 0L);
        h.registrarDesembolso();
        assertThat(h.getMaxDiasAtraso()).isZero();
        h.registrarAtraso(9, REGLAS);
        assertThat(h.getNivel()).isEqualTo(1);
    }
}
