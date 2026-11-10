package com.projects.kapikua_api.cumplimiento.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.shared.infrastructure.HoraLocal;
import com.projects.kapikua_api.cumplimiento.application.port.out.ReclamoRepositoryPort;
import com.projects.kapikua_api.cumplimiento.domain.model.EstadoReclamo;
import com.projects.kapikua_api.cumplimiento.domain.model.MotivoReclamo;
import com.projects.kapikua_api.cumplimiento.domain.model.Reclamo;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
class ReclamoPersistenceAdapter implements ReclamoRepositoryPort {
    private static final String COLUMNAS = """
            id, codigo, usuario_id, motivo, detalle, estado, fecha_registro, fecha_limite, prorroga,
            respuesta, respondido_por, fecha_respuesta""";

    private static final RowMapper<Reclamo> RECLAMO = (rs, n) -> {
        var respondio = rs.getObject("respondido_por", Long.class);
        return Reclamo.reconstituir(rs.getLong("id"), rs.getString("codigo"), new UsuarioId(rs.getLong("usuario_id")),
                MotivoReclamo.valueOf(rs.getString("motivo")), rs.getString("detalle"),
                HoraLocal.leer(rs, "fecha_registro"), rs.getObject("fecha_limite", LocalDate.class),
                EstadoReclamo.valueOf(rs.getString("estado")), rs.getString("prorroga"), rs.getString("respuesta"),
                respondio == null ? null : new UsuarioId(respondio), HoraLocal.leer(rs, "fecha_respuesta"));
    };

    private final JdbcClient jdbc;

    ReclamoPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Reclamo registrar(Reclamo reclamo) {
        return jdbc.sql("""
                        insert into cumplimiento.reclamo (id, codigo, usuario_id, motivo, detalle, fecha_registro, fecha_limite)
                        overriding system value
                        select n, 'R-' || :anio || '-' || lpad(n::text, 6, '0'), :usuario, :motivo, :detalle, :registro, :limite
                        from (select nextval(pg_get_serial_sequence('cumplimiento.reclamo', 'id')) as n) s
                        returning\s""" + COLUMNAS)
                .param("anio", String.valueOf(reclamo.getFechaRegistro().getYear()))
                .param("usuario", reclamo.getUsuarioId().valor())
                .param("motivo", reclamo.getMotivo().name())
                .param("detalle", reclamo.getDetalle())
                .param("registro", reclamo.getFechaRegistro())
                .param("limite", reclamo.getFechaLimite())
                .query(RECLAMO)
                .single();
    }

    @Override
    public Optional<Reclamo> porId(Long id) {
        return jdbc.sql("select " + COLUMNAS + " from cumplimiento.reclamo where id = :id").param("id", id)
                .query(RECLAMO).optional();
    }

    @Override
    public List<Reclamo> deUsuario(UsuarioId usuarioId) {
        return jdbc.sql("select " + COLUMNAS + " from cumplimiento.reclamo where usuario_id = :u order by fecha_registro desc")
                .param("u", usuarioId.valor()).query(RECLAMO).list();
    }

    @Override
    public List<Reclamo> pendientes() {
        return jdbc.sql("select " + COLUMNAS + " from cumplimiento.reclamo where estado = 'REGISTRADO' order by fecha_limite, fecha_registro")
                .query(RECLAMO).list();
    }

    @Override
    public List<Reclamo> atendidos(int limite) {
        return jdbc.sql("select " + COLUMNAS + " from cumplimiento.reclamo where estado = 'ATENDIDO' order by fecha_respuesta desc limit :limite")
                .param("limite", limite).query(RECLAMO).list();
    }

    @Override
    public boolean guardarRespuesta(Reclamo reclamo) {
        return jdbc.sql("""
                        update cumplimiento.reclamo
                        set estado = 'ATENDIDO', respuesta = :respuesta, respondido_por = :admin, fecha_respuesta = :fecha
                        where id = :id and estado = 'REGISTRADO'""")
                .param("respuesta", reclamo.getRespuesta())
                .param("admin", reclamo.getRespondidoPor().valor())
                .param("fecha", reclamo.getFechaRespuesta())
                .param("id", reclamo.getId())
                .update() == 1;
    }

    @Override
    public boolean guardarProrroga(Reclamo reclamo) {
        return jdbc.sql("""
                        update cumplimiento.reclamo set prorroga = :prorroga, fecha_limite = :limite
                        where id = :id and estado = 'REGISTRADO' and prorroga is null""")
                .param("prorroga", reclamo.getProrroga())
                .param("limite", reclamo.getFechaLimite())
                .param("id", reclamo.getId())
                .update() == 1;
    }
}
