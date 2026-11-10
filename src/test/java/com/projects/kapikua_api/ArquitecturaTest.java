package com.projects.kapikua_api;

import com.projects.kapikua_api.evaluacion.domain.model.Evaluacion;
import com.projects.kapikua_api.evaluacion.domain.service.PoliticaCredito;
import com.projects.kapikua_api.shared.ErrorDominio;
import com.projects.kapikua_api.shared.domain.EventoDominio;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.projects.kapikua_api", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {
    private static final String RAIZ = "com.projects.kapikua_api.";
    private static final Set<String> CONTEXTOS = Set.of("identidad", "billetera", "evaluacion", "credito", "cumplimiento", "administracion");

    private static final Set<String> DOMINIO_CON_JPA = Set.of(
            RAIZ + "identidad.domain.model.Usuario",
            RAIZ + "identidad.domain.model.Consentimiento",
            RAIZ + "cumplimiento.domain.model.Parametro");

    private static final Set<String> LENGUAJE_PUBLICADO = Set.of(
            RAIZ + "evaluacion.domain.model.Destino",
            RAIZ + "evaluacion.domain.model.Recomendacion",
            RAIZ + "evaluacion.domain.model.CondicionesSugeridas");

    @ArchTest
    static final ArchRule capasHexagonales = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Dominio").definedBy("..kapikua_api.*.domain..")
            .layer("Aplicacion").definedBy("..kapikua_api.*.application..")
            .layer("Infraestructura").definedBy("..kapikua_api.*.infrastructure..")
            .whereLayer("Infraestructura").mayNotBeAccessedByAnyLayer()
            .whereLayer("Aplicacion").mayOnlyBeAccessedByLayers("Infraestructura")
            .whereLayer("Dominio").mayOnlyBeAccessedByLayers("Aplicacion", "Infraestructura")
            .because("el dominio no conoce casos de uso ni adaptadores, y la aplicación no conoce adaptadores");

    @ArchTest
    static final ArchRule dominioSinSpring = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
            .because("el dominio es Java puro y se prueba sin levantar Spring");

    @ArchTest
    static final ArchRule dominioSinPersistencia = noClasses()
            .that().resideInAPackage("..domain..")
            .and().doNotHaveFullyQualifiedName(RAIZ + "identidad.domain.model.Usuario")
            .and().doNotHaveFullyQualifiedName(RAIZ + "identidad.domain.model.Consentimiento")
            .and().doNotHaveFullyQualifiedName(RAIZ + "cumplimiento.domain.model.Parametro")
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "org.hibernate..", "java.sql..")
            .because("la persistencia vive en los adaptadores de salida (excepciones en DOMINIO_CON_JPA)");

    @ArchTest
    static final ArchRule aplicacionSinDetallesTecnicos = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.web..", "org.springframework.jdbc..", "org.springframework.data..",
                    "org.springframework.security..", "org.springframework.ui..",
                    "jakarta.persistence..", "jakarta.servlet..", "org.hibernate..")
            .because("los casos de uso solo hablan con puertos; HTTP, SQL y seguridad son adaptadores");

    @ArchTest
    static final ArchRule adaptadoresUsanPuertos = noClasses()
            .that().resideInAPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAPackage("..application.service..")
            .because("los adaptadores entran por los puertos de entrada, no por la implementación del caso de uso");

    @ArchTest
    static final ArchRule dominioAislado = classes()
            .that().resideInAPackage("..kapikua_api.*.domain..")
            .should(soloDependerDe("su propio contexto o del kernel compartido", (origen, destino) -> false))
            .because("el modelo de un contexto no se mezcla con el de otro");

    @ArchTest
    static final ArchRule aplicacionUsaPuertosDeOtros = classes()
            .that().resideInAPackage("..kapikua_api.*.application..")
            .should(soloDependerDe("su propio contexto, el kernel compartido o los puertos de entrada de otro contexto",
                    (origen, destino) -> destino.getPackageName().contains(".application.port.in")
                            || LENGUAJE_PUBLICADO.contains(destino.getName())))
            .because("un contexto pide algo a otro solo por sus casos de uso (o por el lenguaje publicado)");

    @ArchTest
    static final ArchRule eventosDeDominioNoSalenDelContexto = classes()
            .should(new ArchCondition<>("no usar eventos de dominio de otro contexto") {
                @Override
                public void check(JavaClass origen, ConditionEvents eventos) {
                    for (Dependency d : origen.getDirectDependenciesFromSelf()) {
                        var destino = d.getTargetClass();
                        if (destino.getPackageName().endsWith(".domain.event")
                                && contexto(destino).isPresent() && !contexto(destino).equals(contexto(origen))) {
                            eventos.add(SimpleConditionEvent.violated(d, d.getDescription()));
                        }
                    }
                }
            })
            .because("hacia afuera se publican eventos de integración (shared.evento) a través del traductor del contexto");

    @ArchTest
    static final ArchRule kernelCompartidoIndependiente = noClasses()
            .that().resideInAPackage("..kapikua_api.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..kapikua_api.identidad..", "..kapikua_api.billetera..", "..kapikua_api.evaluacion..",
                    "..kapikua_api.credito..", "..kapikua_api.cumplimiento..", "..kapikua_api.administracion..")
            .because("el kernel compartido no conoce a ningún contexto");

    @ArchTest
    static final ArchRule sinCiclosEntreModelos = slices()
            .matching("..kapikua_api.(*).domain..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule sinCiclosEntreCasosDeUso = slices()
            .matching("..kapikua_api.(*).application..")
            .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule eventosDeDominio = classes()
            .that().implement(EventoDominio.class)
            .should().resideInAPackage("..domain.event..")
            .andShould().beRecords();

    @ArchTest
    static final ArchRule paqueteDeEventos = classes()
            .that().resideInAPackage("..domain.event..")
            .should().implement(EventoDominio.class);

    @ArchTest
    static final ArchRule erroresDeDominio = classes()
            .that().resideInAPackage("..domain.error..")
            .should().beAssignableTo(ErrorDominio.class)
            .because("los errores de negocio viajan en Result, no como excepciones");

    @ArchTest
    static final ArchRule soloLaPoliticaEmiteEvaluaciones = methods()
            .that().areDeclaredIn(Evaluacion.class)
            .and().haveName("emitir").or().haveName("noApto")
            .should().onlyBeCalled().byClassesThat().belongToAnyOf(PoliticaCredito.class)
            .because("el resultado de una evaluación lo decide la política de crédito (servicio de dominio)");

    @ArchTest
    static final ArchRule entidadesJpaEnPersistencia = classes()
            .that().areAnnotatedWith(Entity.class)
            .and().doNotHaveFullyQualifiedName(RAIZ + "identidad.domain.model.Usuario")
            .and().doNotHaveFullyQualifiedName(RAIZ + "identidad.domain.model.Consentimiento")
            .and().doNotHaveFullyQualifiedName(RAIZ + "cumplimiento.domain.model.Parametro")
            .should().resideInAPackage("..infrastructure.adapter.out.persistence..");

    @ArchTest
    static final ArchRule repositoriosEnPersistencia = classes()
            .that().areAnnotatedWith(Repository.class)
            .should().resideInAnyPackage("..infrastructure.adapter.out.persistence..", "..kapikua_api.administracion..");

    @ArchTest
    static final ArchRule controladoresEnWeb = classes()
            .that().areMetaAnnotatedWith(Controller.class)
            .should().resideInAnyPackage("..infrastructure.adapter.in.web..", "..shared.infrastructure..",
                    "..kapikua_api.administracion..");

    private static Optional<String> contexto(JavaClass clase) {
        var nombre = clase.getName();
        if (!nombre.startsWith(RAIZ)) return Optional.empty();
        var primero = nombre.substring(RAIZ.length()).split("\\.")[0];
        return CONTEXTOS.contains(primero) ? Optional.of(primero) : Optional.empty();
    }

    private interface Permitido {
        boolean test(JavaClass origen, JavaClass destino);
    }

    private static ArchCondition<JavaClass> soloDependerDe(String descripcion, Permitido permitido) {
        return new ArchCondition<>("solo depender de " + descripcion) {
            @Override
            public void check(JavaClass origen, ConditionEvents eventos) {
                var propio = contexto(origen);
                for (Dependency d : origen.getDirectDependenciesFromSelf()) {
                    var destino = d.getTargetClass();
                    var otro = contexto(destino);
                    if (otro.isPresent() && !otro.equals(propio) && !permitido.test(origen, destino)) {
                        eventos.add(SimpleConditionEvent.violated(d, d.getDescription()));
                    }
                }
            }
        };
    }
}
