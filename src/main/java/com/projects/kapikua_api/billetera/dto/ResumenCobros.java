package com.projects.kapikua_api.billetera.dto;

import com.projects.kapikua_api.shared.Dinero;

public record ResumenCobros(int semanas, Dinero totalCobrado, int semanasConCobros, int cantidadCobros,
                            int pagadoresDistintos, long diasAntiguedad) {
}
