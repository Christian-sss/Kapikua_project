package com.projects.kapikua_api.billetera.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.billetera.domain.model.Billetera;
import com.projects.kapikua_api.billetera.domain.model.Celular;
import com.projects.kapikua_api.billetera.domain.model.EstadoBilletera;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Entidad JPA de billetera.billetera. El modelo de dominio es {@link Billetera}. */
@Entity
@Table(schema = "billetera", name = "billetera")
class BilleteraJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "usuario_id", nullable = false, unique = true)
    Long usuarioId;

    @Column(name = "numero_celular", nullable = false, unique = true, length = 9)
    String numeroCelular;

    @Column(name = "nombre_negocio", length = 100)
    String nombreNegocio;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal saldo;

    @Column(name = "saldo_apartado", nullable = false, precision = 12, scale = 2)
    BigDecimal saldoApartado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    EstadoBilletera estado;

    @Column(name = "motivo_bloqueo", length = 150)
    String motivoBloqueo;

    @Column(name = "fecha_apertura", nullable = false)
    OffsetDateTime fechaApertura;

    /** Bloqueo optimista (RNF-08). */
    @Version
    @Column(nullable = false)
    Long version;

    protected BilleteraJpa() {}

    static BilleteraJpa desde(Billetera b) {
        var j = new BilleteraJpa();
        j.id = b.getId();
        j.usuarioId = b.getUsuarioId().valor();
        j.numeroCelular = b.getCelular().numero();
        j.nombreNegocio = b.getNombreNegocio();
        j.saldo = b.getSaldo().valor();
        j.saldoApartado = b.getSaldoApartado().valor();
        j.estado = b.getEstado();
        j.motivoBloqueo = b.getMotivoBloqueo();
        j.fechaApertura = b.getFechaApertura();
        j.version = b.getVersion();
        return j;
    }

    Billetera aDominio() {
        return Billetera.reconstituir(id, new UsuarioId(usuarioId), new Celular(numeroCelular), nombreNegocio,
                Dinero.de(saldo), Dinero.de(saldoApartado), estado, motivoBloqueo, fechaApertura, version);
    }
}
