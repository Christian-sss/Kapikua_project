package com.projects.kapikua_api.shared.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * Zona horaria de Kapikua. El driver de PostgreSQL entrega los timestamptz en UTC: las consultas con
 * JdbcClient los convierten aquí a la hora de Lima, para que las pantallas no muestren la hora 5 horas adelantada.
 * Es el mismo instante; solo cambia el desfase con el que se muestra.
 */
public final class HoraLocal {

    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    private HoraLocal() {}

    /** Un timestamptz de la consulta en hora de Lima; null si la columna es null. */
    public static OffsetDateTime leer(ResultSet rs, String columna) throws SQLException {
        var valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.atZoneSameInstant(ZONA).toOffsetDateTime();
    }
}
