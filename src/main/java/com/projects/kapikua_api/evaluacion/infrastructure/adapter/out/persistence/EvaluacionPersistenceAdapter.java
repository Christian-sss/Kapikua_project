package com.projects.kapikua_api.evaluacion.infrastructure.adapter.out.persistence;

import com.projects.kapikua_api.evaluacion.application.port.out.EvaluacionRepositoryPort;
import com.projects.kapikua_api.evaluacion.domain.model.CondicionesSugeridas;
import com.projects.kapikua_api.evaluacion.domain.model.DecisionRecomendacion;
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
        if (e.esApta()) {
            var c = e.condiciones();
            j.montoSugerido = c.monto().valor();
            j.cuotasSugeridas = c.cuotas();
            j.teaPropuesta = c.tea();
            j.cuotaSugerida = c.cuota().valor();
            j.porcentajeRetencion = BigDecimal.valueOf(c.porcentajeRetencion());
        }
        j.requiereAval = e.requiereAval();
        j.motivo = e.motivo().isEmpty() ? null : e.motivo();
        j.factores = JSON.writeValueAsString(e.factores());
        j.fecha = e.fecha();
        return e.conId(repositorio.saveAndFlush(j).id);
    }

    @Override
    public Optional<Evaluacion> porId(Long id) {
        return repositorio.findById(id).map(EvaluacionPersistenceAdapter::aDominio);
    }

    private static Evaluacion aDominio(EvaluacionJpa j) {
        var condiciones = j.montoSugerido == null ? CondicionesSugeridas.ninguna()
                : new CondicionesSugeridas(Dinero.de(j.montoSugerido), j.cuotasSugeridas, j.teaPropuesta,
                        Dinero.de(j.cuotaSugerida), j.porcentajeRetencion.intValue());
        var solicitud = new SolicitudCredito(Dinero.de(j.montoSolicitado), j.cuotasSolicitadas, j.destino);
        var detalleScore = new Evaluacion.DetalleScore(j.score, JSON.readValue(j.factores, FACTORES));
        var decision = new DecisionRecomendacion(j.recomendacion, condiciones, j.requiereAval, j.motivo);
        return new Evaluacion(j.id, new UsuarioId(j.usuarioId), solicitud, j.clasificacionSbs,
                j.deudaTotalSistema == null ? null : Dinero.de(j.deudaTotalSistema), detalleScore, decision, j.fecha);
    }
}

interface EvaluacionJpaRepository extends JpaRepository<EvaluacionJpa, Long> {}
