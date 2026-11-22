package com.projects.kapikua_api.cumplimiento.repository;

import com.projects.kapikua_api.cumplimiento.model.Parametro;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class ParametroRepository {
    private static final String VIGENTES = """
            select id, codigo, valor, vigente_desde, vigente_hasta, fuente
            from cumplimiento.parametro
            where vigente_desde <= :fecha and (vigente_hasta is null or vigente_hasta >= :fecha)
            """;

    private static final RowMapper<Parametro> PARAMETRO = (rs, n) -> Parametro.reconstituir(rs.getLong("id"),
            rs.getString("codigo"), rs.getBigDecimal("valor"), rs.getObject("vigente_desde", LocalDate.class),
            rs.getObject("vigente_hasta", LocalDate.class), rs.getString("fuente"));

    private final JdbcClient jdbc;

    public ParametroRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Parametro> vigente(String codigo, LocalDate fecha) {
        return jdbc.sql(VIGENTES + " and codigo = :codigo")
                .param("fecha", fecha)
                .param("codigo", codigo)
                .query(PARAMETRO)
                .optional();
    }

    public List<Parametro> vigentes(LocalDate fecha) {
        return jdbc.sql(VIGENTES)
                .param("fecha", fecha)
                .query(PARAMETRO)
                .list();
    }
}
