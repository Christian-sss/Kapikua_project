package com.projects.kapikua_api.identidad.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(schema = "identidad", name = "consentimiento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consentimiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoConsentimiento tipo;

    @Column(name = "version_texto", nullable = false, length = 20)
    private String versionTexto;

    @Column(name = "otorgado_en", nullable = false)
    private OffsetDateTime otorgadoEn;

    @Column(name = "revocado_en")
    private OffsetDateTime revocadoEn;

    static Consentimiento otorgar(TipoConsentimiento tipo, String versionTexto, OffsetDateTime ahora) {
        if (tipo == null || versionTexto == null || versionTexto.isBlank() || ahora == null) {
            throw new IllegalArgumentException("Consentimiento incompleto");
        }
        var c = new Consentimiento();
        c.tipo = tipo;
        c.versionTexto = versionTexto;
        c.otorgadoEn = ahora;
        return c;
    }

    public boolean estaVigente() {
        return revocadoEn == null;
    }

    void revocar(OffsetDateTime ahora) {
        if (!estaVigente()) throw new IllegalStateException("El consentimiento ya fue revocado");
        this.revocadoEn = ahora;
    }
}
