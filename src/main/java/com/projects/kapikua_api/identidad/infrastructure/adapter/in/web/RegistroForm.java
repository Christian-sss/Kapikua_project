package com.projects.kapikua_api.identidad.infrastructure.adapter.in.web;

import com.projects.kapikua_api.identidad.application.command.RegistrarUsuarioCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Formulario de registro. Valida el formato; las reglas de negocio las aplica el dominio. */
@Getter
@Setter
public class RegistroForm {

    @NotBlank(message = "Ingresa tu DNI")
    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI tiene 8 dígitos")
    private String dni;

    @NotNull(message = "Ingresa tu fecha de nacimiento")
    @Past(message = "La fecha de nacimiento no es válida")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaNacimiento;

    @NotBlank(message = "Ingresa tu correo")
    @Email(message = "El correo no es válido")
    @Size(max = 150)
    private String correo;

    @NotBlank(message = "Ingresa tu celular")
    @Pattern(regexp = "^9[0-9]{8}$", message = "El celular tiene 9 dígitos y empieza con 9")
    private String celular;

    @Size(max = 100)
    private String nombreNegocio;

    @NotBlank(message = "Ingresa una contraseña")
    private String contrasena;

    private boolean aceptaTerminos;

    RegistrarUsuarioCommand aComando() {
        var negocio = nombreNegocio == null || nombreNegocio.isBlank() ? null : nombreNegocio.trim();
        return new RegistrarUsuarioCommand(dni, fechaNacimiento, correo, celular, negocio,
                contrasena, aceptaTerminos);
    }
}
