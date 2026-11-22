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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Fila de credito.oferta con sus avales. Solo describe la tabla; las reglas viven en model.Oferta. */
@Entity
@Table(schema = "credito", name = "oferta")
@Getter
@Setter
@NoArgsConstructor
public class OfertaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluacion_id", nullable = false, unique = true)
    private Long evaluacionId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "codigo_solicitud_efa", unique = true, length = 30)
    private String codigoSolicitudEfa;

    @Column(nullable = false, length = 15)
    private String estado;

    @Column(precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "numero_cuotas")
    private Integer numeroCuotas;

    @Column(precision = 6, scale = 2)
    private BigDecimal tea;

    @Column(precision = 6, scale = 2)
    private BigDecimal tcea;

    @Column(name = "monto_cuota", precision = 12, scale = 2)
    private BigDecimal montoCuota;

    @Column(name = "motivo_denegacion", length = 255)
    private String motivoDenegacion;

    @Column(nullable = false)
    private OffsetDateTime fecha;

    @Column(name = "vigente_hasta", nullable = false)
    private LocalDate vigenteHasta;

    @Version
    @Column(nullable = false)
    private Long version;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "oferta_id", nullable = false, updatable = false)
    @OrderBy("id")
    private List<AvalEntity> avales = new ArrayList<>();
}
