package com.projects.kapikua_api.cumplimiento.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Immutable
@Table(schema = "cumplimiento", name = "parametro")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Parametro {
    @Id
    private Long id;

    @Column(nullable = false, length = 40)
    private String codigo;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal valor;

    @Column(name = "vigente_desde", nullable = false)
    private LocalDate vigenteDesde;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(nullable = false, length = 150)
    private String fuente;

    public static Parametro reconstituir(Long id, String codigo, BigDecimal valor, LocalDate vigenteDesde,
                                         LocalDate vigenteHasta, String fuente) {
        var p = new Parametro();
        p.id = id;
        p.codigo = codigo;
        p.valor = valor;
        p.vigenteDesde = vigenteDesde;
        p.vigenteHasta = vigenteHasta;
        p.fuente = fuente;
        return p;
    }

    public boolean estaVigente(LocalDate fecha) {
        return !fecha.isBefore(vigenteDesde) && (vigenteHasta == null || !fecha.isAfter(vigenteHasta));
    }
}
