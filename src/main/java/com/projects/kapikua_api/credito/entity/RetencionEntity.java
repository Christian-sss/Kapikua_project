package com.projects.kapikua_api.credito.entity;

import com.projects.kapikua_api.credito.model.Origen;
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

/** Fila de credito.retencion: lo que se apartó de una venta para la cuota. No se modifica después de insertarse. */
@Entity
@Immutable
@Table(schema = "credito", name = "retencion")
@Getter
@Setter
@NoArgsConstructor
public class RetencionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuota_id", nullable = false)
    private Long cuotaId;

    @Column(name = "transaccion_id", nullable = false, unique = true)
    private Long transaccionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 7)
    private Origen origen;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private OffsetDateTime fecha;
}
