# TEST_RESULTS.md — Projeto Acadêmico Voz Ancestral (Aplicativo Acervo Provisório)

> **Nota de Natureza Documental:** Este documento registra **retrospectivamente** os resultados de testes, compilações e verificações de artefatos efetivamente observados na **Versão 1.0 Demonstrativa** do projeto acadêmico **Voz Ancestral** (aplicativo **Acervo Provisório**). Nenhum teste inexistente, dispositivo físico não utilizado, métrica de desempenho ou percentual de cobertura sem relatório real foi inventado. Compilação, execução na JVM e validação presencial em hardware são tratadas com classificações distintas e não intercambiáveis.

---

## 1. Identificação do Projeto e Versão Avaliada

| Atributo | Informação Verificada | Fonte no Repositório |
|---|---|---|
| **Projeto Acadêmico** | **Voz Ancestral** | `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md` |
| **Aplicativo** | **Acervo Provisório** (`AcervoProvisorio`) | `app/src/main/res/values/strings.xml`, `metadata.json`, `settings.gradle.kts` |
| **Versão Avaliada** | **1.0 Demonstrativa** (`versionCode = 1`, `versionName = "1.0"`) | `app/build.gradle.kts`, `app/build/outputs/apk/debug/output-metadata.json` |
| **Identificador Android (`applicationId`)** | `com.aistudio.parintintin.kagwah` | `app/build.gradle.kts` |
| **Namespace Kotlin/Android** | `com.example` | `app/build.gradle.kts` |
| **Compatibilidade SDK** | `minSdk = 24` \| `targetSdk = 36` \| `compileSdk = 36` | `app/build.gradle.kts` |
| **Escopo Funcional Visível** | Telas **Início** e **Dicionário** (20 registros demonstrativos fictícios `DEMO-001` a `DEMO-020` em português) | `SPECIFICATION.md`, `AppDestination.kt`, `app/src/main/assets/dictionary_seed.json` |

---

## 2. Objetivo do Documento

Consolidar, de forma transparente e auditável:
1. Os testes unitários, de integração, contratuais HTTP, Robolectric e Compose efetivamente **executados na JVM** (`gradle :app:testDebugUnitTest`);
2. O estado real das suítes **instrumentadas Android** (`app/src/androidTest`), explicitando que foram apenas compiladas e **não executadas**;
3. A verificação técnica dos **artefatos de build** gerados (APK Debug v1.0 e impedimento de assinatura Release); e
4. As **limitações, cenários não validados e riscos conhecidos** da Versão 1.0 Demonstrativa, mantendo coerência integral com `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md` e `docs/api-v1-contract.md`.

---

## 3. Escopo da Validação

### Classificações Utilizadas Neste Relatório
| Classificação | Significado Estrito |
|---|---|
| **`APROVADO`** | Teste efetivamente executado na JVM com resultado satisfatório (`0` falhas, `0` erros e `0` ignorados). |
| **`COMPILADO, NÃO EXECUTADO`** | Código de teste compilado com sucesso em APK de testes, porém sem execução em dispositivo físico ou emulador. |
| **`NÃO VALIDADO`** | Cenário para o qual não existe evidência de execução ou relatório comprobatório neste ambiente. |
| **`PENDENTE`** | Atividade de validação, execução externa ou assinatura ainda necessária para fechamento posterior. |

### Limite Demonstrativo do Conteúdo Avaliado
- Os **20 registros** presentes em `app/src/main/assets/dictionary_seed.json` (`DEMO-001` a `DEMO-020`) são **termos e expressões demonstrativos fictícios em português**, sem transcrição IPA, exemplos bilíngues ou arquivos de áudio, e **não representam vocabulário autêntico da língua Parintintin**.
- Os resultados positivos registrados neste documento validam o comportamento estrutural, lógico e visual do aplicativo demonstrativo em ambiente JVM/simulado; **não constituem validação de conteúdo linguístico autêntico** nem comprovação de operação em ambiente de produção.

---

## 4. Ambiente e Ferramentas de Teste Comprovados

As informações abaixo correspondem exclusivamente às configurações e bibliotecas declaradas no repositório (`gradle/libs.versions.toml`, `app/build.gradle.kts`, `gradle.properties`) e aos registros históricos da execução que gerou relatórios XML em `app/build/test-results/testDebugUnitTest/`:

| Componente / Ferramenta | Configuração / Versão Comprovada |
|---|---|
| **Linguagem / Target JVM** | Kotlin `2.2.10`, `JavaVersion.VERSION_11` (`app/build.gradle.kts`) |
| **Android Gradle Plugin / KSP** | AGP `9.1.1`, KSP `2.3.5` (`gradle/libs.versions.toml`) |
| **Configuração Gradle** | `-Xmx4g`, `org.gradle.parallel=true`, `org.gradle.caching=true`, `org.gradle.configuration-cache=true`, `kotlin.compiler.execution.strategy=in-process` (`gradle.properties`) |
| **Opção de Testes Unitários** | `unitTests { isIncludeAndroidResources = true }` (`app/build.gradle.kts`) |
| **Framework Unitário e Coroutines** | JUnit `4.13.2`, `kotlinx-coroutines-test` `1.10.2` |
| **Framework de Simulação Android (JVM)** | Robolectric `4.16.1` (`@Config(sdk = [36])` nas suítes Robolectric/Compose) |
| **Testes de UI Declarativa** | `androidx.compose.ui:ui-test-junit4` e `ui-test-manifest` (Compose BOM `2024.09.00`) |
| **Testes de Rede Controlados** | `com.squareup.okhttp3:mockwebserver` `4.10.0` |
| **Testes Instrumentados (Compilação)** | `HiltTestRunner` (`androidx.test.runner.AndroidJUnitRunner`), `hilt-android-testing` `2.59`, `androidx.test.ext:junit` `1.3.0`, `espresso-core` `3.7.0`, `androidx.test:runner` `1.6.2` |
| **Ambiente de Execução** | Ambiente headless em container Linux **sem emulador Android e sem acesso ADB** (impossibilitando execução de `connectedDebugAndroidTest` e testes presenciais em aparelho físico). |

*Nota sobre cobertura:* Não há plugin ou relatório JaCoCo/cobertura percentual configurado ou gerado no projeto; portanto, **nenhum percentual de cobertura de código é declarado**.

*Nota sobre resolução de artefato do Robolectric neste container:* ao executar `gradle :app:testDebugUnitTest` como usuário `root` (`HOME=/root`) sem variável auxiliar, 14 suítes Robolectric falharam com `java.lang.AssertionError: Failed to fetch maven artifact org.robolectric:android-all-instrumented:16-robolectric-13921718-i7` porque o cache local pré-baixado reside em `/home/appuser/.m2/repository/` e o acesso externo ao Maven Central é bloqueado neste container. Ao definir `JAVA_TOOL_OPTIONS="-Duser.home=/home/appuser"`, o trabalhador JVM resolveu o cache offline normalmente e todas as 25 suítes foram aprovadas.

*Nota sobre persistência de relatórios de build:* Nas execuções verificadas, os relatórios XML são gerados em `app/build/test-results/testDebugUnitTest/` e os relatórios HTML do Gradle em `app/build/reports/tests/testDebugUnitTest/`. Ambos os diretórios integram a estrutura de saída do Gradle dentro de `app/build/`, diretório ignorado pelo Git (`app/.gitignore`).

---

## 5. Estratégia de Testes

A verificação técnica da Versão 1.0 Demonstrativa apoia-se em duas frentes principais:
1. **Execução automatizada na JVM (`gradle :app:testDebugUnitTest`):** abrange regras puras de domínio, parser/validação/SHA-256 do seed, migrações e persistência SQLite/Room (via Robolectric), contratos HTTP/Moshi (via `MockWebServer`), observação de conectividade, coordenação de sincronização, ViewModels e renderização/acessibilidade/responsividade Compose.
2. **Preparação e compilação de testes instrumentados (`:app:assembleDebugAndroidTest`):** valida a integridade de compilação da suíte com Hilt real (`TestDatabaseModule`, `TestNetworkModule`, `StartupSeedHiltInstrumentedTest`, `ExampleInstrumentedTest`), ficando a execução efetiva condicionada a um ambiente externo com dispositivo Android ou emulador.

---

## 6. Testes Automatizados Executados na JVM

- **Comando executado (reexecução verificada em 22/11/2026 com cache Robolectric em `/home/appuser/.m2`):** `JAVA_TOOL_OPTIONS="-Duser.home=/home/appuser" gradle :app:testDebugUnitTest` *(em ambientes onde o `HOME` do usuário atual já contém o cache Maven/Gradle ou possui acesso normal a repositórios, `gradle :app:testDebugUnitTest` ou `sh gradlew :app:testDebugUnitTest` executa diretamente sem `JAVA_TOOL_OPTIONS`)*
- **Execução prévia no mesmo ambiente sem `JAVA_TOOL_OPTIONS` (registrada por transparência):** `gradle :app:testDebugUnitTest` sob `HOME=/root` falhou em 14 suítes Robolectric (`137` testes concluídos, `14` falhas de `MavenArtifactFetcher` para `org.robolectric:android-all-instrumented:16-robolectric-13921718-i7`) por diferença de diretório `user.home`.
- **Status geral da execução válida:** `BUILD SUCCESSFUL` — **`APROVADO`**
- **Totais consolidados (confirmados pelos 25 arquivos XML em `app/build/test-results/testDebugUnitTest/`):**
  - **25 suítes de testes**
  - **238 testes aprovados**
  - **0 falhas (`failures="0"`)**
  - **0 erros (`errors="0"`)**
  - **0 testes ignorados (`skipped="0"`)**

> **Registro histórico anterior e natureza transitória dos relatórios:** Antes da conclusão de `TEC-001`, a execução histórica inicial da Versão 1.0 Demonstrativa registrou **233 testes aprovados em 24 suítes** via `gradle :app:testDebugUnitTest` (`VAL-001`). Após a correção em `EncyclopediaViewModel.kt` e a criação de `EncyclopediaViewModelTest.kt` (+5 testes), a suíte completa passou a contabilizar **238 testes aprovados em 25 suítes** (`0` falhas, `0` erros, `0` ignorados), resultado novamente confirmado integralmente pelos 25 relatórios XML gerados em `app/build/test-results/testDebugUnitTest/`. Em todas as execuções, o Gradle produz os relatórios XML em `app/build/test-results/testDebugUnitTest/` e os relatórios HTML em `app/build/reports/tests/testDebugUnitTest/`. Ambos os diretórios pertencem à estrutura de saída do Gradle dentro de `app/build/`, ignorada pelo controle de versão Git conforme `app/.gitignore`. Por constituírem artefatos transitórios de build, tais relatórios não permanecem necessariamente disponíveis após limpezas, rebuilds ou reinicializações do ambiente; sua ausência em inspeções posteriores não invalida os resultados históricos aqui registrados, podendo os relatórios XML e HTML ser regenerados mediante nova execução de `gradle :app:testDebugUnitTest`.

### Detalhamento Real das 25 Suítes Executadas na JVM
| # | Suíte / Classe (`app/src/test`) | Testes | Falhas | Erros | Ignorados | Status | Escopo Verificado |
|---|---|---|---|---|---|---|---|
| 1 | `ExampleUnitTest` | 1 | 0 | 0 | 0 | `APROVADO` | Teste unitário básico de sanidade na JVM. |
| 2 | `ExampleRobolectricTest` | 1 | 0 | 0 | 0 | `APROVADO` | Sanidade de inicialização com Robolectric (`sdk = 36`). |
| 3 | `AndroidConnectivityPlatformRobolectricTest` | 1 | 0 | 0 | 0 | `APROVADO` | Integração com `ConnectivityManager` via `ConnectivityModule` e registro/remoção de `DefaultNetworkCallback`. |
| 4 | `AndroidNetworkConnectivityObserverTest` | 9 | 0 | 0 | 0 | `APROVADO` | Estados `Offline`, `ConnectedUnvalidated`, `Online`, callbacks e liberação via `AutoCloseable`/`invokeOnCompletion`. |
| 5 | `ParintintinDatabaseMigrationTest` | 6 | 0 | 0 | 0 | `APROVADO` | Migrações Room `1 -> 3` e `2 -> 3`, criação v3 e preservação de favoritos/metadados. |
| 6 | `DictionarySeedChangeDetectorTest` | 3 | 0 | 0 | 0 | `APROVADO` | Detecção de alteração do arquivo de seed local. |
| 7 | `DictionarySeedImporterTest` | 14 | 0 | 0 | 0 | `APROVADO` | Importação inicial e validação de schema/campos de `dictionary_seed.json`. |
| 8 | `SeedSha256CalculatorTest` | 5 | 0 | 0 | 0 | `APROVADO` | Cálculo hexadecimal SHA-256 sem normalização e rejeição de arquivo vazio (`EmptySeedFileException`). |
| 9 | `SeedVersionComparatorTest` | 9 | 0 | 0 | 0 | `APROVADO` | Regras de versão e hash (`SEED_NOT_APPLIED`, `SEED_UPDATED`, `SEED_UP_TO_DATE`, `INCONSISTENT_VERSION`). |
| 10 | `VersionedDictionarySeedUpdaterTest` | 12 | 0 | 0 | 0 | `APROVADO` | Atualização transacional idempotente do seed, lotes de 500 IDs, preservação de favoritos e bloqueio pós-sync oficial. |
| 11 | `ParintintinApiContractTest` | 17 | 0 | 0 | 0 | `APROVADO` | Adaptadores Moshi gerados via KSP e contrato JSON/HTTP de `/v1/manifest.json`, `/v1/dictionary.json` e `/v1/articles.json`. |
| 12 | `RemoteArchiveDataSourceTest` | 3 | 0 | 0 | 0 | `APROVADO` | Tratamento de exceções HTTP/JSON/IO e propagação de `CancellationException`. |
| 13 | `ArchiveSyncRepositoryImplTest` | 33 | 0 | 0 | 0 | `APROVADO` | Fluxos de `SyncState` (`Loading`, `Syncing`, `Synced`, `RemoteEmpty`, `OfflineFallback`), classificação de falhas e monotonicidade temporal. |
| 14 | `OfflineFirstDictionaryRepositoryTest` | 9 | 0 | 0 | 0 | `APROVADO` | Consulta Room, `toggleFavorite`, `RemoteSyncOutcome.Empty` e preservação de favoritos em `upsertAll`. |
| 15 | `OfflineFirstEncyclopediaRepositoryTest` | 6 | 0 | 0 | 0 | `APROVADO` | Contrato offline-first de repositório da Enciclopédia *(estrutura preparatória não exposta na v1.0)*. |
| 16 | `ArchiveSyncCoordinatorImplTest` | 10 | 0 | 0 | 0 | `APROVADO` | Deduplicação de auto-sync, junção `JoinedInFlight`, isolamento em `@ApplicationScope` e cancelamento seguro. |
| 17 | `DictionaryOfflineIntegrationTest` | 10 | 0 | 0 | 0 | `APROVADO` | Fluxo integrado Room + Seed + `DictionaryViewModel` + `MockWebServer` + `SyncFailureClassifier`. |
| 18 | `OfflineArchiveStatusCardTest` | 12 | 0 | 0 | 0 | `APROVADO` | Renderização de todos os estados de `SyncState` e `SyncFailureType` e acionamento do botão manual. |
| 19 | `ResponsiveComponentsTest` | 9 | 0 | 0 | 0 | `APROVADO` | Adaptação responsiva de componentes (`280.dp`, `320.dp`, `fontScale 1.5f`) sem *overflow*. |
| 20 | `DictionaryScreenTest` | 23 | 0 | 0 | 0 | `APROVADO` | Pesquisa, limpeza, filtros combinados, selos demonstrativos, estados vazio/erro e semântica de acessibilidade. |
| 21 | `DictionaryViewModelTest` | 17 | 0 | 0 | 0 | `APROVADO` | *Debounce* de `150 ms`, combinação de filtros, estados `Loading`/`Empty`/`Success`/`Error` e cancelamento. |
| 22 | `HomeScreenTest` | 7 | 0 | 0 | 0 | `APROVADO` | Banner v1.0, card permanente `"MODO DEMONSTRATIVO"`, layout em `280.dp` e módulo planejado. |
| 23 | `HomeViewModelTest` | 14 | 0 | 0 | 0 | `APROVADO` | Disparo de auto-sync, sincronização manual, estatísticas locais e sobrevivência no `@ApplicationScope`. |
| 24 | `ParintintinNavHostTest` | 2 | 0 | 0 | 0 | `APROVADO` | Navegação restrita a **Início** e **Dicionário** na barra inferior e alternância sem placeholders. |
| 25 | `EncyclopediaViewModelTest` | 5 | 0 | 0 | 0 | `APROVADO` | Falhas comuns com fallback, propagação explícita de `CancellationException` *upstream*, cancelamento estruturado em `flatMapLatest` e recuperação via `onRetry()` *(estrutura preparatória não exposta na v1.0)*. |
| — | **Total Consolidado (`25` suítes)** | **238** | **0** | **0** | **0** | **`APROVADO`** | **100% dos testes JVM executados aprovados.** |

---

## 7. Testes Instrumentados Android (`app/src/androidTest`)

- **Status atual:** **`COMPILADO, NÃO EXECUTADO`**
- **Execução em dispositivo ou emulador (`:app:connectedDebugAndroidTest`):** **`PENDENTE`** (não executada neste ambiente por ausência de emulador Android e de acesso ADB).

### Arquivos da Suíte Instrumentada Identificados no Repositório (5 arquivos Kotlin / 3 métodos `@Test`)
| Arquivo (`app/src/androidTest/java/com/example/`) | Papel / Escopo | Status Atual |
|---|---|---|
| `StartupSeedHiltInstrumentedTest.kt` (`2` `@Test`) | Fluxo ponta a ponta com Hilt real (`MainActivity`), banco Room em memória (`TestDatabaseModule`), `MockWebServer` local (`TestNetworkModule` retornando `HTTP 200 []`), verificação idempotente do seed `DEMO-001`..`DEMO-020` e navegação até `DictionaryScreen`. | **`COMPILADO, NÃO EXECUTADO`** |
| `ExampleInstrumentedTest.kt` (`1` `@Test`) | Verificação de `BuildConfig.APPLICATION_ID` no `targetContext` instrumentado. | **`COMPILADO, NÃO EXECUTADO`** |
| `HiltTestRunner.kt`, `di/TestDatabaseModule.kt`, `di/TestNetworkModule.kt` | Infraestrutura de execução Hilt para `androidTest`. | **`COMPILADO, NÃO EXECUTADO`** |

- Na etapa de validação de regressão (Etapa 29.7), a tarefa `:app:assembleDebugAndroidTest` gerou o APK `app-debug-androidTest.apk`. Ressalta-se que, após execuções posteriores no diretório de build (como a tentativa de empacotamento Release na Etapa 30.0), apenas `app/build/outputs/apk/debug/` permaneceu listado em `app/build/outputs/apk/`, mantendo-se o registro de compilação prévia bem-sucedida e a **ausência total de execução instrumentada**.
- **Estes testes NÃO são apresentados como aprovados nem como executados.**

---

## 8. Testes de Integração e Comunicação HTTP

Os testes de integração e de comunicação de rede executados na JVM utilizaram **`MockWebServer` (`4.10.0`)** e instâncias locais de Room/fakes:

| Suíte | Quantidade de Testes | Status | O que Comprova | O que NÃO Comprova |
|---|---|---|---|---|
| `ParintintinApiContractTest` | 17 | **`APROVADO`** *(ambiente controlado)* | Desserialização Moshi (codegen KSP) de `ArchiveManifestDto`, `DictionaryEntryDto` e `CulturalArticleDto`, tolerância a campos extras, rejeição de campos obrigatórios ausentes/nulos, erros HTTP `404`/`500` e JSON malformado/truncado. | Não comprova que `https://archive.parintintin.org/` esteja ativo ou servindo esses schemas em produção. |
| `RemoteArchiveDataSourceTest` | 3 | **`APROVADO`** *(ambiente controlado)* | Conversão segura de falhas de rede/HTTP/parse em `AppResult.Error` e propagação de `CancellationException`. | Não testa latência, TLS real ou comportamento de servidor externo em produção. |
| `DictionaryOfflineIntegrationTest` | 10 | **`APROVADO`** *(Room + `MockWebServer`)* | Integração entre `VersionedDictionarySeedUpdater`, Room (em memória e arquivo SQLite temporário), `OfflineFirstDictionaryRepository`, `ArchiveSyncRepositoryImpl`, `DictionaryViewModel`, `SyncFailureClassifier` e respostas simuladas (`HTTP 500`, `HTTP 200 []`, `HTTP 200` com payload sintético). | Não comprova sincronização remota real contra o backend oficial nem integração do manifesto `/v1/manifest.json`. |
| `ArchiveSyncRepositoryImplTest` & `ArchiveSyncCoordinatorImplTest` | 33 + 10 = 43 | **`APROVADO`** *(ambiente controlado)* | Transições de `SyncState`, preservação do histórico oficial, deduplicação de auto-sync/manual sync e monotonicidade temporal. | Não valida sincronização de artigos da Enciclopédia (fora da v1.0) nem operação em produção. |
| `AndroidConnectivityPlatformRobolectricTest` & `AndroidNetworkConnectivityObserverTest` | 1 + 9 = 10 | **`APROVADO`** *(Robolectric / fake platform)* | Leitura de `ConnectivityManager`, classificação `Offline` / `ConnectedUnvalidated` / `Online` e descarte de callback via `AutoCloseable`. | Não substitui teste de transição Wi-Fi/dados móveis reais em aparelho físico. |

---

## 9. Validação de Banco de Dados, Persistência e Migrações

| Suíte / Evidência | Quantidade de Testes | Status | Comportamentos Validados |
|---|---|---|---|
| `ParintintinDatabaseMigrationTest` | 6 | **`APROVADO`** | Migração `1 -> 3` (para status `SYNCED`, `OFFLINE_FALLBACK` e `sync_metadata` vazia), migração `2 -> 3` (criação de `seed_metadata` com registro de IDs `DEMO-*` sem incluir IDs não-seed), inicialização v3 e preservação de favoritos/marcadores. |
| `SeedSha256CalculatorTest` | 5 | **`APROVADO`** | Cálculo SHA-256 bruto em minúsculas (64 caracteres) e lançamento de `EmptySeedFileException` em fluxo vazio. |
| `SeedVersionComparatorTest` | 9 | **`APROVADO`** | Decisões por versão e hash (`SEED_NOT_APPLIED`, `SEED_UPDATED`, `SEED_UP_TO_DATE`, `INCONSISTENT_VERSION`). |
| `DictionarySeedChangeDetectorTest` | 3 | **`APROVADO`** | Inspeção combinada de alterações no seed local. |
| `DictionarySeedImporterTest` | 14 | **`APROVADO`** | Validação estrita do JSON (IDs únicos/não vazios, campos obrigatórios, enums e timestamps `>= 0`). |
| `VersionedDictionarySeedUpdaterTest` | 12 | **`APROVADO`** | Idempotência, atualização transacional atômica, preservação de `is_favorite`, processamento em lotes (`500` IDs) e bloqueio quando `lastSuccessfulSyncEpochMillis != null`. |
| `OfflineFirstDictionaryRepositoryTest` & `OfflineFirstEncyclopediaRepositoryTest` | 9 + 6 = 15 | **`APROVADO`** | Consultas SQL reativas (`Flow`), preservação de favoritos/marcadores no `upsertAll` e tratamento de respostas vazias. *(Nota: o módulo de Enciclopédia validado no repositório permanece estrutural e não exposto na v1.0.)* |
| Schemas JSON Room exportados | `2.json`, `3.json` | **`APROVADO`** *(artefato presente)* | Exportação ativa via `ksp { arg("room.schemaLocation", ...) }` e `exportSchema = true`. |

---

## 10. Validação de Funcionalidades da Interface

As verificações de interface Compose ocorreram na JVM com **Robolectric (`sdk = 36`) + `composeTestRule`** (não em renderização de GPU de aparelho físico):

| Suíte | Quantidade de Testes | Status | Funcionalidades Validadas |
|---|---|---|---|
| `HomeScreenTest` | 7 | **`APROVADO`** *(Robolectric + Compose Test)* | Exibição do banner `"VERSÃO 1.0 • DEMONSTRATIVA"`, card permanente `"MODO DEMONSTRATIVO"` sem interação prévia, hierarquia sem *overflow* em `280.dp`, módulos planejados com selo `"Planejado"` e cartão expansível de detalhes. |
| `DictionaryScreenTest` | 23 | **`APROVADO`** *(Robolectric + Compose Test)* | Campo de pesquisa, botão limpar, chips de `SemanticCategory` e `GrammaticalClass`, contagem total/filtrada, `stickyHeader` `"MODO DEMONSTRATIVO"`, selos `"Termo demonstrativo"` e `"Nota demonstrativa"`, estados vazio/erro e semântica de acessibilidade (headings/descrições). |
| `OfflineArchiveStatusCardTest` | 12 | **`APROVADO`** *(Robolectric + Compose Test)* | Estados `Loading`, `Idle`, `Syncing`, `Synced`, `RemoteEmpty` e `OfflineFallback` para `NO_INTERNET`, `UNVALIDATED_NETWORK` e `SERVER_UNAVAILABLE`, além do botão manual de sincronização. |
| `ResponsiveComponentsTest` | 9 | **`APROVADO`** *(Robolectric + Compose Test)* | Adaptação de componentes (`IntroNoticeCard`, banners e chips) em larguras estreitas (`280.dp`, `320.dp`) e escalas de fonte ampliadas (`fontScale 1.5f`). |
| `ParintintinNavHostTest` | 2 | **`APROVADO`** *(Robolectric + Compose Test)* | Garantia de que a barra inferior expõe apenas **Início** (`bottom_nav_home`) e **Dicionário** (`bottom_nav_dictionary`), sem `Encyclopedia`, `Favorites` ou `Archive`, e sem exibir placeholders. |
| `HomeViewModelTest` & `DictionaryViewModelTest` | 14 + 17 = 31 | **`APROVADO`** *(JVM / Coroutines Test)* | Disparo único de auto-sync, sincronização manual, *debounce* de `150 ms` na busca e combinação de filtros reativos. |
| `EncyclopediaViewModelTest` | 5 | **`APROVADO`** *(JVM / Coroutines Test)* | Falhas comuns com mensagem/fallback, propagação explícita de `CancellationException` *upstream*, cancelamento estruturado via `flatMapLatest` e recuperação via `onRetry()` *(estrutura preparatória não exposta na v1.0)*. |

*Ressalva:* Os testes acima não testam a tela não exposta `EncyclopediaScreen` nem executam auditoria presencial com leitor de tela **TalkBack** em dispositivo físico.

---

## 11. Compilação e Geração de Artefatos

| Artefato / Build | Comando / Referência | Resultado Verificado | Observações e Restrições |
|---|---|---|---|
| **APK Debug v1.0 Atual (Raiz e `.build-outputs`)** | `:app:assembleDebug` (`Voz-Ancestral-debug.apk` na raiz, `app/build/outputs/apk/debug/app-debug.apk` e `.build-outputs/app-debug.apk`) | **`APROVADO`** *(artefato gerado e inspecionado)* | Três cópias idênticas (SHA-256 `965183144f9aae41efd263e2467062fd28565d52d58007e71f96eeb8de68f633`), tamanho exato de `22.218.647` bytes; `applicationId = com.aistudio.parintintin.kagwah`; `versionCode = 1`; `versionName = "1.0"`; `minSdkVersionForDexing = 24`; assinatura Debug v2 válida (`output-metadata.json`). **Não equivale à validação completa em dispositivos reais.** |
| **APK Debug v1.0 Histórico** | `.build-outputs/Parintintin-v1.0-debug.apk` *(compilação anterior)* | **`APROVADO`** *(artefato histórico preservado)* | Tamanho de `22.753.989` bytes (SHA-256 `3930eb1520316cde1c3b8250013767faaf548965737b5dfd7e9a643d28cf4092`), referente à compilação anterior registrada antes da geração de `Voz-Ancestral-debug.apk`. |
| **APK de Testes Instrumentados** | `:app:assembleDebugAndroidTest` (`app-debug-androidTest.apk`) | **`COMPILADO, NÃO EXECUTADO`** | Compilado com sucesso na etapa de validação de regressão (Etapa 29.7); diretório `androidTest` não está mais presente em `app/build/outputs/apk/` após builds posteriores; execução pendente em ambiente com emulador/ADB. |
| **APK Release Assinado** | `:app:assembleRelease` (`app/build.gradle.kts`) | **`NÃO VALIDADO` / `PENDENTE`** | Na auditoria da Etapa 30.0, o build falhou na tarefa `:app:packageRelease` por ausência do arquivo `my-upload-key.jks` e das variáveis de ambiente `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD`. **Não há APK Release assinado nem publicação/distribuição oficial.** |

---

## 12. Testes e Cenários Não Executados

Os itens abaixo encontram-se explicitamente sem execução comprovada até o momento:

| Cenário / Atividade | Classificação | Motivo / Condição Pendente |
|---|---|---|
| **Execução de `StartupSeedHiltInstrumentedTest` e `ExampleInstrumentedTest` (`:app:connectedDebugAndroidTest`)** | **`COMPILADO, NÃO EXECUTADO`** / **`PENDENTE`** | Ausência de emulador Android e de acesso `adb` no container de execução. |
| **Comunicação e sincronização contra backend oficial (`https://archive.parintintin.org/`)** | **`NÃO VALIDADO`** | Disponibilidade operacional do servidor oficial não confirmada durante a auditoria; testes de rede utilizaram apenas `MockWebServer`. |
| **Consumo do manifesto remoto (`/v1/manifest.json`)** | **`NÃO VALIDADO`** | Endpoint declarado em `ParintintinApiService`, porém ainda não integrado ao fluxo de `ArchiveSyncRepositoryImpl`. |
| **Sincronização remota da Enciclopédia Cultural (`/v1/articles.json`)** | **`NÃO VALIDADO`** | Módulo excluído de `ArchiveSyncRepositoryImpl.syncArchive()` e da barra inferior na v1.0. |
| **Validação manual em aparelho Android físico (fluxo offline-first, alternância Wi-Fi/4G real, rotação, multi-janela)** | **`NÃO VALIDADO`** / **`PENDENTE`** | Nenhum teste manual em hardware físico foi executado neste ambiente. |
| **Auditoria prática de acessibilidade com TalkBack em dispositivo físico** | **`NÃO VALIDADO`** / **`PENDENTE`** | Verificações atuais cobrem apenas semântica Compose via Robolectric. |
| **Testes de desempenho (*benchmarks* de renderização, tempo de inicialização e consumo de CPU/memória)** | **`NÃO VALIDADO`** | Sem *profiling* ou suíte Macrobenchmark configurada ou executada. |
| **Testes instrumentais específicos de detecção de *memory leaks* (ex.: LeakCanary / análise de heap)** | **`NÃO VALIDADO`** | Apenas padrões estruturais (`WhileSubscribed(5000)`, `AutoCloseable`) e testes de ciclo de vida de scope/callback na JVM foram verificados. |
| **Empacotamento e instalação de APK/AAB Release assinado** | **`NÃO VALIDADO`** / **`PENDENTE`** | Keystore Release dedicada e credenciais ausentes no ambiente. |
| **Medição de cobertura percentual de código (ex.: JaCoCo)** | **`NÃO VALIDADO`** | Nenhum relatório de cobertura foi gerado; nenhum percentual é declarado. |

---

## 13. Limitações e Riscos Conhecidos

1. **Ausência de falha na JVM não comprova ausência de defeitos no aplicativo completo:** os 238 testes aprovados em 25 suítes na JVM (incluindo Robolectric) não substituem a execução instrumentada (`:app:connectedDebugAndroidTest`) nem testes exploratórios em dispositivos Android reais.
2. **Dados estritamente demonstrativos (`DEMO-001`..`DEMO-020`):** todos os 20 registros locais são termos e expressões fictícios em português; os testes comprovam o comportamento de armazenamento/pesquisa/filtro, mas **não validam autenticidade linguística da língua Parintintin**.
3. **Servidor oficial não confirmado e sincronização parcial de escopo:** a URL `https://archive.parintintin.org/` não teve disponibilidade operacional confirmada na auditoria, o manifesto remoto não está integrado e não há política definida de exclusão de registros.
4. **Módulo futuro ainda inativo na navegação (`EncyclopediaScreen.kt`):** o saneamento técnico `TEC-001` em `EncyclopediaViewModel.kt` (relançamento explícito de `CancellationException` no operador `.catch`) foi concluído e validado em `EncyclopediaViewModelTest.kt` (5 testes); entretanto, a tela `EncyclopediaScreen` permanece fora da barra de navegação e sem sincronização na v1.0 (`FUT-005`), sem testes de UI Compose para essa tela.
5. **Ausência de artefato Release assinado:** os APKs Debug disponíveis (`Voz-Ancestral-debug.apk` na raiz e cópia histórica `.build-outputs/Parintintin-v1.0-debug.apk`) destinam-se apenas a testes em build Debug e não substituem assinatura Release para distribuição externa.

---

## 14. Resumo Consolidado dos Resultados

| Dimensão Avaliada | Evidência / Quantitativo | Classificação Oficial |
|---|---|---|
| **Testes Automatizados na JVM: domínio, Room, Seed, `MockWebServer`, Robolectric e Compose (`gradle :app:testDebugUnitTest`)** | **25 suítes \| 238 testes \| 0 falhas \| 0 erros \| 0 ignorados** *(reexecutado e confirmado em 22/11/2026 com `JAVA_TOOL_OPTIONS="-Duser.home=/home/appuser"`)* | **`APROVADO`** |
| **Compilação da suíte instrumentada (`:app:assembleDebugAndroidTest`)** | `StartupSeedHiltInstrumentedTest` (`2` `@Test`), `ExampleInstrumentedTest` (`1` `@Test`) (`3` `@Test` no total) | **`COMPILADO, NÃO EXECUTADO`** |
| **Execução dos testes instrumentados em dispositivo/emulador (`:app:connectedDebugAndroidTest`)** | Sem emulador/ADB no ambiente | **`PENDENTE`** *(não executado)* |
| **Geração e integridade do APK Debug v1.0** | `Voz-Ancestral-debug.apk` / `app/build/outputs/apk/debug/app-debug.apk` / `.build-outputs/app-debug.apk` (`22.218.647` bytes; histórico `.build-outputs/Parintintin-v1.0-debug.apk` com `22.753.989` bytes) | **`APROVADO`** *(build Debug)* |
| **APK Release assinado e publicação oficial** | Falha em `:app:packageRelease` por ausência de `my-upload-key.jks` e variáveis de assinatura | **`NÃO VALIDADO`** / **`PENDENTE`** |
| **Integração operacional com backend oficial e manifesto remoto** | Base URL `https://archive.parintintin.org/` sem disponibilidade confirmada; `/v1/manifest.json` (`getArchiveManifest()`) não integrado | **`NÃO VALIDADO`** |
| **Testes manuais em hardware real, TalkBack físico, *benchmarks*, *memory leaks* e cobertura percentual** | Sem execução presencial, sem profiling e sem relatório JaCoCo | **`NÃO VALIDADO`** / **`PENDENTE`** |

---

## 15. Histórico e Observações

- **Natureza do registro:** consolidação documental retrospectiva correspondente à **Versão 1.0 Demonstrativa** do projeto **Voz Ancestral** (`Acervo Provisório`), alinhada aos documentos `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md` e `docs/api-v1-contract.md`.
- **Atualizações futuras previstas:** este documento deverá ser atualizado somente quando houver novas execuções comprovadas (como execução de `:app:connectedDebugAndroidTest` em dispositivo/emulador externo, testes presenciais com TalkBack, geração de APK Release assinado ou futura homologação contra backend oficial ativo).
- **Preservação:** a criação deste relatório não alterou código-fonte, arquivos Gradle, testes automatizados nem os documentos anteriores (`SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `docs/api-v1-contract.md`).
- **Atualização após conclusão de `TEC-001` e reauditoria documental:** mantendo preservado o registro histórico inicial da v1.0 (**24 suítes e 233 testes aprovados**), o consolidado mais recente da suíte completa `:app:testDebugUnitTest` contabiliza **25 suítes e 238 testes aprovados** (`0` falhas, `0` erros, `0` ignorados), revalidados em 22/11/2026 com os 25 arquivos XML gerados em `app/build/test-results/testDebugUnitTest/`, permanecendo `androidTest` (3 métodos `@Test` em 2 arquivos de teste) sem execução por ausência de emulador Android/ADB.
- **Natureza transitória dos relatórios gerados pelo Gradle:** enquanto este arquivo `TEST_RESULTS.md` preserva no repositório tanto o registro histórico inicial (24 suítes e 233 testes aprovados) quanto o consolidado mais recente (25 suítes e 238 testes aprovados), os arquivos XML (`app/build/test-results/testDebugUnitTest/`) e HTML (`app/build/reports/tests/testDebugUnitTest/`) ficam em `app/build/` — fora do controle de versão Git (`app/.gitignore`) —, podendo deixar de existir após limpezas, rebuilds ou reinicializações do ambiente sem invalidar os resultados históricos registrados e sendo regeneráveis por uma nova execução de `gradle :app:testDebugUnitTest`.
