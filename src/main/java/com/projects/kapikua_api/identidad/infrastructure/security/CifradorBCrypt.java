package com.projects.kapikua_api.identidad.infrastructure.security;

import com.projects.kapikua_api.identidad.application.port.out.CifradorContrasena;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class CifradorBCrypt implements CifradorContrasena {

    private final PasswordEncoder encoder;

    CifradorBCrypt(PasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String cifrar(String clave) {
        return encoder.encode(clave);
    }

    @Override
    public boolean coincide(String clave, String hash) {
        return encoder.matches(clave, hash);
    }
}
