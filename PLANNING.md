# PLANNING.md — Projeto Acadêmico Voz Ancestral (Aplicativo Acervo Provisório)

> **Nota de Natureza Documental:** Este planejamento foi **reconstruído retrospectivamente** a partir da inspeção do código-fonte atual, das configurações Gradle, da suíte de testes automatizados, dos artefatos de build, do `SPECIFICATION.md` e do contrato proposto em `docs/api-v1-contract.md`. Datas, reuniões, prazos acadêmicos, durações de atividades e aprovações formais que não possuem evidência verificável no repositório **não foram inventados**.

---

## 1. Identificação e Finalidade

| Atributo | Definição Oficial / Verificada | Origem / Referência |
|---|---|---|
| **Nome do Projeto Acadêmico** | **Voz Ancestral** | `SPECIFICATION.md` (Seção 1) |
| **Nome do Aplicativo** | **Acervo Provisório** (`AcervoProvisorio` no Gradle) | `app/src/main/res/values/strings.xml`, `metadata.json`, `settings.gradle.kts` |
| **Tema** | Preservação digital e documentação da língua Parintintin | `SPECIFICATION.md`, `strings.xml` |
| **Versão Documentada** | **1.0 Demonstrativa** (`versionCode = 1`, `versionName = "1.0"`) | `app/build.gradle.kts` |
| **Identificador Android (`applicationId`)** | `com.aistudio.parintintin.kagwah` (`namespace = "com.example"`) | `app/build.gradle.kts` |
| **Documento-Base de Escopo** | `SPECIFICATION.md` (v1.0 reconstruída retrospectivamente) | Raiz do repositório |

### Finalidade do Planejamento
Organizar, sob perspectiva retrospectiva e prospectiva, as fases de desenvolvimento identificáveis na base de código, as decisões arquiteturais e tecnológicas adotadas, as dependências entre componentes, o plano de validação por testes, os marcos do projeto, os riscos operacionais e a ordem de prioridade para a regularização documental e para a evolução futura do projeto **Voz Ancestral**.

---

## 2. Visão Geral da Estratégia

A arquitetura e a organização atual dos pacotes demonstram uma estratégia de desenvolvimento **incremental, modular e orientada à resiliência offline (*Offline-First*)**, com foco em consolidar a infraestrutura local antes da integração com um servidor remoto definitivo.

Os pilares dessa estratégia verificados no código são:
- **Preservação da implementação existente:** evoluções e regularizações documentais são planejadas sem descaracterizar o funcionamento já validado da Versão 1.0 Demonstrativa (telas **Início** e **Dicionário**).
- **Separação de responsabilidades:** divisão organizada entre interface declarativa e gerenciamento de estado (`presentation`), regras e contratos puros (`domain`) e mecanismos de persistência, seed, conectividade e rede (`data` e `di`). Embora os modelos e contratos de `domain` não dependam de Android SDK, Room ou Retrofit, a arquitetura não possui isolamento absoluto em todos os pontos — destaca-se a dependência pontual na camada `data` (`AndroidNetworkConnectivityObserver`, `ArchiveSyncRepositoryImpl` e `ArchiveSyncCoordinatorImpl`) pelo qualificador `@ApplicationScope`, definido no pacote `di` (`CoroutinesModule.kt`).
- **Desenvolvimento modular:** divisão por responsabilidades coesas (banco Room, engine de seed versionado, repositório offline-first, coordenador de sincronização, observador de conectividade e componentes visuais reutilizáveis).
- **Validação progressiva:** verificação contínua por suítes automatizadas na JVM (testes unitários, integração Room, contratos HTTP com `MockWebServer`, Robolectric e Compose UI Test) e compilação da suíte instrumentada (`androidTest`), mantendo cobertura sobre fluxos críticos e regressão.
- **Documentação das decisões técnicas:** formalização retrospectiva da especificação, do planejamento, das tarefas, dos resultados de testes e do contrato de API (`docs/api-v1-contract.md`) para assegurar rastreabilidade acadêmica.

---

## 3. Metodologia SDD (*Specification-Driven Development*)

Para a condução organizada da regularização e das evoluções subsequentes do projeto, adota-se o fluxo de **Desenvolvimento Orientado à Especificação (SDD)** estruturado em cinco fases:

1. **Specification (Especificação):** definir e documentar formalmente o escopo, os requisitos funcionais, os requisitos não funcionais, as regras de negócio, as limitações e os critérios de aceitação (`SPECIFICATION.md`), mantendo coerência estrita entre texto e comportamento do sistema.
2. **Planning (Planejamento):** organizar fases, dependências técnicas, decisões arquiteturais, estratégia de testes, marcos de entrega, riscos e prioridades de regularização e evolução (`PLANNING.md`).
3. **Task Decomposition (Decomposição de Tarefas):** quebrar objetivos de regularização documental, correções pontuais e funcionalidades futuras em unidades de trabalho pequenas, atômicas e verificáveis (`TASKS.md`, criado após este planejamento e atualizado na Etapa 1.7.2 com `DOC-001`–`DOC-007` concluídas).
4. **Implementation (Implementação):** executar modificações incrementais estritamente alinhadas às especificações e contratos acordados, preservando o escopo estável já validado.
5. **Validation (Validação):** comprovar conformidade por compilação, execução da suíte automatizada (`TEST_RESULTS.md`, criado e revisado na regularização documental), revisões de consistência e checagem dos critérios de conclusão.

### Diferenciação entre Reconstrução Retrospectiva e Regularização Documental
- **Processo original de desenvolvimento (reconstrução retrospectiva):** no estado atual do repositório, o código-fonte da Versão 1.0 Demonstrativa (incluindo etapas identificáveis como a implementação de conectividade e sincronização na UI e auditorias de regressão) foi construído antes da formalização integral dos documentos SDD na raiz do projeto. Não há documentação prévia escrita antes do código que permita comprovar a ordem cronológica exata ou reuniões originais de concepção.
- **Processo de regularização documental (SDD formal):** iniciado pela criação sequencial de `SPECIFICATION.md` (Etapa 1.1/1.1.1) e deste `PLANNING.md` (Etapa 1.2) — quando os artefatos subsequentes ainda estavam pendentes — e posteriormente completado com a criação e revisão de `TASKS.md`, `TEST_RESULTS.md`, `AI_USAGE.md` e `README.md`, a correção de `docs/api-v1-contract.md` (Etapa 1.7.1) e a atualização dos status documentais em `TASKS.md` (Etapa 1.7.2), concluindo as sete tarefas documentais (`DOC-001` a `DOC-007`) a partir do código verificado.

---

## 4. Organização das Fases do Projeto

Com base na inspeção dos módulos, contratos, recursos e relatórios de auditoria disponíveis no repositório, identificam-se as seguintes **11 fases** de desenvolvimento e regularização (sem atribuição de datas ou durações não comprovadas):

| Fase | Objetivo | Principais Entregas Verificadas | Situação | Dependências Relevantes |
|---|---|---|---|---|
| **1. Preparação da estrutura Android e arquitetura** | Configurar projeto Gradle/KSP, `applicationId`, tema Material 3, injeção Hilt e pacotes em camadas. | `app/build.gradle.kts`, `gradle/libs.versions.toml`, `ParintintinApplication.kt`, `MainActivity.kt`, módulos em `di/`, pacotes `presentation/`, `domain/`, `data/`, `core/`, `ui/theme/`. | **CONCLUÍDA** | Toolchain Android/Gradle (`agp 9.1.1`, `kotlin 2.2.10`, `ksp 2.3.5`, `hilt 2.59`). |
| **2. Implementação da persistência local com Room** | Estruturar banco SQLite local versionado com suporte a migrações e fluxos reativos. | `ParintintinDatabase` (v3), entidades (`DictionaryEntryEntity`, `CulturalArticleEntity`, `SyncMetadataEntity`, `SeedMetadataEntity`), DAOs, `ParintintinMigrations` (`1->2`, `2->3`), `ParintintinConverters` e schemas JSON exportados (`2.json`, `3.json`). | **CONCLUÍDA** | Fase 1; `DatabaseModule.kt`. |
| **3. Implementação do dicionário e seus modelos** | Modelar domínio linguístico, contratos de repositório, casos de uso e tela de consulta do dicionário demonstrativo. | `DictionaryEntry`, `GrammaticalClass`, `SemanticCategory`, `DictionaryRepository`, `OfflineFirstDictionaryRepository`, `DictionaryViewModel`, `DictionaryScreen.kt`. *(Módulos futuros como `Encyclopedia`, `Favorites` na UI, Detalhes e Áudio permanecem fora do escopo v1.0.)* | **CONCLUÍDA** *(escopo v1.0)* | Fases 1 e 2. |
| **4. Importação e validação do seed demonstrativo** | Garantir carga inicial offline controlada, idempotente e íntegra dos 20 registros demonstrativos. | `app/src/main/assets/dictionary_seed.json` (`DEMO-001`..`DEMO-020`), `DictionarySeedSpec`, `DictionarySeedJsonParser`, `SeedSha256Calculator`, `SeedVersionComparator`, `DictionarySeedChangeDetector`, `VersionedDictionarySeedUpdater`, `SeedModule.kt`. | **CONCLUÍDA** | Fases 2 e 3. |
| **5. Implementação da pesquisa e dos filtros** | Permitir busca textual reativa com *debounce* e filtragem combinada por categoria semântica e classe gramatical. | Consulta SQL parametrizada em `DictionaryDao.observeEntries(...)`, `debounce(150L)` e `combine`/`flatMapLatest` em `DictionaryViewModel`, chips horizontais e estados `Loading`/`Empty`/`Success`/`Error` em `DictionaryScreen.kt`. | **CONCLUÍDA** | Fases 2, 3 e 4. |
| **6. Estratégia Offline-First** | Assegurar leitura exclusivamente a partir do Room e proteção de dados locais e favoritos diante de falhas ou respostas vazias (`HTTP 200 []`). | Fluxos reativos UI←Room, transações atômicas com preservação de `is_favorite`, tratamento explícito de `RemoteSyncOutcome.Empty`, bloqueio do seed após sync oficial (`BLOCKED_BY_OFFICIAL_SYNC`) e preservação de `SyncMetadataEntity`. | **CONCLUÍDA** | Fases 2, 3 e 4. |
| **7. Integração e classificação de falhas de sincronização** | Monitorar conectividade do dispositivo e diferenciar causas de falha na sincronização sem bloquear o uso local. | `NetworkConnectivityObserver`, `AndroidNetworkConnectivityObserver` (`NetworkCallback`), `DeviceConnectivityState`, `SyncFailureClassifier`, `SyncFailureType`, `ArchiveSyncRepositoryImpl`, `ArchiveSyncCoordinatorImpl`, `RemoteArchiveDataSource`, `ParintintinApiService`. | **CONCLUÍDA** *(cliente)* | Fases 1, 2 e 6; `ACCESS_NETWORK_STATE`, `INTERNET`. *(Servidor oficial externo permanece sem disponibilidade confirmada.)* |
| **8. Interface Home e estados de conectividade** | Apresentar na tela inicial o escopo demonstrativo, estatísticas locais e feedback preciso dos estados de conectividade e sincronização. | `HomeScreen.kt`, `HomeViewModel.kt`, `OfflineArchiveStatusCard` e subcomponentes em `PreserveComponents.kt`, navegação restrita a `Home` e `Dictionary` em `ParintintinBottomBar.kt` / `ParintintinNavHost.kt`. | **CONCLUÍDA** | Fases 3, 6 e 7. |
| **9. Testes, integração e regressão** | Validar funcionamento, contratos, migrações, responsividade e ausência de regressão por suítes automatizadas. | 24 suítes JVM/Robolectric/Compose/Room/`MockWebServer` aprovadas (233 testes), ampliadas posteriormente para **25 suítes e 238 testes aprovados** após a conclusão de `TEC-001`; 2 suítes instrumentadas em `app/src/androidTest` compiladas via `:app:assembleDebugAndroidTest`, pendentes de execução em dispositivo/emulador. | **PARCIAL** | Fases 1 a 8; disponibilidade de emulador/ADB para `androidTest`. |
| **10. Preparação da versão 1.0 demonstrativa** | Consolidar artefato instalável v1.0 demonstrativo e verificar configuração de build/assinatura Release. | APK Debug v1.0 atual validado (`Voz-Ancestral-debug.apk` na raiz, `app/build/outputs/apk/debug/app-debug.apk` e `.build-outputs/app-debug.apk` — `22.218.647` bytes, `com.aistudio.parintintin.kagwah`, `versionCode = 1`, `versionName = "1.0"`, assinatura Debug v2) e registro histórico do build anterior (`.build-outputs/Parintintin-v1.0-debug.apk`, `22.753.989` bytes); build Release bloqueado em `:app:packageRelease` por ausência de `my-upload-key.jks` e variáveis de assinatura. | **PARCIAL** | Fase 9; keystore Release dedicada e variáveis seguras para distribuição final. |
| **11. Regularização documental (SDD)** | Reconstruir e alinhar a documentação acadêmica e técnica do projeto ao estado verificado do código. | Conjunto documental concluído (`DOC-001` a `DOC-007`): `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md`, `AI_USAGE.md` e `README.md` criados e revisados; `docs/api-v1-contract.md` corrigido (Etapa 1.7.1) e status atualizados em `TASKS.md` (Etapa 1.7.2). | **CONCLUÍDA** | Fases 1 a 10 e auditoria técnica geral. |

*Resumo situacional das fases:* **9 concluídas** (Fases 1 a 8 e 11), **2 parciais** (Fases 9 e 10).

---

## 5. Decisões Arquiteturais e Tecnológicas

| Decisão | Evidência no Repositório | Justificativa Técnica Verificada |
|---|---|---|
| **Clean Architecture em pacotes (`presentation`, `domain`, `data`)** | Pacotes dedicados em `app/src/main/java/com/example/` e casos de uso em `ArchiveUseCases.kt`. | Organiza a separação de responsabilidades entre apresentação, regras/contratos de domínio (`SyncFailureClassifier`, `DictionaryEntry`, `SyncState`) e implementações de dados, mantendo `domain` livre de Android SDK, Room e Retrofit e facilitando testes unitários na JVM; registra-se o acoplamento pontual de classes de `data` ao qualificador `@ApplicationScope` do pacote `di`. |
| **MVVM com estado imutável (`StateFlow`)** | `HomeViewModel`, `DictionaryViewModel`; `EncyclopediaViewModel` presente apenas como estrutura não exposta na v1.0. | Centraliza a lógica de apresentação e transformação de fluxos reativos em `ViewModel`, expondo `UiState` via `SharingStarted.WhileSubscribed(5000)` para sobreviver a recomposições e mudanças de configuração. Na experiência visível da v1.0, apenas `HomeViewModel` e `DictionaryViewModel` são acessíveis ao usuário. |
| **Jetpack Compose + Material 3** | `MainActivity.kt`, `Theme.kt`, `Type.kt`, `Color.kt`, telas e `PreserveComponents.kt`. | Permite construção declarativa de UI responsiva (`BoxWithConstraints`, `FlowRow`, `LazyColumn` com `stickyHeader`), suporte a tema claro/escuro e semântica de acessibilidade integrada. |
| **Hilt (`@Singleton` e `@HiltViewModel`)** | `ParintintinApplication.kt`, 7 módulos em `di/`, `hilt-navigation-compose`. | Padroniza o grafo de dependências entre camadas, desacopla interfaces de domínio (`NetworkConnectivityObserver`, repositórios, coordenador) das implementações e viabiliza substituição de bindings em testes (`androidTest/di/`). |
| **Kotlin Coroutines e `Flow` / `StateFlow`** | `CoroutinesModule.kt` (`@ApplicationScope` com `SupervisorJob() + dispatchers.io`), `DefaultDispatcherProvider`, `Mutex`, `async`. | Estruturas assíncronas cujo comportamento de *thread* depende dos *dispatchers* configurados: nos fluxos confirmados no código, `CoroutinesModule` associa `dispatchers.io` (`Dispatchers.IO`) ao `@ApplicationScope`, enquanto `VersionedDictionarySeedUpdater`, `DictionarySeedChangeDetector`, `DictionarySeedImporter`, os repositórios offline-first e `ArchiveSyncRepositoryImpl` aplicam explicitamente `withContext(dispatchers.io)` ou `.flowOn(dispatchers.io)` para leitura de seed, hashing SHA-256 invocado no updater/detector, consultas Room e chamadas remotas. `Mutex` (`updateMutex`, `syncMutex`) serializa escritas e `ArchiveSyncCoordinatorImpl` utiliza `applicationScope.async(start = CoroutineStart.LAZY)` para manter a sincronização desacoplada da tela. |
| **Room (Schema v3 com `exportSchema = true`)** | `ParintintinDatabase.kt`, `ParintintinMigrations.kt`, `app/schemas/.../2.json` e `3.json`. | Fornece persistência relacional tipada em SQLite com observação reativa (`Flow`), suporte nativo a transações atômicas (`withTransaction`) e rastreabilidade explícita de migrações de schema. |
| **Retrofit + OkHttp + Moshi (codegen via KSP)** | `NetworkModule.kt`, `ParintintinApiService.kt`, `ArchiveDtos.kt`, `RemoteArchiveDataSource.kt`. | Define cliente HTTP tipado sobre HTTPS com timeouts explícitos (`5 s` conexão / `10 s` leitura), logging restrito a `BuildConfig.DEBUG` e adaptadores JSON gerados em compilação (`@JsonClass(generateAdapter = true)`). |
| **Estratégia *Offline-First*** | `OfflineFirstDictionaryRepository.kt`, `ArchiveSyncRepositoryImpl.kt`, `HomeScreen.kt`, `DictionaryScreen.kt`. | Garante disponibilidade imediata do acervo local independentemente de rede ou servidor ativo, tratando falhas de sincronização como estado informativo (`OfflineFallback`) e não como erro bloqueante da consulta. |
| **Seed JSON versionado com SHA-256** | `VersionedDictionarySeedUpdater.kt`, `SeedVersionComparator.kt`, `SeedSha256Calculator.kt`, `dictionary_seed.json`. | Assegura carga inicial determinística e idempotente dos 20 registros `DEMO-001`..`DEMO-020`, detectando alterações reais de conteúdo por hash hexadecimal bruto e bloqueando sobrescrita após sincronização oficial (`BLOCKED_BY_OFFICIAL_SYNC`). |
| **Suítes automatizadas multicamadas (JVM + Robolectric + MockWebServer + `androidTest`)** | 25 arquivos em `app/src/test` e 2 suítes em `app/src/androidTest`. | Permite validar regras puras, migrações Room, serialização HTTP, reatividade de ViewModels e renderização/acessibilidade Compose sem dependência contínua de hardware físico. |

---

## 6. Dependências entre Funcionalidades

```text
[dictionary_seed.json] ──▶ [VersionedDictionarySeedUpdater + SHA-256]
                                      │
                                      ▼ (transação Room + proteção pós-sync oficial)
[Room: ParintintinDatabase] ◀──▶ [Repositórios Offline-First & Sync] ◀──▶ [RemoteArchiveDataSource / Retrofit]
         │                                       ▲                                   ▲
         │ (Flow reativo)                        │                                   │
         ▼                                       │                                   │
[ObserveArchiveStats / ObserveEntries]    [AndroidNetworkConnectivityObserver]  [Backend Oficial Ativo*]
         │                                       │                                (*dependência externa pendente)
         ▼                                       ▼
[HomeViewModel / DictionaryViewModel] ◀── [SyncFailureClassifier]
         │
         ▼ (StateFlow<UiState>)
[HomeScreen / DictionaryScreen / OfflineArchiveStatusCard]

[Conteúdo Linguístico Validado e Autorizado*] ──▶ Substituição futura dos registros DEMO-001..DEMO-020
```

Relações técnicas verificadas:
- **Distinção entre estruturas existentes e experiência visível na v1.0:** a experiência funcional disponível ao usuário na Versão 1.0 Demonstrativa permanece concentrada exclusivamente em **Início** e **Dicionário**. Embora existam no código estruturas preparatórias da Enciclopédia (`EncyclopediaDao`, `OfflineFirstEncyclopediaRepository`, `EncyclopediaViewModel` e `EncyclopediaScreen`), isso **não** implica disponibilização na navegação nem sincronização operacional da Enciclopédia na v1.0 (o módulo possui `0` artigos locais e `ArchiveSyncRepositoryImpl.syncArchive()` sincroniza somente o dicionário).
- **Room como base da consulta local:** na experiência ativa da v1.0, a listagem, pesquisa e filtragem de verbetes e os metadados de status provêm exclusivamente de fluxos observáveis do Room (`DictionaryDao` e `SyncMetadataDao`). Estruturalmente, `ArchiveSyncRepositoryImpl.observeArchiveStats()` também combina contagens de `EncyclopediaDao` via `OfflineFirstEncyclopediaRepository` (`0` registros na v1.0).
- **Seed como mecanismo de inicialização demonstrativa:** `VersionedDictionarySeedUpdater` popula e atualiza `dictionary_entries` a partir do asset `dictionary_seed.json`, ficando bloqueado caso exista registro de sincronização oficial prévia (`lastSuccessfulSyncEpochMillis != null`).
- **Repositórios como abstração de persistência e rede:** `OfflineFirstDictionaryRepository` e `ArchiveSyncRepositoryImpl` encapsulam transações Room do dicionário, preservação de `is_favorite` e chamadas a `RemoteArchiveDataSource`; `OfflineFirstEncyclopediaRepository` implementa contrato análogo na camada `data`, porém sem acionamento pelo fluxo de sincronização nem acesso pela navegação v1.0.
- **ViewModels como responsáveis pelos estados apresentados na UI:** `HomeViewModel` e `DictionaryViewModel` combinam casos de uso e entradas do usuário (busca com *debounce* e filtros) em estados tipados (`HomeUiState`, `DictionaryUiState`) consumidos via `collectAsStateWithLifecycle()`.
- **Conectividade como insumo para classificação de falhas:** `AndroidNetworkConnectivityObserver` fornece `StateFlow<DeviceConnectivityState>` ao `SyncFailureClassifier`, permitindo ao `ArchiveSyncRepositoryImpl` distinguir `NO_INTERNET`, `UNVALIDATED_NETWORK` e `SERVER_UNAVAILABLE` dentro de `SyncState.OfflineFallback`.
- **Backend oficial como dependência para sincronização real:** a transição efetiva do modo fallback para atualização remota oficial em ambiente real depende da disponibilização operacional da API compatível com `docs/api-v1-contract.md`, da implementação do manifesto (`/v1/manifest.json`) e da definição de política de exclusão.
- **Conteúdo validado e autorizado como dependência para substituição dos dados DEMO:** a substituição dos 20 registros `DEMO-001`..`DEMO-020` por vocabulário autêntico Parintintin e artigos culturais depende de curadoria linguística, validação e autorização formal de publicação.

---

## 7. Marcos do Projeto (*Milestones*)

> **Nota sobre aprovações:** Não há registro documental de datas ou aprovações acadêmicas formais prévias no repositório; a situação abaixo reflete exclusivamente as evidências técnicas verificadas no código e nas auditorias.

| Marco | Descrição | Evidência Principal | Situação |
|---|---|---|---|
| **M1: Estrutura arquitetural** | Definição Gradle/KSP, pacotes `presentation`/`domain`/`data`/`di`, tema M3 e `AppDestination`. | `app/build.gradle.kts`, módulos em `di/`, `Theme.kt`. | **CONCLUÍDO** |
| **M2: Persistência local** | Banco Room v3 funcional, migrações `1->2` e `2->3`, schemas exportados e DAOs reativos. | `ParintintinDatabase.kt`, `ParintintinMigrations.kt`, `2.json`, `3.json`. | **CONCLUÍDO** |
| **M3: Dicionário demonstrativo** | Seed `DEMO-001`..`DEMO-020` com hash SHA-256, `DictionaryViewModel` e listagem em `DictionaryScreen`. | `dictionary_seed.json`, `VersionedDictionarySeedUpdater.kt`, `DictionaryScreen.kt`. | **CONCLUÍDO** |
| **M4: Pesquisa e filtros** | Busca textual com *debounce* de `150 ms` e filtros SQL combinados por categoria e classe gramatical. | `DictionaryDao.kt`, `DictionaryViewModel.kt`, `DictionaryScreen.kt`. | **CONCLUÍDO** |
| **M5: Sincronização e conectividade** | Coordenador com deduplicação, estratégia offline-first, `AndroidNetworkConnectivityObserver` e `SyncFailureClassifier`. O cliente Android possui essas estruturas implementadas, **o que não comprova disponibilidade operacional do backend oficial nem sincronização remota bem-sucedida em produção** (servidor oficial permanece sem disponibilidade operacional confirmada). | `ArchiveSyncCoordinatorImpl.kt`, `ArchiveSyncRepositoryImpl.kt`, `AndroidNetworkConnectivityObserver.kt`. | **CONCLUÍDO** *(cliente)* |
| **M6: Interface demonstrativa integrada** | `HomeScreen` com avisos demonstrativos, `OfflineArchiveStatusCard` detalhado e navegação exclusiva `Início` ↔ `Dicionário`. | `HomeScreen.kt`, `PreserveComponents.kt`, `ParintintinNavHost.kt`, `ParintintinBottomBar.kt`. | **CONCLUÍDO** |
| **M7: Validação técnica** | Execução da suíte JVM/Robolectric/Compose/Room/`MockWebServer` (marco inicial de 233 testes, ampliado posteriormente para 238 testes após `TEC-001`) e compilação da suíte instrumentada (`androidTest`). | 24 suítes iniciais em `app/src/test` aprovadas (posteriormente 25 suítes com `EncyclopediaViewModelTest`); `:app:assembleDebugAndroidTest` compilado; execução em dispositivo/emulador pendente. | **PARCIAL** |
| **M8: Versão 1.0 demonstrativa** | Geração e verificação do APK Debug v1.0 (`Voz-Ancestral-debug.apk` e histórico `.build-outputs/Parintintin-v1.0-debug.apk`) e auditoria de configuração Release. | `Voz-Ancestral-debug.apk` na raiz (`22.218.647` bytes, idêntico a `app/build/outputs/apk/debug/app-debug.apk` e `.build-outputs/app-debug.apk`) e histórico `.build-outputs/Parintintin-v1.0-debug.apk` (`22.753.989` bytes) validados; APK Release pendente de keystore e variáveis de assinatura. | **PARCIAL** |
| **M9: Regularização documental** | Reconstrução e entrega do conjunto documental SDD e correção de inconsistência contratual. | Sete tarefas documentais concluídas (`DOC-001` a `DOC-007`): `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md` (atualizado na Etapa 1.7.2), `TEST_RESULTS.md`, `AI_USAGE.md` e `README.md` presentes e revisados, e `docs/api-v1-contract.md` corrigido na Etapa 1.7.1. | **CONCLUÍDO** |
| **M10: Evolução oficial pós-v1.0** | Operacionalização de backend oficial, manifesto, conteúdo linguístico autêntico e habilitação dos módulos Enciclopédia, Favoritos, Detalhes e Áudio. | Estruturas parciais e contrato proposto em `docs/api-v1-contract.md`. | **PENDENTE** |

*Resumo situacional dos marcos:* **7 concluídos** (`M1` a `M6` e `M9`), **2 parciais** (`M7` e `M8`) e **1 pendente** (`M10`).

---

## 8. Planejamento de Testes e Validação

A estratégia de verificação do projeto é composta por seis frentes técnicas, diferenciando claramente o que já foi executado com sucesso na JVM do que foi apenas compilado para execução externa:

| Frente de Teste | Escopo Verificado | Suítes / Artefatos Representativos | Estado Comprovado |
|---|---|---|---|
| **Testes Unitários** | Regras puras de domínio, versão/hash de seed, classificação de falhas, coordenador de sync e ViewModels. | `ExampleUnitTest`, `SeedSha256CalculatorTest`, `SeedVersionComparatorTest`, `DictionarySeedChangeDetectorTest`, `DictionarySeedImporterTest`, `VersionedDictionarySeedUpdaterTest`, `ArchiveSyncCoordinatorImplTest`, `ArchiveSyncRepositoryImplTest`, `OfflineFirstDictionaryRepositoryTest`, `OfflineFirstEncyclopediaRepositoryTest`, `AndroidNetworkConnectivityObserverTest`, `DictionaryViewModelTest`, `HomeViewModelTest`, `EncyclopediaViewModelTest`. | **Executado na JVM — Aprovado** |
| **Testes de Integração Room** | Migrações `1->2` e `2->3`, persistência de metadados, preservação de favoritos e fluxo seed→Room→repositório. | `ParintintinDatabaseMigrationTest`, `DictionaryOfflineIntegrationTest`. | **Executado na JVM — Aprovado** |
| **Testes HTTP com `MockWebServer`** | Contratos JSON Moshi, rejeição de nulos obrigatórios, tolerância a campos extras, `HTTP 200 []`, `4xx`/`5xx` e timeouts. | `ParintintinApiContractTest`, `RemoteArchiveDataSourceTest`. | **Executado na JVM — Aprovado** |
| **Testes Robolectric** | Comportamento de framework Android (`ConnectivityManager`, recursos, `Application`) na JVM. | `ExampleRobolectricTest`, `AndroidConnectivityPlatformRobolectricTest`. | **Executado na JVM — Aprovado** |
| **Testes de Interface Compose** | Renderização, textos demonstrativos, filtros, estados de conectividade em `OfflineArchiveStatusCard`, responsividade (`280.dp`, `600.dp`, `fontScale 1.3f`) e navegação. | `HomeScreenTest`, `DictionaryScreenTest`, `OfflineArchiveStatusCardTest`, `ResponsiveComponentsTest`, `ParintintinNavHostTest`. | **Executado na JVM (Robolectric + Compose Test) — Aprovado** |
| **Testes Instrumentados Android (`androidTest`)** | Fluxo completo com Hilt real em `MainActivity`, banco Room em dispositivo, seed idempotente e navegação real. | `StartupSeedHiltInstrumentedTest`, `ExampleInstrumentedTest`, `HiltTestRunner`. | **Apenas compilado (`:app:assembleDebugAndroidTest`) — Execução em dispositivo/emulador PENDENTE** |

### Consolidação Verificada de Execução
- Conforme os relatórios de auditoria técnica e regressão do projeto, a execução inicial da v1.0 de `gradle :app:testDebugUnitTest` registrou **233 testes aprovados em 24 suítes** na JVM, sem falhas; posteriormente, com a conclusão de `TEC-001` e a adição de `EncyclopediaViewModelTest` (+5 testes), a suíte completa passou a contabilizar **238 testes aprovados em 25 suítes** (`0` falhas, `0` ignorados). Essa contagem completa (**238/238** em **25** arquivos XML) foi novamente confirmada em 22/11/2026 com `JAVA_TOOL_OPTIONS="-Duser.home=/home/appuser" gradle :app:testDebugUnitTest` (contornando a localização do cache offline do Robolectric em `/home/appuser/.m2` quando o shell roda sob `HOME=/root`).
- Os testes instrumentados em `app/src/androidTest` (2 arquivos de teste totalizando `3` métodos `@Test`, apoiados por `HiltTestRunner`, `TestDatabaseModule` e `TestNetworkModule`) foram compilados em etapa anterior, mas **não puderam ser executados neste ambiente** devido à ausência de emulador Android e de acesso a `adb` (`connectedDebugAndroidTest` permanece pendente em ambiente externo com dispositivo/emulador).
- Não existem nesta versão testes instrumentais específicos de detecção de *memory leaks* (ex.: LeakCanary) nem auditoria manual com TalkBack ou *benchmarks* em aparelho físico.

---

## 9. Riscos e Estratégias de Mitigação

| Risco Identificado | Impacto | Estratégia de Mitigação Realista |
|---|---|---|
| **1. Ausência de servidor oficial operacional confirmado** | Sincronização de rede em execução normal não recebe acervo oficial, permanecendo no estado `OfflineFallback` (`NO_INTERNET`, `UNVALIDATED_NETWORK` ou `SERVER_UNAVAILABLE`). | Manter operação integral *Offline-First* baseada no Room; preservar contrato explícito em `docs/api-v1-contract.md`; validar o cliente com `MockWebServer` e planejar a homologação contra backend real antes de liberar atualização remota. |
| **2. Conteúdo linguístico autêntico ainda não validado** | O aplicativo não pode ser utilizado como ferramenta lexicográfica autêntica da língua Parintintin na versão atual, e erros de identificação poderiam induzir interpretação falsa. | Manter todos os 20 registros identificados de `DEMO-001` a `DEMO-020` como termos e expressões demonstrativos fictícios em português, preservar os avisos permanentes `"MODO DEMONSTRATIVO"` na Home e no Dicionário e manter campos IPA/exemplos/áudio vazios até curadoria especializada. |
| **3. Dependência de autorização para publicação cultural** | Risco ético, acadêmico e jurídico de divulgar vocabulário ou narrativas culturais sem consentimento adequado. | Condicionar a substituição do seed e a ativação da Enciclopédia Cultural à obtenção formal de autorização comunitária/institucional e à validação linguística documentada antes da publicação de dados reais. |
| **4. Ausência de execução dos testes instrumentados em dispositivo** | Regressões específicas de ciclo de vida Android real, `ConnectivityManager` de hardware, TalkBack ou grafo Hilt em runtime podem não ser detectadas apenas na JVM. | Executar `:app:connectedDebugAndroidTest` (incluindo `StartupSeedHiltInstrumentedTest`) e testes manuais de usabilidade/TalkBack em dispositivo Android ou emulador externo antes da entrega acadêmica final. |
| **5. Assinatura Release não configurada e artefatos Debug no workspace** | O build `:app:assembleRelease` falha em `:app:packageRelease` por ausência de `my-upload-key.jks` e variáveis `KEYSTORE_PATH`/`STORE_PASSWORD`/`KEY_PASSWORD`. Adicionalmente, o `.gitignore` atual não ignora `*.apk` nem `.build-outputs/`, e o script `gradlew` presente na raiz está sem permissão de execução (`+x`) no container. | Utilizar o APK Debug verificado (`Voz-Ancestral-debug.apk` na raiz ou `.build-outputs/app-debug.apk`) exclusivamente para validação interna/acadêmica controlada, gerar o APK/AAB Release assinado mediante fornecimento seguro de keystore dedicada fora do código-fonte e observar `sh gradlew` ou `chmod +x gradlew` em ambientes Unix antes de executar o wrapper. |
| **6. Limitações de módulos e artefatos ainda provisórios** | Embora a divergência documental anterior entre as Seções 6.3 e 6.4 de `docs/api-v1-contract.md` já tenha sido corrigida na Etapa 1.7.1 (`DOC-007`) e o saneamento `TEC-001` em `EncyclopediaViewModel.kt` já tenha sido concluído e validado com `EncyclopediaViewModelTest.kt`, permanecem no repositório as estruturas preparatórias de `Encyclopedia` e as telas provisórias `Favorites` e `Archive` fora da navegação funcional da v1.0, o que pode gerar confusão em manutenção futura. | Manter `Encyclopedia`, `Favorites` e `Archive` fora da navegação funcional da v1.0 até a implementação completa e autorização das evoluções pós-v1.0. |

---

## 10. Funcionalidades Futuras e Prioridades

### Prioridade 1 — Regularização Documental e Saneamento Técnico Pontual (Concluídos)
Sequência de itens priorizados para o fechamento do ciclo documental e técnico da Versão 1.0 Demonstrativa, agora integralmente concluída (`DOC-003` a `DOC-007` e `TEC-001`):
1. **`TASKS.md` — CONCLUÍDO (`DOC-003`):** decomposição executável e rastreável das tarefas de regularização, correções e evolução futura, criada e revisada na raiz do repositório.
2. **`TEST_RESULTS.md` — CONCLUÍDO (`DOC-004`):** registro consolidado e auditável dos resultados históricos das 24 suítes JVM (233 testes — posteriormente ampliados no relatório para **25 suítes e 238 testes aprovados** após `TEC-001`), da compilação de `androidTest` e das pendências em dispositivo físico, criado e revisado na raiz do repositório.
3. **`AI_USAGE.md` — CONCLUÍDO (`DOC-005`):** registro transparente do uso de ferramentas de IA no ciclo do projeto acadêmico, criado e revisado na raiz do repositório.
4. **`README.md` — CONCLUÍDO (`DOC-006`):** visão geral de apresentação, instruções de compilação/execução de testes e escopo real do projeto, criado e revisado na raiz do repositório.
5. **Correção da inconsistência no contrato da API (`docs/api-v1-contract.md`) — CONCLUÍDO (`DOC-007`, Etapa 1.7.1):** Seções 1, 6.1, 6.3 e 6.4 alinhadas ao código real de `ArchiveSyncRepositoryImpl.syncArchive()`, deixando explícito que a enciclopédia permanece fora da sincronização na v1.0.
6. **Correção do tratamento de `CancellationException` em `EncyclopediaViewModel.kt` — CONCLUÍDO (`TEC-001`):** saneamento técnico preparatório aplicado em `EncyclopediaViewModel.kt`, relançando explicitamente `CancellationException` no operador `.catch` antes de emitir `EncyclopediaArticlesState.Error` (alinhado ao padrão já adotado em `DictionaryViewModel.kt`) e validado por `EncyclopediaViewModelTest.kt` (5 testes aprovados, com regressão completa de 238 testes em 25 suítes na JVM), mantendo o módulo fora da navegação da v1.0.

### Prioridade 2 — Evolução Funcional Futura (Pós-v1.0)
Funcionalidades planejadas para ciclos posteriores à Versão 1.0 Demonstrativa e à regularização documental:
1. **Conteúdo linguístico autêntico validado e autorizado:** substituição controlada dos registros `DEMO-001`..`DEMO-020` por vocabulário e notas culturais devidamente validados e autorizados.
2. **Integração com backend oficial ativo:** publicação e homologação dos endpoints HTTPS definidos em `docs/api-v1-contract.md`.
3. **Manifesto remoto (`/v1/manifest.json`) e política de exclusão:** consumo prévio de versão/contagens no cliente e definição entre modelo por snapshot completo ou *tombstones*.
4. **Enciclopédia Cultural completa:** inclusão de `encyclopediaRepository.syncFromRemote()` na sincronização (com critério transacional explícito), eventual suporte a seed/conteúdo oficial e exposição na navegação principal.
5. **Interface de Favoritos:** adição de botão/ação de favoritar nos cards da `DictionaryScreen` utilizando `ToggleFavoriteEntryUseCase` já existente e substituição da `ProvisionalDestinationScreen` em `AppDestination.Favorites`.
6. **Tela de detalhes de verbetes:** construção de tela e rota dedicada conectada a `ObserveDictionaryEntryByIdUseCase` para exibir fonética IPA, exemplos bilíngues e notas completas.
7. **Reprodução de áudio:** definição do contrato de mídia offline/online para `audioUrl` e integração de reprodutor compatível com Media3/ExoPlayer.

---

## 11. Critérios de Conclusão

### 11.1 Critérios para Documentação Regularizada
*(Situação atual: **critérios atendidos** com a conclusão das sete tarefas documentais `DOC-001` a `DOC-007` — bem como do saneamento técnico `TEC-001` registrado na Seção 11.3 —, permanecendo abertas nas Seções 11.2 e 11.3 apenas as pendências externas de validação em dispositivo/Release, autorizações de conteúdo e definições contratuais pós-v1.0.)*
- `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md`, `AI_USAGE.md` e `README.md` criados e revisados na raiz do repositório (`DOC-001` a `DOC-006` concluídas; `TASKS.md` atualizado na Etapa 1.7.2).
- `docs/api-v1-contract.md` atualizado na Etapa 1.7.1 (`DOC-007` concluída) para eliminar a divergência das Seções 1, 6.1 e 6.3 em relação à Seção 6.4 e à implementação atual de `ArchiveSyncRepositoryImpl.syncArchive()`.
- Coerência verificada entre os documentos SDD, o código-fonte, os recursos (`strings.xml`, `dictionary_seed.json`) e os relatórios de auditoria, sem atribuição falsa de funcionalidades futuras como concluídas e sem confundir conclusão documental com validação em hardware real.

### 11.2 Critérios para Versão 1.0 Demonstrativa Tecnicamente Validada
*(Critérios restritos à experiência demonstrativa visível da v1.0 — telas **Início** e **Dicionário** — sem incluir pré-requisitos exclusivos de módulos futuros não expostos.)*
- Suíte JVM (`gradle :app:testDebugUnitTest`) integralmente aprovada (marco histórico v1.0 de **233 testes aprovados em 24 suítes**, ampliado posteriormente para **238 testes aprovados em 25 suítes** após `TEC-001`), com cobertura mantida para Room, seed SHA-256, conectividade, sincronização e UI Compose.
- APK Debug v1.0 gerado e íntegro (`com.aistudio.parintintin.kagwah`, `versionCode = 1`, `versionName = "1.0"`), com registro explícito da **ausência de assinatura Release configurada no ambiente**.
- Execução confirmada em ambiente externo dos testes instrumentados (`:app:connectedDebugAndroidTest`, atualmente apenas compilados) e verificação manual básica (inclusive acessibilidade TalkBack e fluxo offline-first) em dispositivo Android ou emulador antes da entrega acadêmica final.

### 11.3 Critérios de Saneamento Técnico e Preparação para Funcionalidades Futuras
*(Critérios para saneamento de estruturas preparatórias e planejamento de implementação pós-v1.0.)*
- **Saneamento técnico prévio à ativação do módulo `Encyclopedia` (`TEC-001` — CONCLUÍDO):** relançamento explícito de `CancellationException` aplicado no operador `.catch` de `EncyclopediaViewModel.kt` e validado por `EncyclopediaViewModelTest.kt` (5 testes aprovados, mantendo aprovação integral de 238 testes em 25 suítes na JVM), permanecendo o módulo fora da exposição na v1.0.
- Escopo v1.0 congelado e documentado sem pendências de regularização.
- Requisitos de conteúdo autêntico (curadoria linguística e autorização comunitária/institucional) formalmente aprovados fora do código.
- Decisões pendentes do contrato da API (Seção 7 de `docs/api-v1-contract.md`: política de exclusão, formato de `version`, validação de contagens, cache HTTP e áudio) definidas antes da codificação do servidor e da integração no cliente.

---

## 12. Histórico e Limitações do Planejamento

- **Versão do documento:** `1.0` (Reconstrução retrospectiva alinhada ao estado da Etapa 30.0 e ao `SPECIFICATION.md`).
- **Metodologia de elaboração:** este planejamento foi reconstruído **retrospectivamente** a partir dos artefatos verificáveis no repositório (código Kotlin/Compose, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `docs/api-v1-contract.md`, `app/schemas/`, suítes em `app/src/test` e `app/src/androidTest`, `.build-outputs/` e `SPECIFICATION.md`).
- **Limitações explicitamente registradas:**
  - Não foram inventadas datas de início/término, estimativas de horas, cronogramas semanais, atas de reunião, aprovações formais de marcos ou prazos acadêmicos sem evidência no repositório.
  - Operações Git não foram executadas durante esta etapa em respeito estrito às regras de preservação, razão pela qual o histórico de commits não foi consultado via CLI Git.
  - Motivações históricas anteriores à estrutura atual do código não foram presumidas além do que está tecnicamente evidenciado na arquitetura e nos comentários/documentos presentes no projeto.
