package com.projects.kapikua_api.billetera.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Fila de billetera.movimiento: el efecto de una transacción sobre una billetera. */
@Entity
@Immutable
@Table(schema = "billetera", name = "movimiento")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "billetera_id", nullable = false)
    private Long billeteraId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "saldo_posterior", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoPosterior;

    @Column(nullable = false)
    private OffsetDateTime fecha;
}
