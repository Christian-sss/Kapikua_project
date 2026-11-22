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

    public static Oferta crear(
            Long evaluacionId,
            UsuarioId usuarioId,
            OffsetDateTime ahora,
            ZoneId zona,
            int vigenciaDias
    ) {
        var hoy = ahora.atZoneSameInstant(zona).toLocalDate();
        return new Oferta(null, evaluacionId, usuarioId, null, EstadoOferta.PENDIENTE_EFA, null, null, ahora,
                hoy.plusDays(vigenciaDias), null, List.of());
    }

    public static Oferta reconstituir(
            Long id,
            Long evaluacionId,
            UsuarioId usuarioId,
            String codigoSolicitudEfa,
            String estado,
            CondicionesCredito condiciones,
            String motivoDenegacion,
            OffsetDateTime fecha,
            LocalDate vigenteHasta,
            Long version,
            List<Aval> avales
    ) {
        var estadoReconstituido = EstadoOferta.desdeNombre(estado);

        var oferta = new Oferta(id, evaluacionId, usuarioId, codigoSolicitudEfa, estadoReconstituido, condiciones, motivoDenegacion,
                fecha, vigenteHasta, version, avales);

        estadoReconstituido.validarReconstitucion(oferta);

        return oferta;
    }

    public OffsetDateTime finVigencia(ZoneId zona) {
        return vigenteHasta.plusDays(1).atStartOfDay(zona).toOffsetDateTime();
    }

    public boolean actualizarVencimientos(OffsetDateTime ahora, ZoneId zona, Duration plazoAval) {
        var cambio = false;
        var fin = finVigencia(zona);
        for (var aval : avales) {
            if (!aval.esPendiente()) continue;
            var vencimiento = aval.venceEn(plazoAval, fin);
            if (!ahora.isBefore(vencimiento)) cambio |= aval.rechazar(vencimiento).esOk();
        }
        return (!ahora.isBefore(fin) && estado.vencer(this, ahora, fin).esOk()) || cambio;
    }

    public Result<Aval, ErrorCredito> designarAval(UsuarioId avalId, boolean requiereAval, int maxAvales,
                                                   OffsetDateTime ahora) {
        return estado.verificarGestionAval().flatMap(ignorado -> invitarAval(avalId, requiereAval, maxAvales, ahora));
    }

    private Result<Aval, ErrorCredito> invitarAval(
            UsuarioId avalId,
            boolean requiereAval,
            int maxAvales,
            OffsetDateTime ahora
    ) {
        if (!requiereAval) return Result.err(new ErrorCredito.AvalNoRequerido());
        if (avalId.equals(usuarioId)) return Result.err(new ErrorCredito.AvalMismoDeudor());
        if (avales
                .stream()
                .anyMatch(a -> a
                        .getUsuarioAvalId()
                        .equals(avalId)
                )) {

            return Result.err(new ErrorCredito.AvalYaInvitado());
        }

        if (avales.stream().anyMatch(Aval::enCurso)) return Result.err(new ErrorCredito.AvalEnCurso());
        if (avales.size() >= maxAvales) return Result.err(new ErrorCredito.AvalesAgotados(maxAvales));
        var aval = Aval.invitar(usuarioId, avalId, ahora);
        avales.add(aval);
        return Result.ok(aval);
    }

    public Result<CambioEstado, ErrorCredito> responderAval(UsuarioId avalId, boolean acepta, int porcentajeRetencion,
                                                            String dispositivo, OffsetDateTime ahora) {
        var invitacion = invitacionPendientePara(avalId);
        if (invitacion.isEmpty()) return Result.err(new ErrorCredito.InvitacionNoEncontrada());
        var aval = invitacion.get();
        return estado.verificarGestionAval().flatMap(ignorado -> acepta
                ? aval.aceptar(porcentajeRetencion, dispositivo, ahora)
                : aval.rechazar(ahora));
    }

    public Optional<Aval> invitacionPendientePara(UsuarioId avalId) {
        return avales.stream().filter(a -> a.getUsuarioAvalId().equals(avalId) && a.esPendiente()).findFirst();
    }

    public Optional<Aval> avalAceptado() {
        return avales.stream().filter(Aval::estaAceptado).findFirst();
    }

    public Optional<Aval> avalAceptadoOActivo() {
        return avales.stream().filter(Aval::tieneGarantia).findFirst();
    }

    public enum Escalamiento { NINGUNO, AVISADO, ACTIVADO }

    public Escalamiento escalarAval(int diasAtraso, int diasAviso, int diasActivacion, OffsetDateTime ahora) {
        var aval = avalAceptadoOActivo();
        if (aval.isEmpty()) return Escalamiento.NINGUNO;
        if (diasAtraso >= diasActivacion && aval.get().activar(ahora).esOk()) return Escalamiento.ACTIVADO;
        if (diasAtraso >= diasAviso && aval.get().avisar(ahora)) return Escalamiento.AVISADO;
        return Escalamiento.NINGUNO;
    }

    public void liberarAval(OffsetDateTime ahora) {
        avalAceptadoOActivo().ifPresent(a -> a.liberar(ahora));
    }

    public boolean listaParaEfa(boolean requiereAval) {
        return estado.esperaDecisionEfa() && codigoSolicitudEfa == null
                && (!requiereAval || avalAceptado().isPresent());
    }

    public Result<CambioEstado, ErrorCredito> confirmar(String codigoSolicitud, CondicionesCredito condicionesEfa,
                                                        OffsetDateTime ahora) {
        return estado.confirmar(this, codigoSolicitud, condicionesEfa, ahora);
    }

    public Result<CambioEstado, ErrorCredito> denegar(String codigoSolicitud, String motivo, OffsetDateTime ahora) {
        return estado.denegar(this, codigoSolicitud, motivo, ahora);
    }

    public Result<Void, ErrorCredito> verificarAceptable(boolean requiereAval, OffsetDateTime ahora, ZoneId zona) {
        return estado.verificarAceptable(this, requiereAval, ahora, zona);
    }

    public Result<CambioEstado, ErrorCredito> aceptar(boolean requiereAval, OffsetDateTime ahora, ZoneId zona) {
        return estado.aceptar(this, requiereAval, ahora, zona);
    }

    public Result<CambioEstado, ErrorCredito> vencer(OffsetDateTime ahora, ZoneId zona) {
        return estado.vencer(this, ahora, finVigencia(zona));
    }

    Result<CambioEstado, ErrorCredito> confirmarCon(String codigoSolicitud, CondicionesCredito condicionesEfa,
                                                     OffsetDateTime ahora) {
        if (codigoSolicitud == null || codigoSolicitud.isBlank() || condicionesEfa == null) {
            return Result.err(new ErrorCredito.RespuestaEfaInvalida());
        }
        var cambio = cambiarEstado(EstadoOferta.CONFIRMADA, "Confirmada por la entidad aliada", ahora);
        codigoSolicitudEfa = codigoSolicitud;
        condiciones = condicionesEfa;
        return Result.ok(cambio);
    }

    CambioEstado denegarCon(String codigoSolicitud, String motivo, OffsetDateTime ahora) {
        var motivoAplicado = motivo == null || motivo.isBlank() ? "Denegada por la entidad aliada" : motivo;
        var cambio = cambiarEstado(EstadoOferta.DENEGADA, motivoAplicado, ahora);
        codigoSolicitudEfa = codigoSolicitud;
        motivoDenegacion = motivoAplicado;
        return cambio;
    }

    Result<CambioEstado, ErrorCredito> vencerSiCorresponde(OffsetDateTime ahora, OffsetDateTime fin) {
        if (ahora.isBefore(fin)) return Result.err(new ErrorCredito.OfertaAunVigente());
        return Result.ok(cambiarEstado(EstadoOferta.VENCIDA, "Terminó la vigencia de la oferta", fin));
    }

    CambioEstado cambiarEstado(EstadoOferta nuevo, String motivo, OffsetDateTime fecha) {
        var cambio = new CambioEstado(estado.nombre(), nuevo.nombre(), motivo, fecha);
        estado = nuevo;
        return cambio;
    }

    void validarConfirmacion() {
        if (codigoSolicitudEfa == null || condiciones == null) {
            throw new IllegalArgumentException("La oferta requiere el código y las condiciones que confirmó la EFA");
        }
    }

    public boolean enCurso() { return estado.enCurso(); }
    public boolean esperaDecisionEfa() { return estado.esperaDecisionEfa(); }
    public boolean estaConfirmada() { return estado.estaConfirmada(); }
    public boolean estaAceptada() { return estado.estaAceptada(); }

    public Long getId() { return id; }
    public Long getEvaluacionId() { return evaluacionId; }
    public UsuarioId getUsuarioId() { return usuarioId; }
    public String getCodigoSolicitudEfa() { return codigoSolicitudEfa; }
    public String getEstado() { return estado.nombre(); }
    public CondicionesCredito getCondiciones() { return condiciones; }
    public String getMotivoDenegacion() { return motivoDenegacion; }
    public OffsetDateTime getFecha() { return fecha; }
    public LocalDate getVigenteHasta() { return vigenteHasta; }
    public Long getVersion() { return version; }
    public List<Aval> getAvales() { return Collections.unmodifiableList(avales); }
}
