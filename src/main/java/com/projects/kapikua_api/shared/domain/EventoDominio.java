package com.projects.kapikua_api.shared.domain;

/**
 * Algo que ocurrió en el dominio y que a los expertos les importa (DDD). Lo registra el agregado en el
 * momento del hecho; la aplicación solo lo publica después de guardar. Los eventos de dominio no salen de su
 * contexto: cada contexto los traduce a eventos de integración ({@code shared.evento}) cuando otro los necesita.
 */
public interface EventoDominio {
}
