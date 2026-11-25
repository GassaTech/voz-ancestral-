# PLANNING.md — Voz Ancestral: Catálogo Digital de Terras e Povos Indígenas do Brasil

> **Nota de Natureza Documental:** Este planejamento consolida o histórico verificado do projeto **Voz Ancestral** — preservando integralmente as **Fases 1 a 11** e marcos **M1 a M10** da etapa inicial (*Acervo Provisório* v1.0 Demonstrativa), documentando as **Fases 12 a 17** e marcos **M11 a M16** correspondentes à evolução para o **Catálogo Digital de Terras e Povos Indígenas do Brasil** (Etapas 1 a 5.4) e registrando a **Fase 18 / Marco M17** de saneamento de identidade e remoção de resíduos do protótipo antigo. Datas, cronogramas ou aprovações formais sem evidência verificável no repositório não foram inventados.

---

## 1. Identificação e Finalidade

| Atributo | Definição Verificada | Origem / Referência |
|---|---|---|
| **Identidade Atual do Projeto** | **Voz Ancestral — Catálogo Digital de Terras e Povos Indígenas do Brasil** | Evolução consolidada e saneada na Fase 18 |
| **Nome Configurado no App/Gradle** | `app_name = "Voz Ancestral"` \| `rootProject.name = "VozAncestral"` \| `Theme.VozAncestral` | `app/src/main/res/values/strings.xml`, `settings.gradle.kts`, `themes.xml`, `AndroidManifest.xml` |
| **Versão no Gradle** | `versionCode = 1`, `versionName = "1.0"` | `app/build.gradle.kts` |
| **Identificadores Técnicos de Compatibilidade** | `applicationId = "com.aistudio.parintintin.kagwah"` \| `namespace = "com.example"` \| `ParintintinDatabase` (`parintintin_archive.db` v4) | Preservados para compatibilidade de pacote, `R` e migrações Room (`app/build.gradle.kts`, `ParintintinDatabase.kt`) |
| **Documento-Base de Escopo** | `SPECIFICATION.md` (`v2.1`) | Raiz do repositório |

### Finalidade do Planejamento
Registrar, com rastreabilidade entre especificação, código e testes, como o aplicativo evoluiu da prova de conceito demonstrativa v1.0 (*Início* + *Dicionário*) para a integração com a **API ArcGIS REST do IBAMA / SIGAGEO (Camada 25)** e entrega da experiência em **3 abas** (*Início*, *Dicionário*, *Territórios*), além de mapear os riscos, dependências e próximos passos recomendados.

---

## 2. Visão Geral da Estratégia

A evolução do projeto seguiu uma estratégia **incremental por camadas**, evitando reescritas disruptivas da infraestrutura existente:

1. **Reaproveitamento de fundação *Offline-First*:** a estrutura Hilt, `ConnectivityManager`, `ArchiveSyncCoordinatorImpl`, `SyncMetadataEntity` e os componentes visuais Material 3 foram preservados e estendidos para atender o catálogo territorial.
2. **Contrato remoto primeiro, persistência depois, UI por último:**
   - **Etapa 1:** cliente Retrofit/Moshi e modelos normalizados para IBAMA / SIGAGEO Camada 25 (`docs/arcgis-territories-api.md`);
   - **Etapas 2–3:** Room Schema v4 (`MIGRATION_3_4`), `IndigenousLandQueryBuilder` com CTEs em SQLite e paginação transacional em `RoomIndigenousLandRepository`, conectados ao `ArchiveSyncRepositoryImpl`;
   - **Etapas 4–5.2:** `TerritoriesViewModel` com busca/filtros/ordenamento reativos, revisão corretiva de nulos/vazios/espaços sempre ao final (tanto na JVM quanto no SQLite via Robolectric) e proteção de exceções em `syncManually()`;
   - **Etapas 5.3–5.4:** UI acessível e responsiva (`TerritoriesScreen` + componentes) e exposição controlada como 3ª aba principal na barra inferior (`ParintintinNavHost` / `ParintintinBottomBar`).
3. **Validação acumulativa por camada:** os registros históricos das etapas anteriores documentaram 303 testes aprovados em 35 suítes após a Etapa 5.4. Na reexecução de validação desta entrega, foram obtidos **303 testes executados, 299 aprovados e 4 falhas** em `ParintintinDatabaseMigrationTest`, também reproduzidas no projeto original. O resultado atual está detalhado em `TEST_RESULTS.md`.

---

## 3. Metodologia SDD (*Specification-Driven Development*)

O desenvolvimento e a atualização documental mantêm o ciclo SDD em cinco etapas:
1. **Specification (`SPECIFICATION.md`):** definição rigorosa do que o app entrega hoje, regras de negócio (`RN-01`–`RN-13`) e limite de escopo dos dados territoriais.
2. **Planning (`PLANNING.md`):** organização das fases, decisões arquiteturais, marcos e riscos.
3. **Task Decomposition (`TASKS.md`):** decomposição rastreável das tarefas concluídas, parciais, pendentes e bloqueadas (`DOC`, `VAL`, `TEC`, `TER`, `FUT`).
4. **Implementation:** execução incremental no código Kotlin/Compose preservando contratos e restrições de cada etapa.
5. **Validation (`TEST_RESULTS.md`):** comprovação objetiva dos testes executados na JVM, compilação `androidTest` e distinção clara do que foi confirmado externamente versus o que permanece pendente.

---

## 4. Organização das Fases do Projeto

### 4.1. Ciclo Inicial — Versão 1.0 Demonstrativa (Preservado)

| Fase | Objetivo | Principais Entregas Verificadas | Situação | Dependências Relevantes |
|---|---|---|---|---|
| **1. Estrutura Android e arquitetura** | Configurar Gradle/KSP, Hilt, tema M3 e camadas desacopladas. | `app/build.gradle.kts`, `di/`, `core/common/`, `ui/theme/`. | **CONCLUÍDA** | Toolchain Android/Gradle (`AGP 9.1.1`, `Kotlin 2.2.10`, `KSP 2.3.5`). |
| **2. Persistência local Room (v1–v3)** | Estruturar banco SQLite v3 com migrações `1->2` e `2->3`. | `ParintintinDatabase`, `ParintintinMigrations`, schemas `2.json`, `3.json`. | **CONCLUÍDA** | Fase 1; `DatabaseModule`. |
| **3. Dicionário demonstrativo** | Modelar domínio linguístico e tela de consulta demonstrativa. | `DictionaryEntry`, `OfflineFirstDictionaryRepository`, `DictionaryViewModel`, `DictionaryScreen`. | **CONCLUÍDA** | Fases 1 e 2. |
| **4. Seed JSON versionado** | Carga offline idempotente e íntegra dos 20 itens `DEMO-001`..`DEMO-020`. | `dictionary_seed.json`, `VersionedDictionarySeedUpdater`, `SeedSha256Calculator`. | **CONCLUÍDA** | Fases 2 e 3. |
| **5. Pesquisa e filtros do Dicionário** | Busca textual com *debounce* (`150 ms`) e filtros combinados. | `DictionaryDao`, `DictionaryViewModel`, `DictionaryScreen`. | **CONCLUÍDA** | Fases 2, 3 e 4. |
| **6. Estratégia Offline-First** | Leitura exclusiva do Room e preservação de dados diante de falhas. | Transações atômicas, preservação de `is_favorite`, `BLOCKED_BY_OFFICIAL_SYNC`. | **CONCLUÍDA** | Fases 2, 3 e 4. |
| **7. Conectividade e sync inicial** | Monitorar rede e coordenar sincronização sem bloquear a UI. | `AndroidNetworkConnectivityObserver`, `ArchiveSyncCoordinatorImpl`, `SyncFailureClassifier`. *(Antigos endpoints `/v1/` foram posteriormente substituídos na Fase 12/14.)* | **CONCLUÍDA** *(infraestrutura base)* | Fases 1, 2 e 6. |
| **8. Interface Home e estados** | Tela inicial, avisos demonstrativos e `OfflineArchiveStatusCard`. | `HomeScreen`, `HomeViewModel`, `PreserveComponents`. | **CONCLUÍDA** | Fases 3, 6 e 7. |
| **9. Validação v1.0 (252/27)** | Consolidar suíte JVM v1.0 e compilar `androidTest`. | 27 suítes / 252 testes aprovados na época; `androidTest` compilado. | **PARCIAL** *(substituída em execução pela Fase 16; `androidTest` segue não executado em dispositivo)* | Fases 1 a 8. |
| **10. Empacotamento Debug v1.0** | Gerar APK Debug v1.0 e regularizar `gradlew`/`.gitignore`. | `gradlew +x`, `signingConfigs` preservados; assinatura Release ausente. *(Observação atual: `Voz-Ancestral-debug.apk` não permanece na raiz no estado corrente.)* | **PARCIAL** | Fase 9; keystore Release externa. |
| **11. Documentação SDD v1.0** | Formalizar conjunto documental da v1.0 (`DOC-001`–`DOC-007`). | Documentos SDD e `docs/api-v1-contract.md` da etapa inicial. | **CONCLUÍDA** | Fases 1 a 10. |

### 4.2. Ciclo de Evolução — Catálogo de Terras Indígenas (Etapas 1 a 5.4)

| Fase | Objetivo | Principais Entregas Verificadas | Situação | Dependências Relevantes |
|---|---|---|---|---|
| **12. Etapa 1 — Cliente ArcGIS IBAMA / SIGAGEO** | Integrar `GET query` da camada 25 no `NetworkModule` único com Moshi resiliente. | `ArcGisTerritoriesQuerySpec`, `IbamaTerritoriesApiService`, `ArcGisTerritoriesDtos`, `IndigenousLandAttributesJsonAdapter`, `ArcGisTerritoriesMapper`, `RemoteTerritoriesDataSource`, `docs/arcgis-territories-api.md` (elevou testes para 272/30). | **CONCLUÍDA** | Fases 1 e 7; `MockWebServer` para contrato HTTP. |
| **13. Etapa 2 — Room Schema v4 e Query Builder SQLite** | Persistir `IndigenousLandEntity`, migrar `3 -> 4` e implementar consulta avançada sem overflow de parser SQLite. | `ParintintinDatabase` (`version = 4`, schema `4.json`), `MIGRATION_3_4`, `IndigenousLandDao`, `IndigenousLandQueryBuilder` (CTEs de até 10 `REPLACE()`, datas julianas, nulos/vazios/espaços sempre ao final). | **CONCLUÍDA** | Fase 2 e Fase 12. |
| **14. Etapas 3 e 4 — Repositório, Sync Unificado e Casos de Uso** | Integrar paginação completa com `ArchiveSyncRepositoryImpl`, atualizar `Home` e expor casos de uso territoriais. | `RoomIndigenousLandRepository` (loop paginado com `visitedOffsets`, deduplicação e `withTransaction`), `TerritoriesUseCases`, integração em `ArchiveSyncRepositoryImpl` (`IBAMA/SIGAGEO Camada 25`), atualização de `ArchiveStats`/`SyncState` na Home e testes de integração (`TerritoriesSyncIntegrationTest`). *(Substituiu `ParintintinApiService` / `RemoteArchiveDataSource`.)* | **CONCLUÍDA** | Fases 6, 7, 12 e 13. |
| **15. Etapas 5.1 e 5.2 — `TerritoriesViewModel` e Revisões Corretivas** | Gerenciar busca com *debounce*, filtros combináveis, ordenação em 4 campos, sync manual e validar nulos/vazios em SQLite/Robolectric. | `TerritoriesViewModel`, revisão de ordenação DESC mantendo nulos/vazios/espaços no final, tratamento seguro de exceções em `syncManually()` com `manualSyncErrorMessage` e testes adicionais em `TerritoriesViewModelTest` (14 testes) e `IndigenousLandDaoTest` (11 testes). | **CONCLUÍDA** | Fases 13 e 14. |
| **16. Etapas 5.3 e 5.4 — UI Territorial e Navegação Principal** | Construir `TerritoriesScreen` e integrá-la como 3ª aba principal na barra inferior preservando estado. | `TerritoriesScreen`, `TerritoryCard`, `TerritoryControlsSection`, `TerritoriesSyncStatusBar`, `CatalogSharedComponents`, `AppDestination.Territories`, `ParintintinBottomBar`, `ParintintinNavHost`, `TerritoriesScreenTest` (10 testes), `ParintintinNavHostTest` (4 testes), além de atualização de `StartupSeedHiltInstrumentedTest` / `TestNetworkModule` para ArcGIS `/query?`. Execução local consolidada em **303 testes / 35 suítes JVM** e `:app:compileDebugAndroidTestKotlin`. *(Externamente: **627 registros** validados e **GitHub Actions** aprovado segundo o responsável.)* | **CONCLUÍDA** *(JVM/Compilação)* \| **PENDENTE** *(Hardware/`connectedDebugAndroidTest`)* | Fases 8, 14 e 15. |
| **17. Atualização Documental Pós-Etapa 5.4 (`DOC-008`)** | Alinhar integralmente os documentos raiz à nova identidade e ao código atual antes da refatoração de limpeza. | Atualização inicial pós-Etapa 5.4 de `README.md`, `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md` e `AI_USAGE.md`. | **CONCLUÍDA** | Fases 12 a 16. |
| **18. Revisão de Identidade e Saneamento Pós-Etapa 5.4 (`FUT-010`)** | Remover referências, rotas e DTOs obsoletos do protótipo *Acervo Provisório*, preservando Room v4, arquitetura e conteúdo Parintintin válido. | Atualização de `app_name` (`Voz Ancestral`), `rootProject.name` (`VozAncestral`), `Theme.VozAncestral`, `strings.xml` e `metadata.json`; remoção do card demonstrativo da `HomeScreen`, de `ProvisionalDestinationScreen`, das rotas inalcançáveis `Favorites`/`Archive` e de `ArchiveManifestDto`/`CulturalArticleDto`; atualização de 7 suítes de teste e dos 8 documentos técnicos. | **CONCLUÍDA** | Fases 16 e 17. |

---

## 5. Decisões Arquiteturais e Tecnológicas

| Decisão | Evidência no Repositório | Justificativa Técnica |
|---|---|---|
| **Instância Retrofit única apontando para IBAMA / SIGAGEO Camada 25** | `NetworkModule.kt`, `ArcGisTerritoriesQuerySpec.kt`, `IbamaTerritoriesApiService.kt` | Reutilizou a configuração Hilt/OkHttp existente, ajustando timeouts para `15 s` / `30 s` e removendo os clientes obsoletos de `archive.parintintin.org` para evitar dualidade de endpoints inativos. |
| **Adaptador Moshi manual (`IndigenousLandAttributesJsonAdapter`)** | `ArcGisJsonAdapters.kt`, `NetworkModule.kt` | Garante compatibilidade com KSP2 sem reflexão frágil, tolerando campos ausentes, `null`, strings em branco, propriedades extras e variações escalares do ArcGIS. |
| **Encadeamento de CTEs no `IndigenousLandQueryBuilder`** | `IndigenousLandQueryBuilder.kt`, `IndigenousLandQueryBuilderTest.kt` | Limita cada estágio a no máximo `10` chamadas `REPLACE()`, evitando *stack overflow* no parser SQLite durante normalização de acentos combinada com filtros e ordenação. |
| **Ordenação determinística com `null`/vazios/espaços sempre ao final** | `IndigenousLandQueryBuilder.kt`, `IndigenousLandDaoTest.kt`, `TerritoriesViewModelTest.kt` | Impede que a inversão `DESC` promova registros sem nome, código, área ou data ao topo da listagem, preservando desempate até `id ASC`. |
| **Deduplicação determinística de IDs e paginação transacional** | `IndigenousLandMappers.kt`, `RoomIndigenousLandRepository.kt` | Evita duplicatas entre páginas e garante atomicidade em `withTransaction` caso ocorra falha de rede no meio da paginação. |
| **Barra inferior estritamente com 3 destinos** | `AppDestination.kt`, `ParintintinBottomBar.kt`, `ParintintinNavHost.kt` | Mantém apenas as telas funcionalmente completas (**Início**, **Dicionário** e **Territórios**), preservando a rota preparatória `Encyclopedia` fora da barra inferior até implementação integral e removendo as antigas rotas de *placeholder* `Favorites` e `Archive`. |
| **Isolamento de falhas na sincronização manual (`TerritoriesViewModel`)** | `TerritoriesViewModel.kt`, `TerritoriesSyncStatusBar.kt` | Exceções não cancelantes são exibidas via `manualSyncErrorMessage` dispensável (quando não cobertas por `OfflineFallback`), mantendo `syncJobLock` ativo e propagando `CancellationException`. |

---

## 6. Dependências entre Funcionalidades

```text
[API ArcGIS REST IBAMA / SIGAGEO (Camada 25)]
          │ (GET query paginado, returnGeometry=false, 100/pág)
          ▼
[RemoteTerritoriesDataSource] ──▶ [ArcGisTerritoriesMapper]
          │
          ▼ (loop paginado + visitedOffsets + deduplicação + withTransaction)
[RoomIndigenousLandRepository] ◀──▶ [ArchiveSyncRepositoryImpl] ◀── [ArchiveSyncCoordinatorImpl]
          │                                       ▲
          │ (RawQuery + CTEs SQLite)              │
          ▼                                       │
[IndigenousLandDao (Room v4)]       [AndroidNetworkConnectivityObserver] + [SyncFailureClassifier]
          │
          ├──▶ [ObserveIndigenousLands / FilterOptions / Count] ──▶ [TerritoriesViewModel] ──▶ [TerritoriesScreen]
          └──▶ [ObserveArchiveStats / ObserveSyncState] ──────────▶ [HomeViewModel] ──────────▶ [HomeScreen]

[dictionary_seed.json (DEMO-001..DEMO-020)] ──▶ [VersionedDictionarySeedUpdater] ──▶ [DictionaryDao] ──▶ [DictionaryScreen]
```

---

## 7. Marcos do Projeto (*Milestones*)

| Marco | Descrição | Evidência Principal | Situação |
|---|---|---|---|
| **M1–M6** | Base arquitetural, Room v3, Dicionário demonstrativo, busca/filtros, Offline-First/conectividade e Home v1.0. | `ParintintinDatabase`, `DictionaryScreen`, `HomeScreen`, `ArchiveSyncCoordinatorImpl`. | **CONCLUÍDO** |
| **M7–M8** | Validação técnica v1.0 (252/27), compilação `androidTest` e empacotamento Debug v1.0. | Relatórios v1.0 históricos; pendente de hardware/Release. | **PARCIAL** *(superado na suíte JVM por M15)* |
| **M9** | Regularização documental SDD v1.0 (`DOC-001`–`DOC-007`). | Documentos da etapa v1.0 concluídos anteriormente. | **CONCLUÍDO** |
| **M10** | Conteúdo linguístico autêntico, áudio, Enciclopédia, Favoritos e backend de acervo linguístico. | Estruturas preparatórias no repositório. | **PENDENTE** |
| **M11** | Cliente ArcGIS REST IBAMA / SIGAGEO Camada 25 e contrato documental (Etapa 1). | `IbamaTerritoriesApiService.kt`, `docs/arcgis-territories-api.md`, 272/30 testes. | **CONCLUÍDO** |
| **M12** | Persistência Room Schema v4, `MIGRATION_3_4` e `IndigenousLandQueryBuilder` (Etapa 2). | `4.json`, `ParintintinMigrations.kt`, `IndigenousLandDaoTest.kt`, `IndigenousLandQueryBuilderTest.kt`. | **CONCLUÍDA** |
| **M13** | Sincronização paginada integrada ao `ArchiveSyncRepositoryImpl` e casos de uso (Etapas 3–4). | `RoomIndigenousLandRepository.kt`, `TerritoriesUseCases.kt`, `TerritoriesSyncIntegrationTest.kt`. | **CONCLUÍDO** |
| **M14** | `TerritoriesViewModel` completo + revisão corretiva de ordenação e exceções (Etapa 5.2). | `TerritoriesViewModel.kt`, `TerritoriesViewModelTest.kt` (14 testes), `IndigenousLandDaoTest.kt` (11 testes). | **CONCLUÍDO** |
| **M15** | Interface `TerritoriesScreen` e integração à navegação principal em 3 abas (Etapas 5.3–5.4). | `TerritoriesScreen.kt`, `ParintintinNavHost.kt`, `ParintintinBottomBar.kt`; compilação de `:app:compileDebugAndroidTestKotlin` aprovada. Validação JVM atual: **303 testes executados, 299 aprovados e 4 falhas** em migrações Room/Robolectric, também reproduzidas no projeto original. *(Externamente: 627 registros e GitHub Actions reportados pelo responsável.)* | **CONCLUÍDO** *(implementação/compilação)* \| **PARCIAL** *(testes JVM)* \| **PENDENTE** *(hardware)* |
| **M16** | Alinhamento documental integral pós-Etapa 5.4 (`DOC-008`). | 6 documentos raiz atualizados com coerência nesta etapa. | **CONCLUÍDO** |
| **M17** | Revisão de identidade e saneamento de código, recursos, testes e documentação (`FUT-010`). | Remoção de `Acervo Provisório`, `Theme.MyApplication`, `ProvisionalDestinationScreen`, `Favorites`/`Archive` e `ArchiveManifestDto`/`CulturalArticleDto`; compilação Kotlin e AndroidTest aprovada. Suíte JVM atual: **299/303 aprovados, 4 falhas** registradas em `TEST_RESULTS.md`. | **CONCLUÍDO** *(saneamento)* \| **PARCIAL** *(validação JVM)* |

---

## 8. Planejamento de Testes e Validação

| Frente | Escopo Coberto | Suítes Representativas | Estado Comprovado |
|---|---|---|---|
| **Testes Unitários (Domínio, Mappers, Seed, Sync, ViewModels)** | Regras puras, mapeamento ArcGIS/Room, seed SHA-256, `SyncFailureClassifier`, coordenador, `HomeViewModel`, `DictionaryViewModel`, `TerritoriesViewModel` (inclusive nulos e exceções), `EncyclopediaViewModel`. | `ArchiveUseCasesAndClassifierTest`, `ArcGisTerritoriesMapperTest`, `IndigenousLandMappersTest`, `TerritoriesViewModelTest` (14), `HomeViewModelTest` (14), `DictionaryViewModelTest` (17), `ArchiveSyncRepositoryImplTest` (35). | **Executado na JVM — Aprovado** |
| **Testes SQLite / Room / Migrações** | Migrações `1->3`, `2->3`, `3->4`, esquema v4, índices únicos, CTEs de busca sem acentos, filtros combinados, ordenação ASC/DESC com nulos, vazios e espaços ao final. | `ParintintinDatabaseMigrationTest` (7), `IndigenousLandDaoTest` (11), `IndigenousLandQueryBuilderTest` (7). | **Executado na JVM (Robolectric) — Aprovado** |
| **Contratos HTTP e Integração Ponta a Ponta (`MockWebServer`)** | Contrato IBAMA Camada 25 (`IbamaTerritoriesApiContractTest` — 13), paginação e fallback (`RoomIndigenousLandRepositoryTest` — 10, `TerritoriesSyncIntegrationTest` — 2, `DictionaryOfflineIntegrationTest` — 10). | `IbamaTerritoriesApiContractTest`, `RemoteTerritoriesDataSourceTest`, `RoomIndigenousLandRepositoryTest`, `TerritoriesSyncIntegrationTest`. | **Executado na JVM — Aprovado** |
| **Interface Compose e Navegação (`Robolectric`)** | `HomeScreen`, `DictionaryScreen`, `TerritoriesScreen` (estados, acessibilidade, `w720dp`), `OfflineArchiveStatusCard`, `ResponsiveComponents`, `ParintintinNavHostTest` (3 abas, preservação de estado e voltar). | `TerritoriesScreenTest` (10), `ParintintinNavHostTest` (4), `DictionaryScreenTest` (23), `HomeScreenTest` (7), `OfflineArchiveStatusCardTest` (13). | **Executado na JVM — Aprovado** |
| **Testes Instrumentados (`androidTest`)** | Fluxo Hilt real (`MainActivity`) com banco em memória e `MockWebServer` respondendo `/query?` vazio. | `StartupSeedHiltInstrumentedTest` (2 `@Test`), `ExampleInstrumentedTest` (1 `@Test`). | **Compilado (`:app:compileDebugAndroidTestKotlin`) — Execução em hardware PENDENTE** |

---

## 9. Riscos e Estratégias de Mitigação

| Risco Identificado | Impacto | Estratégia de Mitigação |
|---|---|---|
| **1. Indisponibilidade ou alteração de schema no Geoportal IBAMA** | Sincronização falha entrando em `OfflineFallback` ou erro de desserialização. | Operação 100% *Offline-First* preservando dados locais; adaptador Moshi tolerante a campos desconhecidos e tipos mistos; validação contra API real recomendada em dispositivo físico. |
| **2. Confusão sobre alcance cultural/linguístico dos dados territoriais** | Interpretação equivocada de que `etnia_nome` equivale a perfil cultural, histórico ou linguístico completo. | Declaração explícita em `README.md`, `SPECIFICATION.md`, `PLANNING.md` e rótulo preciso `"Etnias associadas"` nos cards de `TerritoryCard`. |
| **3. Coexistência de identificadores internos legados com a identidade atual** | Identificadores técnicos de pacote e banco (`com.aistudio.parintintin.kagwah`, `ParintintinDatabase`, `ParintintinNavHost`, `parintintin_archive.db`) coexistem com a identidade **Voz Ancestral**. | Textos visíveis, tema (`Theme.VozAncestral`), `app_name`, `rootProject.name`, `metadata.json`, rotas/DTOs obsoletos e docs técnicos foram saneados na **Fase 18 (`FUT-010`)**; identificadores internos estruturais e de Room v4 foram preservados deliberadamente para evitar quebra de `R`, pacote e migrações `1->2->3->4`. |
| **4. Ausência de execução `androidTest` e TalkBack no container** | Regressões reais de hardware, rede celular/Wi-Fi, leitor de tela e renderização em GPU podem não aparecer na JVM. | Executar `:app:connectedDebugAndroidTest` e checklist presencial em aparelho Android (incluindo TalkBack e sincronização contra `pamgia.ibama.gov.br`). |
| **5. Ausência de remoção remota na sincronização territorial** | Registros eventualmente removidos na camada 25 permanecem no SQLite local após `upsertAll`. | Avaliar definição de política de reconciliação/exclusão em ciclo futuro (`FUT-004`). |

---

## 10. Prioridades de Evolução Pós-Etapa 5.4

1. **Validação presencial e CI contínuo:** executar `:app:connectedDebugAndroidTest`, validar sincronização real dos 627 territórios no aparelho Android com TalkBack e consolidar assinatura Release quando houver keystore dedicada.
2. **Higiene complementar de empacotamento (`.gitignore`):** avaliar inclusão de `*.apk` no `.gitignore` caso artefatos binários locais não devam ser rastreados.
3. **Evoluções funcionais do catálogo e acervo (`FUT-001`–`FUT-009`):** tela de detalhes de territórios/verbetes, política de reconciliação remota, conteúdo linguístico Parintintin autorizado, Enciclopédia Cultural, Favoritos na UI e áudio de pronúncia. *(Nota: `FUT-010` já foi concluída na Fase 18.)*
