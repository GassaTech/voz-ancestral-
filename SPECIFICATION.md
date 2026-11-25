# SPECIFICATION.md — Voz Ancestral: Catálogo Digital de Terras e Povos Indígenas do Brasil

> **Nota de Natureza Documental:** Esta especificação descreve com exatidão o estado técnico verificado no código-fonte, recursos, configurações Gradle e testes automatizados do projeto **Voz Ancestral** após a conclusão das **Etapas 1 a 5.4** (adaptação territorial IBAMA / SIGAGEO, Room v4, `TerritoriesViewModel`, `TerritoriesScreen` e integração à navegação principal em 3 abas), preservando o registro histórico da **Versão 1.0 Demonstrativa** (*Acervo Provisório*) e separando explicitamente comportamentos comprovados de itens parciais ou planejados.

---

## 1. Identificação

| Atributo | Definição Verificada no Repositório | Origem no Projeto |
|---|---|---|
| **Identidade do Projeto** | **Voz Ancestral — Catálogo Digital de Terras e Povos Indígenas do Brasil** | Evolução confirmada sobre a base do projeto *Voz Ancestral* |
| **Nome Configurado no App / Gradle** | **Acervo Provisório** (`AcervoProvisorio` no Gradle) | `app/src/main/res/values/strings.xml` (`app_name`), `settings.gradle.kts` |
| **Nome no AI Studio (`metadata.json`)** | `Voz Ancestral` | `metadata.json` *(descrição ainda cita apenas língua Parintintin)* |
| **Tema Atual** | Catálogo digital *offline-first* de Terras Indígenas do Brasil e etnias associadas (API IBAMA / SIGAGEO Camada 25), mantendo a base demonstrativa de preservação da língua Parintintin | `TerritoriesScreen.kt`, `IbamaTerritoriesApiService.kt`, `HomeScreen.kt`, `DictionaryScreen.kt` |
| **Versão Configurada** | `versionCode = 1`, `versionName = "1.0"` | `app/build.gradle.kts` |
| **Identificador Android (`applicationId`)** | `com.aistudio.parintintin.kagwah` (`namespace = "com.example"`) | `app/build.gradle.kts` |
| **Compatibilidade SDK** | `minSdk = 24` \| `targetSdk = 36` \| `compileSdk = 36` | `app/build.gradle.kts` |

> **Aviso Obrigatório de Precisão dos Dados:**
> A API ArcGIS REST do IBAMA / SIGAGEO (Camada 25) fornece **informações territoriais oficiais** (nome, código, UF, municípios, área superficial, fase do processo fundiário, modalidade e data de atualização) e os nomes das **etnias associadas aos territórios** (`etnia_nome` / `ethnicities`). **Essa fonte não constitui uma base completa de perfis culturais, históricos ou linguísticos dos povos indígenas.**
> Adicionalmente, os 20 verbetes exibidos na aba **Dicionário** (`DEMO-001` a `DEMO-020`) permanecem sendo registros fictícios em português para demonstração técnica.

---

## 2. Contexto e Problema

A consulta pública a dados territoriais oficiais de Terras Indígenas e o acesso a registros culturais e linguísticos enfrentam limitações recorrentes de conectividade em campo, heterogeneidade de formatos em APIs geoespaciais (campos nulos, strings com múltiplos delimitadores, variação de acentuação e datas em formatos mistos) e necessidade de rigor ético quanto à distinção entre dados públicos territoriais e acervos linguísticos/culturais autorizados.

Para atender a esse cenário:
- O aplicativo consolida uma arquitetura **Offline-First** baseada em **Room (Schema v4)** para permitir consulta, busca sem acentos, filtros combináveis e ordenação local determinística mesmo sem internet.
- A integração de rede foi redirecionada para a **API pública ArcGIS REST do IBAMA / SIGAGEO (MapServer, camada 25)**, realizando paginação controlada sem transferir geometrias pesadas (`returnGeometry = false`).
- A aba **Dicionário** continua isolada em **Modo Demonstrativo** (`DEMO-001`..`DEMO-020`) com bloqueio de sobrescrita após sincronização oficial (`BLOCKED_BY_OFFICIAL_SYNC` — ver nota de compatibilidade na Seção 12).

---

## 3. Objetivo Geral

Disponibilizar um aplicativo Android *offline-first* em **Kotlin + Jetpack Compose + Clean Architecture + MVVM** que atue como catálogo digital consultável de **Terras Indígenas do Brasil** a partir de dados oficiais sincronizados da camada 25 do IBAMA / SIGAGEO em banco SQLite/Room local, mantendo preservadas as telas **Início** e **Dicionário** da etapa inicial.

---

## 4. Objetivos Específicos

1. **Integrar a API ArcGIS REST do IBAMA / SIGAGEO (Camada 25):** consultar paginadamente `GET query` com Moshi customizado (`IndigenousLandAttributesJsonAdapter`), tratamento explícito de erros HTTP e erros ArcGIS dentro de `HTTP 200 OK`, sem inventar valores para campos ausentes.
2. **Persistir e indexar dados territoriais no Room (Schema v4):** evoluir `ParintintinDatabase` com `MIGRATION_3_4`, tabela `indigenous_lands`, índice único em `terrai_cod` e preservação de todos os dados pré-existentes da v3.
3. **Prover consulta SQL offline robusta (`IndigenousLandQueryBuilder`):** implementar normalização/remoção de acentos em SQLite com estágios CTE encadeados (sem exceder o limite de profundidade do parser SQLite), *escape* de curingas `LIKE`, filtragem combinável por **UF**, **Fase** e **Modalidade**, e ordenação ascendente/descendente em 4 critérios com valores nulos, vazios e somente espaços mantidos sempre ao final.
4. **Entregar interface territorial responsiva e acessível (`TerritoriesScreen`):** listar territórios com busca em *debounce* de `150 ms`, chips de filtro dinâmicos, controles de ordenação, estados visuais distintos (`Loading`, `Empty`, `Success`, `Error`), barra de status/sincronização manual e suporte a leitores de tela.
5. **Integrar o catálogo à navegação principal em 3 abas:** registrar `AppDestination.Territories` em `ParintintinBottomBar` e `ParintintinNavHost` ao lado de **Início** e **Dicionário**, preservando estado entre alternâncias e comportamento de retorno.

---

## 5. Público-Alvo (Indicação Não Formalizada em Contrato Externo)

Não há documento de definição formal de personas no repositório; com base nas funcionalidades implementadas e nos textos de interface, o aplicativo atende a estudantes, pesquisadores, educadores, gestores públicos e cidadãos interessados em consultar informações territoriais oficiais sobre Terras Indígenas no Brasil e suas etnias associadas, além de avaliadores acadêmicos do projeto **Voz Ancestral**.

---

## 6. Escopo Atual do Aplicativo

A navegação inferior (`AppDestination.primaryDestinations` em `AppDestination.kt` e `ParintintinBottomBar.kt`) expõe exatamente **três destinos principais**: **Início (`Home`)**, **Dicionário (`Dictionary`)** e **Territórios (`Territories`)**.

### 6.1. Tela Início (`HomeScreen` / `HomeViewModel`)
- Mantém o cabeçalho hero v1.0 (`"VERSÃO 1.0 • DEMONSTRATIVA"` / `"Preservação da Língua Parintintin"`), o card permanente `"MODO DEMONSTRATIVO"`, o card `"Acervo em construção"`, a seção estática `"Módulos Planejados"` e o painel expansível `"Detalhes da Etapa Inicial"`.
- O cartão **Armazenamento Local (Offline-First)** (`OfflineArchiveStatusCard`) exibe contagens locais em tempo real para **Terras Indígenas** (`home_offline_territories_chip`), **verbetes** (`home_offline_entries_chip`) e **artigos** (`home_offline_articles_chip`), estado da sincronização (`Loading`, `Idle`, `Syncing`, `Synced` com contagem de Terras Indígenas sincronizadas, `RemoteEmpty` e `OfflineFallback` classificado em `NO_INTERNET`, `UNVALIDATED_NETWORK` e `SERVER_UNAVAILABLE`) e botão de sincronização manual.
- Dispara `syncCoordinator.requestAutoSync()` uma única vez por processo em `HomeViewModel.init`.

### 6.2. Tela Dicionário (`DictionaryScreen` / `DictionaryViewModel`)
- Mantém integralmente a listagem dos 20 registros demonstrativos `DEMO-001`..`DEMO-020` do `dictionary_seed.json`, `stickyHeader` `"MODO DEMONSTRATIVO"`, busca com *debounce* de `150 ms`, filtros por categoria semântica e classe gramatical, estados `Loading`/`Empty`/`Success`/`Error` e avisos explícitos de caráter fictício em português.

### 6.3. Tela Territórios (`TerritoriesScreen` / `TerritoriesViewModel` — Etapas 5.1 a 5.4)
- **Cabeçalho (`TerritoriesHeaderCard`):** título `"Terras Indígenas"`, badge de total local (`territories_total_count_badge`) e selo `"Consulta offline via Room"`.
- **Barra de status e sincronização (`TerritoriesSyncStatusBar`):** reflete `SyncState` do `ArchiveSyncCoordinator`, oferece botão `"Sincronizar agora"` / `"Tentar novamente"` desabilitado durante `Syncing` e exibe banner dispensável (`territories_manual_sync_error_banner`) quando uma falha não cancelante ocorre durante sincronização manual sem `OfflineFallback`.
- **Pesquisa textual (`TerritorySearchField`):** campo `territories_search_input` com *debounce* de `150 ms` e botão de limpeza `territories_clear_search`.
- **Filtros combináveis (`TerritoryFilterChipRow`):** linhas horizontais dinâmicas geradas a partir dos dados locais para **Unidade Federativa (UF)**, **Fase do processo** e **Modalidade**, cada qual com opção `"Todas"` e seleção alternável.
- **Ordenação (`TerritorySortSection`):** seleção entre **Nome** (`NAME`), **Código** (`TERRITORY_CODE`), **Superfície** (`SURFACE_AREA`) e **Atualização** (`UPDATED_AT`), com botão alternador entre **Crescente** (`ASCENDING`) e **Decrescente** (`DESCENDING`).
- **Listagem e responsividade:** `LazyColumn` centrada com largura máxima `840.dp`, renderizando 1 coluna em largura `< 600.dp` e grade de 2 colunas em largura `>= 600.dp`. Cada `TerritoryCard` exibe código (quando presente), nome (ou `"Terra Indígena sem nome informado"`), tags de UF/fase/modalidade, bloco de **Etnias associadas** e metadados secundários (municípios, superfície em `ha` formatada em `pt-BR`, data de atualização bruta), omitindo campos nulos/ausentes sem inventar valores.
- **Estados:** `Loading`, `Empty(hasActiveFilter = true)` com ação `"Limpar filtros"`, `Empty(hasActiveFilter = false)` com orientação para sincronização manual, `Error` com `"Tentar novamente"` e `Success`.

---

## 7. Limitações de Dados e Comportamentos Residuais

- **Escopo estritamente territorial e de etnias associadas na API:** `IbamaTerritoriesApiService` consulta apenas os campos tabulares `objectid,terrai_cod,terrai_nom,etnia_nome,municipio_,uf_sigla,superficie,fase_ti,modalidade,data_atual` com `returnGeometry=false`. Perfis etnográficos, línguas faladas detalhadas, dados demográficos, históricos ou polígonos geográficos não fazem parte do modelo ou da API consumida.
- **Estratégia de atualização local em `upsertAll`:** a sincronização em `RoomIndigenousLandRepository` executa paginação completa e persiste via `indigenousLandDao.upsertAll(deduplicatedEntities)` dentro de `database.withTransaction`. Não há política implementada de remoção automática de registros eventualmente excluídos na origem remota (com exceção de `deleteAll()` manual no repositório).
- **Resíduos textuais e documentais da v1.0:**
  - `strings.xml` mantém `app_name = "Acervo Provisório"`, `home_provisional_badge = "VERSÃO 1.0 • DEMONSTRATIVA"` e `home_hero_title = "Preservação da Língua Parintintin"`;
  - `settings.gradle.kts` mantém `rootProject.name = "AcervoProvisorio"`;
  - `ArchiveDtos.kt` ainda contém `DictionaryEntryDto`, `CulturalArticleDto` e `ArchiveManifestDto` (utilizado por `DictionarySeedJsonParser` e testes de mapper), enquanto `ParintintinApiService` e `RemoteArchiveDataSource` foram removidos na transição para a API do IBAMA;
  - `docs/api-v1-contract.md` ainda descreve o antigo contrato proposto (`https://archive.parintintin.org/`), atualmente inativo no cliente.

---

## 8. Funcionalidades Parciais, Não Implementadas e Futuras

| Funcionalidade | Estado Atual no Repositório | Evidência |
|---|---|---|
| **Catálogo de Terras Indígenas (Pesquisa, Filtros, Ordenação, Sync IBAMA)** | **Implementado e ativo** na aba **Territórios** e na infraestrutura Room/Retrofit. *(Sincronização de **627 registros** confirmada externamente pelo responsável; no container offline, validado com `MockWebServer` e testes Robolectric/Room.)* | `TerritoriesScreen.kt`, `TerritoriesViewModel.kt`, `IndigenousLandQueryBuilder.kt`, `RoomIndigenousLandRepository.kt`, `IbamaTerritoriesApiService.kt` |
| **Enciclopédia Cultural** | **Parcial (preparatório, fora da navegação e sem sync):** possui DAO, Entity, Repository (`OfflineFirstEncyclopediaRepository`), `EncyclopediaViewModel` e `EncyclopediaScreen`, porém está excluída de `primaryDestinations` e sem seed/API. | `AppDestination.kt`, `EncyclopediaScreen.kt`, `OfflineFirstEncyclopediaRepository.kt` |
| **Favoritos** | **Parcial (persistência de Dicionário/Enciclopédia sem UI):** colunas/DAO preservados, porém sem botão nos cards e com rota `AppDestination.Favorites` apontando para `ProvisionalDestinationScreen` inacessível pela barra inferior. | `DictionaryDao.kt`, `ParintintinNavHost.kt` |
| **Tela de Detalhes (Verbetes e Territórios)** | **Parcial no domínio / Sem UI:** existem `ObserveDictionaryEntryByIdUseCase` e `ObserveIndigenousLandByIdUseCase`, porém sem telas ou rotas de detalhes implementadas. | `ArchiveUseCases.kt`, `TerritoriesUseCases.kt`, `ParintintinNavHost.kt` |
| **Pronúncias em Áudio / Acervo Oficial** | **Não implementada:** apenas campo `audioUrl: String?` no modelo do dicionário, card informativo `"Planejado"` na Home e rota provisória `AppDestination.Archive`. | `DictionaryEntry.kt`, `HomeScreen.kt`, `ParintintinNavHost.kt` |
| **Sincronização remota do Dicionário e Enciclopédia** | **Desativada no cliente atual:** `OfflineFirstDictionaryRepository` e `OfflineFirstEncyclopediaRepository` realizam apenas leitura/favoritos no Room; `ArchiveSyncRepositoryImpl` sincroniza apenas o seed local e `indigenousLandRepository.syncFromRemote()`. | `ArchiveSyncRepositoryImpl.kt`, `OfflineFirstDictionaryRepository.kt`, `NetworkModule.kt` |

---

## 9. Requisitos Funcionais

### 9.1. Requisitos Originais da Base (Mantidos / Atualizados)

| ID | Descrição | Situação Atual | Critério de Aceitação Verificado |
|---|---|---|---|
| **RF-001** | Exibir tela inicial (*Home*) com identificação, aviso demonstrativo, card de status offline-first, módulos planejados e detalhes expansíveis. | **Implementado** (`HomeScreen.kt`, `PreserveComponents.kt`) | `HomeScreen` exibe banner, `MODO DEMONSTRATIVO`, `OfflineArchiveStatusCard` (com chips de Terras Indígenas, verbetes e artigos locais) e módulos planejados. |
| **RF-002** | Disparar automaticamente uma tentativa de inicialização do seed demonstrativo e sincronização remota ao iniciar `HomeViewModel`. | **Implementado** (`HomeViewModel.kt`, `ArchiveSyncCoordinatorImpl.kt`) | `requestAutoSync()` executa uma vez por processo, atualiza o seed e chama `indigenousLandRepository.syncFromRemote()`. |
| **RF-003** | Permitir sincronização manual sob demanda na **Início** e em **Territórios**, evitando chamadas concorrentes duplicadas. | **Implementado** (`HomeViewModel.kt`, `TerritoriesViewModel.kt`, `ArchiveSyncCoordinatorImpl.kt`) | Botões desabilitam durante `Syncing`; `syncJobLock`, `syncMutex` e `JoinedInFlight` evitam execuções paralelas simultâneas. |
| **RF-004** | Exibir os 20 verbetes locais (`DEMO-001`..`DEMO-020`) no **Dicionário** ordenados alfabeticamente com selos demonstrativos. | **Implementado** (`DictionaryScreen.kt`, `DictionaryDao.kt`) | Exibe contagem local, `stickyHeader` `"MODO DEMONSTRATIVO"` e badges `"Termo demonstrativo"`. |
| **RF-005** | Permitir pesquisa textual no **Dicionário** com *debounce* de `150 ms` e limpeza rápida. | **Implementado** (`DictionaryViewModel.kt`, `DictionaryScreen.kt`) | Atualiza filtro após `150 ms` sobre termo, tradução e nota; `dictionary_clear_search` limpa a consulta. |
| **RF-006** | Permitir filtros combináveis por categoria semântica e classe gramatical no **Dicionário**. | **Implementado** (`DictionaryViewModel.kt`, `DictionaryScreen.kt`) | Chips combinam critérios com busca textual e oferecem limpeza global. |
| **RF-007** | Apresentar estados visuais distintos (`Loading`, `Empty`, `Success`, `Error`) no **Dicionário**. | **Implementado** (`DictionaryViewModel.kt`, `DictionaryScreen.kt`) | Exibe loading, estado vazio diferenciado com limpeza de filtros e erro com `"Tentar novamente"`. |
| **RF-008** | Permitir navegação principal entre **Início**, **Dicionário** e **Territórios** por barra inferior preservando estado e ocultando telas provisórias. *(Atualizado na Etapa 5.4.)* | **Implementado** (`AppDestination.kt`, `ParintintinBottomBar.kt`, `ParintintinNavHost.kt`) | Barra inferior contém exatamente 3 destinos (`bottom_nav_home`, `bottom_nav_dictionary`, `bottom_nav_territories`), alternando com `saveState = true`, `restoreState = true`, `launchSingleTop = true` e retorno para `Home`. |
| **RF-009** | Aplicar e atualizar o seed demonstrativo (`dictionary_seed.json`) de forma idempotente e transacional por versão e SHA-256. | **Implementado** (`VersionedDictionarySeedUpdater.kt`) | Atualiza atomicamente se houver alteração ou retorna `ALREADY_UP_TO_DATE` sem reescrita. |
| **RF-010** | Diferenciar visualmente os estados de sincronização e falhas de conectividade (`Loading`, `Idle`, `Syncing`, `Synced`, `RemoteEmpty`, `OfflineFallback` por `NO_INTERNET`, `UNVALIDATED_NETWORK`, `SERVER_UNAVAILABLE`). | **Implementado** (`OfflineArchiveStatusCard`, `TerritoriesSyncStatusBar`, `ArchiveSyncRepositoryImpl.kt`) | Exibe título/descrição específicos para cada causa, preservando contagem sincronizada de Terras Indígenas em `SyncState.Synced`. |
| **RF-011** | Preservar dados locais e histórico de última sincronização diante de falhas ou resposta remota vazia. | **Implementado** (`ArchiveSyncRepositoryImpl.kt`, `RoomIndigenousLandRepository.kt`, `SyncMetadataDao.kt`) | Falhas ou retorno vazio não removem Terras Indígenas nem verbetes locais nem sobrescrevem `lastSuccessfulSyncEpochMillis`. |

### 9.2. Novos Requisitos Funcionais do Catálogo de Territórios (Etapas 1–5.4)

| ID | Descrição | Situação Atual | Critério de Aceitação Verificado |
|---|---|---|---|
| **RF-012** | Consultar paginadamente a API ArcGIS REST do IBAMA / SIGAGEO (Camada 25) e persistir Terras Indígenas no Room sem duplicação. | **Implementado** (`IbamaTerritoriesApiService.kt`, `RemoteTerritoriesDataSource.kt`, `RoomIndigenousLandRepository.kt`) | Percorre `resultOffset` enquanto `exceededTransferLimit == true && items.isNotEmpty()`, deduplica por `id` determinístico e persiste via `database.withTransaction` (`upsertAll`). |
| **RF-013** | Listar Terras Indígenas na tela **Territórios** exclusivamente a partir do banco Room local, exibindo nome, código, UF, etnias associadas, fase, modalidade e metadados disponíveis. | **Implementado** (`TerritoriesScreen.kt`, `TerritoryCard.kt`, `TerritoriesViewModel.kt`) | Exibe registros locais sem acesso direto à rede na UI; campos ausentes/nulos não exibem texto inventado. |
| **RF-014** | Pesquisar Terras Indígenas com *debounce* de `150 ms`, tolerância a acentos/maiúsculas e *escape* seguro de curingas `LIKE`, e limpar busca/filtros. | **Implementado** (`TerritoriesViewModel.kt`, `IndigenousLandQueryBuilder.kt`, `TerritoryControlsSection.kt`) | Busca corresponde a nome, código, etnias e municípios ignorando diacríticos e protegendo `%`, `_` e `\`; `onClearFilters()` restaura busca e seleções. |
| **RF-015** | Filtrar Terras Indígenas de forma combinável por **UF**, **Fase do processo** e **Modalidade** com base nas opções distintas presentes no Room. | **Implementado** (`RoomIndigenousLandRepository.observeFilterOptions`, `TerritoriesScreen.kt`) | Chips de UF, fase e modalidade filtram cumulativamente em SQLite e exibem contagem de resultados filtrados. |
| **RF-016** | Ordenar Terras Indígenas pelos 4 critérios (**Nome**, **Código**, **Superfície**, **Atualização**) em ordem crescente ou decrescente mantendo nulos, vazios e espaços sempre ao final. | **Implementado** (`IndigenousLandQueryBuilder.kt`, `TerritoryControlsSection.kt`, `TerritoriesViewModel.kt`) | Alternância de campo/direção ordena no SQLite mantendo `null`, `""` e `"   "` ao final em ASC e DESC, com desempate até `id ASC`. |
| **RF-017** | Exibir estados visuais de carregamento, lista vazia (com/sem filtros), erro de consulta com nova tentativa e erro não cancelante de sincronização manual com dispensa explícita. | **Implementado** (`TerritoriesScreen.kt`, `TerritoriesSyncStatusBar.kt`, `TerritoriesViewModel.kt`) | Exibe indicadores/cards adequados; exceção comum em `syncManually()` exibe `territories_manual_sync_error_banner` sem silenciar a falha, enquanto `CancellationException` é relançada. |

---

## 10. Requisitos Não Funcionais

| ID | Categoria | Descrição | Situação / Evidência no Código |
|---|---|---|---|
| **RNF-001** | Usabilidade e Design System | Material Design 3, tema claro/escuro, tipografia customizada (`Lora`, `Plus Jakarta Sans`, `JetBrains Mono`), *edge-to-edge* e `WindowInsets.safeDrawing`. | **Implementado e verificado** (`Theme.kt`, `Type.kt`, `Color.kt`, `MainActivity.kt`, `ParintintinNavHost.kt`). |
| **RNF-002** | Responsividade | `BoxWithConstraints` alternando 1 e 2 colunas (`maxWidth >= 600.dp`, limite `840.dp`), `FlowRow` e `LazyRow` em telas compactas e escalas de fonte ampliadas. | **Implementado e testado em Robolectric** (`HomeScreen.kt`, `DictionaryScreen.kt`, `TerritoriesScreen.kt`, `TerritoriesScreenTest.kt`, `ResponsiveComponentsTest.kt`). |
| **RNF-003** | Acessibilidade | `minimumInteractiveComponentSize()`, semântica `heading()` em seções, `contentDescription` específico para chips/badges/progresso/ordenação. | **Implementado e testado em Robolectric** (`TerritoriesScreenTest.kt`, `TerritoryCard.kt`, `TerritoryControlsSection.kt`). *Ressalva:* auditoria manual com TalkBack em dispositivo físico continua pendente. |
| **RNF-004** | Operação Offline (*Offline-First*) | Consulta integral de verbetes e territórios no Room sem rede e preservação de registros locais diante de falhas de sincronização. | **Implementado e testado em integração JVM/Room** (`TerritoriesSyncIntegrationTest.kt`, `DictionaryOfflineIntegrationTest.kt`). |
| **RNF-005** | Confiabilidade e Concorrência | Serialização via `Mutex` (`updateMutex`, `syncMutex`) e `syncJobLock`, transações Room (`withTransaction`), detecção de loop de paginação (`visitedOffsets`) e relançamento de `CancellationException`. | **Implementado e testado em JVM** (`RoomIndigenousLandRepositoryTest.kt`, `TerritoriesViewModelTest.kt`, `ArchiveSyncRepositoryImplTest.kt`). |
| **RNF-006** | Ciclo de Vida e Recursos | `SharingStarted.WhileSubscribed(5000)` nos ViewModels, `@ApplicationScope` (`core.common`) e liberação de `NetworkCallback` via `AutoCloseable`. | **Implementado e testado em escopo JVM** (`HomeViewModelTest.kt`, `TerritoriesViewModelTest.kt`, `AndroidNetworkConnectivityObserverTest.kt`). *Ressalva:* sem profiling de *memory leaks* em hardware. |
| **RNF-007** | Segurança de Transporte e SQL | HTTPS obrigatório para `https://pamgia.ibama.gov.br/...`, logging restrito a `BuildConfig.DEBUG`, consultas SQLite 100% parametrizadas com `SimpleSQLiteQuery`, validação de UF (`^[A-Z]{2}$`) e *escape* de wildcards `LIKE`. | **Implementado e testado** (`NetworkModule.kt`, `IndigenousLandQueryBuilderTest.kt`, `RemoteTerritoriesDataSourceTest.kt`). |
| **RNF-008** | Evolução de Banco de Dados | Schemas Room exportados (`2.json`, `3.json`, `4.json`) e migrações explícitas (`1->2`, `2->3`, `3->4`) sem destruição de dados. | **Implementado e testado** (`ParintintinDatabase.kt`, `ParintintinMigrations.kt`, `ParintintinDatabaseMigrationTest.kt`). |
| **RNF-009** | Estabilidade de Parser SQLite | Encadeamento de CTEs no `IndigenousLandQueryBuilder` limitado a 10 chamadas `REPLACE()` por estágio para evitar *stack overflow* no parser SQLite de versões antigas do Android. | **Implementado e testado** (`IndigenousLandQueryBuilder.kt`, `IndigenousLandQueryBuilderTest.kt`). |

---

## 11. Requisitos Técnicos

| Tecnologia / Padrão | Configuração Verificada | Papel Real no Projeto |
|---|---|---|
| **Kotlin + KSP** | Kotlin `2.2.10` \| KSP `2.3.5` | Linguagem principal e processamento de anotações (Room, Hilt, Moshi). |
| **Jetpack Compose + Navigation Compose** | BOM `2024.09.00` \| Navigation `2.8.9` | UI declarativa Material 3 e rotas tipadas `@Serializable` em 3 abas principais. |
| **Hilt** | `2.59` (`hilt-navigation-compose 1.2.0`) | Injeção `@Singleton` e `@HiltViewModel`. |
| **Room** | `2.7.0` (Schema **v4**) | Persistência local SQLite para verbetes, artigos, metadados e Terras Indígenas (`SupportSQLiteQuery` via `@RawQuery`). |
| **Retrofit + OkHttp + Moshi** | Retrofit `2.12.0`, OkHttp `4.10.0`, Moshi `1.15.2` | Cliente HTTPS para `https://pamgia.ibama.gov.br/server/rest/services/SIGAGEO/bases/MapServer/25/` com timeouts `15s`/`30s` e `IndigenousLandAttributesJsonAdapter` customizado. |
| **Coroutines + Flow + Serialization** | Coroutines `1.10.2` \| Kotlinx Serialization `1.8.0` | Fluxos reativos UI←Room, `debounce(150L)`, `flatMapLatest` e conversão JSON de listas no Room. |
| **Testes Automatizados** | JUnit `4.13.2`, Robolectric `4.16.1`, `MockWebServer 4.10.0`, Hilt Testing `2.59` | **35 suítes / 303 testes aprovados na JVM** + suíte `androidTest` compilada via `:app:compileDebugAndroidTestKotlin`. |

---

## 12. Regras de Negócio

1. **Fonte única de verdade local (`RN-01`):** `HomeScreen`, `DictionaryScreen` e `TerritoriesScreen` leem exclusivamente fluxos do Room (`DictionaryDao`, `IndigenousLandDao`, `SyncMetadataDao`).
2. **Integridade e versionamento do seed demonstrativo (`RN-02` e `RN-03`):** `VersionedDictionarySeedUpdater` valida bytes crus via SHA-256 e versão (`CURRENT_SEED_VERSION = 2`), mantendo idempotência (`ALREADY_UP_TO_DATE`).
3. **Preservação de favoritos e proteção pós-sync oficial (`RN-04` e `RN-05`):** atualizações do seed ocorrem em `withTransaction` preservando `is_favorite`; havendo `lastSuccessfulSyncEpochMillis != null`, o seed demonstrativo retorna `BLOCKED_BY_OFFICIAL_SYNC`. *(Nota de arquitetura atual: após a migração de `ArchiveSyncRepositoryImpl` para a API do IBAMA, `lastSuccessfulSyncEpochMillis` passa a ser preenchido quando ocorre sincronização territorial com sucesso, portanto uma sincronização territorial bem-sucedida também bloqueia futuras reaplicações do seed demonstrativo sobre um banco já inicializado.)*
4. **Fidelidade aos nulos na origem ArcGIS (`RN-06`):** `ArcGisTerritoriesMapper` e `IndigenousLandMappers` preservam campos ausentes/em branco como `null` (ou listas vazias), rejeitam `superficie` negativa ou não finita, e não geram textos substitutos no domínio/banco.
5. **Identificador determinístico de Terras Indígenas (`RN-07`):** `IndigenousLandMappers.resolveDeterministicTerritoryId(...)` prioriza `terrai_cod` normalizado, depois `objectId`, e por fim hash determinístico estável dos atributos disponíveis.
6. **Paginação segura e proteção contra loops (`RN-08`):** `RoomIndigenousLandRepository.syncFromRemote()` avança `resultOffset` somente quando `exceededTransferLimit == true && items.isNotEmpty()`, mantendo `visitedOffsets` para abortar offsets repetidos/inválidos e descartando a operação antes de abrir transação no Room caso qualquer página falhe.
7. **Tratamento de resposta remota vazia e falhas (`RN-09` e `RN-10`):** se todas as páginas retornarem zero itens válidos (`RemoteSyncOutcome.Empty`), os dados locais são preservados e grava-se `STATUS_REMOTE_EMPTY`; em erros de rede/HTTP/ArcGIS/desserialização, `SyncFailureClassifier` diferencia `NO_INTERNET`, `UNVALIDATED_NETWORK` e `SERVER_UNAVAILABLE` sem apagar dados nem sobrescrever `lastSuccessfulSyncEpochMillis`.
8. **Ordenação com nulos e vazios sempre no final (`RN-11`):** em SQLite (`IndigenousLandQueryBuilder`) e em memória (`TerritoriesViewModel` / testes), tanto em ordem `ASC` quanto `DESC`, registros com campo ordenável `null`, `""` ou apenas espaços (`TRIM = ''`) permanecem sempre ao final, com desempate determinístico até `id ASC`.
9. **Datas heterogêneas e busca sem acentos (`RN-12`):** `IndigenousLandQueryBuilder` converte datas `ISO-8601`, `dd/MM/yyyy` e timestamps Unix (segundos/milissegundos) para dia juliano no SQLite e remove acentuação via estágios CTE de até 10 `REPLACE()` por nível.
10. **Segurança de cancelamento e sincronização manual (`RN-13`):** exceções comuns durante `syncManually()` são convertidas em mensagem exibível e dispensável na UI (quando não cobertas por `OfflineFallback`), mantendo o bloqueio contra chamadas concorrentes (`syncJobLock`) e relançando obrigatoriamente `CancellationException`.

---

## 13. Arquitetura e Fluxos

### 13.1. Fluxo de Sincronização Territorial (IBAMA / SIGAGEO → Room)
1. `HomeViewModel.init` (auto-sync único) ou toque do usuário em `home_sync_button` / `territories_sync_button` aciona `ArchiveSyncCoordinatorImpl`.
2. `ArchiveSyncRepositoryImpl.syncArchive()` adquire `syncMutex`, emite `SyncState.Syncing`, executa `runSeedUpdateSafely()` e chama `indigenousLandRepository.syncFromRemote()`.
3. `RoomIndigenousLandRepository` pagina `GET query` em `IbamaTerritoriesApiService` via `RemoteTerritoriesDataSource` (`resultRecordCount = 100`, `orderByFields = "terrai_cod ASC, objectid ASC"`), deduplica por `id` e persiste atomicamente em `database.withTransaction { indigenousLandDao.upsertAll(...) }`.
4. Conforme o resultado:
   - `RemoteSyncOutcome.Synced(count)` → grava `STATUS_SYNCED`, `lastSuccessfulEntriesCount = count`, `lastSuccessfulRemoteVersion = "IBAMA/SIGAGEO Camada 25"`;
   - `RemoteSyncOutcome.Empty` → grava `STATUS_REMOTE_EMPTY` preservando base local;
   - `AppResult.Error` / exceção comum → classifica falha via `SyncFailureClassifier` e registra `STATUS_OFFLINE_FALLBACK`.

### 13.2. Fluxo de Consulta Local (Territórios)
1. `ParintintinNavHost` compõe `TerritoriesScreen` na rota `AppDestination.Territories` e obtém `TerritoriesViewModel` via `hiltViewModel()`.
2. Mudanças em `searchQuery` (`debounce(150L)`), UF, fase, modalidade e `TerritorySortOption` acionam `flatMapLatest` → `ObserveIndigenousLandsUseCase` → `RoomIndigenousLandRepository.observeTerritories(...)` → `IndigenousLandDao.observeTerritories(IndigenousLandQueryBuilder.build(...))`.
3. Paralelamente, `ObserveIndigenousLandFilterOptionsUseCase`, `ObserveIndigenousLandsCountUseCase` e `ObserveSyncStateUseCase` mantêm filtros, contadores e barra de status atualizados em tempo real.

---

## 14. Critérios Gerais de Aceitação

1. O aplicativo inicializa sem servidor ativo ou conexão, mantendo os 20 verbetes demonstrativos e os territórios previamente salvos acessíveis offline.
2. A barra inferior exibe exatamente **Início**, **Dicionário** e **Territórios**, preservando o estado de busca/filtros ao alternar abas e retornando à **Início** pelo botão voltar a partir de **Territórios**.
3. A tela **Territórios** permite pesquisar sem acentos, combinar filtros de UF, fase e modalidade, alternar entre os 4 critérios de ordenação em ASC/DESC mantendo nulos/vazios ao final, limpar filtros e acionar sincronização manual segura contra concorrência.
4. A API IBAMA / SIGAGEO é exibida respeitando estritamente os campos disponíveis (informações territoriais e etnias associadas), sem inventar dados ou apresentar perfis culturais/linguísticos completos inexistentes.
5. Toda a suíte automatizada JVM (`:app:testDebugUnitTest`) é executada e aprovada (`35` suítes / `303` testes, `0` falhas).

---

## 15. Restrições, Inconsistências Remanescentes e Riscos

| Tipo | Descrição | Impacto |
|---|---|---|
| **Dependência Externa (IBAMA / SIGAGEO)** | Disponibilidade, latência e esquema da URL `https://pamgia.ibama.gov.br/.../MapServer/25/query` dependem de serviço governamental externo. No container offline, os testes HTTP usam `MockWebServer`; a carga real de **627 registros** foi confirmada externamente pelo responsável. | Em ambientes sem acesso ao Geoportal ou em instabilidade externa, o app entra em `OfflineFallback` preservando dados locais. |
| **Precisão de Domínio (Territórios vs. Perfis Culturais)** | A camada 25 fornece apenas dados fundiários/territoriais e nomes de etnias associadas. | Documentação e UI devem evitar afirmar que o app possui perfis culturais, históricos ou linguísticos completos dos povos indígenas. |
| **Resíduos da Identidade v1.0** | `strings.xml` (`Acervo Provisório`), `settings.gradle.kts` (`AcervoProvisorio`), `metadata.json`, `HomeScreen` e `docs/api-v1-contract.md` ainda refletem textos/contratos da fase inicial. | Pode gerar ambiguidade visual entre a identidade *Voz Ancestral: Catálogo Digital de Terras e Povos Indígenas do Brasil* e o nome exibido no launcher/Home até refatoração específica de textos. |
| **Validação em Hardware e Release** | Sem execução de `:app:connectedDebugAndroidTest`, sem auditoria manual com TalkBack em dispositivo físico e sem assinatura Release no ambiente. | Comportamento em GPU/rede real e acessibilidade presencial permanecem pendentes de validação em dispositivo físico. |
| **Efeitos Colaterais de `lastSuccessfulSyncEpochMillis`** | O registro de sucesso territorial grava `lastSuccessfulSyncEpochMillis` no mesmo singleton `SyncMetadataEntity`, o que também aciona `BLOCKED_BY_OFFICIAL_SYNC` no `VersionedDictionarySeedUpdater`. | Caso o `dictionary_seed.json` receba futura versão demonstrativa local após um sync territorial, não será reaplicado automaticamente sem revisão dessa regra. |

---

## 16. Histórico do Documento

- **Versão `1.0`:** Reconstrução retrospectiva da Versão 1.0 Demonstrativa (*Acervo Provisório*, 2 abas, Room v3, `archive.parintintin.org`, 27 suítes / 252 testes).
- **Versão `2.0` (Atual):** Atualização integral pós-**Etapas 1 a 5.4**, refletindo a nova identidade **Voz Ancestral: Catálogo Digital de Terras e Povos Indígenas do Brasil**, integração IBAMA / SIGAGEO Camada 25, Room v4 (`MIGRATION_3_4` e `IndigenousLandQueryBuilder`), `TerritoriesViewModel` / `TerritoriesScreen`, navegação em 3 abas (`Início`, `Dicionário`, `Territórios`), **35 suítes / 303 testes JVM aprovados** e separação estrita entre fatos locais, confirmações externas (`627` registros, GitHub Actions) e pendências.
