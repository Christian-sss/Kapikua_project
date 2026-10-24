package com.projects.kapikua_api.identidad.domain.model;


import com.projects.kapikua_api.identidad.domain.error.RegistroError;
import com.projects.kapikua_api.shared.Result;

public class Usuario {

    private Long id;
    private String passwordHash;
    private Rol rolUsuario;


    public Rol getRolUsuario() {
        return rolUsuario;
    }

    public static Result<Usuario, RegistroError> crear() {







        return Result.err(new RegistroError.CelularDuplicado());
    }




}
