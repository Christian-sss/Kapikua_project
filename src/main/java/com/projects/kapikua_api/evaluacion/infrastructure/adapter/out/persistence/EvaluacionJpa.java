package com.projects.kapikua_api.evaluacion.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.evaluacion.domain.model.ClasificacionSbs;
import com.projects.kapikua_api.evaluacion.domain.model.Destino;
import com.projects.kapikua_api.evaluacion.domain.model.Recomendacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** evaluacion.evaluacion: solo inserción (trigger trg_evaluacion_inmutable, RN-19). */
@Entity
@Immutable
@Table(schema = "evaluacion", name = "evaluacion")
class EvaluacionJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "usuario_id", nullable = false)
    Long usuarioId;

    @Column(name = "monto_solicitado", nullable = false, precision = 12, scale = 2)
    BigDecimal montoSolicitado;

    @Column(name = "cuotas_solicitadas", nullable = false)
    Integer cuotasSolicitadas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    Destino destino;

    @Enumerated(EnumType.STRING)
    @Column(name = "clasificacion_sbs", length = 12)
    ClasificacionSbs clasificacionSbs;

    @Column(name = "deuda_total_sistema", precision = 12, scale = 2)
    BigDecimal deudaTotalSistema;

    Integer score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    Recomendacion recomendacion;

    @Column(name = "monto_sugerido", precision = 12, scale = 2)
    BigDecimal montoSugerido;

    @Column(name = "cuotas_sugeridas")
    Integer cuotasSugeridas;

    @Column(name = "tea_propuesta", precision = 6, scale = 2)
    BigDecimal teaPropuesta;

    @Column(name = "cuota_sugerida", precision = 12, scale = 2)
    BigDecimal cuotaSugerida;

    @Column(name = "porcentaje_retencion", precision = 5, scale = 2)
    BigDecimal porcentajeRetencion;

    @Column(name = "requiere_aval", nullable = false)
    boolean requiereAval;

    @Column(length = 255)
    String motivo;

    /** FactorEvaluado[] como JSONB: nombre, valor, puntaje y peso de cada factor del score. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    String factores;

    @Column(nullable = false)
    OffsetDateTime fecha;

    protected EvaluacionJpa() {}
}
