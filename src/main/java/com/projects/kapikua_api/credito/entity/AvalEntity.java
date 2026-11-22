package com.projects.kapikua_api.credito.entity;

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

/** Fila de credito.aval: un aval invitado a respaldar una oferta. */
@Entity
@Table(schema = "credito", name = "aval")
@Getter
@Setter
@NoArgsConstructor
public class AvalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_deudor_id", nullable = false)
    private Long usuarioDeudorId;

    @Column(name = "usuario_aval_id", nullable = false)
    private Long usuarioAvalId;

    @Column(nullable = false, length = 10)
    private String estado;

    @Column(name = "porcentaje_retencion_autorizado", precision = 5, scale = 2)
    private BigDecimal porcentajeRetencionAutorizado;

    @Column(name = "fecha_invitacion", nullable = false)
    private OffsetDateTime fechaInvitacion;

    @Column(name = "fecha_respuesta")
    private OffsetDateTime fechaRespuesta;

    @Column(name = "dispositivo_aceptacion", length = 150)
    private String dispositivoAceptacion;

    @Column(name = "fecha_aviso")
    private OffsetDateTime fechaAviso;

    @Column(name = "fecha_activacion")
    private OffsetDateTime fechaActivacion;

    @Column(name = "fecha_liberacion")
    private OffsetDateTime fechaLiberacion;

    @Version
    @Column(nullable = false)
    private Long version;
}
