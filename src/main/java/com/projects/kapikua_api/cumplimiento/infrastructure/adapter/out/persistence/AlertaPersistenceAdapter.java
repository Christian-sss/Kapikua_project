package com.projects.kapikua_api.cumplimiento.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.shared.infrastructure.HoraLocal;
import com.projects.kapikua_api.cumplimiento.application.port.out.AlertaRepositoryPort;
import com.projects.kapikua_api.cumplimiento.domain.model.Alerta;
import com.projects.kapikua_api.cumplimiento.domain.model.EstadoAlerta;
import com.projects.kapikua_api.cumplimiento.domain.model.ReglaAlerta;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
class AlertaPersistenceAdapter implements AlertaRepositoryPort {
    private static final String COLUMNAS = """
            select id, billetera_id, transaccion_id, regla, detalle, estado, comentario, revisado_por, fecha, fecha_revision
            from cumplimiento.alerta""";

    private static final RowMapper<Alerta> ALERTA = (rs, n) -> {
        var revisor = rs.getObject("revisado_por", Long.class);
        return Alerta.reconstituir(rs.getLong("id"), rs.getLong("billetera_id"), rs.getLong("transaccion_id"),
                ReglaAlerta.valueOf(rs.getString("regla")), rs.getString("detalle"),
                HoraLocal.leer(rs, "fecha"), EstadoAlerta.valueOf(rs.getString("estado")),
                rs.getString("comentario"), revisor == null ? null : new UsuarioId(revisor),
                HoraLocal.leer(rs, "fecha_revision"));
    };

    private final JdbcClient jdbc;

    AlertaPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean registrar(Alerta alerta) {
        return jdbc.sql("""
                        insert into cumplimiento.alerta (billetera_id, transaccion_id, regla, detalle, fecha)
                        values (:billetera, :transaccion, :regla, :detalle, :fecha)
                        on conflict (transaccion_id, regla) do nothing""")
                .param("billetera", alerta.getBilleteraId())
                .param("transaccion", alerta.getTransaccionId())
                .param("regla", alerta.getRegla().name())
                .param("detalle", alerta.getDetalle())
                .param("fecha", alerta.getFecha())
                .update() == 1;
    }

    @Override
    public Map<ReglaAlerta, OffsetDateTime> ultimasPorRegla(Long billeteraId) {
        var ultimas = new EnumMap<ReglaAlerta, OffsetDateTime>(ReglaAlerta.class);
        jdbc.sql("select regla, max(fecha) as fecha from cumplimiento.alerta where billetera_id = :b group by regla")
                .param("b", billeteraId)
                .query((rs, n) -> ultimas.put(ReglaAlerta.valueOf(rs.getString("regla")), HoraLocal.leer(rs, "fecha")))
                .list();
        return ultimas;
    }

    @Override
    public Optional<Alerta> porId(Long id) {
        return jdbc.sql(COLUMNAS + " where id = :id").param("id", id).query(ALERTA).optional();
    }

    @Override
    public List<Alerta> pendientes() {
        return jdbc.sql(COLUMNAS + " where estado = 'PENDIENTE' order by fecha").query(ALERTA).list();
    }

    @Override
    public List<Alerta> revisadas(int limite) {
        return jdbc.sql(COLUMNAS + " where estado <> 'PENDIENTE' order by fecha_revision desc limit :limite")
                .param("limite", limite).query(ALERTA).list();
    }

    @Override
    public boolean guardarRevision(Alerta alerta) {
        return jdbc.sql("""
                        update cumplimiento.alerta
                        set estado = :estado, comentario = :comentario, revisado_por = :revisor, fecha_revision = :fecha
                        where id = :id and estado = 'PENDIENTE'""")
                .param("estado", alerta.getEstado().name())
                .param("comentario", alerta.getComentario())
                .param("revisor", alerta.getRevisadoPor().valor())
                .param("fecha", alerta.getFechaRevision())
                .param("id", alerta.getId())
                .update() == 1;
    }
}
