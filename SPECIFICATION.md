# SPECIFICATION.md — Projeto Acadêmico Voz Ancestral (Aplicativo Acervo Provisório)

> **Nota de Natureza Documental:** Esta especificação foi **reconstruída retrospectivamente** a partir da inspeção do código-fonte, dos recursos, das configurações Gradle e da auditoria técnica da **Versão 1.0 Demonstrativa**. Este documento não foi redigido previamente ao desenvolvimento e descreve com exatidão apenas os comportamentos comprovados na implementação atual, separando-os explicitamente das funcionalidades parciais e planejadas.

---

## 1. Identificação

| Atributo | Definição Oficial / Verificada | Origem no Projeto |
|---|---|---|
| **Nome do Projeto Acadêmico** | **Voz Ancestral** | Escopo acadêmico definido para documentação |
| **Nome do Aplicativo** | **Acervo Provisório** | `app/src/main/res/values/strings.xml` (`app_name`), `metadata.json` (`name`) |
| **Nome do Módulo/Raiz Gradle** | `AcervoProvisorio` (`:app`) | `settings.gradle.kts` |
| **Tema** | Preservação digital e documentação da língua Parintintin | `app/src/main/res/values/strings.xml` (`home_hero_title`, `home_hero_subtitle`) |
| **Versão Atual** | **1.0 Demonstrativa** (`versionCode = 1`, `versionName = "1.0"`) | `app/build.gradle.kts`, `strings.xml` (`home_provisional_badge`) |
| **Identificador Android (`applicationId`)** | `com.aistudio.parintintin.kagwah` | `app/build.gradle.kts` |
| **Namespace Kotlin/Android** | `com.example` | `app/build.gradle.kts` |
| **Compatibilidade SDK** | `minSdk = 24` (Android 7.0) \| `targetSdk = 36` \| `compileSdk = 36` | `app/build.gradle.kts` |
| **Natureza da Versão** | Demonstrativa com registros fictícios em português | `strings.xml`, `app/src/main/assets/dictionary_seed.json` |

### Descrição Resumida
O aplicativo **Acervo Provisório**, vinculado ao projeto acadêmico **Voz Ancestral**, é um cliente Android *offline-first* voltado à organização, preservação digital e consulta local de registros linguísticos e culturais da língua Parintintin. Na **versão 1.0 demonstrativa**, o aplicativo disponibiliza as telas **Início** (*Home*) e **Dicionário**, utilizando exclusivamente 20 verbetes fictícios em português (`DEMO-001` a `DEMO-020`) armazenados em banco SQLite/Room local e preparados para futura sincronização com o acervo remoto oficial (cuja disponibilidade operacional não foi confirmada no ambiente de auditoria).

---

## 2. Contexto e Problema

Iniciativas de documentação e preservação de línguas indígenas enfrentam desafios técnicos relacionados à disponibilidade contínua de materiais linguísticos organizados, à consulta em ambientes com conectividade limitada ou intermitente e à necessidade de validação e autorização adequadas antes da publicação de conteúdo autêntico.

No escopo específico deste projeto:
- O aplicativo busca estruturar uma base digital local capaz de operar sem dependência imediata de conexão ativa com a internet (**estratégia *Offline-First***), conforme refletido em `HomeScreen.kt`, `DictionaryScreen.kt`, `OfflineFirstDictionaryRepository.kt` e `ParintintinDatabase.kt`.
- Como o acervo linguístico e cultural autêntico ainda se encontra em preparação e depende de validação e autorização adequadas (`strings.xml`, `home_intro_body`), a versão 1.0 demonstrativa adota uma base controlada de registros fictícios para validar a arquitetura, o armazenamento local, os filtros de consulta e o comportamento de sincronização sem expor dados não autorizados.

---

## 3. Objetivo Geral

Desenvolver a base técnica e funcional de um aplicativo Android *offline-first* no contexto do projeto acadêmico **Voz Ancestral** (`Acervo Provisório`), demonstrando a arquitetura, a persistência local versionada, a consulta offline de verbetes e o monitoramento reativo de conectividade e sincronização para futura preservação e divulgação da língua Parintintin.

---

## 4. Objetivos Específicos

1. **Estruturar a arquitetura em camadas (Clean Architecture + MVVM):** separar responsabilidades entre `presentation`, `domain` e `data`, com injeção de dependências via Hilt e fluxos reativos via Kotlin Coroutines e `Flow`/`StateFlow`.
2. **Garantir operação *Offline-First* com persistência local em Room:** disponibilizar consulta contínua aos verbetes locais independentemente da disponibilidade de rede ou servidor remoto.
3. **Controlar a carga inicial e atualizações da base demonstrativa:** validar, calcular hash SHA-256 e aplicar transacionalmente o seed empacotado (`dictionary_seed.json`), preservando marcações locais e protegendo eventuais dados oficiais já sincronizados.
4. **Oferecer interface de consulta na versão 1.0 demonstrativa:** permitir listagem, pesquisa textual com *debounce* e filtragem combinada por categoria semântica e classe gramatical na tela **Dicionário**, com sinalização explícita de **MODO DEMONSTRATIVO**.
5. **Monitorar conectividade e classificar falhas de sincronização:** distinguir na tela **Início** a ausência de conexão, a rede sem validação de internet, a indisponibilidade do servidor remoto e a resposta remota vazia (`HTTP 200 []`), preservando o histórico de sincronização oficial quando existente.
6. **Preparar evolução modular para etapas futuras:** manter contratos, entidades e interfaces estruturados para futura integração de servidor oficial, manifesto de versão, Enciclopédia Cultural, favoritos na interface, tela de detalhes e reprodução de áudio.

---

## 5. Público-Alvo

> **Registro de Ausência Documental:** **Não foi possível comprovar** a existência de um público-alvo formalmente especificado no código-fonte, nos recursos de texto ou em documentação prévia do repositório (`README.md` e documentos prévios não existiam no momento desta reconstrução).

**Proposta indicativa (não formalmente validada no código):** considerando o tema declarado em `strings.xml` e `metadata.json` (*documentação linguística, memória cultural e consulta offline*), os potenciais usuários futuros de uma versão com acervo oficial validado podem incluir estudantes, pesquisadores, educadores e membros da comunidade interessados na língua e cultura Parintintin, além de avaliadores acadêmicos na versão 1.0 demonstrativa. Esta indicação deve ser tratada apenas como proposta a ser formalmente definida na documentação de produto.

---

## 6. Escopo da Versão 1.0

A barra de navegação principal (`AppDestination.primaryDestinations` em `AppDestination.kt` e `ParintintinBottomBar.kt`) expõe exclusivamente dois destinos na versão 1.0 demonstrativa: **Início (`Home`)** e **Dicionário (`Dictionary`)**.

Estão efetivamente disponíveis ao usuário na versão 1.0:

1. **Tela Home (`HomeScreen` / `HomeViewModel`):**
   - Banner principal com identificação `"VERSÃO 1.0 • DEMONSTRATIVA"` e título `"Preservação da Língua Parintintin"`.
   - Card permanente `"MODO DEMONSTRATIVO"` informando que os dados são fictícios.
   - Card `"Acervo em construção"` explicando a ausência de vocabulário autêntico nesta versão.
   - Card `"Armazenamento Local (Offline-First)"` (`OfflineArchiveStatusCard`) exibindo contagem local de verbetes e artigos no Room, estado atual de sincronização, histórico de última tentativa/última sincronização oficial e botão de ação manual (`"Sincronizar agora"` / `"Sincronizando…"` / `"Tentar novamente"`).
   - Seção `"Módulos Planejados"` exibindo cards estáticos informativos com selo `"Planejado"` (sem navegação).
   - Cartão expansível `"Detalhes da Etapa Inicial"` com suporte a estado expandido/recolhido e descrição acessível.
2. **Dicionário Demonstrativo (`DictionaryScreen` / `DictionaryViewModel`):**
   - Cabeçalho com contagem total de verbetes armazenados localmente e selo `"Consulta offline via Room"`.
   - `stickyHeader` permanente `"MODO DEMONSTRATIVO"` acima da listagem.
   - Exibição dos 20 verbetes demonstrativos ordenados alfabeticamente (`ORDER BY parintintin_word COLLATE NOCASE ASC`), com selo `"Termo demonstrativo"`, palavra, tradução em português, chips de classe gramatical e categoria semântica, e seção `"Nota demonstrativa"`.
3. **Pesquisa Textual (`DictionaryViewModel` + `DictionaryDao`):**
   - Campo de busca reativo com *debounce* de `150 ms` (`SEARCH_DEBOUNCE_MILLIS`), botão de limpar pesquisa e consulta SQL `LIKE` sobre `parintintin_word`, `portuguese_translation` e `cultural_note`.
4. **Filtros por Categoria Semântica e Classe Gramatical:**
   - Chips de seleção horizontal (`LazyRow`) para 6 categorias semânticas (`SemanticCategory`) e 7 classes gramaticais (`GrammaticalClass`), combináveis entre si e com a pesquisa textual, com opção de alternar seleção ou limpar todos os filtros.
5. **Persistência Local (Room v3):**
   - Banco `parintintin_archive.db` com entidades `DictionaryEntryEntity`, `CulturalArticleEntity`, `SyncMetadataEntity` e `SeedMetadataEntity`, migrações `1 -> 2` e `2 -> 3` e `TypeConverter` para listas de strings.
6. **Inicialização por Seed JSON Versionado:**
   - Leitura de `app/src/main/assets/dictionary_seed.json`, validação estrita de schema e regras, cálculo de SHA-256 bruto e aplicação transacional na tabela `dictionary_entries` e `seed_metadata`.
7. **Estratégia Offline-First e Estados de Sincronização/Conectividade:**
   - A interface lê exclusivamente fluxos (`Flow`) do Room; a sincronização automática é disparada uma vez por ciclo de vida do processo em `HomeViewModel.init` via `ArchiveSyncCoordinatorImpl`, atualizando `SyncState` e classificando falhas conforme `AndroidNetworkConnectivityObserver`.
8. **Navegação entre Home e Dicionário:**
   - Navegação tipada com `Navigation Compose` (`@Serializable`), `launchSingleTop = true`, `saveState = true` e `restoreState = true` via `ParintintinNavHost.kt`.

---

## 7. Funcionalidades Demonstrativas e Limitações

- **Quantidade e identificação dos registros:** O arquivo `app/src/main/assets/dictionary_seed.json` contém exatamente **20 registros**, identificados sequencialmente de **`DEMO-001` a `DEMO-020`** (`CURRENT_SEED_VERSION = 2` em `DictionarySeedSpec.kt`).
- **Natureza fictícia em português:** Todos os 20 registros são **Termos e expressões demonstrativos fictícios em português** (por exemplo: `Riolume`, `Folhaverde`, `Brisamata`, `Flor d'água`, `Noitedegrilo`), com `grammatical_class = "NOUN"` e distribuídos nas categorias semânticas `NATURE` (17), `ANIMALS` (2) e `FOOD` (1).
- **Ausência de autenticidade linguística:** Os registros **não representam vocabulário autêntico da língua Parintintin**. A interface reforça essa condição de forma persistente na Home (`demo_mode_notice_card`), no `stickyHeader` do Dicionário e em cada card (`"Termo demonstrativo"` e `"Nota demonstrativa"`).
- **Ausência de pronúncia e exemplos:** Todos os 20 registros possuem `phonetic_ipa = ""`, `example_parintintin = ""`, `example_portuguese = ""` e nenhuma URL de áudio (`audio_url` ausente/nulo).
- **Dependência de validação e servidor:** A inclusão de vocabulário real e artigos culturais oficiais depende de **servidor remoto ativo** e de **validação e autorização adequadas** (`strings.xml`, `home_intro_body`; `docs/api-v1-contract.md`).
- **Limitações técnicas observadas no código atual:**
  - `NetworkModule.kt` aponta para `https://archive.parintintin.org/`, porém **não foi possível confirmar a disponibilidade operacional do servidor remoto** no ambiente e durante as verificações desta auditoria (os testes HTTP automatizados foram realizados com `MockWebServer`).
  - `ArchiveSyncRepositoryImpl` não consulta `GET v1/manifest.json` (`getArchiveManifest()` declarado sem integração), registrando `lastSuccessfulRemoteVersion = "manifesto_nao_integrado"`.
  - O tipo específico de falha (`SyncFailureType`) é mantido em memória (`lastClassifiedFailure`) e não persistido em coluna própria no Room; em uma reinicialização fria do app, antes da nova tentativa automática concluir, um `STATUS_OFFLINE_FALLBACK` salvo é reclassificado temporariamente pela conectividade atual do dispositivo.
  - Em `PreserveComponents.kt` (linhas 436–446), o ramo que exibe a contagem dupla de sincronização (`home_sync_synced_counts`, exigindo `entriesCount != null && articlesCount != null`) é inalcançável na v1.0 porque `ArchiveSyncRepositoryImpl` mantém `articlesCount = null`.

---

## 8. Funcionalidades Parciais, Não Implementadas e Futuras

Para evitar ambiguidade entre código estrutural existente e entregas reais da **Versão 1.0 Demonstrativa**, os itens fora da experiência completa da v1.0 classificam-se em:

### 8.1. Síntese de Classificação por Estado

| Classificação | Itens Identificados no Repositório |
|---|---|
| **Implementado e ativo na v1.0** | Telas **Início** e **Dicionário**, busca com *debounce* (`150 ms`), filtros combinados por categoria e classe gramatical, banco Room v3 com migrações, seed demonstrativo versionado por SHA-256 (`DEMO-001`..`DEMO-020`), monitoramento de conectividade (`ConnectivityManager`) e coordenação *offline-first* (`SyncState`). |
| **Parcialmente implementado (estrutural / não exposto ou sem validação externa)** | Módulo **Enciclopédia Cultural** (camadas de dados, `EncyclopediaViewModel` com `TEC-001` e `EncyclopediaScreen` existentes, porém sem exposição na barra inferior, sem seed e fora de `syncArchive()`); infraestrutura de dados/usecase de **Favoritos** sem botão na UI e com rota provisória; `ObserveDictionaryEntryByIdUseCase` sem tela de detalhes; campo `audioUrl` sem player; cliente Retrofit/Moshi implementado para `/v1/dictionary.json` sem servidor oficial comprovado em produção; suíte `androidTest` apenas compilada; build Debug gerado sem assinatura Release. |
| **Não implementado / Funcionalidades futuras** | Consumo e validação de `/v1/manifest.json` no fluxo de sincronização; sincronização remota de `/v1/articles.json`; política de exclusão remota de registros; conteúdo linguístico e cultural autêntico da língua Parintintin (IPA, exemplos e áudios); tela de detalhes de verbetes; UI funcional de Favoritos e Acervo Oficial; player de áudio; execução em dispositivo físico/emulador e empacotamento Release assinado. |

### 8.2. Detalhamento das Funcionalidades Parciais e Futuras

| Funcionalidade Futura | Estado Atual no Repositório | Evidência |
|---|---|---|
| **Enciclopédia Cultural** | **Parcial (preparatório, fora da v1.0):** possui DAO, Entity, Repository, UseCases, `EncyclopediaViewModel`, `EncyclopediaScreen` e rota no `NavHost`, porém está **excluída da barra inferior**, **fora de `ArchiveSyncRepositoryImpl.syncArchive()`** e sem seed local (`0 artigos locais`). Nota técnica: o tratamento explícito de `CancellationException` em `EncyclopediaViewModel` foi corrigido e validado com testes unitários (`TEC-001`), mantendo o módulo fora da v1.0. | `AppDestination.kt`, `ArchiveSyncRepositoryImpl.kt` (linhas 196, 220–222), `EncyclopediaScreen.kt`, `EncyclopediaViewModel.kt`, `EncyclopediaViewModelTest.kt` |
| **Favoritos** | **Parcial (persistência sem UI):** colunas `is_favorite`/`is_bookmarked`, métodos de DAO (`toggleFavorite`, `getFavoriteIds`) e `ToggleFavoriteEntryUseCase` existem e são preservados durante o `upsertAll`, mas **não há botão de favoritar** na `DictionaryScreen` e a rota `AppDestination.Favorites` renderiza `ProvisionalDestinationScreen` inacessível pela UI. | `DictionaryScreen.kt`, `ParintintinNavHost.kt` (linhas 76–84), `ArchiveUseCases.kt` |
| **Tela de Detalhes de Verbetes** | **Parcial / Não implementada na UI:** apenas `ObserveDictionaryEntryByIdUseCase` declarado no domínio, sem tela, rota ou navegação implementadas. | `ArchiveUseCases.kt` (linhas 35–40), `ParintintinNavHost.kt` |
| **Pronúncias em Áudio** | **Não implementada:** apenas o campo anulável `audioUrl: String?` nos modelos/DTO/entidade e um card informativo `"Planejado"` na Home; não há player de áudio nem arquivos sonoros. | `DictionaryEntry.kt`, `HomeScreen.kt` (linhas 50–54) |
| **Acervo Oficial (Tela Dedicada)** | **Não implementada:** rota `AppDestination.Archive` renderiza apenas `ProvisionalDestinationScreen` com selo `"TELA PROVISÓRIA • ETAPA 2.2"` e não é exposta na barra inferior. | `ParintintinNavHost.kt` (linhas 85–93), `AppDestination.kt` |
| **Integração com Servidor Remoto Ativo** | **Parcial (cliente sem backend confirmado):** base URL `https://archive.parintintin.org/` configurada no cliente (`NetworkModule.kt`), porém sem disponibilidade operacional confirmada do servidor remoto no ambiente de auditoria (integração HTTP testada apenas em ambiente controlado com `MockWebServer`); consumo do manifesto `/v1/manifest.json` (`getArchiveManifest()`) e política de exclusão remota permanecem **não implementados**. | `NetworkModule.kt`, `ParintintinApiService.kt`, `docs/api-v1-contract.md` |

---

## 9. Requisitos Funcionais

| ID | Descrição | Situação Atual | Critério de Aceitação |
|---|---|---|---|
| **RF-001** | Exibir tela inicial (*Home*) com identificação da versão demonstrativa, introdução sobre construção do acervo, aviso permanente de modo demonstrativo, card de status offline-first, módulos planejados e detalhes técnicos expansíveis. | **Implementado** (`HomeScreen.kt`, `HomeViewModel.kt`, `PreserveComponents.kt`) | Ao abrir o app, `HomeScreen` é exibida como destino inicial com o banner `"VERSÃO 1.0 • DEMONSTRATIVA"`, o card `"MODO DEMONSTRATIVO"` visível sem interação e a seção de módulos futuros rotulada como `"Planejado"`. |
| **RF-002** | Disparar automaticamente uma tentativa de inicialização de seed e sincronização por execução do processo ao inicializar `HomeViewModel`. | **Implementado** (`HomeViewModel.kt`, `ArchiveSyncCoordinatorImpl.kt`) | Na inicialização da `HomeViewModel`, `requestAutoSync()` executa a atualização do seed e uma tentativa de sincronização remota; novas recomposições ou retornos à Home dentro do mesmo processo retornam `AlreadyTriggeredForLaunch` sem duplicar requisições. |
| **RF-003** | Permitir sincronização manual sob demanda na Home, prevenindo execuções paralelas concorrentes. | **Implementado** (`HomeViewModel.kt`, `PreserveComponents.kt`, `ArchiveSyncCoordinatorImpl.kt`) | Ao tocar no botão de sincronização (`home_sync_button`), o botão é desabilitado enquanto `SyncState.Syncing` estiver ativo e cliques simultâneos juntam-se à operação em voo (`JoinedInFlight`) ou são bloqueados por `syncJobLock` e `syncMutex`. |
| **RF-004** | Exibir os verbetes armazenados no Room na tela **Dicionário**, ordenados alfabeticamente por termo (`parintintin_word COLLATE NOCASE ASC`), com sinalização clara de conteúdo demonstrativo. | **Implementado** (`DictionaryScreen.kt`, `DictionaryViewModel.kt`, `DictionaryDao.kt`) | Com os 20 registros `DEMO-001`..`DEMO-020` no Room, `DictionaryScreen` exibe contagem `"20 verbetes locais"`, `stickyHeader` `"MODO DEMONSTRATIVO"` e cards com selo `"Termo demonstrativo"` e `"Nota demonstrativa"` para IDs iniciados por `"DEMO-"`. |
| **RF-005** | Permitir pesquisa textual em tempo real nos verbetes com *debounce* de `150 ms` e limpeza rápida da busca. | **Implementado** (`DictionaryViewModel.kt`, `DictionaryScreen.kt`, `DictionaryDao.kt`) | Digitar no campo `dictionary_search_input` filtra os verbetes após `150 ms` verificando correspondência parcial (`LIKE`) em palavra, tradução em português e nota cultural; tocar em `dictionary_clear_search` limpa imediatamente o texto. |
| **RF-006** | Permitir filtragem por categoria semântica (`SemanticCategory`) e classe gramatical (`GrammaticalClass`), isoladamente ou em combinação com a busca textual, além de limpeza global de filtros. | **Implementado** (`DictionaryViewModel.kt`, `DictionaryScreen.kt`, `DictionaryDao.kt`) | Selecionar chips nas seções de categoria e classe gramatical restringe os resultados no Room via SQL e exibe contagem `"%1$d encontrados"`; tocar em `"Limpar filtros"` zera busca e seleções. |
| **RF-007** | Apresentar estados visuais distintos de carregamento, lista vazia (geral ou filtrada) e erro de consulta na tela Dicionário, com ações de recuperação. | **Implementado** (`DictionaryViewModel.kt`, `DictionaryScreen.kt`) | `DictionaryEntriesState.Loading` exibe indicador de progresso; `Empty(hasActiveFilter = true)` oferece `"Limpar filtros"`; `Error` exibe mensagem e botão `"Tentar novamente"` (`onRetry()`). |
| **RF-008** | Permitir navegação entre **Início** e **Dicionário** por barra inferior preservando estado e sem expor telas provisórias ou futuras. | **Implementado** (`AppDestination.kt`, `ParintintinBottomBar.kt`, `ParintintinNavHost.kt`) | A barra inferior exibe apenas `bottom_nav_home` (`"Início"`) e `bottom_nav_dictionary` (`"Dicionário"`), alternando entre as telas com `launchSingleTop = true` e `restoreState = true`, sem exibir `Encyclopedia`, `Favorites` ou `Archive`. |
| **RF-009** | Aplicar e atualizar o seed local (`dictionary_seed.json`) automaticamente por versão e hash SHA-256 de forma idempotente e transacional. | **Implementado** (`VersionedDictionarySeedUpdater.kt`, `SeedVersionComparator.kt`, `SeedSha256Calculator.kt`) | Se versão ou SHA-256 mudarem (e não houver sync oficial), os verbetes e `seed_metadata` são atualizados atomicamente em `database.withTransaction`; se já atualizados, retorna `ALREADY_UP_TO_DATE` sem reescrita. |
| **RF-010** | Diferenciar visualmente os estados de sincronização e conectividade na Home (`Loading`, `Idle`, `Syncing`, `Synced`, `RemoteEmpty`, e `OfflineFallback` por `NO_INTERNET`, `UNVALIDATED_NETWORK` e `SERVER_UNAVAILABLE`). | **Implementado** (`OfflineArchiveStatusCard` em `PreserveComponents.kt`, `ArchiveSyncRepositoryImpl.kt`, `AndroidNetworkConnectivityObserver.kt`) | Quando a sincronização falha sem internet, exibe `"Sem conexão com a internet"`; em rede sem validação, `"Conexão ainda não validada"`; com internet validada e falha remota, `"Não foi possível sincronizar"`; em `HTTP 200 []`, exibe estado `RemoteEmpty` diferenciando presença ou ausência de histórico oficial. |
| **RF-011** | Preservar histórico de última sincronização oficial e marcações locais (`is_favorite`) diante de falhas ou respostas vazias (`HTTP 200 []`). | **Implementado** (`ArchiveSyncRepositoryImpl.kt`, `SyncMetadataDao.kt`, `OfflineFirstDictionaryRepository.kt`) | Falhas ou `HTTP 200 []` não apagam registros locais nem sobrescrevem `lastSuccessfulSyncEpochMillis`, `lastSuccessfulEntriesCount` ou `is_favorite`. |

---

## 10. Requisitos Não Funcionais

| ID | Categoria | Descrição | Situação / Evidência no Código |
|---|---|---|---|
| **RNF-001** | Usabilidade e Design System | Aderir ao Material Design 3 com paleta dedicada claro/escuro, tipografia customizada (`Lora`, `Plus Jakarta Sans`, `JetBrains Mono`), modo *edge-to-edge* e respeito a `WindowInsets.safeDrawing`. | **Implementado e verificado por inspeção/testes Robolectric** (`Theme.kt`, `Type.kt`, `Color.kt`, `MainActivity.kt`, `ParintintinNavHost.kt`). |
| **RNF-002** | Responsividade | Adaptar layout para telas compactas e médias/expandidas (`BoxWithConstraints` com `maxWidth >= 600.dp` alternando entre 1 e 2 colunas, largura máxima `840.dp`, `FlowRow` para evitar sobreposição de chips e empilhamento em telas estreitas de `280.dp` ou `fontScale >= 1.3f`). | **Implementado e testado em Robolectric** (`HomeScreen.kt`, `DictionaryScreen.kt`, `PreserveComponents.kt`, `HomeScreenTest.kt`, `ResponsiveComponentsTest.kt`). |
| **RNF-003** | Acessibilidade | Garantir alvos táteis mínimos (`minimumInteractiveComponentSize()`), semântica de cabeçalhos (`heading()`), `contentDescription` para controles/indicadores e `stateDescription`/`onClickLabel` em componentes expansíveis. | **Implementado e parcialmente testado em Robolectric** (`PreserveComponents.kt`, `DictionaryScreen.kt`, `HomeScreenTest.kt`, `DictionaryScreenTest.kt`). *Nota:* não houve auditoria manual com TalkBack em dispositivo físico. |
| **RNF-004** | Operação Offline (*Offline-First*) | Garantir consulta integral dos verbetes locais sem conexão de rede e proteção dos dados armazenados durante falhas remotas. | **Implementado e testado em integração JVM/Room** (`OfflineFirstDictionaryRepository.kt`, `DictionaryOfflineIntegrationTest.kt`, `ArchiveSyncRepositoryImplTest.kt`). |
| **RNF-005** | Confiabilidade e Concorrência | Serializar escritas de seed (`updateMutex`) e sincronizações (`syncMutex`), usar transações Room (`withTransaction`), prevenir *overflow* de cláusula SQL `IN` em lotes de `500` IDs e propagar `CancellationException` na cadeia de sincronização. | **Implementado e testado em JVM** (`VersionedDictionarySeedUpdater.kt`, `ArchiveSyncRepositoryImpl.kt`, `DictionaryViewModel.kt`). *Nota:* `EncyclopediaViewModel.kt` (não exposto na v1.0) também passou a relançar explicitamente `CancellationException` no `.catch` (`TEC-001`, validado por `EncyclopediaViewModelTest.kt`). |
| **RNF-006** | Prevenção de Memory Leaks e Recursos | Utilizar `SharingStarted.WhileSubscribed(5000)` nos ViewModels, `@ApplicationScope` com `SupervisorJob` para observadores de processo e cancelar `NetworkCallback` via `AutoCloseable` ao término do escopo. | **Implementação estrutural verificada por inspeção estática** (`HomeViewModel.kt`, `DictionaryViewModel.kt`, `CoroutinesModule.kt`, `AndroidNetworkConnectivityObserver.kt`) e **testes automatizados específicos de escopo/ciclo de vida** para `@ApplicationScope` e registro/remoção de `NetworkCallback` (`HomeViewModelTest.kt`, `ArchiveSyncCoordinatorImplTest.kt`, `AndroidNetworkConnectivityObserverTest.kt`, `AndroidConnectivityPlatformRobolectricTest.kt`). *Ressalva:* **não houve teste instrumental ou de *profiling* específico para detecção de *memory leaks***. |
| **RNF-007** | Segurança de Transporte e Falhas | Exigir HTTPS para a API remota (`https://archive.parintintin.org/`, sem `usesCleartextTraffic` no manifesto), habilitar logs HTTP apenas em debug (`BuildConfig.DEBUG`) e capturar exceções HTTP/JSON/IO sem encerrar o aplicativo. | **Implementado** (`NetworkModule.kt`, `AndroidManifest.xml`, `RemoteArchiveDataSource.kt`). *Nota:* não houve testes de penetração ou auditoria de segurança externa. |
| **RNF-008** | Manutenibilidade e Evolução de Banco | Exportar schemas JSON do Room (`exportSchema = true`) e fornecer migrações explícitas (`MIGRATION_1_2`, `MIGRATION_2_3`) sem `fallbackToDestructiveMigration`. | **Implementado e testado** (`ParintintinDatabase.kt`, `ParintintinMigrations.kt`, `ParintintinDatabaseMigrationTest.kt`). |
| **RNF-009** | Desempenho | Executar operações de I/O, hash SHA-256, parse JSON e consultas Room fora da *Main Thread* via `DispatcherProvider.io`, aplicar *debounce* de `150 ms` na pesquisa e paginação de chaves em `LazyColumn`. | **Implementado estruturalmente** (`DefaultDispatcherProvider`, `DictionaryViewModel.kt`). *Nota:* **não foi possível comprovar** métricas numéricas de tempo de renderização ou consumo de memória/CPU por ausência de *benchmarks* ou *profiling* em dispositivo real. |

---

## 11. Requisitos Técnicos

| Tecnologia / Padrão | Versão / Configuração Verificada | Papel Real no Projeto |
|---|---|---|
| **Kotlin** | `2.2.10` (Java 11 target) | Linguagem única de implementação e testes (`app/build.gradle.kts`, `gradle/libs.versions.toml`). |
| **Jetpack Compose + Material 3** | Compose BOM `2024.09.00` | Interface declarativa completa (`MainActivity.kt`, telas, componentes, tema). |
| **Navigation Compose** | `2.8.9` | Roteamento tipado com `@Serializable` (`AppDestination.kt`, `ParintintinNavHost.kt`). |
| **Clean Architecture + MVVM** | Pacotes `presentation`, `domain`, `data`, `core`, `di` | Isolamento de regras de domínio/UseCases e estado reativo em ViewModels. |
| **Hilt** | `2.59` (`hilt-navigation-compose 1.2.0`) | Grafo de DI em `SingletonComponent` e injeção de `@HiltViewModel`. |
| **Kotlin Coroutines + Flow / StateFlow** | `1.10.2` | Execução assíncrona em `Dispatchers.IO`, sincronia com `Mutex`/`Deferred` e fluxos reativos UI←Room. |
| **Room** | `2.7.0` (Schema v3) | Persistência SQLite local, transações, `Flow` reativo, `TypeConverters` e migrações. |
| **Retrofit + OkHttp** | Retrofit `2.12.0`, OkHttp `4.10.0` | **Cliente HTTP configurado no app** (`https://archive.parintintin.org/`), pronto para endpoints JSON e testado com `MockWebServer` em ambiente controlado, porém sem disponibilidade operacional confirmada do servidor remoto no ambiente de auditoria (manifesto `/v1/manifest.json` pendente de integração). |
| **Moshi** | `1.15.2` | Desserialização JSON com codegen para DTOs da API e validação do `dictionary_seed.json`. |
| **Kotlinx Serialization JSON** | `1.8.0` | Rotas tipadas do Navigation Compose, `ParintintinConverters` e `applied_seed_ids_json`. |
| **KSP** | `2.3.5` | Processamento de anotações em compilação para Room, Hilt e Moshi (KAPT **não** é utilizado). |
| **Testes Automatizados** | JUnit `4.13.2`, Robolectric `4.16.1`, Compose UI Test, Coroutines Test `1.10.2`, MockWebServer `4.10.0`, Hilt Testing `2.59` | 25 suítes em `app/src/test` (238 testes aprovados na JVM) e 2 arquivos de teste (`3` `@Test` + 3 arquivos de suporte Hilt) em `app/src/androidTest` (compilados anteriormente; execução dependente de emulador/ADB). |

> **Observação sobre dependências de catálogo não utilizadas na v1.0:** `app/build.gradle.kts` mantém comentadas bibliotecas como Firebase (`firebase-bom`, `ai`, `firestore`, `auth`, `appcheck`), CameraX, Play Services Location, Coil, DataStore Preferences e Accompanist Permissions, além dos plugins `google-services` (`MissingGoogleServicesStrategy.WARN`) e `secrets-gradle-plugin` (`.env.example` apenas com placeholder comentado). Nenhuma dessas integrações está ativa na v1.0.

---

## 12. Regras de Negócio

1. **Fonte única de verdade local (`RN-01`):** A exibição de verbetes e estatísticas nas telas `DictionaryScreen` e `HomeScreen` provém exclusivamente da observação reativa do banco Room (`DictionaryDao` e `SyncMetadataDao`), garantindo consulta offline contínua (`OfflineFirstDictionaryRepository.kt`, `ArchiveSyncRepositoryImpl.kt`).
2. **Controle de versão e integridade do seed por SHA-256 (`RN-02`):** Antes de abrir transação no banco, `VersionedDictionarySeedUpdater` lê todos os bytes crus de `dictionary_seed.json`, calcula o hash SHA-256 hexadecimal sem normalização (`SeedSha256Calculator` rejeita arquivos de 0 bytes com `EmptySeedFileException`) e valida estritamente cada objeto JSON (`DictionarySeedJsonParser`: IDs não vazios e sem duplicidade, palavras/traduções não vazias, enums válidos de `GrammaticalClass` e `SemanticCategory`, timestamps `>= 0`).
3. **Critério de decisão de versão do seed (`RN-03`):** `SeedVersionComparator` compara `DictionarySeedSpec.CURRENT_SEED_VERSION` (`2`) e o hash atual com `SeedMetadataEntity`:
   - Sem metadata, versão aplicada `< atual` ou hash ausente -> `SEED_NOT_APPLIED` (aplica seed);
   - Versão aplicada `== atual` e hash diferente -> `SEED_UPDATED` (reaplica seed);
   - Versão aplicada `== atual` e hash igual -> `SEED_UP_TO_DATE` (`ALREADY_UP_TO_DATE`, não grava no banco);
   - Versão aplicada `< 0` ou `> atual` -> `INCONSISTENT_VERSION` (`BLOCKED_INCONSISTENT_VERSION`, bloqueia aplicação).
4. **Atualização transacional atômica e preservação de favoritos (`RN-04`):** A aplicação do seed e qualquer sincronização bem-sucedida ocorrem dentro de `database.withTransaction`, lendo `dictionaryDao.getFavoriteIds()` antes do `upsertAll` para preservar a flag `is_favorite` de cada ID existente e atualizando `seed_metadata` na mesma transação (`VersionedDictionarySeedUpdater.kt`, `OfflineFirstDictionaryRepository.kt`).
5. **Proteção da base após sincronização oficial (`RN-05`):** Se `sync_metadata.last_successful_sync_epoch_millis != null`, `VersionedDictionarySeedUpdater` interrompe a atualização demonstrativa retornando `SeedUpdateOutcomeStatus.BLOCKED_BY_OFFICIAL_SYNC`, impedindo que registros `DEMO-*` sobrescrevam ou reapareçam sobre um acervo oficial sincronizado.
6. **Tratamento de resposta remota `HTTP 200 []` (`RN-06`):** Quando `GET v1/dictionary.json` retorna lista vazia (`[]`), `OfflineFirstDictionaryRepository` retorna `RemoteSyncOutcome.Empty` sem abrir transação nem remover dados locais. `ArchiveSyncRepositoryImpl` grava `STATUS_REMOTE_EMPTY` via `SyncMetadataDao.recordRemoteEmptyAttempt(...)`, **não** atualiza `lastSuccessfulSyncEpochMillis` (mantendo desbloqueadas futuras atualizações do seed caso nunca tenha havido sync oficial, ou preservando o histórico oficial anterior se já tiver existido) e exibe estado específico `SyncState.RemoteEmpty` na UI.
7. **Preservação do histórico de sincronização diante de falhas (`RN-07`):** Em erros de rede, HTTP ou parse (`AppResult.Error`), `SyncMetadataDao.recordAttemptFailure(...)` atualiza apenas `lastAttemptEpochMillis`, `lastAttemptStatus = STATUS_OFFLINE_FALLBACK` e `lastFailureReason`, preservando intactos `lastSuccessfulSyncEpochMillis`, `lastSuccessfulEntriesCount`, `lastSuccessfulArticlesCount` e `lastSuccessfulRemoteVersion`.
8. **Diferenciação dos estados de conectividade (`RN-08`):** Ao ocorrer falha de sincronização, `SyncFailureClassifier.classify(networkConnectivityObserver.connectivityState.value)` mapeia:
   - `DeviceConnectivityState.Offline` (sem rede ativa com `NET_CAPABILITY_INTERNET`) -> `SyncFailureType.NO_INTERNET`;
   - `DeviceConnectivityState.ConnectedUnvalidated` (`NET_CAPABILITY_INTERNET` sem `NET_CAPABILITY_VALIDATED`) -> `SyncFailureType.UNVALIDATED_NETWORK`;
   - `DeviceConnectivityState.Online` (rede validada pelo sistema, porém erro na chamada ao servidor) -> `SyncFailureType.SERVER_UNAVAILABLE`.
9. **Deduplicação e monotonicidade temporal de sincronizações (`RN-09`):** `ArchiveSyncCoordinatorImpl` permite apenas um auto-sync por instância do processo (`launchSyncTriggered`), une chamadas manuais concorrentes à tarefa em andamento (`JoinedInFlight`) e `ArchiveSyncRepositoryImpl` utiliza `syncMutex` e ajuste monotônico de timestamp (`computeNextTimestamp`) para assegurar que novos eventos nunca tenham timestamp inferior ao último persistido.

---

## 13. Arquitetura e Fluxos

### 13.1 Organização das Camadas

- **Presentation (`com.example.presentation` e `com.example.ui.theme`):** Composables sem lógica de persistência ou rede (`HomeScreen`, `DictionaryScreen`, `PreserveComponents`, `ParintintinNavHost`, `ParintintinBottomBar`), apoiados por `@HiltViewModel` (`HomeViewModel`, `DictionaryViewModel`), que expõem `StateFlow<UiState>` via `stateIn(..., WhileSubscribed(5000), ...)`.
- **Domain (`com.example.domain`):** Modelos puros em Kotlin, contratos de repositórios (`DictionaryRepository`, `EncyclopediaRepository`, `SyncStatusRepository`), observador de rede (`NetworkConnectivityObserver`), coordenador (`ArchiveSyncCoordinator`), classificador (`SyncFailureClassifier`) e casos de uso (`ArchiveUseCases.kt`), sem dependências de Android SDK, Room ou Retrofit.
- **Data (`com.example.data`, `com.example.di`, `com.example.core.common`):** Implementações concretas ligadas por Hilt `@Singleton`, persistência Room (`ParintintinDatabase`), engine de seed (`VersionedDictionarySeedUpdater`), cliente Retrofit/Moshi (`RemoteArchiveDataSource`, `ParintintinApiService`), monitor `ConnectivityManager.NetworkCallback` (`AndroidNetworkConnectivityObserver`) e repositórios com `Flow` + `Mutex` + `withTransaction`.

### 13.2 Fluxo de Consulta Local (Dicionário)

1. `ParintintinNavHost` compõe `DictionaryScreen`, que obtém `DictionaryViewModel` via `hiltViewModel()` e coleta `uiState` com `collectAsStateWithLifecycle()`.
2. Ações de digitação (`onQueryChange`), seleção de categoria (`onSelectCategory`) ou classe gramatical (`onSelectGrammaticalClass`) atualizam `MutableStateFlow`s no `DictionaryViewModel`.
3. O operador `combine` aplica `debounce(150L)` exclusivamente na busca textual e `flatMapLatest` aciona `ObserveDictionaryEntriesUseCase` -> `OfflineFirstDictionaryRepository.observeEntries(...)` -> `DictionaryDao.observeEntries(...)`.
4. O Room executa a consulta SQL reativa em `dispatchers.io`, mapeia entidades via `ArchiveMappers.toDomain()` e emite `DictionaryEntriesState` (`Loading`, `Empty`, `Success` ou `Error`) para renderização na UI.

### 13.3 Fluxo de Inicialização, Seed e Sincronização

1. `ParintintinApplication` inicializa o grafo Hilt (`@HiltAndroidApp`); `MainActivity` (`@AndroidEntryPoint`) compõe `ParintintinAppShell`, iniciando na rota `AppDestination.Home`.
2. `HomeViewModel.init` dispara `syncCoordinator.requestAutoSync()` dentro de `viewModelScope`.
3. `ArchiveSyncCoordinatorImpl` agenda `syncStatusRepository.syncArchive()` em `@ApplicationScope` (sobrevivendo a recomposições e troca de abas).
4. `ArchiveSyncRepositoryImpl.syncArchive()` adquire `syncMutex`, define estado transitório `SyncState.Syncing`, executa `dictionarySeedUpdater.updateIfNeeded()` (gravando os 20 verbetes `DEMO-001`..`DEMO-020` no Room se aplicável) e em seguida invoca `dictionaryRepository.syncFromRemote()` (`RemoteArchiveDataSource.fetchDictionaryEntries()` -> `ParintintinApiService.getDictionaryEntries()`).
5. Conforme o desfecho:
   - Lista não vazia: persiste em transação Room preservando favoritos e transita para `SyncState.Synced`;
   - `HTTP 200 []`: registra `STATUS_REMOTE_EMPTY` preservando seed/histórico e transita para `SyncState.RemoteEmpty`;
   - Falha remota/rede: consulta `networkConnectivityObserver.connectivityState.value`, classifica `SyncFailureType` via `SyncFailureClassifier`, registra `STATUS_OFFLINE_FALLBACK` preservando último sync oficial e transita para `SyncState.OfflineFallback`.
6. Concomitantemente, `HomeViewModel` observa `ObserveArchiveStatsUseCase` (`DictionaryDao` + `EncyclopediaDao`) e `ObserveSyncStateUseCase`, mantendo o card `OfflineArchiveStatusCard` sincronizado em tempo real com o banco local.

---

## 14. Critérios Gerais de Aceitação (Versão 1.0 Demonstrativa)

1. O aplicativo inicializa em modo *Offline-First*, sem depender de servidor externo ou conexão com a internet, exibindo a tela **Início** e — condicionada à disponibilidade, validade estrutural e processamento bem-sucedido do arquivo de seed local (`dictionary_seed.json`), bem como à inicialização íntegra do banco Room — populando automaticamente os 20 verbetes `DEMO-001` a `DEMO-020` na primeira execução.
2. Reexecuções subsequentes do fluxo de inicialização com o mesmo `dictionary_seed.json` não duplicam verbetes (mantendo exatamente 20 registros locais).
3. O usuário navega exclusivamente entre **Início** e **Dicionário** pela barra inferior, sem acesso a telas provisórias ou módulos inacabados.
4. A tela **Dicionário** permite consultar todos os 20 termos offline, pesquisar por texto com limpeza rápida e combinar filtros por categoria semântica e classe gramatical.
5. Tanto a **Início** quanto o **Dicionário** mantêm visível de forma explícita o aviso `"MODO DEMONSTRATIVO"` e os cards identificam os termos como `"Termo demonstrativo"`, sem apresentar nenhum conteúdo fictício como autêntico da língua Parintintin.
6. Falhas de conexão, rede sem validação de internet, indisponibilidade de servidor e retorno remoto vazio (`HTTP 200 []`) são exibidos corretamente em `OfflineArchiveStatusCard` sem apagar os dados locais.
7. A suíte de testes unitários, de integração Room, contratos HTTP e Robolectric/Compose executada via `gradle :app:testDebugUnitTest` passa integralmente sem falhas.

---

## 15. Restrições e Riscos

| Tipo | Descrição | Impacto |
|---|---|---|
| **Restrição de Infraestrutura** | **Disponibilidade do servidor remoto não confirmada:** `NetworkModule.kt` aponta para `https://archive.parintintin.org/`, porém não foi possível confirmar a disponibilidade operacional do servidor remoto no ambiente e durante as verificações desta auditoria; os testes HTTP automatizados foram realizados com `MockWebServer`. | Nas condições e na configuração verificadas durante a auditoria, não foi possível estabelecer uma sincronização com um servidor oficial operacional, resultando em `OfflineFallback` (`NO_INTERNET`, `UNVALIDATED_NETWORK` ou `SERVER_UNAVAILABLE`) até que haja resposta válida do backend configurado. |
| **Restrição de Conteúdo** | **Ausência de vocabulário autêntico validado e dependência de autorização:** os 20 registros `DEMO-001`..`DEMO-020` são fictícios em português, sem IPA, áudio ou exemplos reais. | O aplicativo não pode ser utilizado como dicionário linguístico autêntico enquanto não houver curadoria, validação e autorização formal de conteúdo. |
| **Risco de Validação em Runtime Real** | **Testes instrumentados não executados:** `StartupSeedHiltInstrumentedTest` e `ExampleInstrumentedTest` foram compilados, mas não executados em emulador ou dispositivo físico neste ambiente (sem ADB/emulador). | O fluxo completo com Hilt real em dispositivo físico/emulador depende de execução externa com `connectedDebugAndroidTest`. |
| **Risco de Distribuição** | **Assinatura Release não configurada no ambiente:** `my-upload-key.jks` e variáveis de assinatura não existem no container. | Apenas o APK Debug é gerável neste ambiente (`Voz-Ancestral-debug.apk` na raiz, idêntico a `app/build/outputs/apk/debug/app-debug.apk`) até o fornecimento seguro de keystore dedicada. |
| **Risco Documental / Identidade** | **Histórico inicial de lacunas SDD (já regularizado) e identidade demonstrativa:** no momento da reconstrução inicial deste `SPECIFICATION.md`, ainda faltavam `README.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md`, `AI_USAGE.md` e a correção de `docs/api-v1-contract.md`; posteriormente, todas as sete tarefas documentais (`DOC-001` a `DOC-007`) foram concluídas e revisadas na raiz e no contrato da API, permanecendo o aplicativo intencionalmente identificado como `"Acervo Provisório"` em `strings.xml` na v1.0. | Regularização documental concluída para a Versão 1.0 Demonstrativa; mantém-se o registro explícito da natureza provisória do aplicativo até futura evolução oficial. |

---

## 16. Histórico e Natureza da Especificação

- **Versão do documento:** `1.0` (Reconstrução retrospectiva da Versão 1.0 Demonstrativa).
- **Origem:** Documento elaborado **retrospectivamente** após a implementação e auditoria técnica geral do projeto **Voz Ancestral** (`Acervo Provisório`, `com.aistudio.parintintin.kagwah`), utilizando como fonte de verdade os arquivos reais do repositório no estado de código da Etapa 30.0.
- **Limitação explícita de registro histórico:** Não foram encontradas especificações prévias escritas antes do código nem definição formal documentada do público-alvo; tais lacunas foram registradas transparentemente nas seções pertinentes.
