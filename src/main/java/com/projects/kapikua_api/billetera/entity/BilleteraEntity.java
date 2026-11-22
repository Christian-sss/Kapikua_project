package com.projects.kapikua_api.billetera.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Fila de billetera.billetera. Solo describe la tabla; las reglas viven en model.Billetera. */
@Entity
@Table(schema = "billetera", name = "billetera")
@Getter
@Setter
@NoArgsConstructor
public class BilleteraEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    private Long usuarioId;

    @Column(name = "numero_celular", nullable = false, unique = true, length = 9)
    private String numeroCelular;

    @Column(name = "nombre_negocio", length = 100)
    private String nombreNegocio;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldo;

    @Column(name = "saldo_apartado", nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoApartado;

    @Column(nullable = false, length = 10)
    private String estado;

    @Column(name = "motivo_bloqueo", length = 150)
    private String motivoBloqueo;

    @Column(name = "fecha_apertura", nullable = false)
    private OffsetDateTime fechaApertura;

    @Version
    @Column(nullable = false)
    private Long version;
}
