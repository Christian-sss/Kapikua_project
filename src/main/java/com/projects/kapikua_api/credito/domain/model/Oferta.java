package com.projects.kapikua_api.credito.domain.model;

import com.projects.kapikua_api.credito.domain.error.ErrorCredito;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Raíz de agregado: la oferta de crédito que nace con la evaluación y que la entidad aliada confirma o deniega
 * (RN-18, RN-19). Vence a los 7 días; ese plazo incluye la búsqueda del aval. Contiene de 0 a 3 avales.
 * Es Java puro: el adaptador de persistencia la convierte a su entidad JPA.
 */
public final class Oferta {

    private final Long id;
    private final Long evaluacionId;
    private final UsuarioId usuarioId;
    private String codigoSolicitudEfa;
    private EstadoOferta estado;
    private CondicionesCredito condiciones;
    private String motivoDenegacion;
    private final OffsetDateTime fecha;
    private final LocalDate vigenteHasta;
    private final Long version;
    private final List<Aval> avales;

    private Oferta(Long id, Long evaluacionId, UsuarioId usuarioId, String codigoSolicitudEfa, EstadoOferta estado,
                   CondicionesCredito condiciones, String motivoDenegacion, OffsetDateTime fecha,
                   LocalDate vigenteHasta, Long version, List<Aval> avales) {
        this.id = id;
        this.evaluacionId = Objects.requireNonNull(evaluacionId);
        this.usuarioId = Objects.requireNonNull(usuarioId);
        this.codigoSolicitudEfa = codigoSolicitudEfa;
        this.estado = Objects.requireNonNull(estado);
        this.condiciones = condiciones;
        this.motivoDenegacion = motivoDenegacion;
        this.fecha = Objects.requireNonNull(fecha);
        this.vigenteHasta = Objects.requireNonNull(vigenteHasta);
        this.version = version;
        this.avales = new ArrayList<>(avales);
    }

    /** CU-08, paso 6: la oferta nace PENDIENTE_EFA junto con la recomendación, vigente {@code vigenciaDias} días. */
    public static Oferta crear(Long evaluacionId, UsuarioId usuarioId, OffsetDateTime ahora, ZoneId zona, int vigenciaDias) {
        var hoy = ahora.atZoneSameInstant(zona).toLocalDate();
        return new Oferta(null, evaluacionId, usuarioId, null, EstadoOferta.PENDIENTE_EFA, null, null, ahora,
                hoy.plusDays(vigenciaDias), null, List.of());
    }

    public static Oferta reconstituir(Long id, Long evaluacionId, UsuarioId usuarioId, String codigoSolicitudEfa,
                                      EstadoOferta estado, CondicionesCredito condiciones, String motivoDenegacion,
                                      OffsetDateTime fecha, LocalDate vigenteHasta, Long version, List<Aval> avales) {
        return new Oferta(id, evaluacionId, usuarioId, codigoSolicitudEfa, estado, condiciones, motivoDenegacion,
                fecha, vigenteHasta, version, avales);
    }

    // ------------------------------------------------------------------ vigencia (RN-19, RN-21)

    /** Último instante de vigencia: el fin del día {@code vigenteHasta}. */
    public OffsetDateTime finVigencia(ZoneId zona) {
        return vigenteHasta.plusDays(1).atStartOfDay(zona).toOffsetDateTime();
    }

    /**
     * Aplica los plazos vencidos: la oferta en curso pasa a VENCIDA tras su vigencia, y la invitación pendiente
     * a RECHAZADO tras 48 horas sin respuesta. Devuelve true si algo cambió (hay que guardarla).
     */
    public boolean actualizarVencimientos(OffsetDateTime ahora, ZoneId zona, Duration plazoAval) {
        var cambio = false;
        var fin = finVigencia(zona);
        for (var a : avales) {
            if (a.esPendiente() && !ahora.isBefore(a.venceEn(plazoAval, fin))) {
                a.rechazar(a.venceEn(plazoAval, fin));
                cambio = true;
            }
        }
        if (estado.enCurso() && !ahora.isBefore(fin)) {
            estado = EstadoOferta.VENCIDA;
            cambio = true;
        }
        return cambio;
    }

    // ------------------------------------------------------------------ aval (RN-21, CU-09)

    /**
     * Designa un aval. Las reglas que dependen de otras ofertas o de Evaluación (garantía activa, mora,
     * aval cruzado, clasificación) las verifica el caso de uso antes de llamar aquí.
     */
    public Result<Aval, ErrorCredito> designarAval(UsuarioId avalId, boolean requiereAval, int maxAvales,
                                                   OffsetDateTime ahora) {
        if (estado != EstadoOferta.PENDIENTE_EFA) return Result.err(new ErrorCredito.OfertaEstadoInvalido(estado.name()));
        if (!requiereAval) return Result.err(new ErrorCredito.AvalNoRequerido());
        if (avalId.equals(usuarioId)) return Result.err(new ErrorCredito.AvalMismoDeudor());
        if (avales.stream().anyMatch(a -> a.getUsuarioAvalId().equals(avalId))) {
            return Result.err(new ErrorCredito.AvalYaInvitado());
        }
        if (avales.stream().anyMatch(a -> a.getEstado().enCurso())) return Result.err(new ErrorCredito.AvalEnCurso());
        if (avales.size() >= maxAvales) return Result.err(new ErrorCredito.AvalesAgotados(maxAvales));
        var aval = Aval.invitar(usuarioId, avalId, ahora);
        avales.add(aval);
        return Result.ok(aval);
    }

    /** El aval invitado acepta (con el porcentaje de retención sugerido) o rechaza. */
    public Result<Void, ErrorCredito> responderAval(UsuarioId avalId, boolean acepta, int porcentajeRetencion,
                                                    String dispositivo, OffsetDateTime ahora) {
        var invitacion = invitacionPendientePara(avalId);
        if (invitacion.isEmpty()) return Result.err(new ErrorCredito.InvitacionNoEncontrada());
        if (estado != EstadoOferta.PENDIENTE_EFA) return Result.err(new ErrorCredito.OfertaEstadoInvalido(estado.name()));
        if (acepta) invitacion.get().aceptar(porcentajeRetencion, dispositivo, ahora);
        else invitacion.get().rechazar(ahora);
        return Result.ok();
    }

    public Optional<Aval> invitacionPendientePara(UsuarioId avalId) {
        return avales.stream().filter(a -> a.getUsuarioAvalId().equals(avalId) && a.esPendiente()).findFirst();
    }

    public Optional<Aval> avalAceptado() {
        return avales.stream().filter(a -> a.getEstado() == EstadoAval.ACEPTADO).findFirst();
    }

    /** El aval que respalda el préstamo: aceptado o, desde el día 15 de atraso, activado (RN-25). */
    public Optional<Aval> avalAceptadoOActivo() {
        return avales.stream().filter(a -> a.getEstado() == EstadoAval.ACEPTADO || a.getEstado() == EstadoAval.ACTIVADO)
                .findFirst();
    }

    /** Qué pasó con el aval en el escalamiento de hoy. */
    public enum Escalamiento { NINGUNO, AVISADO, ACTIVADO }

    /**
     * RN-25: con {@code diasAtraso} días de atraso del préstamo, avisa al aval (día 9) y activa su garantía (día 15).
     * Los días vienen de los parámetros.
     */
    public Escalamiento escalarAval(int diasAtraso, int diasAviso, int diasActivacion, OffsetDateTime ahora) {
        var aval = avalAceptadoOActivo();
        if (aval.isEmpty()) return Escalamiento.NINGUNO;
        if (diasAtraso >= diasActivacion && aval.get().activar(ahora)) return Escalamiento.ACTIVADO;
        if (diasAtraso >= diasAviso && aval.get().avisar(ahora)) return Escalamiento.AVISADO;
        return Escalamiento.NINGUNO;
    }

    /** RN-32: el préstamo se pagó. */
    public void liberarAval(OffsetDateTime ahora) {
        avalAceptadoOActivo().ifPresent(a -> a.liberar(ahora));
    }

    /** Con aval aceptado, o sin necesidad de aval, ya se puede pedir la decisión a la entidad aliada. */
    public boolean listaParaEfa(boolean requiereAval) {
        return estado == EstadoOferta.PENDIENTE_EFA && codigoSolicitudEfa == null
                && (!requiereAval || avalAceptado().isPresent());
    }

    // ------------------------------------------------------------------ decisión de la EFA (RN-18, RF-CR-03)

    public Result<Void, ErrorCredito> confirmar(String codigoSolicitud, CondicionesCredito condicionesEfa) {
        if (estado != EstadoOferta.PENDIENTE_EFA) return Result.err(new ErrorCredito.OfertaEstadoInvalido(estado.name()));
        this.codigoSolicitudEfa = Objects.requireNonNull(codigoSolicitud);
        this.condiciones = Objects.requireNonNull(condicionesEfa);
        this.estado = EstadoOferta.CONFIRMADA;
        return Result.ok();
    }

    public Result<Void, ErrorCredito> denegar(String codigoSolicitud, String motivo) {
        if (estado != EstadoOferta.PENDIENTE_EFA) return Result.err(new ErrorCredito.OfertaEstadoInvalido(estado.name()));
        this.codigoSolicitudEfa = codigoSolicitud;
        this.motivoDenegacion = motivo == null || motivo.isBlank() ? "Denegada por la entidad aliada" : motivo;
        this.estado = EstadoOferta.DENEGADA;
        return Result.ok();
    }

    // ------------------------------------------------------------------ aceptación (CU-10)

    /** RN-19, RN-21: oferta CONFIRMADA y vigente; en los niveles 1 y 2, con el aval aceptado. */
    public Result<Void, ErrorCredito> verificarAceptable(boolean requiereAval, OffsetDateTime ahora, ZoneId zona) {
        if (estado == EstadoOferta.VENCIDA || (estado.enCurso() && !ahora.isBefore(finVigencia(zona)))) {
            return Result.err(new ErrorCredito.OfertaNoVigente());
        }
        if (estado != EstadoOferta.CONFIRMADA) return Result.err(new ErrorCredito.OfertaEstadoInvalido(estado.name()));
        if (requiereAval && avalAceptado().isEmpty()) return Result.err(new ErrorCredito.AvalRequerido());
        return Result.ok();
    }

    public Result<Void, ErrorCredito> aceptar(boolean requiereAval, OffsetDateTime ahora, ZoneId zona) {
        var verificacion = verificarAceptable(requiereAval, ahora, zona);
        if (verificacion instanceof Result.Err<Void, ErrorCredito>) return verificacion;
        estado = EstadoOferta.ACEPTADA;
        return Result.ok();
    }

    public Long getId() { return id; }
    public Long getEvaluacionId() { return evaluacionId; }
    public UsuarioId getUsuarioId() { return usuarioId; }
    public String getCodigoSolicitudEfa() { return codigoSolicitudEfa; }
    public EstadoOferta getEstado() { return estado; }
    public CondicionesCredito getCondiciones() { return condiciones; }
    public String getMotivoDenegacion() { return motivoDenegacion; }
    public OffsetDateTime getFecha() { return fecha; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
    public Long getVersion() { return version; }
    public List<Aval> getAvales() { return Collections.unmodifiableList(avales); }
}
