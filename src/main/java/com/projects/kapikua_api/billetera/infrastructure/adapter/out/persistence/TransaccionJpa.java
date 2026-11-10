package com.projects.kapikua_api.billetera.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.billetera.domain.model.TipoTransaccion;
import com.projects.kapikua_api.billetera.domain.model.Transaccion;
import com.projects.kapikua_api.shared.Dinero;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** billetera.transaccion y billetera.movimiento: solo inserción (triggers de la base, RNF-10). */
@Entity
@Immutable
@Table(schema = "billetera", name = "transaccion")
class TransaccionJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    TipoTransaccion tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal monto;

    @Column(nullable = false, unique = true, length = 60)
    String referencia;

    @Column(length = 150)
    String descripcion;

    @Column(nullable = false)
    OffsetDateTime fecha;

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "transaccion_id", nullable = false, updatable = false)
    List<MovimientoJpa> movimientos = new ArrayList<>();

    protected TransaccionJpa() {}

    static TransaccionJpa desde(Transaccion t) {
        var j = new TransaccionJpa();
        j.tipo = t.tipo();
        j.monto = t.monto().valor();
        j.referencia = t.referencia();
        j.descripcion = t.descripcion();
        j.fecha = t.fecha();
        t.movimientos().forEach(m -> j.movimientos.add(new MovimientoJpa(m.billeteraId(), m.monto(),
                m.saldoPosterior().valor(), t.fecha())));
        return j;
    }

    Transaccion aDominio() {
        return new Transaccion(id, tipo, Dinero.de(monto), referencia, descripcion, fecha,
                movimientos.stream()
                        .map(m -> new Transaccion.Movimiento(m.billeteraId, m.monto, Dinero.de(m.saldoPosterior)))
                        .toList());
    }
}

@Entity
@Immutable
@Table(schema = "billetera", name = "movimiento")
class MovimientoJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "billetera_id", nullable = false)
    Long billeteraId;

    /** Positivo entra, negativo sale. */
    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal monto;

    @Column(name = "saldo_posterior", nullable = false, precision = 12, scale = 2)
    BigDecimal saldoPosterior;

    @Column(nullable = false)
    OffsetDateTime fecha;

    protected MovimientoJpa() {}

    MovimientoJpa(Long billeteraId, BigDecimal monto, BigDecimal saldoPosterior, OffsetDateTime fecha) {
        this.billeteraId = billeteraId;
        this.monto = monto;
        this.saldoPosterior = saldoPosterior;
        this.fecha = fecha;
    }
}
