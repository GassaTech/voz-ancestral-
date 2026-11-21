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
| **`NÃO APLICÁVEL`** | Tarefa fora do escopo ou das condições do ciclo atual. |

---

## 3. Tarefas Documentais

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`DOC-001`** | Criação e revisão pontual do documento `SPECIFICATION.md` na raiz do repositório. | **`CONCLUÍDA`** | Nenhuma (inspeção técnica prévia do código e auditoria geral). | `SPECIFICATION.md` presente na raiz com as 16 seções obrigatórias, `RF-001`–`RF-011`, `RNF-001`–`RNF-009`, `RN-01`–`RN-09`, ajustes da Etapa 1.1.1 aplicados e consistência com o código verificada. | `/SPECIFICATION.md` (256 linhas); relatórios aprovados das Etapas 1.1 e 1.1.1. |
| **`DOC-002`** | Criação e revisão corretiva pontual do documento `PLANNING.md` na raiz do repositório. | **`CONCLUÍDA`** | `DOC-001`. | `PLANNING.md` presente na raiz com as 12 seções obrigatórias (`4.1`–`4.12`), 11 fases, 10 marcos (`M1`–`M10`), distinção entre código estrutural e experiência v1.0 e revisão corretiva aplicada. | `/PLANNING.md` (232 linhas); relatório aprovado da Etapa 1.2 e sua revisão corretiva. |
| **`DOC-003`** | Criação do documento `TASKS.md` na raiz do repositório. | **`CONCLUÍDA`** | `DOC-001`, `DOC-002`. | `TASKS.md` presente na raiz com as 9 seções obrigatórias, quantitativos consistentes, coerência integral com `SPECIFICATION.md` e `PLANNING.md` e revisão humana concluída. | `/TASKS.md` (criado, revisado na Seção 8.2 e atualizado na Etapa 1.7.2). |
| **`DOC-004`** | Criação do documento `TEST_RESULTS.md` na raiz do repositório. | **`CONCLUÍDA`** | `DOC-001`, `DOC-002`, `DOC-003`, `VAL-001`, `VAL-002`, `VAL-005` (e referência às limitações de `VAL-003`, `VAL-004` e `VAL-006`). | `TEST_RESULTS.md` criado e revisado na raiz consolidando de forma auditável as 24 suítes JVM (233 testes aprovados), o status de compilação de `androidTest`, o estado do APK Debug e as lacunas de execução em dispositivo real. | `/TEST_RESULTS.md` (244 linhas, incluindo correção terminológica nas Seções 6 e 14). |
| **`DOC-005`** | Criação do documento `AI_USAGE.md` na raiz do repositório. | **`CONCLUÍDA`** | `DOC-001`, `DOC-002`, `DOC-003`. | `AI_USAGE.md` criado e revisado na raiz registrando de maneira transparente o uso e os limites de ferramentas de IA no ciclo acadêmico do projeto, sem informações não comprovadas. | `/AI_USAGE.md` (144 linhas, incluindo ajuste pontual na Seção 5). |
| **`DOC-006`** | Criação do documento `README.md` na raiz do repositório. | **`CONCLUÍDA`** | `DOC-001`, `DOC-002`, `DOC-003`, `DOC-004`, `DOC-005` *(recomendado após os demais documentos SDD)*. | `README.md` criado e revisado na raiz apresentando identificação oficial (**Voz Ancestral** / **Acervo Provisório** v1.0 Demonstrativa), escopo real (`Início` e `Dicionário`), instruções verificadas de build/testes e referências aos documentos SDD. | `/README.md` (238 linhas, com ajustes pontuais nas Seções 5, 7 e 8). |
| **`DOC-007`** | Revisão da inconsistência das Seções 1, 6.1 e 6.3 de `docs/api-v1-contract.md` em relação à Seção 6.4 e à implementação real. | **`CONCLUÍDA`** | `DOC-001`, `DOC-002`. | Tabela da Seção 1 e Seções 6.1 e 6.3 (com coerência terminológica na Seção 6.4) de `docs/api-v1-contract.md` ajustadas para deixar explícito que na v1.0 `ArchiveSyncRepositoryImpl.syncArchive()` sincroniza somente `dictionaryRepository.syncFromRemote()`, mantendo `encyclopediaRepository` fora do fluxo de sincronização. | `docs/api-v1-contract.md` (linhas 21–25 e 246–284); `ArchiveSyncRepositoryImpl.kt` (linhas 196, 220–222); Etapa 1.7.1. |

---

## 4. Tarefas de Validação

> **Distinção Obrigatória:** Execução automatizada na JVM, compilação de suítes instrumentadas (`androidTest`) e validação manual/presencial em aparelho físico são tratadas em tarefas separadas para impedir que compilação ou registro documental seja confundido com execução em dispositivo real.

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`VAL-001`** | Consolidação técnica dos resultados das 24 suítes JVM (`gradle :app:testDebugUnitTest`) e dos 233 testes aprovados (unitários, integração Room, `MockWebServer`, Robolectric e Compose UI Test). | **`CONCLUÍDA`** | Código e suítes em `app/src/test` estáveis. | Execução comprovada de `gradle :app:testDebugUnitTest` com 233/233 testes aprovados em 24 suítes e totais registrados em `PLANNING.md` e `TASKS.md` (a formalização dedicada em arquivo próprio ocorre em `DOC-004`). | 24 arquivos em `app/src/test/java/com/example/`; relatório de auditoria técnica geral; `PLANNING.md` (Seção 8). |
| **`VAL-002`** | Documentação da compilação bem-sucedida da suíte instrumentada `androidTest` (`:app:assembleDebugAndroidTest`), diferenciando explicitamente compilação de execução em dispositivo. | **`CONCLUÍDA`** | Código-fonte e `app/src/androidTest` estáveis. | Compilação via `:app:assembleDebugAndroidTest` comprovada no relatório de regressão e documentada sem afirmar execução em dispositivo (`SPECIFICATION.md` e `PLANNING.md`, com consolidação final prevista em `DOC-004`). | `StartupSeedHiltInstrumentedTest.kt`, `ExampleInstrumentedTest.kt`, `HiltTestRunner.kt`; relatório da Etapa 29.7; `PLANNING.md` (Seção 8). |
| **`VAL-003`** | Execução futura da suíte instrumentada `:app:connectedDebugAndroidTest` em dispositivo Android físico ou emulador compatível. | **`BLOQUEADA`** | `VAL-002`; disponibilidade externa de dispositivo Android ou emulador conectado via ADB. | Execução integral de `StartupSeedHiltInstrumentedTest` e `ExampleInstrumentedTest` via `:app:connectedDebugAndroidTest` concluída com aprovação em dispositivo ou emulador externo e resultado registrado em `TEST_RESULTS.md`. | `app/src/androidTest/java/com/example/StartupSeedHiltInstrumentedTest.kt`; ambiente atual sem emulador/ADB. |
| **`VAL-004`** | Execução de validação manual, auditoria de acessibilidade com TalkBack e *benchmarks* de desempenho em aparelho físico, mantendo o registro transparente das limitações atuais enquanto não realizadas. | **`PENDENTE`** | Disponibilidade de aparelho Android físico e APK Debug v1.0 (`VAL-005`). | Roteiro de testes manuais (fluxo *Offline-First*, conectividade e navegação), verificação prática com TalkBack e medições de desempenho executados em dispositivo físico e documentados em `TEST_RESULTS.md`. | `SPECIFICATION.md` (Seção 10 — `RNF-003`, `RNF-009` e Seção 15); `PLANNING.md` (Seção 8). |
| **`VAL-005`** | Verificação técnica do artefato APK Debug v1.0 (`Parintintin-v1.0-debug.apk`). | **`CONCLUÍDA`** | Build `:app:assembleDebug` e cópia verificada do artefato. | APK localizado e conferido com tamanho válido (`22.753.989` bytes), `applicationId = com.aistudio.parintintin.kagwah`, `versionCode = 1`, `versionName = "1.0"` e assinatura Debug v2 válida. | `.build-outputs/app-debug.apk` e `.build-outputs/Parintintin-v1.0-debug.apk`; relatório de recuperação do APK Debug v1.0. |
| **`VAL-006`** | Configuração segura de assinatura Release e geração de APK/AAB Release assinado para eventual distribuição. | **`BLOQUEADA`** | Fornecimento externo de keystore Release (`my-upload-key.jks`) e variáveis `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD`. | Execução bem-sucedida de `:app:assembleRelease` (ou geração de AAB) assinada com keystore Release dedicada fora do código-fonte e verificação do artefato resultante. *(Pendência atual já registrada após falha esperada em `:app:packageRelease` na Etapa 30.0.)* | `app/build.gradle.kts` (linhas 29–53); relatório da Etapa 30.0; `PLANNING.md` (Seções 4 e 9). |

---

## 5. Tarefas de Saneamento Técnico

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`TEC-001`** | Correção do tratamento de `CancellationException` no operador `.catch` de `EncyclopediaViewModel.kt`. | **`CONCLUÍDA`** | Nenhuma para aplicação no código; **pré-requisito técnico cumprido antes de eventual ativação futura do módulo `Encyclopedia` (`FUT-005`)**. | Bloco `.catch` em `EncyclopediaViewModel.kt` (linhas 70–80) atualizado com `import kotlinx.coroutines.CancellationException` (linha 11) para relançar explicitamente `CancellationException` antes de emitir `EncyclopediaArticlesState.Error` (alinhado ao padrão de `DictionaryViewModel.kt`), validado pelos 5 testes de `EncyclopediaViewModelTest.kt` e pela regressão completa `:app:testDebugUnitTest` (**238 testes aprovados em 25 suítes**, `0` falhas, `0` ignorados). | `app/src/main/java/com/example/presentation/encyclopedia/EncyclopediaViewModel.kt` (linhas 11 e 70–80); `app/src/test/java/com/example/presentation/encyclopedia/EncyclopediaViewModelTest.kt` (5 testes); `TEST_RESULTS.md` (Seção 6); `PLANNING.md` (Seções 10 e 11.3). |

> **Esclarecimento Obrigatório sobre `TEC-001`:**
> - O arquivo `EncyclopediaViewModel.kt` recebeu o tratamento explícito de `CancellationException` e foi validado pela suíte dedicada `EncyclopediaViewModelTest.kt` (5 testes aprovados, cobrindo `CancellationException` lançada pelo fluxo *upstream*, cancelamento estruturado via `flatMapLatest`, falhas comuns e `onRetry()`), mantendo aprovação integral na regressão JVM (**238 testes em 25 suítes**).
> - Trata-se exclusivamente de **saneamento técnico preventivo** em estrutura preparatória; o módulo `Encyclopedia` (`EncyclopediaDao`, `OfflineFirstEncyclopediaRepository`, `EncyclopediaScreen`) continua fora da barra de navegação e da sincronização na Versão 1.0 Demonstrativa (`FUT-005`).
> - Como a experiência funcional visível da Versão 1.0 Demonstrativa permanece concentrada em **Início** e **Dicionário**, a conclusão de `TEC-001` não alterou funcionalidades visíveis nem o escopo da v1.0.

---

## 6. Funcionalidades Futuras (Pós-v1.0)

> **Aviso de Escopo:** Nenhuma das tarefas abaixo integra as funcionalidades entregues ao usuário na Versão 1.0 Demonstrativa. Todas permanecem **`PENDENTE`** ou **`BLOQUEADA`** conforme suas dependências técnicas, operacionais e autorizativas reais.

| ID | Descrição | Status Atual | Dependências | Critério Objetivo de Conclusão | Evidência / Referência |
|---|---|---|---|---|---|
| **`FUT-001`** | Obtenção e validação de conteúdo linguístico autêntico da língua Parintintin, com autorização comunitária e institucional apropriada, para futura substituição dos dados demonstrativos `DEMO-001`..`DEMO-020`. | **`BLOQUEADA`** | Curadoria linguística especializada e autorização formal externa para publicação digital. | Conjunto validado de verbetes autênticos (com transcrição IPA, classes, categorias e exemplos revisados) formalmente autorizado e pronto para consumo sem infringir direitos culturais ou linguísticos. | `SPECIFICATION.md` (Seções 7, 8 e 15); `strings.xml` (`home_intro_body`). |
| **`FUT-002`** | Disponibilização operacional e homologação do backend oficial HTTPS compatível com `docs/api-v1-contract.md`. | **`BLOQUEADA`** | Definição das decisões contratuais pendentes (Seção 7 de `docs/api-v1-contract.md`), provisão de servidor externo e homologação dos endpoints. | Servidor HTTPS respondendo em ambiente operacional aos endpoints `/v1/manifest.json`, `/v1/dictionary.json` e `/v1/articles.json` conforme contrato revisado e aprovado em testes de integração reais. | `NetworkModule.kt` (`https://archive.parintintin.org/`); `docs/api-v1-contract.md`; `PLANNING.md` (Seção 9). |
| **`FUT-003`** | Integração do consumo do manifesto remoto (`/v1/manifest.json`) no fluxo de sincronização do cliente Android. | **`BLOQUEADA`** | `FUT-002` e definição definitiva do formato de `version` e validação de contagens (`entries_count`/`articles_count`). | `ArchiveSyncRepositoryImpl` consultando `ParintintinApiService.getArchiveManifest()`, validando versão/contagens conforme contrato e substituindo `"manifesto_nao_integrado"` pela versão real sincronizada, com testes automatizados aprovados. | `ParintintinApiService.kt`; `ArchiveSyncRepositoryImpl.kt` (linhas 223–224, 431–432); `docs/api-v1-contract.md` (Seção 2.1). |
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
1. **Cadeia documental base concluída (`DOC-001`–`DOC-003`):** `SPECIFICATION.md`, `PLANNING.md` e `TASKS.md` criados, revisados e consolidados como **`CONCLUÍDA`**.
2. **Regularização documental complementar da v1.0 concluída (`DOC-004`–`DOC-007`):**
   - `TEST_RESULTS.md` criado e revisado (`DOC-004` — **`CONCLUÍDA`**).
   - `AI_USAGE.md` criado e revisado (`DOC-005` — **`CONCLUÍDA`**).
   - `README.md` criado e revisado referenciando o conjunto SDD completo (`DOC-006` — **`CONCLUÍDA`**).
   - `docs/api-v1-contract.md` revisado e corrigido nas Seções 1, 6.1, 6.3 e 6.4 (`DOC-007` — **`CONCLUÍDA`**).
3. **Saneamento técnico preparatório concluído (`TEC-001` — `CONCLUÍDA`):** correção aplicada em `EncyclopediaViewModel.kt` com criação e aprovação de `EncyclopediaViewModelTest.kt` (5 testes) e regressão JVM completa (`238` testes em `25` suítes), mantendo inalterada a navegação da v1.0.
4. **Validação externa da v1.0 (quando houver ambiente físico/emulador e credenciais):** executar `VAL-003` (`:app:connectedDebugAndroidTest`), `VAL-004` (validação manual, TalkBack e *benchmarks*) e, se aplicável à entrega final, `VAL-006` (assinatura Release).
5. **Ciclo pós-v1.0 (somente após autorizações e infraestrutura):** desbloquear `FUT-001`, `FUT-009` e `FUT-002`, seguidos por `FUT-003`, `FUT-004`, `FUT-005`, `FUT-006`, `FUT-007` e `FUT-008`.

---

## 8. Resumo de Situação

Os totais abaixo foram calculados exclusivamente a partir das **23 tarefas** efetivamente registradas nas Seções 3, 4, 5 e 6 deste documento, **sem contabilizar tarefas futuras (`FUT-*`) como entregas da Versão 1.0 Demonstrativa**:

### 8.1 Quantitativo por Categoria e Status
| Categoria | Prefixo | `CONCLUÍDA` | `PARCIAL` | `PENDENTE` | `BLOQUEADA` | `NÃO APLICÁVEL` | Total |
|---|---|---|---|---|---|---|---|
| **Documentação** | `DOC` | 7 | 0 | 0 | 0 | 0 | **7** |
| **Validação e Testes** | `VAL` | 3 | 0 | 1 | 2 | 0 | **6** |
| **Saneamento Técnico** | `TEC` | 1 | 0 | 0 | 0 | 0 | **1** |
| **Funcionalidades Futuras (Pós-v1.0)** | `FUT` | 0 | 0 | 2 | 7 | 0 | **9** |
| **Total Geral** | — | **11** | **0** | **3** | **9** | **0** | **23** |

### 8.2 Síntese Objetiva da Versão 1.0 vs. Pós-v1.0
- **Regularização documental, validação e saneamento técnico associado ao projeto (`DOC` + `VAL` + `TEC` = 14 tarefas):** `11` concluídas (`DOC-001`, `DOC-002`, `DOC-003`, `DOC-004`, `DOC-005`, `DOC-006`, `DOC-007`, `VAL-001`, `VAL-002`, `VAL-005`, `TEC-001`), `0` parciais, `1` pendente (`VAL-004`) e `2` bloqueadas por recursos externos ao ambiente (`VAL-003` e `VAL-006`).
  - *Observação:* as tarefas `DOC` e `VAL` estão relacionadas à documentação e à validação da versão demonstrativa; já `TEC-001` constituiu correção preparatória concluída em `EncyclopediaViewModel.kt`, sem exposição do módulo `Encyclopedia` nem alteração da experiência atual da v1.0, concentrada em **Início** e **Dicionário**.
- **Escopo Pós-v1.0 (`FUT` = 9 tarefas):** `0` concluídas, `2` pendentes (`FUT-006`, `FUT-007`) e `7` bloqueadas (`FUT-001`, `FUT-002`, `FUT-003`, `FUT-004`, `FUT-005`, `FUT-008`, `FUT-009`).

---

## 9. Histórico e Limitações

- **Versão do documento:** `1.0` (Reconstrução retrospectiva alinhada ao `SPECIFICATION.md` e ao `PLANNING.md`, com status documentais atualizados na Etapa 1.7.2 e conclusão de `TEC-001` registrada após sua implementação e validação na JVM).
- **Natureza da reconstrução:** Este documento foi elaborado retrospectivamente a partir das evidências verificáveis no código-fonte, nas suítes de testes, nos artefatos `.build-outputs/` e na documentação existente (`SPECIFICATION.md`, `PLANNING.md`, `TEST_RESULTS.md`, `AI_USAGE.md`, `README.md`, `docs/api-v1-contract.md`).
- **Limitações registradas:**
  - Não foram inventados históricos de execução prévios, datas, responsáveis individuais, estimativas de esforço, prazos acadêmicos ou aprovações formais inexistentes no repositório.
  - Operações Git não foram executadas nesta etapa em cumprimento às regras de preservação do projeto.
  - Os status registrados representam um retrato fiel do estado atual verificado após a conclusão de `DOC-001`–`DOC-007` e `TEC-001`, devendo ser atualizados incrementalmente à medida que as tarefas ainda abertas (`VAL-003`, `VAL-004`, `VAL-006` e `FUT-001`–`FUT-009`) forem efetivamente executadas e comprovadas.
