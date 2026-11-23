package com.example.data.remote.api

/**
 * Parâmetros padrão de consulta à camada 25 (Terras Indígenas) do Geoportal IBAMA / SIGAGEO.
 */
object ArcGisTerritoriesQuerySpec {
    const val BASE_URL =
        "https://pamgia.ibama.gov.br/server/rest/services/SIGAGEO/bases/MapServer/25/"

    const val DEFAULT_WHERE = "1=1"
    const val DEFAULT_OUT_FIELDS =
        "objectid,terrai_cod,terrai_nom,etnia_nome,municipio_,uf_sigla,superficie,fase_ti,modalidade,data_atual"
    const val DEFAULT_RETURN_GEOMETRY = false
    const val DEFAULT_ORDER_BY_FIELDS = "terrai_cod ASC, objectid ASC"
    const val DEFAULT_PAGE_SIZE = 100
    const val DEFAULT_FORMAT = "json"
}
