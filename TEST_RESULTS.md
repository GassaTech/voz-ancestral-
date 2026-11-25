# TEST_RESULTS.md — Voz Ancestral: Catálogo Digital de Terras e Povos Indígenas do Brasil

> **Nota de Natureza Documental:** Este relatório consolida de forma transparente os resultados efetivamente observados na execução local mais recente neste container (`gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileDebugKotlin` após o saneamento de identidade pós-Etapa 5.4) e os artefatos presentes no workspace. Execução na JVM, compilação de `androidTest`, confirmações externas reportadas pelo responsável (**627 registros** e **GitHub Actions**) e validações pendentes em aparelho físico são tratadas de forma estritamente separada.

---

## 1. Identificação do Projeto e Versão Avaliada

| Atributo | Informação Verificada | Fonte no Repositório |
|---|---|---|
| **Identidade do Projeto** | **Voz Ancestral — Catálogo Digital de Terras e Povos Indígenas do Brasil** | `README.md`, `SPECIFICATION.md` |
| **Nome Configurado no App/Gradle** | `Voz Ancestral` (`VozAncestral`, `Theme.VozAncestral`) | `app/src/main/res/values/strings.xml`, `settings.gradle.kts`, `themes.xml`, `AndroidManifest.xml` |
| **Versão no Gradle** | `versionCode = 1`, `versionName = "1.0"` | `app/build.gradle.kts`, `app/build/outputs/apk/debug/output-metadata.json` |
| **Identificador Android (`applicationId`)** | `com.aistudio.parintintin.kagwah` (`namespace = "com.example"`, preservado por compatibilidade) | `app/build.gradle.kts` |
| **Compatibilidade SDK** | `minSdk = 24` \| `targetSdk = 36` \| `compileSdk = 36` | `app/build.gradle.kts` |
| **Escopo Funcional Validado** | Barra inferior com **3 abas** (`Início`, `Dicionário`, `Territórios`), Room Schema **v4** (`MIGRATION_3_4`) e cliente ArcGIS REST **IBAMA / SIGAGEO Camada 25** | `AppDestination.kt`, `ParintintinDatabase.kt`, `IbamaTerritoriesApiService.kt` |

---

## 2. Objetivo e Classificações

| Classificação | Significado Estrito |
|---|---|
| **`APROVADO`** | Teste efetivamente executado nesta máquina/container na JVM com resultado `0` falhas, `0` erros e `0` ignorados. |
| **`COMPILADO, NÃO EXECUTADO`** | Código de `androidTest` compilado com `:app:compileDebugAndroidTestKotlin`, sem execução em emulador ou dispositivo físico. |
| **`CONFIRMADO EXTERNAMENTE`** | Resultado reportado pelo responsável pelo projeto fora deste container offline (não reproduzível localmente por falta de saída de rede externa, `.git` ou arquivos de workflow CI). |
| **`NÃO VALIDADO` / `PENDENTE`** | Cenário sem execução ou evidência direta no container atual. |

---

## 3. Escopo e Precisão dos Dados Avaliados

- **Catálogo de Terras Indígenas (`IbamaTerritoriesApiService`):** os testes locais validam contratos JSON, paginação, persistência Room v4, normalização de acentos, filtros combináveis, ordenação com nulos/vazios/espaços sempre no final e UI Compose usando **`MockWebServer`** e SQLite via Robolectric. Conforme a natureza da camada 25 do IBAMA/SIGAGEO, a fonte fornece dados territoriais oficiais e **etnias associadas** (`etnia_nome` / `ethnicities`), **não representando perfis culturais, históricos ou linguísticos completos dos povos indígenas**.
- **Dicionário Demonstrativo (`dictionary_seed.json`):** os **20 verbetes** (`DEMO-001`..`DEMO-020`) continuam sendo registros fictícios em português para demonstração técnica.

---

## 4. Ambiente e Ferramentas Verificados

- **Linguagem / Target JVM:** Kotlin `2.2.10`, `JavaVersion.VERSION_11` (`app/build.gradle.kts`)
- **Build / KSP:** AGP `9.1.1`, Gradle `9.3.1`, KSP `2.3.5`
- **Frameworks de Teste:** JUnit `4.13.2`, `kotlinx-coroutines-test` `1.10.2`, Robolectric `4.16.1` (`sdk = 36`), Compose UI Test (`2024.09.00`), `MockWebServer` `4.10.0`, Hilt Testing `2.59`, Espresso `3.7.0`
- **Ambiente de Execução Local:** container Linux headless **sem emulador Android e sem acesso ADB**. O cache offline do Robolectric reside em `/home/appuser/.m2`, exigindo `JAVA_TOOL_OPTIONS="-Duser.home=/home/appuser"` quando executado como `root`.
- **Cobertura de código:** não há plugin JaCoCo configurado; nenhum percentual de cobertura é declarado.

---

## 5. Testes Automatizados Executados na JVM (`:app:testDebugUnitTest`)

- **Comando executado neste ambiente (após o saneamento de código, recursos e testes):**
  ```bash
  JAVA_TOOL_OPTIONS="-Duser.home=/home/appuser" gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileDebugKotlin --console=plain
  ```
- **Resultado:** compilação Kotlin aprovada; testes JVM executados com **resultado parcial**.
- **Totais consolidados da execução local:**
  - **35 suítes de testes**
  - **303 testes executados**
  - **299 testes aprovados**
  - **4 falhas**
  - **0 testes ignorados**

> **Falhas identificadas:** os quatro testes que falharam pertencem a `ParintintinDatabaseMigrationTest` e apresentam `SQLiteCantOpenDatabaseException` ao abrir o banco temporário. O mesmo comportamento foi reproduzido no projeto original, portanto não há evidência de que tenha sido introduzido pela revisao. A causa ainda não foi isolada.
>
> **Compilação e APK:** `:app:compileDebugKotlin`, `:app:compileDebugAndroidTestKotlin` e `:app:assembleDebug` concluídos com `BUILD SUCCESSFUL`. APK Debug gerado em `app/build/outputs/apk/debug/app-debug.apk`, com **22.421.135 bytes** e SHA-256 `DB5D6BDB1C5CF3B697F7231B053EF33272171DC3EF5E0A94BF1E5C7BB56F69D0`. Cópia em Downloads verificada com hash idêntico. A compilação de `androidTest` não equivale à execução dos testes instrumentados.
> **Evolução histórica preservada:** `233 testes / 24 suítes` (marco inicial v1.0) → `238 / 25` (`TEC-001`) → `252 / 27` (fechamento v1.0 com `/v1/manifest.json`) → `272 / 30` (Etapa 1 ArcGIS IBAMA) → **303 testes / 35 suítes** (consolidação atual após Etapas 2 a 5.4).

### Detalhamento Completo das 35 Suítes Executadas na JVM

| # | Suíte (`app/src/test`) | Testes | Falhas | Erros | Ignorados | Status | Escopo Verificado |
|---|---|---|---|---|---|---|---|
| 1 | `ExampleUnitTest` | 1 | 0 | 0 | 0 | `APROVADO` | Teste unitário básico de sanidade na JVM. |
| 2 | `ExampleRobolectricTest` | 1 | 0 | 0 | 0 | `APROVADO` | Sanidade de inicialização com Robolectric (`sdk = 36`), validando `app_name = "Voz Ancestral"`. |
| 3 | `AndroidConnectivityPlatformRobolectricTest` | 1 | 0 | 0 | 0 | `APROVADO` | Integração com `ConnectivityManager` via `ConnectivityModule` e registro/remoção de callback. |
| 4 | `AndroidNetworkConnectivityObserverTest` | 9 | 0 | 0 | 0 | `APROVADO` | Estados `Offline`, `ConnectedUnvalidated`, `Online`, callbacks e liberação via `AutoCloseable`. |
| 5 | `IndigenousLandDaoTest` | 11 | 0 | 0 | 0 | `APROVADO` | CRUD no Room/SQLite real, índice único em `terrai_cod`, preservação de nulos, índices da tabela `indigenous_lands`, busca sem acentos/curingas `LIKE`, filtros combinados, ordenação ASC/DESC mantendo nulos, vazios (`""`) e apenas espaços (`"   "`) no final com desempate até `id ASC`, e opções distintas. |
| 6 | `IndigenousLandQueryBuilderTest` | 7 | 0 | 0 | 0 | `APROVADO` | Normalização e *escape* de curingas, validação segura de UF (`1 = 0`), placeholders parametrizados, desempate final por `id ASC` em todas as ordenações e limite de `10` `REPLACE()` por estágio CTE. |
| 7 | `ParintintinDatabaseMigrationTest` | 7 | 4 | 0 | 0 | `FALHA PARCIAL` | Tres testes aprovados; quatro falharam com `SQLiteCantOpenDatabaseException` ao abrir banco temporário. Falha reproduzida tambem no projeto original. Causa pendente de investigacao. |
| 8 | `DictionarySeedChangeDetectorTest` | 3 | 0 | 0 | 0 | `APROVADO` | Detecção de alteração do arquivo de seed local. |
| 9 | `DictionarySeedImporterTest` | 14 | 0 | 0 | 0 | `APROVADO` | Importação inicial e validação estrita de schema/campos de `dictionary_seed.json`. |
| 10 | `SeedSha256CalculatorTest` | 5 | 0 | 0 | 0 | `APROVADO` | Cálculo SHA-256 em minúsculas e rejeição de arquivo vazio (`EmptySeedFileException`). |
| 11 | `SeedVersionComparatorTest` | 9 | 0 | 0 | 0 | `APROVADO` | Regras de versão e hash (`SEED_NOT_APPLIED`, `SEED_UPDATED`, `SEED_UP_TO_DATE`, `INCONSISTENT_VERSION`). |
| 12 | `VersionedDictionarySeedUpdaterTest` | 12 | 0 | 0 | 0 | `APROVADO` | Atualização transacional idempotente do seed, lotes de 500 IDs, preservação de favoritos e bloqueio pós-sync oficial. |
| 13 | `ArcGisTerritoriesMapperTest` | 5 | 0 | 0 | 0 | `APROVADO` | Separação de múltiplos delimitadores (`,`, `;`, `/`, `|`), listas vazias, rejeição de `superficie` negativa/não finita, normalização de UFs e cálculo de `nextOffset` com `attributes` nulos. |
| 14 | `ArchiveMappersTest` | 5 | 0 | 0 | 0 | `APROVADO` | Mapeamentos `DictionaryEntryDto`↔Entity↔Domínio, `CulturalArticleEntity`↔Domínio e todos os estados de `SyncMetadataEntity` (incluindo `territoriesCount`). |
| 15 | `IndigenousLandMappersTest` | 4 | 0 | 0 | 0 | `APROVADO` | Conversão `RemoteIndigenousLand` ↔ `IndigenousLand` ↔ `IndigenousLandEntity`, preservação de nulos sem dados inventados e derivação determinística de ID (`terrai_cod` → `objectId` → hash estável). |
| 16 | `IbamaTerritoriesApiContractTest` | 13 | 0 | 0 | 0 | `APROVADO` | Endpoint base `NetworkModule` (IBAMA/SIGAGEO Camada 25), adaptadores Moshi/KSP, parâmetros GET padrão, campos nulos/vazios, múltiplas etnias, HTTP `404`/`500`, erro ArcGIS em HTTP `200`, paginação de 1ª/2ª página, prevenção de loop em `exceededTransferLimit` e JSON malformado. |
| 17 | `RemoteTerritoriesDataSourceTest` | 4 | 0 | 0 | 0 | `APROVADO` | Classificação de erros de rede/SSL/timeout/desserialização/HTTP/ArcGIS, validação de paginação e relançamento de `CancellationException`. |
| 18 | `ArchiveSyncRepositoryImplTest` | 35 | 0 | 0 | 0 | `APROVADO` | Fluxos de `SyncState`, integração do seed + `indigenousLandRepository.syncFromRemote()`, preservação de dados/histórico diante de falhas ou `RemoteEmpty`, classificação de falhas e monotonicidade temporal. |
| 19 | `OfflineFirstDictionaryRepositoryTest` | 5 | 0 | 0 | 0 | `APROVADO` | Consulta reativa ao Room e `toggleFavorite` no repositório do dicionário. |
| 20 | `OfflineFirstEncyclopediaRepositoryTest` | 2 | 0 | 0 | 0 | `APROVADO` | Consulta reativa ao Room e `toggleBookmark` no repositório preparatório da Enciclopédia. |
| 21 | `RoomIndigenousLandRepositoryTest` | 10 | 0 | 0 | 0 | `APROVADO` | CRUD/consulta/filtros offline, paginação completa sem contagem fixa, deduplicação, preservação local em falha de 1ª/2ª página ou resposta vazia, serialização concorrente por `syncMutex` e ordenação. |
| 22 | `ArchiveSyncCoordinatorImplTest` | 10 | 0 | 0 | 0 | `APROVADO` | Deduplicação de auto-sync, junção `JoinedInFlight`, isolamento em `@ApplicationScope` e cancelamento seguro. |
| 23 | `ArchiveUseCasesAndClassifierTest` | 2 | 0 | 0 | 0 | `APROVADO` | `SyncFailureClassifier` e delegação dos casos de uso de `ArchiveUseCases`. |
| 24 | `DictionaryOfflineIntegrationTest` | 10 | 0 | 0 | 0 | `APROVADO` | Integração Room + Seed + `ArchiveSyncRepositoryImpl` + `DictionaryViewModel` + `SyncFailureClassifier`. |
| 25 | `TerritoriesSyncIntegrationTest` | 2 | 0 | 0 | 0 | `APROVADO` | Sincronização paginada completa contra `MockWebServer` persistindo no SQLite e consulta offline após encerrar o servidor, além de preservação local e diferenciação `OfflineFallback` vs. `RemoteEmpty`. |
| 26 | `OfflineArchiveStatusCardTest` | 13 | 0 | 0 | 0 | `APROVADO` | Renderização de todos os estados de `SyncState` com terminologia de catálogo (incluindo contagem de Terras Indígenas sincronizadas), falhas e botão manual. |
| 27 | `ResponsiveComponentsTest` | 9 | 0 | 0 | 0 | `APROVADO` | Adaptação responsiva de componentes de catálogo em larguras estreitas (`280.dp`, `320.dp`) e escalas de fonte ampliadas (`fontScale 1.5f`). |
| 28 | `DictionaryScreenTest` | 23 | 0 | 0 | 0 | `APROVADO` | Pesquisa, limpeza, filtros combinados, aviso demonstrativo específico do Dicionário, estados vazio/erro e semântica de acessibilidade. |
| 29 | `DictionaryViewModelTest` | 17 | 0 | 0 | 0 | `APROVADO` | *Debounce* de `150 ms`, combinação de filtros, estados `Loading`/`Empty`/`Success`/`Error` e cancelamento. |
| 30 | `EncyclopediaViewModelTest` | 5 | 0 | 0 | 0 | `APROVADO` | Fallback de erros comuns, propagação de `CancellationException`, `flatMapLatest` e `onRetry()` *(estrutura preparatória não exposta na navegação)*. |
| 31 | `HomeScreenTest` | 7 | 0 | 0 | 0 | `APROVADO` | Banner inicial (*Terras e Povos Indígenas do Brasil*), ausência de card demonstrativo na Home, layout em `280.dp`, módulos e contagem local na Home. |
| 32 | `HomeViewModelTest` | 14 | 0 | 0 | 0 | `APROVADO` | Disparo de auto-sync, sincronização manual, estatísticas locais e sobrevivência no `@ApplicationScope`. |
| 33 | `ParintintinNavHostTest` | 4 | 0 | 0 | 0 | `APROVADO` | Exposição exclusiva de **Início**, **Dicionário** e **Territórios** na barra inferior (sem rotas provisórias obsoletas), seleção visual correta do destino, preservação do estado de pesquisa entre alternâncias de abas e retorno para **Início** via botão voltar a partir de **Territórios**. |
| 34 | `TerritoriesScreenTest` | 10 | 0 | 0 | 0 | `APROVADO` | Pesquisa e limpeza, filtros combináveis de UF/fase/modalidade com acessibilidade, 4 critérios de ordenação + direção, estados `Loading`/`Empty` (com e sem filtros)/`Error`/`Success`, tratamento seguro de nulos/ausentes, barra de sync + erro manual dispensável e layout 2 colunas em `w720dp`. |
| 35 | `TerritoriesViewModelTest` | 14 | 0 | 0 | 0 | `APROVADO` | Estado inicial `Loading`, carregamento `Success`, `Empty` com/sem filtros, *debounce* de `150 ms`, filtros combinados, ordenação nos 4 campos (incluindo verificação específica de nulos sempre no final em ASC e DESC), recarregamento reativo, tratamento de erro/retry, proteção de `CancellationException`, cancelamento por `flatMapLatest`, e tratamento seguro de exceção comum em `syncManually()` mantendo bloqueio contra chamadas simultâneas. |
| — | **Total Consolidado (`35` suites)** | **303** | **4** | **0** | **0** | **`PARCIAL`** | **299 aprovados e 4 falhas na suíte de migrações.** |

---

## 6. Testes Instrumentados Android (`app/src/androidTest`)

- **Status atual:** **`COMPILADO, NÃO EXECUTADO`** (`:app:compileDebugAndroidTestKotlin` aprovado com `BUILD SUCCESSFUL`).
- **Atualização verificada no código de `androidTest`:**
  - `TestNetworkModule.kt` passou a prover `IbamaTerritoriesApiService` (com `IndigenousLandAttributesJsonAdapter` + `MockWebServer`).
  - `StartupSeedHiltInstrumentedTest.kt` (`2` `@Test`) foi atualizado para enfileirar resposta vazia válida de `/query?` (`{"fields":[],"features":[],"exceededTransferLimit":false}`), validar o fluxo ponta a ponta `HomeViewModel.init` → Seed `DEMO-001`..`DEMO-020` → `SyncState.RemoteEmpty` → navegação para `DictionaryScreen` e verificar que a requisição atingiu `/query?`.
  - `ExampleInstrumentedTest.kt` (`1` `@Test`) verifica `BuildConfig.APPLICATION_ID`.
- **Pendências locais:** na rodada atual foi executado `:app:compileDebugAndroidTestKotlin` (sem executar `:app:assembleDebugAndroidTest`, logo `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk` não está presente neste momento) e não houve execução de `:app:connectedDebugAndroidTest` por ausência de emulador/ADB.

---

## 7. Artefatos de Build e Estado do Workspace

| Artefato / Verificação | Situação Verificada Neste Container | Observações |
|---|---|---|
| **APK Debug do App (`app/build/outputs/apk/debug/app-debug.apk`)** | **Ainda nao validado nesta rodada local** | O APK desta revisao sera gerado e verificado apos a integracao no projeto oficial. O tamanho e SHA-256 informados em relatorios anteriores nao foram confirmados nesta maquina. |
| **APK Único Histórico na Raiz (`Voz-Ancestral-debug.apk`)** | **Não presente na raiz** no estado atual do workspace | O arquivo único da v1.0 documentado anteriormente não existe mais na raiz; existem apenas arquivos de transferência `.zip` e `.b64` de etapas anteriores. |
| **APK Release Assinado (`:app:assembleRelease`)** | **`NÃO VALIDADO` / `BLOQUEADO`** | Depende de keystore Release (`my-upload-key.jks`) e variáveis de assinatura externas ao container. |

---

## 8. Confirmações Externas e Limitações do Ambiente Offline

Os dois itens abaixo foram **confirmados externamente pelo responsável pelo projeto**, mas **não puderam ser reproduzidos diretamente neste container offline**:

1. **Sincronização real de 627 registros territoriais (IBAMA / SIGAGEO Camada 25):**
   - **Situação local:** o cliente `IbamaTerritoriesApiService` / `RoomIndigenousLandRepository` está completo no código e validado com paginação multi-página em `MockWebServer`; não há acesso de rede externa neste container para consultar `https://pamgia.ibama.gov.br` ao vivo.
   - **Confirmação externa:** validação dos **627 registros territoriais** confirmada externamente pelo responsável. *(Nota: ocorrências do número `627` dentro de `app/src/test/` representam fixtures de teste unitário/integração simulado, além do registro em `docs/arcgis-territories-api.md`.)*
2. **Aprovação no GitHub Actions:**
   - **Situação local:** não existem diretórios `.git` nem `.github/workflows/` neste container, não sendo possível inspecionar definições de workflow ou logs de execução do CI localmente.
   - **Confirmação externa:** execução aprovada no **GitHub Actions** confirmada externamente pelo responsável.

---

## 9. Cenários Pendentes e Checklist Externo para Validação em Hardware

Permanecem dependentes de execução externa em aparelho Android real ou emulador com ADB:
1. Executar `gradle :app:connectedDebugAndroidTest` para rodar `StartupSeedHiltInstrumentedTest` e `ExampleInstrumentedTest` com Hilt real.
2. Instalar o build Debug atual (`./gradlew :app:assembleDebug` ou instalação direta pelo Android Studio) em aparelho físico e validar:
   - sincronização real contra `https://pamgia.ibama.gov.br/.../MapServer/25/query` (conferindo carga dos 627 territórios e exibição na aba **Territórios** e no card da **Início**);
   - consulta offline em modo avião com pesquisa sem acentos, filtros combinados de UF/fase/modalidade e ordenação ASC/DESC em **Territórios**;
   - preservação de estado ao alternar entre **Início**, **Dicionário** e **Territórios**, e comportamento do botão voltar;
   - rotação retrato/paisagem, escalas grandes de fonte e auditoria presencial com leitor de tela **TalkBack**.
3. Gerar build Release assinado quando houver keystore e credenciais externas configuradas.

---

## 10. Resumo Consolidado dos Resultados

| Dimensão Avaliada | Evidência / Quantitativo | Classificação |
|---|---|---|
| **Testes Automatizados na JVM (`:app:testDebugUnitTest`)** | **35 suites; 303 testes; 299 aprovados; 4 falhas; 0 ignorados** | **`PARCIAL`** |
| **Compilação de `androidTest` (`:app:compileDebugAndroidTestKotlin`)** | `StartupSeedHiltInstrumentedTest` (`2` `@Test`), `ExampleInstrumentedTest` (`1` `@Test`), `TestNetworkModule` atualizados para ArcGIS `/query?` | **`COMPILADO, NÃO EXECUTADO`** |
| **Carga de 627 registros IBAMA e Pipeline GitHub Actions** | Reportados pelo responsável; ambiente local sem acesso externo, `.git` ou `.github/workflows/` | **`CONFIRMADO EXTERNAMENTE`** *(não reproduzido offline)* |
| **APK Debug (`:app:assembleDebug`)** | **Gerado e verificado; 22.421.135 bytes; SHA-256 `DB5D6BDB1C5CF3B697F7231B053EF33272171DC3EF5E0A94BF1E5C7BB56F69D0`**; cópia em Downloads conferida | **`CONCLUÍDO`** *(geração e integridade; teste físico pendente)* |
| **Execução `:app:connectedDebugAndroidTest`, TalkBack em hardware, *profiling* e APK Release** | Sem emulador/ADB, aparelho físico, JaCoCo ou keystore Release no container | **`PENDENTE` / `NÃO VALIDADO`** |
