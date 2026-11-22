package com.projects.kapikua_api.credito.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila de credito.cuota: una cuota del cronograma del préstamo. */
@Entity
@Table(schema = "credito", name = "cuota")
@Getter
@Setter
@NoArgsConstructor
public class CuotaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer numero;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal capital;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal interes;

    @Column(name = "interes_moratorio", nullable = false, precision = 12, scale = 2)
    private BigDecimal interesMoratorio;

    @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "monto_apartado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoApartado;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(nullable = false, length = 10)
    private String estado;
}
