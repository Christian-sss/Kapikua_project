package com.projects.kapikua_api.evaluacion.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.evaluacion.application.port.out.EvaluacionRepositoryPort;
import com.projects.kapikua_api.evaluacion.domain.model.CondicionesSugeridas;
import com.projects.kapikua_api.evaluacion.domain.model.Evaluacion;
import com.projects.kapikua_api.evaluacion.domain.model.FactorEvaluado;
import com.projects.kapikua_api.evaluacion.domain.model.SolicitudCredito;
import com.projects.kapikua_api.shared.Dinero;
import com.projects.kapikua_api.shared.UsuarioId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
class EvaluacionPersistenceAdapter implements EvaluacionRepositoryPort {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final TypeReference<List<FactorEvaluado>> FACTORES = new TypeReference<>() {};

    private final EvaluacionJpaRepository repositorio;

    EvaluacionPersistenceAdapter(EvaluacionJpaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    public Evaluacion registrar(Evaluacion e) {
        var j = new EvaluacionJpa();
        j.usuarioId = e.usuarioId().valor();
        j.montoSolicitado = e.solicitud().monto().valor();
        j.cuotasSolicitadas = e.solicitud().cuotas();
        j.destino = e.solicitud().destino();
        j.clasificacionSbs = e.clasificacionSbs();
        j.deudaTotalSistema = e.deudaTotalSistema() == null ? null : e.deudaTotalSistema().valor();
        j.score = e.score();
        j.recomendacion = e.recomendacion();
        if (e.condiciones() != null) {
            var c = e.condiciones();
            j.montoSugerido = c.monto().valor();
            j.cuotasSugeridas = c.cuotas();
            j.teaPropuesta = c.tea();
            j.cuotaSugerida = c.cuota().valor();
            j.porcentajeRetencion = BigDecimal.valueOf(c.porcentajeRetencion());
        }
        j.requiereAval = e.requiereAval();
        j.motivo = e.motivo();
        j.factores = JSON.writeValueAsString(e.factores());
        j.fecha = e.fecha();
        return e.conId(repositorio.saveAndFlush(j).id);
    }

    @Override
    public Optional<Evaluacion> porId(Long id) {
        return repositorio.findById(id).map(EvaluacionPersistenceAdapter::aDominio);
    }

    private static Evaluacion aDominio(EvaluacionJpa j) {
        var condiciones = j.montoSugerido == null ? null : new CondicionesSugeridas(Dinero.de(j.montoSugerido),
                j.cuotasSugeridas, j.teaPropuesta, Dinero.de(j.cuotaSugerida), j.porcentajeRetencion.intValue());
        return new Evaluacion(j.id, new UsuarioId(j.usuarioId),
                new SolicitudCredito(Dinero.de(j.montoSolicitado), j.cuotasSolicitadas, j.destino),
                j.clasificacionSbs, j.deudaTotalSistema == null ? null : Dinero.de(j.deudaTotalSistema), j.score,
                j.recomendacion, condiciones, j.requiereAval, j.motivo, JSON.readValue(j.factores, FACTORES), j.fecha);
    }
}

interface EvaluacionJpaRepository extends JpaRepository<EvaluacionJpa, Long> {}
