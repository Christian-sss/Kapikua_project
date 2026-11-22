package com.projects.kapikua_api.credito.entity;

import com.projects.kapikua_api.credito.model.Origen;
import com.projects.kapikua_api.credito.model.TipoPago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Fila de credito.pago: un pago aplicado al préstamo y cómo se imputó. No se modifica después de insertarse. */
@Entity
@Immutable
@Table(schema = "credito", name = "pago")
@Getter
@Setter
@NoArgsConstructor
public class PagoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prestamo_id", nullable = false)
    private Long prestamoId;

    @Column(name = "cuota_id")
    private Long cuotaId;

    @Column(name = "transaccion_id", nullable = false)
    private Long transaccionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoPago tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    private Origen origen;

    @Column(nullable = false)
    private boolean automatico;

    @Column(length = 13)
    private String modalidad;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "imputado_moratorio", nullable = false, precision = 12, scale = 2)
    private BigDecimal imputadoMoratorio;

    @Column(name = "imputado_interes", nullable = false, precision = 12, scale = 2)
    private BigDecimal imputadoInteres;

    @Column(name = "imputado_capital", nullable = false, precision = 12, scale = 2)
    private BigDecimal imputadoCapital;

    @Column(nullable = false)
    private OffsetDateTime fecha;
}
