# TASKS.md — Voz Ancestral: Catálogo Digital de Terras e Povos Indígenas do Brasil

> **Nota de Natureza Documental:** Este inventário rastreia as tarefas verificadas do projeto **Voz Ancestral**, preservando integralmente o registro histórico da **Versão 1.0 Demonstrativa** (*Acervo Provisório*) e incorporando as entregas das **Etapas 1 a 5.4** (Catálogo de Terras Indígenas via API ArcGIS IBAMA / SIGAGEO, Room v4, `TerritoriesViewModel`, `TerritoriesScreen`, navegação em 3 abas) e a rodada atual de alinhamento documental.

---

## 1. Identificação e Finalidade

| Atributo | Definição Verificada | Referência |
|---|---|---|
| **Identidade Atual do Projeto** | **Voz Ancestral — Catálogo Digital de Terras e Povos Indígenas do Brasil** | `README.md`, `SPECIFICATION.md`, `PLANNING.md` |
| **Identificadores Residuais no App/Gradle** | `Acervo Provisório` (`AcervoProvisorio`) | `app/src/main/res/values/strings.xml`, `settings.gradle.kts`, `metadata.json` |
| **Versão no Gradle** | `versionCode = 1`, `versionName = "1.0"` | `app/build.gradle.kts` |
| **Identificador Android (`applicationId`)** | `com.aistudio.parintintin.kagwah` (`namespace = "com.example"`) | `app/build.gradle.kts` |

### Relação com `SPECIFICATION.md` e `PLANNING.md`
Este documento materializa a fase de **Decomposição de Tarefas** (*SDD*), conectando os requisitos de `SPECIFICATION.md` (`RF-001`–`RF-017`, `RNF-001`–`RNF-009`, `RN-01`–`RN-13`) e as fases de `PLANNING.md` (`Fases 1–17`, `Marcos M1–M16`) em **35 unidades verificáveis**, distribuídas entre:
1. **Documentação (`DOC-*`)**;
2. **Validação e Testes (`VAL-*`)**;
3. **Saneamento Técnico (`TEC-*`)**;
4. **Catálogo de Terras Indígenas (`TER-*` — Etapas 1 a 5.4)**; e
5. **Funcionalidades Futuras (`FUT-*`)**.

---

## 2. Convenção de Identificação e Status

### Prefixos por Categoria
| Prefixo | Categoria | Escopo |
|---|---|---|
| **`DOC-XXX`** | Tarefas Documentais | Criação, revisão e alinhamento dos documentos SDD e contratos técnicos. |
| **`VAL-XXX`** | Validação e Testes | Execuções de testes JVM, compilação `androidTest`, artefatos de build e validações externas. |
| **`TEC-XXX`** | Saneamento Técnico | Correções estruturais e arquiteturais realizadas na base inicial v1.0. |
| **`TER-XXX`** | Catálogo Territorial | Implementação, revisões corretivas e integração do catálogo de Terras Indígenas (Etapas 1 a 5.4). |
| **`FUT-XXX`** | Funcionalidades Futuras | Evoluções pendentes ou bloqueadas por conteúdo autorizado, backend linguístico ou refatoração de identidade. |

### Estados Adotados
| Status | Definição Objetiva |
|---|---|
| **`CONCLUÍDA`** | Execução comprovada por evidência direta no código-fonte, recursos ou relatórios de build/teste. |
| **`PARCIAL`** | Etapa iniciada ou validada em parte, com item complementar pendente ou confirmação apenas externa ao container. |
| **`PENDENTE`** | Tarefa identificada e ainda não executada. |
| **`BLOQUEADA`** | Execução impedida por pré-requisito ou recurso externo (emulador/ADB, keystore Release, curadoria/backend linguístico). |
| **`NÃO APLICÁVEL`** | Tarefa cancelada ou fora do escopo atual (`0` ocorrências). |

---

## 3. Tarefas Documentais (`DOC`)

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`DOC-001`** | Criação e revisão da especificação inicial `SPECIFICATION.md` (v1.0). | **`CONCLUÍDA`** | Inspeção técnica da v1.0. | Documento inicial criado e validado na v1.0 (`RF-001`–`RF-011`). | Histórico preservado em `SPECIFICATION.md` |
| **`DOC-002`** | Criação e revisão do planejamento inicial `PLANNING.md` (v1.0). | **`CONCLUÍDA`** | `DOC-001`. | Documento inicial com Fases 1–11 e Marcos M1–M10 criado na v1.0. | Histórico preservado em `PLANNING.md` |
| **`DOC-003`** | Criação do inventário inicial `TASKS.md` (v1.0). | **`CONCLUÍDA`** | `DOC-001`, `DOC-002`. | Decomposição inicial rastreável criada e consolidada. | Histórico preservado neste arquivo |
| **`DOC-004`** | Criação e consolidação inicial de `TEST_RESULTS.md` (v1.0 — 252/27). | **`CONCLUÍDA`** | `VAL-001`, `VAL-002`, `VAL-005`. | Relatório inicial das 27 suítes JVM e checklist externo publicado. | Histórico preservado em `TEST_RESULTS.md` |
| **`DOC-005`** | Criação de `AI_USAGE.md` (v1.0). | **`CONCLUÍDA`** | `DOC-001`–`DOC-003`. | Registro transparente de uso de IA publicado na raiz. | Histórico preservado em `AI_USAGE.md` |
| **`DOC-006`** | Criação de `README.md` (v1.0). | **`CONCLUÍDA`** | `DOC-001`–`DOC-005`. | Apresentação inicial do projeto publicada na raiz. | Histórico preservado em `README.md` |
| **`DOC-007`** | Revisão histórica de `docs/api-v1-contract.md` (v1.0). | **`CONCLUÍDA`** | `DOC-001`, `DOC-002`. | Contrato legado `/v1/manifest.json` + `/v1/dictionary.json` documentado na etapa v1.0 *(hoje desativado no cliente atual após `TER-003`)*. | `docs/api-v1-contract.md` |
| **`DOC-008`** | Alinhamento integral da documentação raiz pós-Etapa 5.4 para a identidade **Voz Ancestral: Catálogo Digital de Terras e Povos Indígenas do Brasil**. | **`CONCLUÍDA`** | `TER-001`–`TER-006`, `VAL-001`, `VAL-002`. | Atualização coerente de `README.md`, `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md` e `AI_USAGE.md` com **35 suítes / 303 testes**, escopo exato da API IBAMA Camada 25 e sem alterar código/Gradle/XML. | Arquivos Markdown raiz atualizados; XMLs `testDebugUnitTest` |

---

## 4. Tarefas de Validação (`VAL`)

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`VAL-001`** | Consolidação e execução completa das suítes JVM (`:app:testDebugUnitTest`): evolução `233/24` → `238/25` → `252/27` → `272/30` → **303 testes aprovados em 35 suítes**. | **`CONCLUÍDA`** | `TER-001`–`TER-006`. | Execução local via `JAVA_TOOL_OPTIONS="-Duser.home=/home/appuser" gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin` concluída com **35/35** suítes e **303/303** testes aprovados (`0` falhas, `0` erros, `0` ignorados). | 35 arquivos XML em `app/build/test-results/testDebugUnitTest/`; `TEST_RESULTS.md` |
| **`VAL-002`** | Atualização e compilação da suíte instrumentada `androidTest` após migração para ArcGIS `/query?`. | **`CONCLUÍDA`** | `TER-003`, `TER-006`. | `StartupSeedHiltInstrumentedTest.kt` e `TestNetworkModule.kt` atualizados para `IbamaTerritoriesApiService` / `/query?` e compilados com sucesso via `:app:compileDebugAndroidTestKotlin`. *(Nota: geração completa de `app-debug-androidTest.apk` via `:app:assembleDebugAndroidTest` ocorreu em marco anterior v1.0 e não foi reexecutada nesta rodada.)* | `StartupSeedHiltInstrumentedTest.kt`, `TestNetworkModule.kt`, output Gradle `:app:compileDebugAndroidTestKotlin` |
| **`VAL-003`** | Execução da suíte instrumentada `:app:connectedDebugAndroidTest` em dispositivo Android físico ou emulador. | **`BLOQUEADA`** | `VAL-002`; disponibilidade externa de emulador/ADB. | Execução aprovada de `StartupSeedHiltInstrumentedTest` e `ExampleInstrumentedTest` em runtime Android real. | Sem emulador/ADB no container |
| **`VAL-004`** | Validação manual em aparelho físico (offline, sync real IBAMA, rotação, fontes grandes e auditoria **TalkBack**). | **`PENDENTE`** | Aparelho Android físico e APK Debug atual. | Checklist externo executado e registrado em `TEST_RESULTS.md` (Seção 12). | `TEST_RESULTS.md` (Seção 12) |
| **`VAL-005`** | Consolidação e verificação do APK Debug pós-Etapas 5.2–5.4. | **`PARCIAL`** | Build `:app:assembleDebug` atual. | `app/build/outputs/apk/debug/app-debug.apk` existe no módulo (`22.421.131` bytes), porém o artefato consolidado na raiz (`Voz-Ancestral-debug.apk` documentado na v1.0) não está presente no diretório atual nem foi reempacotado após a Etapa 5.4. | `app/build/outputs/apk/debug/app-debug.apk`; `output-metadata.json` |
| **`VAL-006`** | Configuração segura de assinatura Release e geração de APK/AAB assinado. | **`BLOQUEADA`** | Keystore `my-upload-key.jks` e variáveis de ambiente externas. | `:app:assembleRelease` concluído e assinado externamente. | `app/build.gradle.kts` (`signingConfigs` preservado) |
| **`VAL-007`** | Validação externa da carga real de **627 registros** da API IBAMA/SIGAGEO e aprovação do pipeline **GitHub Actions**. | **`PARCIAL`** | Ambiente externo com rede/CI. | Confirmado externamente pelo responsável pelo projeto (`627` registros territoriais e GitHub Actions aprovado); **não reproduzível localmente** neste container offline por ausência de `.git`, `.github/workflows/` e acesso de rede externa. | Informação confirmada pelo responsável; limitação registrada em `TEST_RESULTS.md` |

---

## 5. Tarefas de Saneamento Técnico (`TEC` — Base v1.0 Preservada)

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`TEC-001`** | Propagação explícita de `CancellationException` no `.catch` de `EncyclopediaViewModel.kt`. | **`CONCLUÍDA`** | Nenhuma. | `EncyclopediaViewModelTest.kt` (5 testes aprovados); módulo mantido fora da barra inferior. | `EncyclopediaViewModel.kt`, `EncyclopediaViewModelTest.kt` |
| **`TEC-002`** | Desacoplamento `data` → `di` movendo `@ApplicationScope` para `core.common`. | **`CONCLUÍDA`** | Nenhuma. | `CoroutinesQualifiers.kt` em `core.common`; zero referências de `data/` para `di/`. | `CoroutinesQualifiers.kt` |
| **`TEC-003`** | Higiene de repositório da v1.0 (`chmod +x gradlew`, atualização de `.gitignore` e remoção de `.build-outputs/`). | **`CONCLUÍDA`** | Nenhuma. | `gradlew` executável (`-rwxr-xr-x`); `.gitignore` com `*.aab` e `/.build-outputs/`. *(Nota: `*.apk` não consta atualmente no `.gitignore`.)* | `gradlew`, `.gitignore` |
| **`TEC-004`** | Exibição correta de contagem sincronizada no `OfflineArchiveStatusCard`. | **`CONCLUÍDA`** | Nenhuma. | Evoluído na Etapa 4 para exibir `home_sync_synced_territory_singular` / `home_sync_synced_territories_count` em `SyncState.Synced`. | `PreserveComponents.kt`, `OfflineArchiveStatusCardTest.kt` |

---

## 6. Tarefas do Catálogo de Terras Indígenas (`TER` — Etapas 1 a 5.4)

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`TER-001`** | **Etapa 1:** Cliente Retrofit/Moshi para API ArcGIS REST IBAMA / SIGAGEO Camada 25 (`GET query`, timeouts `15s`/`30s`, `IndigenousLandAttributesJsonAdapter`, `RemoteTerritoriesDataSource`, `ArcGisTerritoriesMapper`). | **`CONCLUÍDA`** | Infraestrutura HTTP (`NetworkModule`). | Contratos, paginação, tratamento de erros HTTP/ArcGIS e mapeamento de nulos/listas validados com `MockWebServer`. | `ArcGisTerritoriesQuerySpec.kt`, `IbamaTerritoriesApiService.kt`, `IbamaTerritoriesApiContractTest.kt` (13), `RemoteTerritoriesDataSourceTest.kt` (4), `ArcGisTerritoriesMapperTest.kt` (5) |
| **`TER-002`** | **Etapa 2:** Evolução do banco Room para Schema v4 (`IndigenousLandEntity`, `MIGRATION_3_4`, índice único `terrai_cod`, `IndigenousLandDao` e `IndigenousLandQueryBuilder` com CTEs sem overflow). | **`CONCLUÍDA`** | `TER-001`, Room v3. | Schema `4.json` exportado; migração `3->4`, busca sem acentos, curingas `LIKE`, filtros combinados e ordenação validados em SQLite via Robolectric. | `ParintintinDatabase.kt`, `ParintintinMigrations.kt`, `IndigenousLandQueryBuilder.kt`, `ParintintinDatabaseMigrationTest.kt` (7), `IndigenousLandQueryBuilderTest.kt` (7) |
| **`TER-003`** | **Etapas 3 e 4:** Repositório `RoomIndigenousLandRepository` (paginação completa com `visitedOffsets`, deduplicação e `withTransaction`), `TerritoriesUseCases`, integração com `ArchiveSyncRepositoryImpl` e atualização da Home. | **`CONCLUÍDA`** | `TER-001`, `TER-002`. | Sincronização paginada integrada ao `ArchiveSyncCoordinatorImpl`; preservação local diante de falhas ou retorno vazio; remoção dos clientes obsoletos `archive.parintintin.org`. | `RoomIndigenousLandRepository.kt`, `ArchiveSyncRepositoryImpl.kt`, `TerritoriesUseCases.kt`, `RoomIndigenousLandRepositoryTest.kt` (10), `TerritoriesSyncIntegrationTest.kt` (2) |
| **`TER-004`** | **Etapas 5.1 e 5.2:** `TerritoriesViewModel` completo + revisão corretiva de ordenação `DESC` mantendo nulos/vazios/espaços sempre ao final e tratamento seguro de exceções em `syncManually()` + testes SQLite em `IndigenousLandDaoTest`. | **`CONCLUÍDA`** | `TER-002`, `TER-003`. | Busca com *debounce* `150 ms`, 4 critérios de ordenação com `null`/`""`/`"   "` sempre no final em ASC/DESC e desempate por `id ASC`, `manualSyncErrorMessage` dispensável, proteção contra chamadas simultâneas e `CancellationException`. | `TerritoriesViewModel.kt`, `TerritoriesViewModelTest.kt` (14), `IndigenousLandDaoTest.kt` (11) |
| **`TER-005`** | **Etapa 5.3:** Interface `TerritoriesScreen`, `TerritoryCard`, `TerritoryControlsSection`, `TerritoriesSyncStatusBar` e `CatalogSharedComponents`. | **`CONCLUÍDA`** | `TER-004`. | Tela responsiva (1/2 colunas, `840.dp`), estados `Loading`/`Empty`/`Success`/`Error`, filtros de UF/fase/modalidade, ordenação, sync manual, acessibilidade e semântica testados via Compose/Robolectric. | `TerritoriesScreen.kt`, `TerritoryCard.kt`, `TerritoryControlsSection.kt`, `TerritoriesSyncStatusBar.kt`, `CatalogSharedComponents.kt`, `TerritoriesScreenTest.kt` (10) |
| **`TER-006`** | **Etapa 5.4:** Integração de `AppDestination.Territories` à navegação principal em 3 abas (`Início`, `Dicionário`, `Territórios`). | **`CONCLUÍDA`** | `TER-005`. | Barra inferior exibindo exatamente 3 destinos com ícone `Public`, preservação de estado ao alternar abas, seleção visual e retorno para `Home`. | `AppDestination.kt`, `ParintintinBottomBar.kt`, `ParintintinNavHost.kt`, `ParintintinNavHostTest.kt` (4) |

---

## 7. Funcionalidades Futuras (`FUT`)

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`FUT-001`** | Obtenção e validação de vocabulário autêntico da língua Parintintin para substituir `DEMO-001`..`DEMO-020`. | **`BLOQUEADA`** | Curadoria linguística e autorização formal externa. | Conjunto validado de verbetes autênticos pronto para consumo autorizado. | `SPECIFICATION.md`, `strings.xml` |
| **`FUT-002`** | Disponibilização de backend oficial específico para acervo linguístico/cultural (`/v1/dictionary.json`, `/v1/articles.json`). | **`BLOQUEADA`** | Infraestrutura externa e revisão contratual. | API HTTPS específica de acervo linguístico operacional e homologada. | `docs/api-v1-contract.md` *(legado)* |
| **`FUT-003`** | Reavaliação de manifesto versionado e cache/skip para futura API linguística *(antigo cliente `/v1/manifest.json` removido na transição para ArcGIS IBAMA)*. | **`PENDENTE`** | `FUT-002`. | Política de versionamento/ETag definida e implementada caso seja adicionada API linguística. | `ArchiveSyncRepositoryImpl.kt`, `NetworkModule.kt` |
| **`FUT-004`** | Definição e implementação de política de exclusão/reconciliação remota para Terras Indígenas e futuros acervos (`upsertAll` atual não remove registros ausentes na resposta remota). | **`PENDENTE`** | Definição funcional sobre remoção ou arquivamento local. | Reconciliação segura em transação Room preservando integridade offline. | `RoomIndigenousLandRepository.kt`, `OfflineFirstDictionaryRepository.kt` |
| **`FUT-005`** | Integração de sincronização e exposição da Enciclopédia Cultural na navegação principal. | **`BLOQUEADA`** | `TEC-001` *(concluída)*, `FUT-002`, `FUT-009`. | Enciclopédia sincronizada, populada com conteúdo autorizado e acessível na navegação. | `EncyclopediaScreen.kt`, `OfflineFirstEncyclopediaRepository.kt` |
| **`FUT-006`** | Interface funcional de Favoritos no Dicionário e/ou Territórios substituindo `ProvisionalDestinationScreen`. | **`PENDENTE`** | Decisão de design/UX pós-Etapa 5.4. | Controle de favoritos na UI e rota funcional dedicada testada. | `ParintintinNavHost.kt`, `DictionaryDao.kt` |
| **`FUT-007`** | Tela de detalhes completos para Terras Indígenas (`ObserveIndigenousLandByIdUseCase`) e/ou verbetes (`ObserveDictionaryEntryByIdUseCase`). | **`PENDENTE`** | Definição de layout e navegação de detalhe. | Rota tipada e tela de detalhe conectadas aos casos de uso existentes. | `TerritoriesUseCases.kt`, `ArchiveUseCases.kt` |
| **`FUT-008`** | Reprodução de áudio de pronúncia nos verbetes (`audioUrl`). | **`BLOQUEADA`** | `FUT-001` e contrato de mídia. | Player integrado com suporte offline e acessibilidade. | `DictionaryEntry.kt`, `HomeScreen.kt` |
| **`FUT-009`** | Conteúdo cultural autêntico e autorizado para a Enciclopédia Cultural. | **`BLOQUEADA`** | Curadoria cultural e autorização formal. | Artigos culturais validados disponíveis no Room. | `CulturalArticle.kt` |
| **`FUT-010`** | Alinhamento de strings em runtime, metadados e documentos auxiliares (`strings.xml`, `HomeScreen`, `settings.gradle.kts`, `metadata.json`, `docs/api-v1-contract.md`, `.gitignore` para `*.apk`) à nova identidade do projeto. | **`PENDENTE`** | Aprovação do responsável para alterações de código/recursos. | Nome exibido no launcher, Home e docs auxiliares totalmente alinhados a **Voz Ancestral — Catálogo Digital de Terras e Povos Indígenas do Brasil**. | `strings.xml`, `settings.gradle.kts`, `metadata.json`, `HomeScreen.kt`, `docs/` |

---

## 8. Resumo de Situação

Os totais abaixo correspondem exatamente às **35 tarefas** registradas nas Seções 3, 4, 5, 6 e 7:

### 8.1 Quantitativo por Categoria e Status
| Categoria | Prefixo | `CONCLUÍDA` | `PARCIAL` | `PENDENTE` | `BLOQUEADA` | `NÃO APLICÁVEL` | Total |
|---|---|---|---|---|---|---|---|
| **Documentação** | `DOC` | 8 | 0 | 0 | 0 | 0 | **8** |
| **Validação e Testes** | `VAL` | 2 | 2 | 1 | 2 | 0 | **7** |
| **Saneamento Técnico (Base v1.0)** | `TEC` | 4 | 0 | 0 | 0 | 0 | **4** |
| **Catálogo Territorial (Etapas 1–5.4)** | `TER` | 6 | 0 | 0 | 0 | 0 | **6** |
| **Funcionalidades Futuras** | `FUT` | 0 | 0 | 5 | 5 | 0 | **10** |
| **Total Geral** | — | **20** | **2** | **6** | **7** | **0** | **35** |

### 8.2 Síntese Objetiva
- **Entregas Técnicas, Documentais e de Validação Local (`DOC` + `VAL` + `TEC` + `TER` = 25 tarefas):** `20` concluídas (`DOC-001`–`DOC-008`, `VAL-001`, `VAL-002`, `TEC-001`–`TEC-004`, `TER-001`–`TER-006`), `2` parciais (`VAL-005` e `VAL-007`), `1` pendente (`VAL-004`) e `2` bloqueadas por recursos externos (`VAL-003` e `VAL-006`).
- **Evoluções Futuras (`FUT` = 10 tarefas):** `5` pendentes (`FUT-003`, `FUT-004`, `FUT-006`, `FUT-007`, `FUT-010`) e `5` bloqueadas por conteúdo autêntico, curadoria ou backend linguístico (`FUT-001`, `FUT-002`, `FUT-005`, `FUT-008`, `FUT-009`).
