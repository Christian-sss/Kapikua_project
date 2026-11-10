package com.projects.kapikua_api.identidad.domain.model;

import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.identidad.domain.event.UsuarioRegistrado;
import com.projects.kapikua_api.shared.Result;
import com.projects.kapikua_api.shared.UsuarioId;
import com.projects.kapikua_api.shared.domain.AgregadoRaiz;
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
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
import java.util.regex.Pattern;

@Entity
@Table(schema = "identidad", name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario extends AgregadoRaiz {
    private static final Pattern DNI = Pattern.compile("^[0-9]{8}$");
    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int EDAD_MINIMA = 18;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    @Getter(AccessLevel.NONE)
    private String celularRegistro;

    @Transient
    @Getter(AccessLevel.NONE)
    private String nombreNegocioRegistro;

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

    public static Result<Usuario, RegistroError> registrar(String dni,
                                                           String correo,
                                                           String passwordHash,
                                                           LocalDate fechaNacimiento,
                                                           Optional<IdentidadVerificada> identidadReniec,
                                                           boolean figuraEnListas,
                                                           String versionTerminos,
                                                           String celular,
                                                           String nombreNegocio,
                                                           OffsetDateTime ahora) {
        if (passwordHash == null || passwordHash.isBlank() || ahora == null) {
            throw new IllegalArgumentException("Faltan datos para registrar al usuario");
        }
        if (dni == null || !DNI.matcher(dni).matches()) {
            return Result.err(new RegistroError.DatoInvalido("DNI"));
        }
        var correoNormalizado = correo == null ? "" : correo.trim().toLowerCase();
        if (!CORREO.matcher(correoNormalizado).matches()) {
            return Result.err(new RegistroError.DatoInvalido("correo"));
        }
        if (fechaNacimiento == null || fechaNacimiento.isAfter(ahora.toLocalDate())) {
            return Result.err(new RegistroError.DatoInvalido("fecha de nacimiento"));
        }
        if (identidadReniec.isEmpty()) {
            return Result.err(new RegistroError.DniNoValidado());
        }
        if (Period.between(fechaNacimiento, ahora.toLocalDate()).getYears() < EDAD_MINIMA) {
            return Result.err(new RegistroError.MenorDeEdad());
        }
        if (figuraEnListas) {
            return Result.err(new RegistroError.EnListasRestrictivas());
        }

        var u = new Usuario();
        u.dni = dni;
        u.correo = correoNormalizado;
        u.passwordHash = passwordHash;
        u.nombres = identidadReniec.get().nombres();
        u.apellidos = identidadReniec.get().apellidos();
        u.fechaNacimiento = fechaNacimiento;
        u.rol = Rol.CLIENTE;
        u.identidadValidada = true;
        u.fechaValidacion = ahora;
        u.activo = true;
        u.fechaCreacion = ahora;
        u.celularRegistro = celular;
        u.nombreNegocioRegistro = nombreNegocio;
        u.consentimientos.add(
                Consentimiento.otorgar(TipoConsentimiento.TERMINOS_SERVICIO, versionTerminos, ahora));
        return Result.ok(u);
    }

    @PostPersist
    void alGuardarseNuevo() {
        if (celularRegistro != null) registrar(new UsuarioRegistrado(new UsuarioId(id), celularRegistro, nombreNegocioRegistro));
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
