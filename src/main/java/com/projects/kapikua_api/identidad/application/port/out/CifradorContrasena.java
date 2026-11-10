package com.projects.kapikua_api.identidad.application.port.out;

public interface CifradorContrasena {

    String cifrar(String clave);

    boolean coincide(String clave, String hash);
}
