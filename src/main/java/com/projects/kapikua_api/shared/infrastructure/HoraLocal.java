package com.projects.kapikua_api.shared.infrastructure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class HoraLocal {
    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    private HoraLocal() {}

    public static OffsetDateTime leer(ResultSet rs, String columna) throws SQLException {
        var valor = rs.getObject(columna, OffsetDateTime.class);
        return valor == null ? null : valor.atZoneSameInstant(ZONA).toOffsetDateTime();
    }
}
