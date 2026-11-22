package com.projects.kapikua_api.identidad.dto;

import com.projects.kapikua_api.identidad.entity.DatosRegistroUsuario;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Datos del formulario de registro (CU-01). Los nombres y apellidos los escribe el cliente tal como figuran
 * en su DNI y se contrastan con RENIEC; si llegan vacíos se usan los que devuelva RENIEC.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroForm {
    @NotBlank(message = "Ingresa tu DNI")
    @Pattern(regexp = "^[0-9]{8}$", message = "El DNI tiene 8 dígitos")
    private String dni;

    @NotNull(message = "Ingresa tu fecha de nacimiento")
    @Past(message = "La fecha de nacimiento no es válida")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaNacimiento;

    @NotBlank(message = "Ingresa tus nombres")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombres;

    @NotBlank(message = "Ingresa tus apellidos")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String apellidos;

    @NotBlank(message = "Ingresa tu correo")
    @Pattern(regexp = DatosRegistroUsuario.FORMATO_CORREO, message = "El correo no es válido")
    @Size(max = 150)
    @Email
    private String correo;

    @NotBlank(message = "Ingresa tu celular")
    @Pattern(regexp = "^9[0-9]{8}$", message = "El celular tiene 9 dígitos y empieza con 9")
    private String celular;

    @Size(max = 100)
    private String nombreNegocio;

    @NotBlank(message = "Ingresa una contraseña")
    private String contrasena;

    private boolean aceptaTerminos;

    /** Registro sin nombres escritos: se toman los que devuelva RENIEC. */
    public RegistroForm(String dni, LocalDate fechaNacimiento, String correo, String celular,
                        String nombreNegocio, String contrasena, boolean aceptaTerminos) {
        this(dni, fechaNacimiento, null, null, correo, celular, nombreNegocio, contrasena, aceptaTerminos);
    }

    @Override
    public String toString() {
        return "RegistroForm[dni=****, correo=" + correo + ", contrasena=****]";
    }
}
