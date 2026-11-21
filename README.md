# Voz Ancestral — Acervo Provisório (v1.0 Demonstrativa)

Aplicativo Android desenvolvido no âmbito do projeto acadêmico **Voz Ancestral**, voltado à organização, preservação digital e consulta *offline-first* de registros linguísticos e culturais relacionados à língua **Parintintin**.

> **Aviso de Escopo (Versão 1.0 Demonstrativa):** Esta versão possui caráter **acadêmico e estritamente demonstrativo**. A interface visível ao usuário está concentrada nas telas **Início** e **Dicionário**, utilizando **20 registros fictícios em português** (`DEMO-001` a `DEMO-020`) para validação técnica da arquitetura, da persistência local e do comportamento de sincronização. **Nenhum registro desta versão representa vocabulário autêntico da língua Parintintin.**

---

## 1. Identificação do Projeto

| Atributo | Informação Verificada |
|---|---|
| **Projeto Acadêmico** | **Voz Ancestral** |
| **Nome do Aplicativo** | **Acervo Provisório** (`AcervoProvisorio` no Gradle) |
| **Versão Atual** | **1.0 Demonstrativa** (`versionCode = 1`, `versionName = "1.0"`) |
| **Identificador Android (`applicationId`)** | `com.aistudio.parintintin.kagwah` |
| **Namespace Kotlin/Android** | `com.example` |
| **Natureza do Artefato** | Protótipo funcional acadêmico e demonstrativo (*Offline-First*), não publicado em loja oficial |

---

## 2. Apresentação

Iniciativas de documentação e preservação digital de línguas indígenas enfrentam desafios recorrentes de acesso contínuo ao conteúdo em regiões com conectividade limitada ou intermitente, além da exigência ética e técnica de validação especializada e autorização formal antes da publicação de dados linguísticos e culturais autênticos.

Nesse contexto, o projeto **Voz Ancestral** propõe o aplicativo **Acervo Provisório** como base tecnológica Android projetada segundo a estratégia **Offline-First**. Na **Versão 1.0 Demonstrativa**, o aplicativo consolida a infraestrutura local em banco SQLite/Room, a carga controlada de dados demonstrativos versionados, os mecanismos de pesquisa e filtragem e o monitoramento reativo de conectividade e sincronização remota, sem depender de servidor ativo para consulta e sem expor vocabulário não autorizado.

---

## 3. Funcionalidades Disponíveis

A navegação principal da Versão 1.0 Demonstrativa disponibiliza exclusivamente dois destinos na barra inferior (**Início** e **Dicionário**):

- **Tela Home (`Início`):**
  - Exibe o banner de identificação `"VERSÃO 1.0 • DEMONSTRATIVA"` e o aviso permanente `"MODO DEMONSTRATIVO"`.
  - Apresenta esclarecimento sobre o acervo em construção e ausência de vocabulário autêntico nesta etapa.
  - Exibe o card **Armazenamento Local (Offline-First)** (`OfflineArchiveStatusCard`), com contagem de verbetes e artigos no banco local, status atual da sincronização, histórico de última tentativa/última sincronização oficial e botão manual de sincronização.
  - Lista módulos futuros de forma puramente informativa com o selo `"Planejado"` (sem navegação) e oferece cartão expansível com detalhes técnicos da etapa inicial.
- **Dicionário Demonstrativo (`Dicionário`):**
  - Lista em ordem alfabética (`COLLATE NOCASE ASC`) os 20 verbetes locais (`DEMO-001` a `DEMO-020`), exibindo contagem total e selo de consulta offline via Room.
  - Mantém cabeçalho fixo (`stickyHeader`) `"MODO DEMONSTRATIVO"` e identifica os itens com os selos `"Termo demonstrativo"` e `"Nota demonstrativa"`.
- **Pesquisa Textual:**
  - Busca reativa em tempo real com *debounce* de `150 ms` sobre termo, tradução em português e nota cultural via consulta SQL `LIKE`, acompanhada de botão para limpeza rápida da busca.
- **Filtros por Categoria Semântica e Classe Gramatical:**
  - Seleção combinável por chips horizontais para **6 categorias semânticas** (`SemanticCategory`) e **7 classes gramaticais** (`GrammaticalClass`), integrados à pesquisa textual e com opção de limpeza global de filtros.
  - Exibição explícita de estados visuais de carregamento, lista vazia (geral ou por filtro ativo) e erro de consulta com ação de nova tentativa.
- **Persistência Local com Room (Schema v3):**
  - Banco `parintintin_archive.db` com migrações explícitas (`1 -> 2` e `2 -> 3`), exportação de schemas JSON (`2.json`, `3.json`) e inicialização/atualização transacional e idempotente de `app/src/main/assets/dictionary_seed.json` controlada por versão e hash **SHA-256**.
- **Funcionamento *Offline-First*:**
  - A interface observa exclusivamente fluxos (`Flow`) do Room; falhas de rede ou respostas remotas vazias (`HTTP 200 []`) não bloqueiam a consulta nem apagam registros locais, favoritos ou o histórico de sincronização oficial prévia.
  - Caso já exista sincronização oficial registrada (`lastSuccessfulSyncEpochMillis != null`), novas atualizações do seed demonstrativo são bloqueadas (`BLOCKED_BY_OFFICIAL_SYNC`).
- **Estados de Conectividade e Sincronização:**
  - Observação contínua da rede via `ConnectivityManager.NetworkCallback` (`AndroidNetworkConnectivityObserver`) e coordenação com deduplicação em `@ApplicationScope` (`ArchiveSyncCoordinatorImpl`).
  - Transições tipadas de `SyncState` (`Loading`, `Idle`, `Syncing`, `Synced`, `RemoteEmpty` e `OfflineFallback`) e classificação de falhas entre `NO_INTERNET`, `UNVALIDATED_NETWORK` e `SERVER_UNAVAILABLE`.

---

## 4. Limitações da Versão 1.0

- **Registros fictícios em português:** os 20 registros `DEMO-001` a `DEMO-020` presentes em `dictionary_seed.json` são termos e expressões demonstrativos fictícios em português (ex.: *Riolume*, *Folhaverde*, *Brisamata*).
- **Ausência de vocabulário autêntico:** nenhum dos verbetes desta versão representa vocabulário autêntico da língua Parintintin.
- **Ausência de áudio e pronúncia autêntica:** todos os registros possuem `phonetic_ipa`, `example_parintintin` e `example_portuguese` vazios e não há arquivos sonoros, URLs de áudio ou reprodutor de mídia implementado.
- **Sincronização restrita ao dicionário:** `ArchiveSyncRepositoryImpl.syncArchive()` sincroniza apenas `/v1/dictionary.json`; o endpoint `/v1/manifest.json` não é consumido no fluxo atual, a Enciclopédia permanece fora da sincronização e não há política implementada de exclusão remota de registros.
- **Disponibilidade do servidor oficial não confirmada:** o cliente aponta para `https://archive.parintintin.org/`, porém não foi possível confirmar a disponibilidade operacional de um servidor oficial ativo durante o desenvolvimento e as auditorias; os testes HTTP utilizaram exclusivamente `MockWebServer`.
- **Funcionalidades fora da v1.0:** **Enciclopédia Cultural**, **interface funcional de Favoritos**, **tela de detalhes dos verbetes**, **reprodução de áudio** e **Acervo Oficial** não estão disponíveis ao usuário nesta versão.
- **Artefato de distribuição e execução em dispositivo:** apenas o build **Debug** v1.0 foi gerado e verificado (`app-debug.apk` / `.build-outputs/Parintintin-v1.0-debug.apk`); não há assinatura **Release** configurada no ambiente, nem execução comprovada em aparelho Android físico neste container.

---

## 5. Tecnologias Utilizadas

Todas as versões abaixo foram confirmadas em `gradle/libs.versions.toml`, `app/build.gradle.kts` e `gradle/wrapper/gradle-wrapper.properties`:

| Categoria | Tecnologia | Versão Comprovada |
|---|---|---|
| **Linguagem** | Kotlin | `2.2.10` |
| **Interface Declarativa** | Jetpack Compose + Material 3 (`Navigation Compose`) | Compose BOM `2024.09.00` \| Navigation `2.8.9` |
| **Arquitetura** | Clean Architecture + MVVM | Pacotes `presentation`, `domain`, `data`, `di`, `core` |
| **Injeção de Dependências** | Dagger Hilt e `hilt-navigation-compose` | Dagger Hilt: `2.59`; `hilt-navigation-compose`: `1.2.0` |
| **Persistência Local** | AndroidX Room (`room-runtime`, `room-ktx`) | `2.7.0` (Schema v3) |
| **Concorrência e Fluxos** | Kotlin Coroutines + `Flow` / `StateFlow` | `1.10.2` |
| **Rede e HTTP** | Retrofit + OkHttp (`logging-interceptor`) | Retrofit `2.12.0` \| OkHttp `4.10.0` |
| **Serialização** | Moshi (codegen) + Kotlinx Serialization JSON | Moshi `1.15.2` \| Serialization `1.8.0` |
| **Build e Processamento** | Gradle + Android Gradle Plugin (AGP) + KSP | Gradle `9.3.1` \| AGP `9.1.1` \| KSP `2.3.5` |
| **Testes Automatizados** | JUnit, Coroutines Test, Robolectric, Compose UI Test, MockWebServer, Hilt Testing, Espresso | JUnit `4.13.2`, Robolectric `4.16.1`, MockWebServer `4.10.0`, Hilt Testing `2.59`, Espresso `3.7.0` |

*Nota:* Bibliotecas opcionais presentes apenas como linhas comentadas em `app/build.gradle.kts` (como Firebase, CameraX, Coil, DataStore e Play Services Location) não fazem parte da implementação ativa da v1.0.

---

## 6. Arquitetura do Projeto

O aplicativo adota **Clean Architecture** combinada ao padrão **MVVM**:

- **`presentation` e `ui.theme`:** telas e componentes Jetpack Compose (`HomeScreen`, `DictionaryScreen`, `PreserveComponents`, `ParintintinNavHost`, `ParintintinBottomBar`), tema Material 3 e `@HiltViewModel`s (`HomeViewModel`, `DictionaryViewModel`) expondo estados imutáveis via `StateFlow`. *(Inclui o pacote preparatório `encyclopedia`, não exposto na navegação da v1.0.)*
- **`domain`:** modelos puros Kotlin (`DictionaryEntry`, `CulturalArticle`, `SyncState`, `DeviceConnectivityState`), contratos de repositório e conectividade, casos de uso (`ArchiveUseCases`), coordenador de sincronização (`ArchiveSyncCoordinator`) e classificador de falhas (`SyncFailureClassifier`), sem dependência de Android SDK, Room ou Retrofit.
- **`data`:** implementações concretas de persistência local (`ParintintinDatabase`, DAOs, migrações e engine de seed `VersionedDictionarySeedUpdater`), cliente HTTP/Moshi (`ParintintinApiService`, `RemoteArchiveDataSource`), observador `ConnectivityManager` (`AndroidNetworkConnectivityObserver`) e repositórios offline-first (`OfflineFirstDictionaryRepository`, `ArchiveSyncRepositoryImpl`, além do preparatório `OfflineFirstEncyclopediaRepository`). Registra-se apenas o acoplamento pontual de classes de `data` ao qualificador `@ApplicationScope` definido no pacote `di`.
- **`di` e `core.common`:** módulos Hilt (`DatabaseModule`, `NetworkModule`, `RepositoryModule`, `SeedModule`, `SyncModule`, `ConnectivityModule`, `CoroutinesModule`) e utilitários compartilhados (`AppResult`, `DispatcherProvider`).

### Estrutura Real do Repositório

```text
.
├── AI_USAGE.md                       # Registro transparente do uso de ferramentas de IA
├── PLANNING.md                       # Planejamento retrospectivo, fases, marcos e riscos
├── README.md                         # Apresentação geral do repositório (este arquivo)
├── SPECIFICATION.md                  # Especificação funcional, não funcional e técnica
├── TASKS.md                          # Decomposição rastreável de tarefas (DOC/VAL/TEC/FUT)
├── TEST_RESULTS.md                   # Consolidação auditável de testes e artefatos
├── build.gradle.kts                  # Configuração Gradle de nível raiz
├── gradle.properties                 # Parâmetros de build JVM/Gradle/Kotlin
├── metadata.json                     # Metadados do projeto no ambiente AI Studio
├── settings.gradle.kts               # Definição do projeto AcervoProvisorio (:app)
├── docs/
│   └── api-v1-contract.md            # Proposta de contrato da API REST v1
├── gradle/
│   ├── libs.versions.toml            # Catálogo central de versões e dependências
│   └── wrapper/
│       └── gradle-wrapper.properties # Configuração de distribuição Gradle 9.3.1
└── app/
    ├── build.gradle.kts              # Configuração do módulo :app (SDK, assinatura, dependências)
    ├── proguard-rules.pro            # Regras ProGuard/R8
    ├── schemas/
    │   └── com.example.data.local.database.ParintintinDatabase/
    │                                 # Schemas JSON exportados do Room (2.json, 3.json)
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
        │   │   │   ├── remote/
        │   │   │   ├── repository/
        │   │   │   └── sync/
        │   │   ├── di/
        │   │   ├── domain/{connectivity,model,repository,sync,usecase}/
        │   │   ├── presentation/{components,dictionary,encyclopedia,home,navigation}/
        │   │   └── ui/theme/
        │   └── res/                  # Drawables, fontes locais, mipmaps, strings e XMLs
        ├── debug/                    # Manifesto e recursos específicos de Debug
        ├── test/                     # 24 suítes automatizadas executáveis na JVM
        └── androidTest/              # Suítes instrumentadas Hilt/Compose (não executadas)
```

---

## 7. Requisitos para Desenvolvimento

Configurações verificadas no projeto (`app/build.gradle.kts` e `gradle/wrapper/gradle-wrapper.properties`):

- **Android mínimo (`minSdk`):** API `24` (Android 7.0)
- **`compileSdk`:** `36` (`minorApiLevel = 1`)
- **`targetSdk`:** `36`
- **Java Target (`compileOptions`):** `11` (`JavaVersion.VERSION_11`) — configuração do bytecode gerado para o aplicativo (não representa a versão mínima de JDK exigida para executar o Gradle)
- **Ferramentas necessárias para abrir e compilar:**
  - **Android Studio** com suporte ao **Android Gradle Plugin `9.1.1`** e **Android SDK Platform `36`** instalados;
  - **JDK 17 ou superior** para executar o **Gradle `9.3.1`** (distinta da configuração de compilação `Java Target 11` do aplicativo);
  - **Gradle `9.3.1`** instalado no ambiente caso deseje compilar via linha de comando (ou uso da distribuição configurada pelo Android Studio conforme `gradle/wrapper/gradle-wrapper.properties`).

---

## 8. Como Executar

### 1. Clonar o repositório
```bash
git clone https://github.com/GassaTech/voz-ancestral-.git
cd voz-ancestral-
```

### 2. Abrir no Android Studio
1. Inicie o **Android Studio** e selecione **Open** (*Abrir*).
2. Aponte para a pasta raiz do repositório (que contém `settings.gradle.kts`).

### 3. Sincronizar o Gradle
- No Android Studio, execute **Sync Project with Gradle Files** para resolver dependências e configurar o projeto no Gradle (o processamento de anotações via KSP ocorre durante as tarefas de build pertinentes, como compilação e testes).
- *Observação técnica:* o repositório inclui `gradle/wrapper/gradle-wrapper.properties` (`gradle-9.3.1-bin.zip`), porém os arquivos `gradlew`, `gradlew.bat` e `gradle-wrapper.jar` não estão presentes na raiz. Em linha de comando, utilize o executável `gradle` compatível instalado no sistema.

### 4. Executar no emulador ou dispositivo Android
- **Via Android Studio:** selecione um emulador ou dispositivo físico conectado via USB/ADB (API `24` ou superior) e clique em **Run (`app`)**.
- **Via linha de comando (build Debug):**
  ```bash
  gradle :app:assembleDebug
  ```
  O APK Debug gerado ficará disponível em `app/build/outputs/apk/debug/app-debug.apk`.
- **Assinatura Release:** o build `:app:assembleRelease` requer keystore própria (`KEYSTORE_PATH` / `my-upload-key.jks`) e variáveis de assinatura (`STORE_PASSWORD`, `KEY_PASSWORD`) não incluídas no repositório.

---

## 9. Testes e Validação

Os resultados detalhados de verificação estão documentados em [`TEST_RESULTS.md`](TEST_RESULTS.md):

- **Testes automatizados na JVM (`gradle :app:testDebugUnitTest`) — APROVADOS:**
  - **233 testes aprovados em 24 suítes** (`0` falhas, `0` erros e `0` ignorados).
  - Abrangem lógica pura de domínio, validação e hash SHA-256 do seed, migrações e persistência SQLite/Room, desserialização e contratos HTTP com `MockWebServer`, observação de conectividade, coordenação de sincronização, ViewModels, navegação e UI Compose via Robolectric (`sdk = 36`).
- **Testes instrumentados Android (`app/src/androidTest`) — COMPILADOS, NÃO EXECUTADOS:**
  - As suítes `StartupSeedHiltInstrumentedTest` e `ExampleInstrumentedTest` (com `HiltTestRunner`, `TestDatabaseModule` e `TestNetworkModule`) foram compiladas via `:app:assembleDebugAndroidTest`, mas **não foram executadas** em emulador ou dispositivo físico neste ambiente devido à ausência de emulador e de acesso ADB.
- **Pendências externas de validação:**
  - Execução de `gradle :app:connectedDebugAndroidTest` em dispositivo ou emulador real, testes manuais em hardware físico, auditoria prática com TalkBack, *benchmarks* de desempenho e validação contra servidor oficial ativo permanecem pendentes.

---

## 10. Documentação Complementar

| Documento | Finalidade |
|---|---|
| [`SPECIFICATION.md`](SPECIFICATION.md) | Especificação funcional, requisitos (`RF`, `RNF`), regras de negócio (`RN`), arquitetura e limitações da v1.0. |
| [`PLANNING.md`](PLANNING.md) | Planejamento retrospectivo, metodologia SDD, 11 fases, 10 marcos (`M1`–`M10`), riscos e prioridades. |
| [`TASKS.md`](TASKS.md) | Decomposição rastreável das 23 tarefas entre documentação (`DOC`), validação (`VAL`), saneamento técnico (`TEC`) e evolução futura (`FUT`). |
| [`TEST_RESULTS.md`](TEST_RESULTS.md) | Registro auditável das 24 suítes JVM (233 testes), compilação de `androidTest`, verificação do APK Debug e cenários não validados. |
| [`AI_USAGE.md`](AI_USAGE.md) | Registro transparente do uso de ferramentas de Inteligência Artificial, responsabilidades humanas e limites acadêmicos. |
| [`docs/api-v1-contract.md`](docs/api-v1-contract.md) | Especificação proposta para o futuro contrato JSON/HTTP da API v1. |

---

## 11. Estado Atual e Trabalhos Futuros

| Situação | Itens Abrangidos |
|---|---|
| **Implementado (v1.0 Demonstrativa)** | Telas **Início** e **Dicionário**, busca textual com *debounce*, filtros por categoria semântica e classe gramatical, banco Room v3 com migrações, seed demonstrativo versionado por SHA-256 (`DEMO-001`..`DEMO-020`), observação de conectividade, coordenação de sincronização offline-first e 233 testes aprovados na JVM. |
| **Parcialmente Implementado / Estruturas Preparatórias** | Módulo `Encyclopedia` presente no código sem exposição na barra inferior nem sincronização (com saneamento `TEC-001` de `CancellationException` pendente em `EncyclopediaViewModel.kt`); campos/use cases de favoritos e detalhes sem UI na v1.0; endpoint `/v1/manifest.json` declarado sem integração; suíte `androidTest` apenas compilada; ajuste pendente na Seção 6.3 de `docs/api-v1-contract.md`. |
| **Planejado (Pós-v1.0)** | Obtenção de conteúdo linguístico e cultural autêntico devidamente validado e autorizado; disponibilização operacional de backend HTTPS oficial; integração do manifesto e definição da política de exclusão remota; ativação da Enciclopédia Cultural, interface de Favoritos, tela de Detalhes e reprodução de áudio; execução em dispositivo físico e empacotamento Release assinado. |

---

## 12. Considerações Finais

A **Versão 1.0 Demonstrativa** do aplicativo **Acervo Provisório** (*projeto Voz Ancestral*) cumpre o papel de prova técnica e arquitetural em modo *offline-first*, demonstrando a viabilidade do armazenamento local versionado, da filtragem offline e da sinalização clara de conectividade e sincronização.

Reitera-se que o conteúdo presente nesta entrega é **exclusivamente demonstrativo e fictício em português**. A futura incorporação de vocabulário, transcrições fonéticas, exemplos bilíngues, áudios e artigos culturais autênticos da língua **Parintintin** dependerá obrigatoriamente de curadoria linguística especializada, validação técnica e autorização formal comunitária e institucional adequadas.
