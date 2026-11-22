package com.projects.kapikua_api.credito.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Fila de credito.prestamo con sus cuotas. Solo describe la tabla; las reglas viven en model.Prestamo. */
@Entity
@Table(schema = "credito", name = "prestamo")
@Getter
@Setter
@NoArgsConstructor
public class PrestamoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "oferta_id", nullable = false, unique = true)
    private Long ofertaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "codigo_entidad", nullable = false, length = 20)
    private String codigoEntidad;

    @Column(name = "codigo_credito_efa", nullable = false, unique = true, length = 30)
    private String codigoCreditoEfa;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal tea;

    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal tcea;

    @Column(name = "porcentaje_retencion", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeRetencion;

    @Column(name = "version_hoja_resumen", nullable = false, length = 20)
    private String versionHojaResumen;

    @Column(name = "fecha_aceptacion", nullable = false)
    private OffsetDateTime fechaAceptacion;

    @Column(name = "dispositivo_aceptacion", nullable = false, length = 150)
    private String dispositivoAceptacion;

    @Column(name = "cargo_autorizado", nullable = false)
    private boolean cargoAutorizado;

    @Column(name = "fecha_autorizacion_cargo")
    private OffsetDateTime fechaAutorizacionCargo;

    @Column(name = "fecha_revocacion_cargo")
    private OffsetDateTime fechaRevocacionCargo;

    @Column(nullable = false, length = 10)
    private String estado;

    @Column(name = "fecha_desembolso", nullable = false)
    private OffsetDateTime fechaDesembolso;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "prestamo_id", nullable = false, updatable = false)
    @OrderBy("numero")
    private List<CuotaEntity> cuotas = new ArrayList<>();
}
