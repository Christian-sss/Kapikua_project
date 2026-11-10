package com.projects.kapikua_api.shared;

/**
 * Lo único que el resto de la aplicación necesita saber del usuario de la petición.
 * Lo implementa el principal de Identidad; así el kernel compartido no depende de Identidad.
 */
public interface UsuarioAutenticado {

    UsuarioId usuarioId();
}
