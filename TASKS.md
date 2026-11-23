# TASKS.md — Projeto Acadêmico Voz Ancestral (Aplicativo Acervo Provisório)

> **Nota de Natureza Documental:** Este inventário de tarefas foi **reconstruído retrospectivamente** a partir do estado real verificado do código-fonte, do `SPECIFICATION.md`, do `PLANNING.md`, do contrato proposto em `docs/api-v1-contract.md` e dos artefatos de teste e build da **Versão 1.0 Demonstrativa**. Não foram inventadas tarefas históricas, datas, responsáveis, prazos ou aprovações sem comprovação.

---

## 1. Identificação e Finalidade

| Atributo | Definição Oficial / Verificada | Referência |
|---|---|---|
| **Projeto Acadêmico** | **Voz Ancestral** | `SPECIFICATION.md`, `PLANNING.md` |
| **Aplicativo** | **Acervo Provisório** (`AcervoProvisorio`) | `app/src/main/res/values/strings.xml`, `settings.gradle.kts`, `metadata.json` |
| **Versão Atual** | **1.0 Demonstrativa** (`versionCode = 1`, `versionName = "1.0"`) | `app/build.gradle.kts` |
| **Identificador Android (`applicationId`)** | `com.aistudio.parintintin.kagwah` | `app/build.gradle.kts` |
| **Namespace Kotlin/Android** | `com.example` | `app/build.gradle.kts` |

### Finalidade e Relação com `SPECIFICATION.md` e `PLANNING.md`
O arquivo `TASKS.md` constitui o artefato da fase de **Decomposição de Tarefas (*Task Decomposition*)** da metodologia SDD (*Specification-Driven Development*), atuando como elo rastreável entre:
- **`SPECIFICATION.md`:** que define **o que** o aplicativo faz na Versão 1.0 Demonstrativa (`Início` e `Dicionário` com 20 registros demonstrativos fictícios `DEMO-001` a `DEMO-020`), seus requisitos (`RF-001`–`RF-011`, `RNF-001`–`RNF-009`, `RN-01`–`RN-09`), limitações e funcionalidades futuras; e
- **`PLANNING.md`:** que organiza **como e em que ordem** as fases, decisões técnicas, validações, riscos e prioridades de regularização e evolução são estruturados.

Este documento registra unidades de trabalho pequenas, independentes e verificáveis por critério objetivo, separando estritamente:
1. **Regularização documental (`DOC-*`)**;
2. **Validação técnica e artefatos (`VAL-*`)**;
3. **Saneamento técnico de código preparatório (`TEC-*`)**; e
4. **Evolução funcional futura pós-v1.0 (`FUT-*`)**.

---

## 2. Convenção de Identificação e Status

### Prefixos por Categoria
| Prefixo | Categoria | Escopo |
|---|---|---|
| **`DOC-XXX`** | Tarefas Documentais | Criação, revisão e alinhamento dos documentos SDD e contratos técnicos. |
| **`VAL-XXX`** | Validação e Testes | Consolidação de execuções de testes, compilação de suítes, verificação de APK e registros de limitações/pendências. |
| **`TEC-XXX`** | Saneamento Técnico | Correções técnicas pontuais em estruturas preparatórias existentes sem alterar o escopo funcional da v1.0. |
| **`FUT-XXX`** | Funcionalidades Futuras | Atividades pós-v1.0 dependentes de curadoria linguística/cultural, autorização formal, servidor oficial ou novas telas. |

### Estados Adotados
| Status | Definição Objetiva |
|---|---|
| **`CONCLUÍDA`** | Execução e entrega comprovadas por evidência direta no repositório ou relatório técnico verificado. |
| **`PENDENTE`** | Tarefa identificada e ainda não executada. |
| **`PARCIAL`** | Tarefa iniciada ou parcialmente validada, aguardando etapa complementar ou revisão final para conclusão. |
| **`BLOQUEADA`** | Execução impedida por condição externa ou pré-requisito obrigatório ainda não atendido. |
| **`NÃO APLICÁVEL`** | Tarefa cancelada (`CANCELADA`) ou fora do escopo (`FORA DO ESCOPO`) do ciclo atual. *(Nenhuma das 26 tarefas inventariadas recebeu essa classificação até o momento.)* |

---

## 3. Tarefas Documentais

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`DOC-001`** | Criação e revisão pontual do documento `SPECIFICATION.md` na raiz do repositório. | **`CONCLUÍDA`** | Nenhuma (inspeção técnica prévia do código e auditoria geral). | `SPECIFICATION.md` presente na raiz com as 16 seções obrigatórias, `RF-001`–`RF-011`, `RNF-001`–`RNF-009`, `RN-01`–`RN-09`, separação explícita entre requisitos implementados, parciais e não implementados e consistência com o código verificada. | `/SPECIFICATION.md`; relatórios das Etapas 1.1, 1.1.1 e revisão documental final. |
| **`DOC-002`** | Criação e revisão corretiva pontual do documento `PLANNING.md` na raiz do repositório. | **`CONCLUÍDA`** | `DOC-001`. | `PLANNING.md` presente na raiz com as 12 seções obrigatórias, 11 fases, 10 marcos (`M1`–`M10`), distinção entre código estrutural e experiência v1.0 e revisão alinhada aos artefatos atuais. | `/PLANNING.md`; relatório da Etapa 1.2, revisão corretiva e revisão documental final. |
| **`DOC-003`** | Criação do documento `TASKS.md` na raiz do repositório. | **`CONCLUÍDA`** | `DOC-001`, `DOC-002`. | `TASKS.md` presente na raiz com as 9 seções obrigatórias, quantitativos consistentes, coerência integral com `SPECIFICATION.md` e `PLANNING.md` e revisão humana concluída. | `/TASKS.md` (criado, revisado na Seção 8.2, atualizado na Etapa 1.7.2 e na revisão documental final). |
| **`DOC-004`** | Criação e atualização do documento `TEST_RESULTS.md` na raiz do repositório. | **`CONCLUÍDA`** | `DOC-001`, `DOC-002`, `DOC-003`, `VAL-001`, `VAL-002`, `VAL-005` (e referência às limitações de `VAL-003`, `VAL-004` e `VAL-006`). | `TEST_RESULTS.md` atualizado consolidando de forma auditável as **27 suítes JVM (252 testes aprovados)** após integração do manifesto e ampliações unitárias (preservando os marcos de 233/24 e 238/25), a recompilação de `androidTest`, o APK único `Voz-Ancestral-debug.apk` e o roteiro de validação externa em dispositivo real. | `/TEST_RESULTS.md` (atualizado após conclusão de `TEC-001`–`TEC-004` e `FUT-003` no cliente). |
| **`DOC-005`** | Criação e atualização do documento `AI_USAGE.md` na raiz do repositório. | **`CONCLUÍDA`** | `DOC-001`, `DOC-002`, `DOC-003`. | `AI_USAGE.md` atualizado na raiz registrando transparentemente o uso e os limites de ferramentas de IA no ciclo acadêmico e nas rodadas de saneamento/testes da v1.0, sem informações não comprovadas. | `/AI_USAGE.md` (atualizado com o fechamento técnico v1.0). |
| **`DOC-006`** | Criação e atualização do documento `README.md` na raiz do repositório. | **`CONCLUÍDA`** | `DOC-001`, `DOC-002`, `DOC-003`, `DOC-004`, `DOC-005`. | `README.md` atualizado na raiz apresentando identificação oficial (**Voz Ancestral** / **Acervo Provisório** v1.0 Demonstrativa), escopo real (`Início` e `Dicionário`), instruções verificadas de build/testes, `gradlew` executável, APK único `Voz-Ancestral-debug.apk`, roteiro de validação externa e referências aos documentos SDD. | `/README.md` (atualizado com o estado final da entrega). |
| **`DOC-007`** | Revisão de `docs/api-v1-contract.md` para refletir o comportamento real do cliente v1.0. | **`CONCLUÍDA`** | `DOC-001`, `DOC-002`. | `docs/api-v1-contract.md` atualizado nas Seções 1, 1.1, 4.1, 4.2, 6.1–6.3 e 7 refletindo a integração real de `GET /v1/manifest.json` e `GET /v1/dictionary.json` no cliente, mantendo `/v1/articles.json` fora da v1.0 e o servidor oficial sem confirmação operacional. | `docs/api-v1-contract.md`; `RemoteArchiveDataSource.kt`; `ArchiveSyncRepositoryImpl.kt`. |

---

## 4. Tarefas de Validação

> **Distinção Obrigatória:** Execução automatizada na JVM, compilação de suítes instrumentadas (`androidTest`) e validação manual/presencial em aparelho físico são tratadas em tarefas separadas para impedir que compilação ou registro documental seja confundido com execução em dispositivo real.

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`VAL-001`** | Consolidação técnica das suítes JVM (`gradle :app:testDebugUnitTest`): evolução de 233/24 para 238/25 (`TEC-001`) e consolidação final em **252 testes aprovados em 27 suítes**. | **`CONCLUÍDA`** | Código e suítes em `app/src/test` estáveis. | Execução comprovada de `:app:testDebugUnitTest` com **252/252** testes aprovados em **27 suítes** (com `JAVA_TOOL_OPTIONS="-Duser.home=/home/appuser"` para cache offline do Robolectric), cobrindo também `ArchiveMappersTest`, `ArchiveUseCasesAndClassifierTest` e a integração de `/v1/manifest.json`. | 27 arquivos em `app/src/test/java/com/example/`; 27 XMLs em `app/build/test-results/testDebugUnitTest/`; `TEST_RESULTS.md`. |
| **`VAL-002`** | Recompilação bem-sucedida da suíte instrumentada `androidTest` (`:app:assembleDebugAndroidTest`), diferenciando compilação de execução em dispositivo. | **`CONCLUÍDA`** | Código-fonte e `app/src/androidTest` atualizados. | Compilação via `:app:assembleDebugAndroidTest` concluída com geração de `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` (`1.056.983` bytes) após atualização de `StartupSeedHiltInstrumentedTest.kt` para o fluxo de manifesto e `core.common.ApplicationScope`. | 5 arquivos em `app/src/androidTest/java/com/example/`; `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`; `TEST_RESULTS.md`. |
| **`VAL-003`** | Execução futura da suíte instrumentada `:app:connectedDebugAndroidTest` em dispositivo Android físico ou emulador compatível. | **`BLOQUEADA`** | `VAL-002`; disponibilidade externa de dispositivo Android ou emulador conectado via ADB. | Execução integral de `StartupSeedHiltInstrumentedTest` e `ExampleInstrumentedTest` via `:app:connectedDebugAndroidTest` concluída com aprovação em dispositivo ou emulador externo e resultado registrado em `TEST_RESULTS.md`. | `app/src/androidTest/java/com/example/StartupSeedHiltInstrumentedTest.kt`; ambiente atual sem emulador/ADB. |
| **`VAL-004`** | Execução de validação manual, auditoria de acessibilidade com TalkBack e *benchmarks* de desempenho em aparelho físico, seguindo roteiro externo documentado. | **`PENDENTE`** | Disponibilidade de aparelho Android físico e APK Debug v1.0 (`VAL-005`). | Roteiro de testes manuais (instalação, fluxo *Offline-First*, modo avião, conectividade, rotação, TalkBack e verificação de logs/crash) executado em dispositivo físico e documentado em `TEST_RESULTS.md`. | `TEST_RESULTS.md` (Seção 12 / checklist externo); `README.md`. |
| **`VAL-005`** | Geração e verificação de integridade do artefato único APK Debug v1.0 (`Voz-Ancestral-debug.apk` na raiz). | **`CONCLUÍDA`** | Build `:app:assembleDebug` e cópia verificada do artefato. | APK Debug final único localizado na raiz (`Voz-Ancestral-debug.apk`, `22.692.801` bytes, SHA-256 `e633f1c268fada603fb5c9a61d2010887a78fd826c0c9cdf6273708ecf25e36d`, idêntico a `app/build/outputs/apk/debug/app-debug.apk`), com remoção de `.build-outputs/` antigos para evitar duplicidade. | `Voz-Ancestral-debug.apk`; `app/build/outputs/apk/debug/app-debug.apk`; `TEST_RESULTS.md` (Seção 11). |
| **`VAL-006`** | Configuração segura de assinatura Release e geração de APK/AAB Release assinado para eventual distribuição. | **`BLOQUEADA`** | Fornecimento externo de keystore Release (`my-upload-key.jks`) e variáveis `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD`. | Execução bem-sucedida de `:app:assembleRelease` (ou geração de AAB) assinada com keystore Release dedicada fora do código-fonte e verificação do artefato resultante. *(Configuração `signingConfigs` em `app/build.gradle.kts` preservada.)* | `app/build.gradle.kts` (linhas 29–58); `PLANNING.md` (Seções 4 e 9). |

---

## 5. Tarefas de Saneamento Técnico

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`TEC-001`** | Correção do tratamento de `CancellationException` no operador `.catch` de `EncyclopediaViewModel.kt`. | **`CONCLUÍDA`** | Pré-requisito cumprido antes de eventual ativação futura de `Encyclopedia` (`FUT-005`). | Bloco `.catch` em `EncyclopediaViewModel.kt` atualizado com `import kotlinx.coroutines.CancellationException` para relançar cancelamentos upstream antes de emitir `EncyclopediaArticlesState.Error`, validado por `EncyclopediaViewModelTest.kt` (5 testes). | `EncyclopediaViewModel.kt`; `EncyclopediaViewModelTest.kt`; `TEST_RESULTS.md`. |
| **`TEC-002`** | Desacoplamento da camada `data` em relação ao pacote `di` movendo `@ApplicationScope` para `core.common`. | **`CONCLUÍDA`** | Nenhuma. | Criado `CoroutinesQualifiers.kt` em `com.example.core.common` com `@ApplicationScope`; atualizados `CoroutinesModule`, `ConnectivityModule`, `AndroidNetworkConnectivityObserver`, `ArchiveSyncRepositoryImpl`, `ArchiveSyncCoordinatorImpl` e `StartupSeedHiltInstrumentedTest`, eliminando todas as referências de `data/` para `di/`. | `CoroutinesQualifiers.kt`; verificação via `grep` (`0` referências `data -> di`). |
| **`TEC-003`** | Higiene de empacotamento e controle de versão: permissão `+x` no `gradlew`, atualização de `.gitignore` e remoção de APKs duplicados antigos. | **`CONCLUÍDA`** | Nenhuma. | Executado `chmod +x gradlew` (`-rwxr-xr-x`), adicionados `*.apk`, `*.aab` e `/.build-outputs/` ao `.gitignore` e removido `.build-outputs/`, consolidando a entrega final em `Voz-Ancestral-debug.apk`. | `gradlew`; `.gitignore`; `Voz-Ancestral-debug.apk`. |
| **`TEC-004`** | Exibição de contagem de verbetes sincronizados na UI quando `articlesCount` é `null` na v1.0. | **`CONCLUÍDA`** | Nenhuma. | Adicionado recurso `home_sync_synced_entries_only` (`"Sincronizados: %1$d verbetes."`) em `strings.xml` e tratado o ramo `entriesCount != null && articlesCount == null` em `OfflineArchiveStatusCard` (`PreserveComponents.kt`), validado em `OfflineArchiveStatusCardTest`. | `strings.xml`; `PreserveComponents.kt`; `OfflineArchiveStatusCardTest.kt`. |

> **Esclarecimento Obrigatório sobre `TEC-001`–`TEC-004`:**
> - Todas as quatro tarefas de saneamento aprimoram a conformidade arquitetural, a qualidade de UI, a higiene do repositório e a robustez sem alterar o escopo funcional da Versão 1.0 Demonstrativa (**Início** e **Dicionário**).
> - O módulo `Encyclopedia` (`TEC-001`) permanece fora da barra de navegação e da sincronização na v1.0 (`FUT-005`).

---

## 6. Funcionalidades Futuras (Pós-v1.0)

> **Aviso de Escopo:** Exceto a parte estritamente de cliente de `FUT-003` (consulta e persistência de `/v1/manifest.json`), as demais entregas abaixo não compõem conteúdo ou novas telas na v1.0 Demonstrativa e dependem de infraestrutura externa ou curadoria formal.

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`FUT-001`** | Obtenção e validação de conteúdo linguístico autêntico da língua Parintintin, com autorização comunitária e institucional apropriada, para futura substituição dos dados demonstrativos `DEMO-001`..`DEMO-020`. | **`BLOQUEADA`** | Curadoria linguística especializada e autorização formal externa para publicação digital. | Conjunto validado de verbetes autênticos (com transcrição IPA, classes, categorias e exemplos revisados) formalmente autorizado e pronto para consumo sem infringir direitos culturais ou linguísticos. | `SPECIFICATION.md` (Seções 7, 8 e 15); `strings.xml` (`home_intro_body`). |
| **`FUT-002`** | Disponibilização operacional e homologação do backend oficial HTTPS compatível com `docs/api-v1-contract.md`. | **`BLOQUEADA`** | Definição das decisões contratuais pendentes (Seção 7 de `docs/api-v1-contract.md`), provisão de servidor externo e homologação dos endpoints. | Servidor HTTPS respondendo em ambiente operacional aos endpoints `/v1/manifest.json`, `/v1/dictionary.json` e `/v1/articles.json` conforme contrato revisado e aprovado em testes de integração reais. | `NetworkModule.kt` (`https://archive.parintintin.org/`); `docs/api-v1-contract.md`; `PLANNING.md` (Seção 9). |
| **`FUT-003`** | Integração do consumo do manifesto remoto (`/v1/manifest.json`) no fluxo de sincronização do cliente Android. | **`CONCLUÍDA`** *(no cliente)* | Homologação externa de produção continua vinculada a `FUT-002`. | `RemoteArchiveDataSource.fetchArchiveManifest()` valida `version` não vazia e contagens `>= 0`; `ArchiveSyncRepositoryImpl.syncArchive()` consulta o manifesto antes de `/v1/dictionary.json` e persiste `lastSuccessfulRemoteVersion = manifest.version` no Room, validado com testes unitários e `MockWebServer`. *(Otimização futura de cache/skip por versão igual permanece registrada em `docs/api-v1-contract.md`, Seção 7.)* | `RemoteArchiveDataSource.kt`; `ArchiveSyncRepositoryImpl.kt`; `ArchiveSyncRepositoryImplTest.kt`; `DictionaryOfflineIntegrationTest.kt`. |
| **`FUT-004`** | Definição contratual e implementação da política de exclusão/reconciliação de registros removidos no servidor. | **`BLOQUEADA`** | `FUT-002` e decisão contratual entre *snapshot* completo e *tombstones* (incluindo tratamento de itens favoritados). | Política documentada em `docs/api-v1-contract.md` e implementada nos repositórios Room com transações seguras e testes cobrindo remoção sem perda indevida de dados em falhas ou `HTTP 200 []`. | `docs/api-v1-contract.md` (Seções 6.5 e 7.1); `OfflineFirstDictionaryRepository.kt`. |
| **`FUT-005`** | Integração futura da sincronização e exposição da Enciclopédia Cultural na experiência do usuário. | **`BLOQUEADA`** | `TEC-001` *(concluída)*, `FUT-002`, `FUT-009` e decisão sobre escopo transacional entre repositórios (`DOC-007`). | `encyclopediaRepository.syncFromRemote()` integrado ao fluxo de sincronização com tratamento explícito de falhas parciais, destino `AppDestination.Encyclopedia` habilitado conforme especificação futura e testes de UI/integração aprovados. | `EncyclopediaScreen.kt`, `EncyclopediaViewModel.kt`, `OfflineFirstEncyclopediaRepository.kt`, `ArchiveSyncRepositoryImpl.kt`. |
| **`FUT-006`** | Implementação funcional da interface de Favoritos na listagem de verbetes e em rota dedicada. | **`PENDENTE`** | Decisão de design/UX pós-v1.0 para reabilitação da rota `AppDestination.Favorites` e ação nos cards. | Controle acessível de favoritar/desfavoritar integrado aos cards de `DictionaryScreen` usando `ToggleFavoriteEntryUseCase`, tela real substituindo `ProvisionalDestinationScreen` em `Favorites` e testes Compose aprovados. | `DictionaryDao.kt` (`toggleFavorite`), `ArchiveUseCases.kt`, `ParintintinNavHost.kt` (linhas 76–84). |
| **`FUT-007`** | Implementação da tela e rota de detalhes completos dos verbetes do dicionário. | **`PENDENTE`** | Definição de layout pós-v1.0; exibição de conteúdo real depende complementarmente de `FUT-001`. | Tela Compose e rota tipada integradas ao `ParintintinNavHost` consumindo `ObserveDictionaryEntryByIdUseCase` para exibir fonética IPA, exemplos bilíngues e notas completas, com testes de navegação e UI aprovados. | `ArchiveUseCases.kt` (`ObserveDictionaryEntryByIdUseCase`), `DictionaryEntry.kt`, `ParintintinNavHost.kt`. |
| **`FUT-008`** | Implementação da reprodução de áudio de pronúncia nos verbetes. | **`BLOQUEADA`** | `FUT-001` (gravações autênticas autorizadas), definição de formato/hospedagem/cache offline de `audio_url` e `FUT-007` (ou integração equivalente em card). | Contrato de mídia definido, player integrado ao aplicativo (com tratamento de estados de carregamento/erro/offline e acessibilidade) e testes cobrindo reprodução sem URL nula ou falhas silenciosas. | `DictionaryEntry.kt` (`audioUrl: String?`), `HomeScreen.kt` (card `"Planejado"`), `docs/api-v1-contract.md` (Seção 7.5). |
| **`FUT-009`** | Integração de conteúdo cultural autêntico e autorizado para a Enciclopédia Cultural. | **`BLOQUEADA`** | Curadoria cultural especializada, autorização formal de publicação e `FUT-002` (ou seed cultural oficial validado). | Conjunto de artigos culturais revisados e autorizados disponível via API (`/v1/articles.json`) ou carga controlada, preservando referências de fonte (`source_reference`) e integridade no Room. | `CulturalArticle.kt`, `CulturalArticleDto`, `docs/api-v1-contract.md` (Seção 2.3). |

---

## 7. Dependências e Ordem Recomendada

### 7.1 Dependências Obrigatórias (Bloqueantes)
1. **Cadeia documental base:** `DOC-001` (`SPECIFICATION.md`) → `DOC-002` (`PLANNING.md`) → `DOC-003` (`TASKS.md`).
2. **Consolidação formal de testes:** conclusão de `DOC-003`, `VAL-001`, `VAL-002` e `VAL-005` (além do registro de situação de `VAL-003`, `VAL-004` e `VAL-006`) → `DOC-004` (`TEST_RESULTS.md`).
3. **Execução instrumentada em runtime real:** disponibilidade externa de dispositivo Android ou emulador com ADB + `VAL-002` → `VAL-003` (`:app:connectedDebugAndroidTest`).
4. **Empacotamento Release assinado:** fornecimento externo de keystore dedicada e variáveis seguras → `VAL-006`.
5. **Ativação segura da Enciclopédia:** `TEC-001` *(concluída)* + `FUT-002` + `FUT-009` → `FUT-005`.
6. **Manifesto e exclusão remota:** `FUT-002` → `FUT-003` e `FUT-004`.
7. **Áudio autêntico:** `FUT-001` + definição contratual de mídia → `FUT-008`.

### 7.2 Ordem Recomendada de Execução (Sem Prazos Inventados)
1. **Cadeia documental base concluída (`DOC-001`–`DOC-007`):** `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md`, `AI_USAGE.md`, `README.md` e `docs/api-v1-contract.md` alinhados ao estado final do código.
2. **Saneamento técnico e integração de manifesto no cliente concluídos (`TEC-001`–`TEC-004` e `FUT-003`):**
   - `TEC-001`: proteção contra `CancellationException` em `EncyclopediaViewModel.kt`.
   - `TEC-002`: `@ApplicationScope` movido para `core.common` eliminando dependência `data -> di`.
   - `TEC-003`: `.gitignore` atualizado, `gradlew` com permissão `+x` e entrega única em `Voz-Ancestral-debug.apk`.
   - `TEC-004`: exibição de contagem de verbetes sincronizados na v1.0 (`home_sync_synced_entries_only`).
   - `FUT-003` *(cliente)*: integração e validação de `GET /v1/manifest.json` em `RemoteArchiveDataSource` e `ArchiveSyncRepositoryImpl`.
3. **Validação externa da v1.0 (quando houver ambiente físico/emulador e credenciais):** executar `VAL-003` (`:app:connectedDebugAndroidTest`), `VAL-004` (validação manual segundo checklist externo em `TEST_RESULTS.md`) e, se aplicável à distribuição final, `VAL-006` (assinatura Release).
4. **Ciclo pós-v1.0 (somente após autorizações e infraestrutura):** desbloquear `FUT-001`, `FUT-009` e `FUT-002`, seguidos por `FUT-004`, `FUT-005`, `FUT-006`, `FUT-007` e `FUT-008`.

---

## 8. Resumo de Situação

Os totais abaixo foram calculados exclusivamente a partir das **26 tarefas** efetivamente registradas nas Seções 3, 4, 5 e 6 deste documento:

### 8.1 Quantitativo por Categoria e Status
| Categoria | Prefixo | `CONCLUÍDA` | `PARCIAL` | `PENDENTE` | `BLOQUEADA` | `NÃO APLICÁVEL` | Total |
|---|---|---|---|---|---|---|---|
| **Documentação** | `DOC` | 7 | 0 | 0 | 0 | 0 | **7** |
| **Validação e Testes** | `VAL` | 3 | 0 | 1 | 2 | 0 | **6** |
| **Saneamento Técnico** | `TEC` | 4 | 0 | 0 | 0 | 0 | **4** |
| **Funcionalidades Futuras (Pós-v1.0)** | `FUT` | 1 | 0 | 2 | 6 | 0 | **9** |
| **Total Geral** | — | **15** | **0** | **3** | **8** | **0** | **26** |

### 8.2 Síntese Objetiva da Versão 1.0 vs. Pós-v1.0
- **Regularização documental, validação e saneamento técnico (`DOC` + `VAL` + `TEC` = 17 tarefas):** `14` concluídas (`DOC-001`–`DOC-007`, `VAL-001`, `VAL-002`, `VAL-005`, `TEC-001`–`TEC-004`), `0` parciais, `1` pendente (`VAL-004`) e `2` bloqueadas por recursos externos ao ambiente (`VAL-003` e `VAL-006`).
- **Escopo Pós-v1.0 (`FUT` = 9 tarefas):** `1` concluída no cliente (`FUT-003`, aguardando apenas servidor real em `FUT-002`), `2` pendentes (`FUT-006`, `FUT-007`) e `6` bloqueadas por conteúdo/servidor/contrato (`FUT-001`, `FUT-002`, `FUT-004`, `FUT-005`, `FUT-008`, `FUT-009`).

---

## 9. Histórico e Limitações

- **Versão do documento:** `1.1` (Alinhado ao fechamento técnico da Versão 1.0 Demonstrativa com `252` testes em `27` suítes JVM e `Voz-Ancestral-debug.apk` único na raiz).
- **Natureza da reconstrução:** Este documento reflete exclusivamente evidências verificáveis no código-fonte, suítes de testes, relatórios XML do Gradle, APK Debug verificado e documentação do repositório.
- **Limitações registradas:**
  - Não foram inventados históricos prévios, prazos acadêmicos ou aprovações formais inexistentes.
  - Operações de publicação em dispositivos físicos (`VAL-003`, `VAL-004`), homologação de servidor externo (`FUT-002`) e assinatura Release (`VAL-006`) dependem de ambiente externo ao container.
