# Voz Ancestral — Catálogo Digital de Terras e Povos Indígenas do Brasil

Aplicativo Android desenvolvido em arquitetura *Offline-First* no âmbito do projeto **Voz Ancestral**, atualmente estruturado como **Catálogo Digital de Terras e Povos Indígenas do Brasil**. O sistema evoluiu a partir da base técnica do antigo protótipo **Acervo Provisório**, reaproveitando a arquitetura Clean Architecture + MVVM, a persistência em Room e a infraestrutura de conectividade/sincronização para integrar dados territoriais oficiais da **API ArcGIS REST do IBAMA / SIGAGEO (Camada 25)**, mantendo também as telas **Início** e **Dicionário** (com 20 verbetes demonstrativos em português).

> **Aviso de Precisão e Escopo dos Dados:**
> 1. **Terras Indígenas (API IBAMA / SIGAGEO, Camada 25):** a API fornece informações territoriais oficiais (nome da Terra Indígena, código `terrai_cod`, UF, municípios, superfície em hectares, fase do processo, modalidade, data de atualização) e as **etnias associadas aos territórios** (`etnia_nome` / `ethnicities`). **Ela não representa uma base completa de perfis culturais, históricos ou linguísticos dos povos indígenas.**
> 2. **Dicionário (Escopo Demonstrativo Preservado):** a aba **Dicionário** mantém os **20 registros demonstrativos fictícios em português** (`DEMO-001` a `DEMO-020`) para preservação da funcionalidade anterior; **nenhum verbete dessa aba representa vocabulário autêntico de línguas indígenas**.
> 3. **Identificadores Residuais de Configuração:** embora a identidade do projeto tenha evoluído para **Voz Ancestral: Catálogo Digital de Terras e Povos Indígenas do Brasil**, o manifesto de strings (`app_name = "Acervo Provisório"`), `settings.gradle.kts` (`rootProject.name = "AcervoProvisorio"`), `applicationId` (`com.aistudio.parintintin.kagwah`), `versionName = "1.0"` e os textos iniciais da tela **Início** ainda preservam os identificadores originais da v1.0.

---

## 1. Identificação do Projeto

| Atributo | Informação Verificada no Repositório | Observação |
|---|---|---|
| **Identidade Atual do Projeto** | **Voz Ancestral: Catálogo Digital de Terras e Povos Indígenas do Brasil** | Evolução do projeto a partir do protótipo *Acervo Provisório* |
| **Nome Configurado no Android/Gradle** | `app_name = "Acervo Provisório"` \| `rootProject.name = "AcervoProvisorio"` | Identificador residual em `strings.xml` e `settings.gradle.kts` |
| **Metadado no AI Studio (`metadata.json`)** | `"Voz Ancestral"` | Descrição ainda refere-se apenas à língua Parintintin |
| **Versão no Gradle** | `versionCode = 1`, `versionName = "1.0"` | `app/build.gradle.kts` |
| **Identificador Android (`applicationId`)** | `com.aistudio.parintintin.kagwah` (`namespace = "com.example"`) | `app/build.gradle.kts` |
| **Fonte Remota Territorial** | API ArcGIS REST — IBAMA / SIGAGEO (`MapServer/25/query`) | `ArcGisTerritoriesQuerySpec.kt`, `NetworkModule.kt` |
| **Destinos Principais de Navegação** | **Início**, **Dicionário** e **Territórios** | `AppDestination.primaryDestinations` (Etapa 5.4) |

---

## 2. Evolução e Apresentação

O projeto **Voz Ancestral** nasceu com foco na preservação digital *offline-first* da língua Parintintin (protótipo **Acervo Provisório** v1.0) e evoluiu de maneira incremental — ao longo das **Etapas 1 a 5.4** — para um **Catálogo Digital de Terras e Povos Indígenas do Brasil**, aproveitando toda a base arquitetural já validada:

- **Reaproveitamento arquitetural:** manutenção integral da separação em camadas (**Clean Architecture + MVVM**), da injeção de dependências com **Dagger Hilt**, do monitoramento de conectividade via `ConnectivityManager` e da coordenação reativa de sincronização (`ArchiveSyncCoordinatorImpl`).
- **Evolução do banco Room (Schema v3 → v4):** adição da tabela `indigenous_lands` (`IndigenousLandEntity`), índices dedicados e migração explícita `MIGRATION_3_4`, com suporte a consultas SQL avançadas via CTEs (`IndigenousLandQueryBuilder`) para busca com remoção de acentos, filtros combináveis e ordenação determinística com valores nulos, vazios e somente espaços mantidos sempre ao final.
- **Integração territorial oficial (IBAMA / SIGAGEO, Camada 25):** substituição do antigo cliente provisório (`archive.parintintin.org`) pela integração paginada com o serviço público ArcGIS REST do IBAMA (`https://pamgia.ibama.gov.br/server/rest/services/SIGAGEO/bases/MapServer/25/`), mantendo consulta local exclusiva ao Room na interface de usuário.

---

## 3. Funcionalidades Disponíveis

A barra de navegação inferior (`ParintintinBottomBar`) expõe exatamente **três destinos principais** (`AppDestination.primaryDestinations`): **Início**, **Dicionário** e **Territórios**.

### 3.1. Territórios (`Territórios` — Etapas 1 a 5.4)
- **Consulta 100% offline via Room:** `TerritoriesScreen` e `TerritoriesViewModel` observam exclusivamente fluxos locais (`ObserveIndigenousLandsUseCase`, `ObserveIndigenousLandFilterOptionsUseCase`, `ObserveIndigenousLandsCountUseCase`), exibindo o catálogo independentemente de conexão ativa.
- **Cards informativos (`TerritoryCard`):** apresentam, conforme disponibilidade dos dados na origem sem preencher valores inventados: nome da Terra Indígena (ou fallback legível quando ausente), código (`terrai_cod`), **UF**, **fase do processo** (`fase_ti`), **modalidade**, **etnias associadas** (`ethnicities` / `etnia_nome`), municípios, superfície em hectares (`pt-BR`) e data de atualização bruta (`data_atual`).
- **Pesquisa textual reativa:** campo de busca com *debounce* de `150 ms`, botão de limpeza rápida e correspondência SQL com normalização de acentos e *escape* de curingas `LIKE` sobre nome, etnias associadas (bruto e JSON), municípios (bruto e JSON) e código do território.
- **Filtros combináveis dinâmicos:** chips horizontais extraídos dinamicamente dos registros locais para **UF**, **Fase do processo** e **Modalidade**, combináveis entre si e com a pesquisa textual, além de limpeza global (`Limpar filtros`).
- **Ordenação pelos quatro critérios com nulos/vazios ao final:** alternância entre **Nome**, **Código**, **Superfície** e **Atualização**, em ordem crescente ou decrescente, garantindo em SQLite e na camada de apresentação que valores `null`, vazios (`""`) ou com apenas espaços (`"   "`) permaneçam sempre ao final, com desempate determinístico até `id ASC`.
- **Estados completos de interface e sincronização manual:** exibição de estados `Loading`, `Empty` (diferenciando base vazia de busca/filtro sem resultados), `Success` (1 coluna em telas compactas e 2 colunas a partir de `600.dp`) e `Error` com `Tentar novamente`, além de barra de status (`TerritoriesSyncStatusBar`) com botão de sincronização manual protegido contra chamadas simultâneas e banner dispensável em caso de erro inesperado.

### 3.2. Início (`Home`)
- Exibe o banner inicial da v1.0, o aviso permanente `MODO DEMONSTRATIVO`, explicação introdutória e o cartão **Armazenamento Local (Offline-First)** (`OfflineArchiveStatusCard`), atualizado para exibir três métricas locais: **Terras Indígenas locais**, **verbetes locais** e **artigos locais**, além da contagem sincronizada de Terras Indígenas (`SyncState.Synced`) e controle de sincronização manual.
- Mantém o disparo automático único de inicialização por processo (`HomeViewModel.init` → `ArchiveSyncCoordinator.requestAutoSync()`), combinando atualização idempotente do seed demonstrativo e sincronização paginada das Terras Indígenas.

### 3.3. Dicionário (`Dicionário`)
- Preserva integralmente a experiência de consulta offline dos **20 verbetes demonstrativos em português** (`DEMO-001` a `DEMO-020`), com pesquisa textual (*debounce* de `150 ms`), filtros combináveis por 6 categorias semânticas e 7 classes gramaticais, cabeçalho fixo `MODO DEMONSTRATIVO` e selos identificadores de conteúdo demonstrativo.

### 3.4. Navegação Principal (Etapa 5.4)
- Navegação tipada com `Navigation Compose` (`@Serializable`) entre **Início**, **Dicionário** e **Territórios**, com `launchSingleTop = true`, `saveState = true` e `restoreState = true` preservando estado de pesquisa/filtros ao alternar abas e retorno padrão via botão voltar para **Início**.

---

## 4. Limitações e Escopo Atual

- **Escopo dos dados da API IBAMA / SIGAGEO:** a camada 25 fornece atributos territoriais oficiais e etnias associadas (`etnia_nome`), **não substituindo fontes antropológicas, históricas ou linguísticas completas dos povos indígenas**. Não há sincronização de geometrias/polígonos (`returnGeometry = false`), nem tela dedicada de detalhes individuais de territórios.
- **Conteúdo linguístico e módulos secundários:** os 20 verbetes do **Dicionário** continuam sendo registros demonstrativos em português (`DEMO-001`..`DEMO-020`), sem vocabulário Parintintin autêntico, IPA ou áudio. As rotas `Encyclopedia`, `Favorites` (`ProvisionalDestinationScreen`) e `Archive` (`ProvisionalDestinationScreen`) permanecem fora da barra inferior.
- **Identificadores e textos residuais:** `strings.xml` (`app_name`, `home_hero_title`, `home_provisional_badge`), `settings.gradle.kts`, `metadata.json` e `docs/api-v1-contract.md` ainda carregam referências da fase inicial *Acervo Provisório* / *Parintintin* (detalhes em [`TEST_RESULTS.md`](TEST_RESULTS.md) e [`SPECIFICATION.md`](SPECIFICATION.md)).
- **Execução de rede neste container offline:** no ambiente local de desenvolvimento/auditoria, todas as validações HTTP utilizaram `MockWebServer` (não há saída de rede externa nem emulador/ADB neste container). A sincronização completa de **627 registros territoriais** e a aprovação no **GitHub Actions** foram **confirmadas externamente pelo responsável pelo projeto** (o diretório `.github/` e o histórico `.git` não estão presentes neste container).
- **Validação em hardware e Release:** testes instrumentados (`app/src/androidTest`) não foram executados por ausência de emulador/ADB, não há auditoria manual com TalkBack em aparelho físico e não há assinatura Release configurada (`my-upload-key.jks` e variáveis de assinatura externos ao repositório).

---

## 5. Tecnologias Utilizadas

Versões confirmadas em `gradle/libs.versions.toml`, `app/build.gradle.kts` e `gradle/wrapper/gradle-wrapper.properties`:

| Categoria | Tecnologia | Versão Comprovada |
|---|---|---|
| **Linguagem** | Kotlin | `2.2.10` (`JavaVersion.VERSION_11` bytecode) |
| **Interface Declarativa** | Jetpack Compose + Material 3 (`Navigation Compose`) | Compose BOM `2024.09.00` \| Navigation `2.8.9` |
| **Arquitetura** | Clean Architecture + MVVM | Pacotes `presentation`, `domain`, `data`, `di`, `core` |
| **Injeção de Dependências** | Dagger Hilt + `hilt-navigation-compose` | Dagger Hilt `2.59` \| `hilt-navigation-compose 1.2.0` |
| **Persistência Local** | AndroidX Room (`room-runtime`, `room-ktx`) | `2.7.0` (Schema **v4**, migrações `1->2`, `2->3`, `3->4`) |
| **Concorrência e Fluxos** | Kotlin Coroutines + `Flow` / `StateFlow` | `1.10.2` |
| **Rede e HTTP** | Retrofit + OkHttp (`logging-interceptor`) | Retrofit `2.12.0` \| OkHttp `4.10.0` (`connect=15s`, `read=30s`) |
| **Serialização** | Moshi (`IndigenousLandAttributesJsonAdapter` + `KotlinJsonAdapterFactory`) + Kotlinx Serialization JSON | Moshi `1.15.2` \| Serialization `1.8.0` |
| **Build e Processamento** | Gradle + Android Gradle Plugin (AGP) + KSP | Gradle `9.3.1` \| AGP `9.1.1` \| KSP `2.3.5` |
| **Testes Automatizados** | JUnit, Coroutines Test, Robolectric, Compose UI Test, MockWebServer, Hilt Testing, Espresso | JUnit `4.13.2`, Robolectric `4.16.1`, MockWebServer `4.10.0`, Hilt Testing `2.59`, Espresso `3.7.0` |

*Nota:* Dependências opcionais comentadas em `app/build.gradle.kts` (Firebase, CameraX, Coil, DataStore, Play Services Location) permanecem inativas.

---

## 6. Arquitetura do Projeto

O projeto segue **Clean Architecture** combinada ao padrão **MVVM**:

- **`presentation` e `ui.theme`:** telas Compose (`HomeScreen`, `DictionaryScreen`, `TerritoriesScreen`, `EncyclopediaScreen` preparatória), componentes compartilhados (`CatalogSharedComponents`, `PreserveComponents`), componentes territoriais (`TerritoryCard`, `TerritoryControlsSection`, `TerritoriesSyncStatusBar`), navegação (`AppDestination`, `ParintintinBottomBar`, `ParintintinNavHost`) e `@HiltViewModel`s (`HomeViewModel`, `DictionaryViewModel`, `TerritoriesViewModel`, `EncyclopediaViewModel`).
- **`domain`:** modelos puros (`IndigenousLand`, `TerritoryFilter`, `TerritorySortOption`, `TerritoryFilterOptions`, `DictionaryEntry`, `CulturalArticle`, `SyncState`, `RemoteSyncOutcome`, `DeviceConnectivityState`), contratos de repositório (`IndigenousLandRepository`, `DictionaryRepository`, `EncyclopediaRepository`, `SyncStatusRepository`), casos de uso (`TerritoriesUseCases`, `ArchiveUseCases`), coordenação e classificação de falhas (`ArchiveSyncCoordinator`, `SyncFailureClassifier`).
- **`data`:** banco Room `ParintintinDatabase` v4 (`IndigenousLandDao`, `IndigenousLandQueryBuilder`, `DictionaryDao`, `EncyclopediaDao`, `SyncMetadataDao`, `SeedMetadataDao`), migrações `ParintintinMigrations`, seed `VersionedDictionarySeedUpdater`, cliente ArcGIS (`ArcGisTerritoriesQuerySpec`, `IbamaTerritoriesApiService`, `RemoteTerritoriesDataSource`, `ArcGisTerritoriesDtos`, `ArcGisJsonAdapters`, `ArcGisTerritoriesMapper`), observador de conectividade (`AndroidNetworkConnectivityObserver`) e repositórios (`RoomIndigenousLandRepository`, `ArchiveSyncRepositoryImpl`, `OfflineFirstDictionaryRepository`, `OfflineFirstEncyclopediaRepository`).
- **`di` e `core.common`:** módulos Hilt (`DatabaseModule`, `NetworkModule`, `RepositoryModule`, `SeedModule`, `SyncModule`, `ConnectivityModule`, `CoroutinesModule`) e utilitários (`AppResult`, `DispatcherProvider`, `CoroutinesQualifiers`).

### Estrutura Real do Repositório

```text
.
├── .env.example                      # Modelo de variáveis locais (placeholder comentado)
├── .gitignore                        # Regras de exclusão Git
├── AI_USAGE.md                       # Registro transparente do uso de ferramentas de IA
├── PLANNING.md                       # Planejamento evolutivo, fases, marcos e riscos
├── README.md                         # Apresentação geral do repositório (este arquivo)
├── SPECIFICATION.md                  # Especificação funcional, não funcional e técnica
├── TASKS.md                          # Decomposição rastreável de tarefas (DOC/VAL/TEC/TER/FUT)
├── TEST_RESULTS.md                   # Consolidação auditável de testes, artefatos e checklist externo
├── build.gradle.kts                  # Configuração Gradle de nível raiz
├── debug.keystore.base64             # Material de keystore Debug do ambiente
├── gradle.properties                 # Parâmetros de build JVM/Gradle/Kotlin
├── gradlew                           # Script wrapper Unix executável (-rwxr-xr-x)
├── gradlew.bat                       # Script wrapper Windows
├── metadata.json                     # Metadados do projeto no ambiente AI Studio
├── settings.gradle.kts               # Definição do projeto AcervoProvisorio (:app)
├── docs/
│   ├── api-v1-contract.md            # Contrato histórico/legado da antiga API v1 (não usada no cliente atual)
│   └── arcgis-territories-api.md     # Documentação técnica da Etapa 1 da API ArcGIS REST (IBAMA Camada 25)
├── gradle/
│   ├── libs.versions.toml            # Catálogo central de versões e dependências
│   └── wrapper/                      # Wrapper configurado para Gradle 9.3.1
└── app/
    ├── build.gradle.kts              # Configuração do módulo :app (SDK, assinatura, dependências)
    ├── proguard-rules.pro            # Regras ProGuard/R8
    ├── schemas/
    │   └── com.example.data.local.database.ParintintinDatabase/
    │                                 # Schemas JSON exportados do Room (2.json, 3.json, 4.json)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── assets/
        │   │   └── dictionary_seed.json
        │   ├── java/com/example/
        │   │   ├── MainActivity.kt
        │   │   ├── ParintintinApplication.kt
        │   │   ├── core/common/
        │   │   ├── data/
        │   │   │   ├── connectivity/
        │   │   │   ├── local/{dao,database,entity,seed}/
        │   │   │   ├── mapper/
        │   │   │   ├── remote/{api,datasource,dto,model}/
        │   │   │   ├── repository/
        │   │   │   └── sync/
        │   │   ├── di/
        │   │   ├── domain/{connectivity,model,repository,sync,usecase}/
        │   │   ├── presentation/{components,dictionary,encyclopedia,home,navigation,territories}/
        │   │   └── ui/theme/
        │   └── res/                  # Drawables, fontes locais, mipmaps, strings e XMLs
        ├── debug/                    # Manifesto e recursos específicos de Debug
        ├── test/                     # 35 suítes automatizadas executáveis na JVM (303 testes)
        └── androidTest/              # 5 arquivos Kotlin / 3 @Test (atualizados para ArcGIS /query? e compilados com :app:compileDebugAndroidTestKotlin)
```

*(Nota de transparência: o diretório do repositório também contém pacotes de transferência gerados anteriormente — `Voz-Ancestral-debug.zip` e `Voz-Ancestral-Fonte-Etapa-*.zip/.b64` — preservados sem alteração.)*

---

## 7. Requisitos para Desenvolvimento

- **Android mínimo (`minSdk`):** API `24` (Android 7.0)
- **`compileSdk` / `targetSdk`:** `36` (`minorApiLevel = 1`)
- **Java Target (`compileOptions`):** `11` (`JavaVersion.VERSION_11`)
- **Ferramentas necessárias:** Android Studio com suporte a **AGP `9.1.1`**, **Android SDK Platform `36`**, **JDK 17+** para executar o **Gradle `9.3.1`**.

---

## 8. Como Executar

### 1. Abrir e sincronizar no Android Studio
1. Abra a pasta raiz do repositório (contendo `settings.gradle.kts`) no **Android Studio**.
2. Execute **Sync Project with Gradle Files** para resolver dependências e configurar o KSP.

### 2. Compilar e executar o aplicativo (Debug)
```bash
./gradlew :app:assembleDebug
```
*(Ou `gradle :app:assembleDebug`.)* O APK Debug gerado fica em `app/build/outputs/apk/debug/app-debug.apk`.
- **Assinatura Release:** `:app:assembleRelease` requer keystore própria (`KEYSTORE_PATH` / `my-upload-key.jks`) e variáveis de assinatura (`STORE_PASSWORD`, `KEY_PASSWORD`) externas ao repositório.

### 3. Executar testes automatizados na JVM e compilar testes instrumentados
```bash
./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin
```
*(Em ambientes headless onde o cache offline do Robolectric esteja armazenado em `/home/appuser/.m2` enquanto o shell executa como `root`, utilize `JAVA_TOOL_OPTIONS="-Duser.home=/home/appuser" gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin`.)*

Para executar a suíte instrumentada em um emulador ou aparelho Android conectado via ADB:
```bash
./gradlew :app:connectedDebugAndroidTest
```

---

## 9. Testes e Validação

Os resultados detalhados de verificação estão consolidados em [`TEST_RESULTS.md`](TEST_RESULTS.md):

- **Testes automatizados na JVM (`:app:testDebugUnitTest`) — APROVADOS:**
  - **303 testes aprovados em 35 suítes** (`0` falhas, `0` erros, `0` ignorados), executados neste ambiente com `JAVA_TOOL_OPTIONS="-Duser.home=/home/appuser" gradle :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin`.
  - Cobrem domínio, mappers, casos de uso, validação/hash SHA-256 do seed, migrações Room (`1->3`, `2->3`, `3->4`), `IndigenousLandDao` e `IndigenousLandQueryBuilder` em SQLite real via Robolectric (incluindo acentuação, curingas `LIKE`, filtros combináveis e ordenação com nulos/vazios/espaços sempre ao final), contratos HTTP ArcGIS com `MockWebServer`, paginação/deduplicação/fallback, ViewModels, `TerritoriesScreen` e navegação de 3 abas em `ParintintinNavHostTest`.
- **Compilação de testes instrumentados (`:app:compileDebugAndroidTestKotlin`) — APROVADA (NÃO EXECUTADA EM DISPOSITIVO):**
  - `StartupSeedHiltInstrumentedTest` e `ExampleInstrumentedTest` foram compilados com sucesso após atualização de `TestNetworkModule` e dos testes para o endpoint ArcGIS `/query?`, porém **não executados** neste container por falta de emulador e ADB.
- **Confirmações externas reportadas pelo responsável (não reproduzíveis localmente neste container offline):**
  - Sincronização e validação de **627 registros territoriais** contra a API ArcGIS REST do IBAMA/SIGAGEO.
  - Execução e aprovação do pipeline no **GitHub Actions** (não verificável diretamente no container por ausência de `.git` e `.github/workflows/`).

---

## 10. Documentação Complementar

| Documento | Finalidade |
|---|---|
| [`SPECIFICATION.md`](SPECIFICATION.md) | Especificação funcional, não funcional, regras de negócio (`RN-01`–`RN-14`), arquitetura, escopo do catálogo territorial e limitações. |
| [`PLANNING.md`](PLANNING.md) | Planejamento retrospectivo e evolutivo (Fases 1–17, Marcos `M1`–`M16`), metodologia SDD, dependências e riscos. |
| [`TASKS.md`](TASKS.md) | Inventário rastreável das **35 tarefas** entre documentação (`DOC`), validação (`VAL`), saneamento técnico (`TEC`), catálogo territorial (`TER`) e evoluções futuras (`FUT`). |
| [`TEST_RESULTS.md`](TEST_RESULTS.md) | Consolidação auditável das **35 suítes JVM (303 testes)**, compilação de `androidTest`, artefatos de build e pendências externas. |
| [`AI_USAGE.md`](AI_USAGE.md) | Registro transparente do uso de ferramentas de IA, responsabilidades humanas e limites acadêmicos. |
| [`docs/arcgis-territories-api.md`](docs/arcgis-territories-api.md) | Especificação técnica inicial (Etapa 1) da consulta paginada à camada 25 do Geoportal IBAMA / SIGAGEO. |
| [`docs/api-v1-contract.md`](docs/api-v1-contract.md) | Especificação histórica/legada da antiga API v1 de acervo linguístico (desativada no cliente atual). |
