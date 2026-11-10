package com.projects.kapikua_api.administracion;

import com.projects.kapikua_api.shared.infrastructure.HoraLocal;
import com.projects.kapikua_api.shared.Dinero;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * CU-16: consultas de SOLO LECTURA sobre los esquemas de todos los contextos. Administración no es un bounded
 * context: no tiene modelo propio ni escribe nada. El DNI se enmascara en el SQL (RN-30, RNF-11): el número
 * completo nunca llega a la aplicación. En pantalla cada reporte muestra hasta {@link #MAX_FILAS} filas y la
 * descarga en Excel hasta {@link #MAX_FILAS_DESCARGA}; los indicadores y gráficos se calculan sobre todo el período.
 */
@Repository
public class ReportesConsulta {

    public static final int MAX_FILAS = 300;
    public static final int MAX_FILAS_DESCARGA = 10_000;

    private static final String DNI = "'****' || right(u.dni, 4)";
    private static final String DIA_LIMA = "(%s at time zone 'America/Lima')::date";

    public record Indicador(String etiqueta, String valor) {}

    public record Reporte<T>(List<T> filas, boolean truncado, List<Indicador> indicadores, List<Grafico> graficos) {
        public boolean vacio() { return filas.isEmpty(); }
    }

    public record FilaCliente(String dni, String nombre, String negocio, String celular, OffsetDateTime registro,
                              Integer nivel, Dinero saldo, String estadoBilletera, boolean activo) {}

    public record FilaTransaccion(Long id, OffsetDateTime fecha, String tipo, Dinero monto, String origen, String destino) {}

    public record FilaCredito(Long id, String codigo, String dni, String nombre, Dinero monto, BigDecimal tea, int cuotas,
                              String estado, OffsetDateTime desembolso, Dinero porCobrar) {}

    public record FilaCuota(Long prestamoId, String dni, int numero, LocalDate vencimiento, Dinero total, Dinero pagado,
                            Dinero moratorio, String estado, LocalDate fechaPago, long diasAtraso) {}

    private final JdbcClient jdbc;
    private final Clock clock;

    public ReportesConsulta(JdbcClient jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- clientes

    public Reporte<FilaCliente> clientes(Periodo p, String estado) {
        return clientes(p, estado, MAX_FILAS);
    }

    public Reporte<FilaCliente> clientes(Periodo p, String estado, int limite) {
        var filtro = """
                from identidad.usuario u
                left join billetera.billetera b on b.usuario_id = u.id
                left join evaluacion.historial_crediticio h on h.usuario_id = u.id
                where u.rol = 'CLIENTE' and u.fecha_creacion >= :desde and u.fecha_creacion < :hasta
                  and (cast(:estado as varchar) is null or b.estado = :estado)""";
        var filas = parametros(jdbc.sql("""
                select %s as dni, u.nombres || ' ' || u.apellidos as nombre, b.nombre_negocio,
                       left(b.numero_celular, 3) || ' *** ' || right(b.numero_celular, 3) as celular,
                       u.fecha_creacion, h.nivel, b.saldo, b.estado, u.activo
                %s order by u.fecha_creacion desc limit %d""".formatted(DNI, filtro, limite + 1)), p, estado)
                .query((rs, n) -> new FilaCliente(rs.getString("dni"), rs.getString("nombre"), rs.getString("nombre_negocio"),
                        rs.getString("celular"), HoraLocal.leer(rs, "fecha_creacion"),
                        rs.getObject("nivel", Integer.class), dinero(rs.getBigDecimal("saldo")), rs.getString("estado"),
                        rs.getBoolean("activo")))
                .list();

        var totales = parametros(jdbc.sql("""
                select count(*) as clientes, count(b.nombre_negocio) as negocios,
                       count(*) filter (where b.estado = 'BLOQUEADA') as bloqueadas, coalesce(sum(b.saldo), 0) as saldo
                """ + filtro), p, estado).query().singleRow();
        var indicadores = List.of(
                new Indicador("Clientes registrados", totales.get("clientes").toString()),
                new Indicador("Con negocio", totales.get("negocios").toString()),
                new Indicador("Billeteras bloqueadas", totales.get("bloqueadas").toString()),
                new Indicador("Saldo en sus billeteras", dinero((BigDecimal) totales.get("saldo")).toString()));

        var porNivel = mapa(parametros(jdbc.sql("""
                select coalesce('Nivel ' || h.nivel, 'Sin evaluar') as etiqueta, count(*) as valor %s
                group by 1 order by 1""".formatted(filtro)), p, estado));
        var graficos = List.of(
                Grafico.de(p.porDia() ? "Registros del día" : "Registros por día", porDia(p, "u.fecha_creacion", "count(*)", filtro, estado), cantidad()),
                Grafico.de("Clientes por nivel crediticio", porNivel, cantidad()));
        return reporte(filas, limite, indicadores, graficos);
    }

    // ---------------------------------------------------------------- transacciones

    public Reporte<FilaTransaccion> transacciones(Periodo p, String tipo) {
        return transacciones(p, tipo, MAX_FILAS);
    }

    public Reporte<FilaTransaccion> transacciones(Periodo p, String tipo, int limite) {
        var filtro = """
                from billetera.transaccion t
                where t.fecha >= :desde and t.fecha < :hasta and (cast(:estado as varchar) is null or t.tipo = :estado)""";
        var titular = """
                (select %s from billetera.movimiento m join billetera.billetera b on b.id = m.billetera_id
                 join identidad.usuario u on u.id = b.usuario_id where m.transaccion_id = t.id and m.monto %s 0 limit 1)""";
        var filas = parametros(jdbc.sql("""
                select t.id, t.fecha, t.tipo, t.monto, %s as origen, %s as destino
                %s order by t.fecha desc, t.id desc limit %d""".formatted(
                        titular.formatted(DNI, "<"), titular.formatted(DNI, ">"), filtro, limite + 1)), p, tipo)
                .query((rs, n) -> {
                    var t = rs.getString("tipo");
                    return new FilaTransaccion(rs.getLong("id"), HoraLocal.leer(rs, "fecha"), t,
                            dinero(rs.getBigDecimal("monto")),
                            contraparte(rs.getString("origen"), t.equals("RECARGA") ? "Pasarela de pagos" : t.equals("DESEMBOLSO") ? "Entidad aliada" : "—"),
                            contraparte(rs.getString("destino"), t.startsWith("PAGO_CUOTA") || t.equals("PAGO_ANTICIPADO") ? "Entidad aliada" : "—"));
                })
                .list();

        var totales = parametros(jdbc.sql("select count(*) as n, coalesce(sum(t.monto), 0) as total " + filtro), p, tipo)
                .query().singleRow();
        var porTipo = mapa(parametros(jdbc.sql("select t.tipo as etiqueta, sum(t.monto) as valor " + filtro + " group by 1 order by 2 desc"), p, tipo));
        var indicadores = List.of(
                new Indicador("Operaciones", totales.get("n").toString()),
                new Indicador("Monto movido", dinero((BigDecimal) totales.get("total")).toString()));
        var graficos = List.of(
                Grafico.de("Monto por tipo", porTipo, montos()),
                Grafico.de(p.porDia() ? "Monto del día" : "Monto por día", porDia(p, "t.fecha", "sum(t.monto)", filtro, tipo), montos()));
        return reporte(filas, limite, indicadores, graficos);
    }

    // ---------------------------------------------------------------- créditos

    public Reporte<FilaCredito> creditos(Periodo p, String estado) {
        return creditos(p, estado, MAX_FILAS);
    }

    public Reporte<FilaCredito> creditos(Periodo p, String estado, int limite) {
        var filtro = """
                from credito.prestamo p join identidad.usuario u on u.id = p.usuario_id
                where p.fecha_desembolso >= :desde and p.fecha_desembolso < :hasta
                  and (cast(:estado as varchar) is null or p.estado = :estado)""";
        var filas = parametros(jdbc.sql("""
                select p.id, p.codigo_credito_efa, %s as dni, u.nombres || ' ' || u.apellidos as nombre, p.monto, p.tea,
                       p.estado, p.fecha_desembolso,
                       (select count(*) from credito.cuota c where c.prestamo_id = p.id and c.estado <> 'ANULADA') as cuotas,
                       (select coalesce(sum(c.capital + c.interes + c.interes_moratorio - c.monto_pagado), 0)
                        from credito.cuota c where c.prestamo_id = p.id and c.estado in ('PENDIENTE', 'VENCIDA')) as por_cobrar
                %s order by p.fecha_desembolso desc limit %d""".formatted(DNI, filtro, limite + 1)), p, estado)
                .query((rs, n) -> new FilaCredito(rs.getLong("id"), rs.getString("codigo_credito_efa"), rs.getString("dni"),
                        rs.getString("nombre"), dinero(rs.getBigDecimal("monto")), rs.getBigDecimal("tea"), rs.getInt("cuotas"),
                        rs.getString("estado"), HoraLocal.leer(rs, "fecha_desembolso"),
                        dinero(rs.getBigDecimal("por_cobrar"))))
                .list();

        var totales = parametros(jdbc.sql("""
                select count(*) as n, coalesce(sum(p.monto), 0) as desembolsado,
                       count(*) filter (where p.estado = 'EN_MORA') as en_mora\s""" + filtro), p, estado).query().singleRow();
        var solicitudes = jdbc.sql("""
                select o.estado as etiqueta, count(*) as valor from credito.oferta o
                where o.fecha >= :desde and o.fecha < :hasta group by 1 order by 2 desc""")
                .param("desde", p.inicio(clock.getZone())).param("hasta", p.fin(clock.getZone()));
        var indicadores = List.of(
                new Indicador("Préstamos desembolsados", totales.get("n").toString()),
                new Indicador("Monto desembolsado", dinero((BigDecimal) totales.get("desembolsado")).toString()),
                new Indicador("En mora", totales.get("en_mora").toString()));
        var graficos = List.of(
                Grafico.de("Préstamos por estado", mapa(parametros(jdbc.sql(
                        "select p.estado as etiqueta, count(*) as valor " + filtro + " group by 1 order by 2 desc"), p, estado)), cantidad()),
                Grafico.de("Solicitudes del período por estado", mapa(solicitudes), cantidad()));
        return reporte(filas, limite, indicadores, graficos);
    }

    // ---------------------------------------------------------------- cobranza

    public Reporte<FilaCuota> cobranza(Periodo p, String estado) {
        return cobranza(p, estado, MAX_FILAS);
    }

    public Reporte<FilaCuota> cobranza(Periodo p, String estado, int limite) {
        var filtro = """
                from credito.cuota c join credito.prestamo p on p.id = c.prestamo_id join identidad.usuario u on u.id = p.usuario_id
                where c.fecha_vencimiento >= :dia_desde and c.fecha_vencimiento < :dia_hasta and c.estado <> 'ANULADA'
                  and (cast(:estado as varchar) is null or c.estado = :estado)""";
        var hoy = LocalDate.now(clock);
        var filas = cuotas(jdbc.sql("""
                select p.id as prestamo, %s as dni, c.numero, c.fecha_vencimiento,
                       c.capital + c.interes + c.interes_moratorio as total, c.monto_pagado, c.interes_moratorio, c.estado, c.fecha_pago
                %s order by c.fecha_vencimiento, p.id, c.numero limit %d""".formatted(DNI, filtro, limite + 1)), p, estado)
                .query((rs, n) -> {
                    var venc = rs.getObject("fecha_vencimiento", LocalDate.class);
                    var pago = rs.getObject("fecha_pago", LocalDate.class);
                    var est = rs.getString("estado");
                    var hasta = pago != null ? pago : est.equals("VENCIDA") ? hoy : venc;
                    return new FilaCuota(rs.getLong("prestamo"), rs.getString("dni"), rs.getInt("numero"), venc,
                            dinero(rs.getBigDecimal("total")), dinero(rs.getBigDecimal("monto_pagado")),
                            dinero(rs.getBigDecimal("interes_moratorio")), est, pago,
                            Math.max(0, ChronoUnit.DAYS.between(venc, hasta)));
                })
                .list();

        var totales = cuotas(jdbc.sql("""
                select count(*) as n, coalesce(sum(c.capital + c.interes + c.interes_moratorio), 0) as esperado,
                       coalesce(sum(c.monto_pagado), 0) as cobrado, coalesce(sum(c.interes_moratorio), 0) as moratorio,
                       count(*) filter (where c.estado = 'PAGADA' and c.fecha_pago <= c.fecha_vencimiento) as a_tiempo,
                       count(*) filter (where c.fecha_vencimiento < :hoy) as vencibles\s""" + filtro), p, estado)
                .param("hoy", hoy).query().singleRow();
        long vencibles = ((Number) totales.get("vencibles")).longValue();
        long aTiempo = ((Number) totales.get("a_tiempo")).longValue();
        var pagosPorOrigen = jdbc.sql("""
                select case when origen = 'AVAL' then 'Pagado por el aval'
                            when automatico then 'Cobro automático' else 'Pago manual' end as etiqueta,
                       sum(monto) as valor
                from credito.pago where fecha >= :desde and fecha < :hasta group by 1 order by 2 desc""")
                .param("desde", p.inicio(clock.getZone())).param("hasta", p.fin(clock.getZone()));
        var retenido = jdbc.sql("select coalesce(sum(monto), 0) from credito.retencion where fecha >= :desde and fecha < :hasta")
                .param("desde", p.inicio(clock.getZone())).param("hasta", p.fin(clock.getZone())).query(BigDecimal.class).single();
        var indicadores = List.of(
                new Indicador("Cuotas del período", totales.get("n").toString()),
                new Indicador("Monto esperado", dinero((BigDecimal) totales.get("esperado")).toString()),
                new Indicador("Monto cobrado", dinero((BigDecimal) totales.get("cobrado")).toString()),
                new Indicador("Interés moratorio", dinero((BigDecimal) totales.get("moratorio")).toString()),
                new Indicador("Pagadas a tiempo", vencibles == 0 ? "—" : (aTiempo * 100 / vencibles) + " % de las ya vencidas"),
                new Indicador("Retenido de ventas", dinero(retenido).toString()));
        var graficos = List.of(
                Grafico.de("Cuotas por estado", mapa(cuotas(jdbc.sql(
                        "select c.estado as etiqueta, count(*) as valor " + filtro + " group by 1 order by 2 desc"), p, estado)), cantidad()),
                Grafico.de("Pagos del período por origen", mapa(pagosPorOrigen), montos()));
        return reporte(filas, limite, indicadores, graficos);
    }

    // ---------------------------------------------------------------- apoyo

    private JdbcClient.StatementSpec parametros(JdbcClient.StatementSpec sql, Periodo p, String estado) {
        return sql.param("desde", p.inicio(clock.getZone())).param("hasta", p.fin(clock.getZone())).param("estado", estado);
    }

    private JdbcClient.StatementSpec cuotas(JdbcClient.StatementSpec sql, Periodo p, String estado) {
        return sql.param("dia_desde", p.desde()).param("dia_hasta", p.hasta()).param("estado", estado);
    }

    /** Un valor por día del período (los días sin datos van en cero). */
    private Map<String, BigDecimal> porDia(Periodo p, String columnaFecha, String agregado, String filtro, String estado) {
        var dia = DIA_LIMA.formatted(columnaFecha);
        var valores = new LinkedHashMap<LocalDate, BigDecimal>();
        parametros(jdbc.sql("select %s as dia, %s as valor %s group by 1".formatted(dia, agregado, filtro)), p, estado)
                .query((rs, n) -> valores.put(rs.getObject("dia", LocalDate.class), rs.getBigDecimal("valor")))
                .list();
        var datos = new LinkedHashMap<String, BigDecimal>();
        for (var d = p.desde(); d.isBefore(p.hasta()); d = d.plusDays(1)) {
            datos.put(String.format("%02d", d.getDayOfMonth()), valores.getOrDefault(d, BigDecimal.ZERO));
        }
        return datos;
    }

    private static Map<String, BigDecimal> mapa(JdbcClient.StatementSpec sql) {
        var datos = new LinkedHashMap<String, BigDecimal>();
        sql.query((rs, n) -> datos.put(rs.getString("etiqueta"), rs.getBigDecimal("valor"))).list();
        return datos;
    }

    private static <T> Reporte<T> reporte(List<T> filas, int limite, List<Indicador> indicadores, List<Grafico> graficos) {
        var truncado = filas.size() > limite;
        return new Reporte<>(truncado ? filas.subList(0, limite) : filas, truncado, indicadores, graficos);
    }

    private static String contraparte(String dni, String porDefecto) {
        return dni != null ? "DNI " + dni : porDefecto;
    }

    private static Dinero dinero(BigDecimal valor) {
        return valor == null ? Dinero.CERO : Dinero.de(valor);
    }

    private static Function<BigDecimal, String> cantidad() {
        return v -> v.stripTrailingZeros().toPlainString();
    }

    private static Function<BigDecimal, String> montos() {
        return v -> Dinero.de(v).toString();
    }
}
