# Contrato e Integração ArcGIS REST — Camada 25 (IBAMA / SIGAGEO) — Etapa 1

> **Escopo:** Documentação técnica da Etapa 1 da adaptação do projeto **Voz Ancestral** para consulta paginada de Terras Indígenas via API ArcGIS REST do Geoportal IBAMA / SIGAGEO.

---

## 1. Endpoints Configurados

- **Base URL (`NetworkModule` / `ArcGisTerritoriesQuerySpec.BASE_URL`):**
  `https://pamgia.ibama.gov.br/server/rest/services/SIGAGEO/bases/MapServer/25/`
- **Endpoint de consulta (`IbamaTerritoriesApiService.queryIndigenousLands`):**
  `GET query` → `https://pamgia.ibama.gov.br/server/rest/services/SIGAGEO/bases/MapServer/25/query`

A instância única de `Retrofit` e `OkHttpClient` em `NetworkModule` foi reutilizada (sem criação de segunda instância Retrofit), com timeouts ajustados para `connectTimeout = 15s` e `readTimeout = 30s`.

---

## 2. Parâmetros de Consulta (`GET query`)

Definidos em `ArcGisTerritoriesQuerySpec` e usados como padrão em `IbamaTerritoriesApiService`:

| Parâmetro | Valor Padrão | Finalidade |
|---|---|---|
| `where` | `1=1` | Selecionar todos os registros da camada |
| `outFields` | `terrai_cod,terrai_nom,etnia_nome,municipio_,uf_sigla,superficie,fase_ti,modalidade,data_atual` | Restringir atributos iniciais da consulta tabular |
| `returnGeometry` | `false` | Evitar transferência de polígonos SIRGAS 2000 (WKID 4674) nesta etapa |
| `orderByFields` | `terrai_cod ASC` | Ordenação determinística para paginação |
| `resultOffset` | `0` (variável por página) | Deslocamento inicial da página |
| `resultRecordCount` | `100` (configurável) | Quantidade de registros solicitados por página |
| `f` | `json` | Formato de resposta JSON |

---

## 3. Estrutura da Resposta e Mapeamento

### DTOs (`ArcGisTerritoriesDtos.kt` + `ArcGisJsonAdapters.kt`)
- **`ArcGisFeatureQueryResponseDto`:** `fields`, `features`, `exceededTransferLimit`, `error`.
- **`ArcGisFeatureDto`:** `attributes` (`IndigenousLandAttributesDto?`).
- **`IndigenousLandAttributesDto`:** mapeia os 18 atributos documentados da camada (`objectid`, `terrai_cod`, `terrai_nom`, `etnia_nome`, `municipio_`, `uf_sigla`, `superficie`, `fase_ti`, `modalidade`, `reestudo_t`, `cr`, `faixa_fron`, `undadm_cod`, `undadm_nom`, `undadm_sig`, `dominio_un`, `data_atual`, `epsg`), todos anuláveis.
- **`IndigenousLandAttributesJsonAdapter`:** adaptador Moshi explícito (sem reflexão e sem conflito com KSP2) que tolera campos ausentes, `null`, strings em branco, propriedades desconhecidas e variações escalares comuns do ArcGIS (ex.: número em campo textual ou decimal como string).

### Modelos Normalizados (`RemoteTerritoriesModels.kt` + `ArcGisTerritoriesMapper.kt`)
- **`RemoteIndigenousLand`:** preserva campos ausentes como `null` (sem dados fictícios); normaliza `etnia_nome`, `municipio_` e `uf_sigla` em listas (`ethnicities`, `municipalities`, `statesUf`) separadas por `,`, `;`, `/` ou `|`, mantendo também os textos brutos normalizados. `superficie` negativa ou não finita é convertida em `null`.
- **`RemoteTerritoriesPage`:** contém `items`, `requestedOffset`, `requestedRecordCount`, `exceededTransferLimit` e `nextOffset`.

---

## 4. Estratégia de Paginação

1. A consulta inicia em `resultOffset = 0` com `resultRecordCount = 100` (ajustável).
2. `ArcGisTerritoriesMapper.toRemoteTerritoriesPage` calcula `nextOffset = requestedOffset + items.size` somente quando `exceededTransferLimit == true` **e** `items.isNotEmpty()`.
3. Caso `exceededTransferLimit == true` com `features` vazio (`[]`), `nextOffset` retorna `null` (`hasMorePages == false`) para prevenir laços infinitos.
4. A sincronização completa dos 627 registros com persistência Room permanece reservada às etapas seguintes.

---

## 5. Tratamento de Erros

Implementado em `RemoteTerritoriesDataSource.fetchTerritoriesPage(...)`:
- **Parâmetros inválidos (`resultOffset < 0` ou `resultRecordCount <= 0`):** retorna `AppResult.Error` antes da chamada HTTP.
- **Erros HTTP (`4xx` / `5xx`):** `HttpException` classificada em `HTTP_CLIENT_ERROR`, `HTTP_SERVER_ERROR` ou `HTTP_OTHER_ERROR`.
- **Erro ArcGIS dentro de HTTP 200 (`response.error != null`):** nunca tratado como sucesso; converte `code`, `message` e `details` em `AppResult.Error` classificado pelo código ArcGIS.
- **Payload sem `features` e sem `error` (`response.features == null`):** retorna `RemoteErrorKind.DESERIALIZATION_ERROR`.
- **Rede / TLS / Timeout / JSON malformado:** classificados em `DNS_FAILURE`, `CONNECTION_FAILURE`, `TIMEOUT`, `SSL_HANDSHAKE_FAILURE`, `IO_FAILURE` e `DESERIALIZATION_ERROR`.
- **Cancelamento estruturado:** `CancellationException` é sempre relançada.

---

## 6. Limitações Conhecidas

1. As requisições desta etapa foram validadas em JVM contra **`MockWebServer`**, não contra o servidor ativo `https://pamgia.ibama.gov.br` em dispositivo físico ou ambiente com acesso direto ao Geoportal do IBAMA.
2. Na Etapa 1, os endpoints legados de `ParintintinApiService` foram temporariamente mantidos compiláveis no mesmo `Retrofit` apenas para transição gradual, tendo sido posteriormente removidos nas Etapas 3 e 4 (`TER-003`).
3. Esta nota refere-se exclusivamente à Etapa 1; a persistência Room v4, o loop completo de sincronização e a UI territorial (`TerritoriesScreen`) foram implementados nas Etapas 2 a 5.4.

---

## 7. Testes Implementados e Resultados Executados

Executados na JVM em `23/11/2026` via `JAVA_TOOL_OPTIONS="-Duser.home=/home/appuser" gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin` (`BUILD SUCCESSFUL`):

| Suíte | Testes | Falhas | Erros | Ignorados | Escopo Verificado |
|---|---|---|---|---|---|
| `IbamaTerritoriesApiContractTest` | 12 | 0 | 0 | 0 | Base URL IBAMA, KSP Moshi, parâmetros GET padrão, registros válidos/vazios, nulos/vazios, múltiplas etnias, HTTP 404/503, erro ArcGIS em HTTP 200, paginação 1ª/2ª página, `exceededTransferLimit`, JSON inválido |
| `RemoteTerritoriesDataSourceTest` | 4 | 0 | 0 | 0 | Classificação de rede/SSL/timeout/EOF/JSON/HTTP/ArcGIS, validação de paginação e relançamento de `CancellationException` |
| `ArcGisTerritoriesMapperTest` | 4 | 0 | 0 | 0 | Delimitadores múltiplos, listas vazias, `superficie` inválida/negativa, normalização de UF e `attributes == null` |
| **Total da Etapa 1 (novas suítes)** | **20** | **0** | **0** | **0** | — |
| **Suíte JVM consolidada (30 suítes)** | **272** | **0** | **0** | **0** | Todas as 27 suítes legadas + 3 suítes da Etapa 1 aprovadas |
