package com.projects.kapikua_api.billetera.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Fila de billetera.transaccion con sus movimientos. No se modifica después de insertarse. */
@Entity
@Immutable
@Table(schema = "billetera", name = "transaccion")
@Getter
@Setter
@NoArgsConstructor
public class TransaccionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, unique = true, length = 60)
    private String referencia;

    @Column(length = 150)
    private String descripcion;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "transaccion_id", nullable = false, updatable = false)
    private List<MovimientoEntity> movimientos = new ArrayList<>();
}
