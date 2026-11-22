package com.projects.kapikua_api.credito.dto;

import com.projects.kapikua_api.shared.Dinero;

import java.util.List;

/** Las cuotas que se van a pagar, antes de confirmar el pago. */
public record VistaPago(List<Integer> cuotas, Dinero total, Dinero moratorio, Dinero deApartado, String referencia) {
}
