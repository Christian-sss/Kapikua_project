package com.projects.kapikua_api.cumplimiento.domain.service;

import com.projects.kapikua_api.cumplimiento.domain.model.*;

import com.projects.kapikua_api.shared.Dinero;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ReglasOperacionesInusuales {
    public static final Duration VENTANA_FRACCIONAMIENTO = Duration.ofHours(24);
    public static final Duration VENTANA_SEGUIDAS = Duration.ofMinutes(10);
    static final int MINIMO_FRACCIONAMIENTO = 3;
    static final int MINIMO_SEGUIDAS = 5;
    static final BigDecimal PORCENTAJE_CERCA = new BigDecimal("0.90");

    public record AlertaDetectada(ReglaAlerta regla, String detalle) {}

    private ReglasOperacionesInusuales() {}

    public static List<AlertaDetectada> evaluar(OperacionObservada actual, List<OperacionObservada> ultimas,
                                                Dinero topePorOperacion, Map<ReglaAlerta, OffsetDateTime> ultimaAlerta) {
        var alertas = new ArrayList<AlertaDetectada>();
        var ahora = actual.fecha();

        var limiteCerca = topePorOperacion.multiplicar(PORCENTAJE_CERCA);
        if (!actual.monto().menorQue(limiteCerca)) {
            alertas.add(new AlertaDetectada(ReglaAlerta.CERCA_DEL_TOPE, "Operación N° %d de %s: 90 %% o más del tope por operación (%s)."
                    .formatted(actual.transaccionId(), actual.monto(), topePorOperacion)));
        }

        var en24h = dentroDe(ultimas, ahora, VENTANA_FRACCIONAMIENTO);
        var total = en24h.stream().map(OperacionObservada::monto).reduce(Dinero.CERO, Dinero::sumar);
        if (en24h.size() >= MINIMO_FRACCIONAMIENTO && total.mayorQue(topePorOperacion)
                && sinAlertaReciente(ultimaAlerta, ReglaAlerta.FRACCIONAMIENTO, ahora, VENTANA_FRACCIONAMIENTO)) {
            alertas.add(new AlertaDetectada(ReglaAlerta.FRACCIONAMIENTO, recortar("%d operaciones en 24 horas suman %s, más que el tope por operación (%s). Operaciones N° %s."
                    .formatted(en24h.size(), total, topePorOperacion, numeros(en24h)))));
        }

        var envios = dentroDe(ultimas, ahora, VENTANA_SEGUIDAS).stream().filter(OperacionObservada::esEnvio).toList();
        if (actual.esEnvio() && envios.size() >= MINIMO_SEGUIDAS
                && sinAlertaReciente(ultimaAlerta, ReglaAlerta.TRANSFERENCIAS_SEGUIDAS, ahora, VENTANA_SEGUIDAS)) {
            alertas.add(new AlertaDetectada(ReglaAlerta.TRANSFERENCIAS_SEGUIDAS, recortar("%d envíos en 10 minutos. Operaciones N° %s."
                    .formatted(envios.size(), numeros(envios)))));
        }
        return alertas;
    }

    private static List<OperacionObservada> dentroDe(List<OperacionObservada> ops, OffsetDateTime ahora, Duration ventana) {
        var desde = ahora.minus(ventana);
        return ops.stream().filter(o -> o.fecha().isAfter(desde) && !o.fecha().isAfter(ahora)).toList();
    }

    private static boolean sinAlertaReciente(Map<ReglaAlerta, OffsetDateTime> ultimaAlerta, ReglaAlerta regla,
                                             OffsetDateTime ahora, Duration ventana) {
        var ultima = ultimaAlerta.get(regla);
        return ultima == null || !ultima.isAfter(ahora.minus(ventana));
    }

    private static String numeros(List<OperacionObservada> ops) {
        return ops.stream().map(o -> String.valueOf(o.transaccionId())).collect(Collectors.joining(", "));
    }

    private static String recortar(String texto) {
        return texto.length() <= 500 ? texto : texto.substring(0, 497) + "...";
    }
}
