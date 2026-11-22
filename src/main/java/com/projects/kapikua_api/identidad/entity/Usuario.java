package com.projects.kapikua_api.identidad.entity;

import com.projects.kapikua_api.identidad.error.RegistroError;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Entity
@Table(schema = "identidad", name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {
    private static final int EDAD_MINIMA = 18;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 8)
    private String dni;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Rol rol;

    @Column(name = "identidad_validada", nullable = false)
    private boolean identidadValidada;

    @Column(name = "fecha_validacion")
    private OffsetDateTime fechaValidacion;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "fecha_creacion", nullable = false)
    private OffsetDateTime fechaCreacion;

    @Getter(AccessLevel.NONE)
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private List<Consentimiento> consentimientos = new ArrayList<>();

    public static Result<Usuario, RegistroError> registrar(DatosRegistroUsuario datos,
                                                           String passwordHash,
                                                           Optional<IdentidadVerificada> identidadReniec,
                                                           boolean figuraEnListas,
                                                           String versionTerminos,
                                                           OffsetDateTime ahora) {
        if (passwordHash == null || passwordHash.isBlank() || ahora == null) {
            throw new IllegalArgumentException("Faltan datos para registrar al usuario");
        }
        var hoy = ahora.toLocalDate();
        if (datos.validar(hoy) instanceof Result.Err<Void, RegistroError> err) {
            return Result.err(err.error());
        }
        if (identidadReniec.isEmpty()) {
            return Result.err(new RegistroError.DniNoValidado());
        }
        if (Period.between(datos.fechaNacimiento(), hoy).getYears() < EDAD_MINIMA) {
            return Result.err(new RegistroError.MenorDeEdad());
        }
        if (figuraEnListas) {
            return Result.err(new RegistroError.EnListasRestrictivas());
        }

        var u = new Usuario();
        u.dni = datos.dni();
        u.correo = datos.correo();
        u.passwordHash = passwordHash;
        u.nombres = identidadReniec.get().nombres();
        u.apellidos = identidadReniec.get().apellidos();
        u.fechaNacimiento = datos.fechaNacimiento();
        u.rol = Rol.CLIENTE;
        u.identidadValidada = true;
        u.fechaValidacion = ahora;
        u.activo = true;
        u.fechaCreacion = ahora;
        u.consentimientos.add(
                Consentimiento.otorgar(TipoConsentimiento.TERMINOS_SERVICIO, versionTerminos, ahora));
        return Result.ok(u);
    }

    public UsuarioId usuarioId() {
        if (id == null) throw new IllegalStateException("El usuario aún no se ha guardado");
        return new UsuarioId(id);
    }

    public String dniEnmascarado() {
        return "****" + dni.substring(dni.length() - 4);
    }

    public void otorgarConsentimiento(TipoConsentimiento tipo, String versionTexto, OffsetDateTime ahora) {
        if (tieneConsentimientoVigente(tipo)) return;
        consentimientos.add(Consentimiento.otorgar(tipo, versionTexto, ahora));
    }

    public void revocarConsentimiento(TipoConsentimiento tipo, OffsetDateTime ahora) {
        consentimientos.stream()
                .filter(c -> c.getTipo() == tipo && c.estaVigente())
                .forEach(c -> c.revocar(ahora));
    }

    public boolean tieneConsentimientoVigente(TipoConsentimiento tipo) {
        return consentimientos.stream().anyMatch(c -> c.getTipo() == tipo && c.estaVigente());
    }

    public List<Consentimiento> consentimientos() {
        return Collections.unmodifiableList(consentimientos);
    }
}
