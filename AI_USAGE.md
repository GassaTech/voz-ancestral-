# AI_USAGE.md — Voz Ancestral: Catálogo Digital de Terras e Povos Indígenas do Brasil

> **Nota de Natureza Documental:** Este documento registra de maneira transparente e fundamentada nas evidências disponíveis a utilização de ferramentas de Inteligência Artificial (IA) ao longo do planejamento, desenvolvimento, revisões corretivas e atualização documental do projeto **Voz Ancestral** — abrangendo o ciclo inicial da **Versão 1.0 Demonstrativa** (*Acervo Provisório*) e a evolução pelas **Etapas 1 a 5.4** para o **Catálogo Digital de Terras e Povos Indígenas do Brasil**. Nenhuma ferramenta sem comprovação, versão especulativa de modelo ou percentual arbitrário de contribuição foi inventado.

---

## 1. Identificação do Projeto

| Atributo | Definição Verificada | Referência |
|---|---|---|
| **Identidade Atual do Projeto** | **Voz Ancestral — Catálogo Digital de Terras e Povos Indígenas do Brasil** | `README.md`, `SPECIFICATION.md`, `PLANNING.md` |
| **Nome Configurado no App/Gradle** | `Voz Ancestral` (`VozAncestral`, `Theme.VozAncestral`) | `app/src/main/res/values/strings.xml`, `settings.gradle.kts`, `themes.xml`, `metadata.json` |
| **Versão no Gradle** | `versionCode = 1`, `versionName = "1.0"` | `app/build.gradle.kts` |
| **Identificador Android (`applicationId`)** | `com.aistudio.parintintin.kagwah` (`namespace = "com.example"`, preservado por compatibilidade) | `app/build.gradle.kts` |

*Observação técnica sobre o produto:* A utilização de IA documentada neste arquivo refere-se exclusivamente ao **processo de engenharia de software, testes e documentação**. O aplicativo em execução **não faz chamadas a modelos generativos ou APIs de IA** (as dependências opcionais de `firebase-ai` em `app/build.gradle.kts` permanecem comentadas/inativas).

---

## 2. Ferramentas de Inteligência Artificial Utilizadas

| Ferramenta de IA | Contexto de Uso Confirmado | Limite Explícito do Registro |
|---|---|---|
| **Google AI Studio** | Ambiente integrado de apoio ao desenvolvimento Android conectado aos arquivos do projeto, utilizado para análise arquitetural, implementação das etapas, criação/revisão de testes JVM/Robolectric/Room/`MockWebServer`, empacotamento de transferências e revisão documental. | Não são declarados nomes/versões internos específicos dos modelos utilizados nem se atribui autoria exclusiva à ferramenta. |
| **ChatGPT** | Ferramenta externa de apoio analítico e organizacional utilizada pelo responsável para planejamento das etapas, definição de requisitos/restrições técnicas, revisão crítica de relatórios e redação de diretrizes enviadas ao ambiente de desenvolvimento. | O ChatGPT não possui acesso direto ao sistema de arquivos, não executa Gradle nem modifica arquivos diretamente no repositório. |

Tecnologias convencionais de engenharia (**Kotlin**, **Jetpack Compose**, **Gradle**, **KSP**, **Room**, **Hilt**, **Retrofit/OkHttp/Moshi**, **JUnit/Robolectric/MockWebServer**, **Git** e **GitHub**) não constituem ferramentas de IA.

---

## 3. Participação da IA nas Etapas do Projeto

| Etapa / Frente | Participação Assistida por IA | Controle e Decisão Humana | Estado Técnico Associado |
|---|---|---|---|
| **Ciclo Inicial v1.0 (Fases 1–11 em `PLANNING.md`)** | Apoio na estruturação Clean Architecture + MVVM, Room v3, seed SHA-256, `HomeScreen`, `DictionaryScreen`, saneamento `TEC-001`–`TEC-004`, 27 suítes / 252 testes e documentos SDD v1.0 (`DOC-001`–`DOC-007`). | Definição de escopo demonstrativo (`DEMO-001`..`DEMO-020`), restrições éticas sobre vocabulário Parintintin e aprovação das entregas. | Base v1.0 concluída e preservada. |
| **Adaptação Territorial IBAMA / SIGAGEO (Etapas 1–5.1 / `TER-001`–`TER-003`)** | Apoio na implementação do cliente ArcGIS Camada 25 (`IbamaTerritoriesApiService`), Room Schema v4 (`MIGRATION_3_4`, `IndigenousLandQueryBuilder` com CTEs), paginação em `RoomIndigenousLandRepository`, integração com `ArchiveSyncRepositoryImpl` e `TerritoriesViewModel`. | Escolha da fonte oficial IBAMA / SIGAGEO Camada 25, definição dos critérios de pesquisa, filtros e ordenação, e validação externa dos 627 registros reais. | Infraestrutura territorial, Room v4 e ViewModel implementados. |
| **Revisão Corretiva e Complementação SQLite (Etapa 5.2 / `TER-004`)** | Revisão pontual de ordenação `DESC` em `TerritoriesViewModel` e proteção de exceções em `syncManually()`, além da ampliação de `TerritoriesViewModelTest` (14 testes) e `IndigenousLandDaoTest` (11 testes validando `""` e `"   "` diretamente no SQLite). | Definição explícita das restrições de arquivos permitidos e revisão dos resultados dos testes. | Ordenação nula/vazia ao final e tratamento seguro de erros validados. |
| **UI Territorial e Navegação em 3 Abas (Etapas 5.3–5.4 / `TER-005`–`TER-006`)** | Implementação de `TerritoriesScreen`, `TerritoryCard`, `TerritoryControlsSection`, `TerritoriesSyncStatusBar`, `CatalogSharedComponents`, integração de `AppDestination.Territories` no `ParintintinNavHost` / `ParintintinBottomBar`, `TerritoriesScreenTest` (10) e `ParintintinNavHostTest` (4), além da geração de pacotes `.zip`/`.b64` para transferência ao Codespaces. | Decisão de produto de manter exatamente 3 abas (**Início**, **Dicionário**, **Territórios**), mantendo módulos inacabados fora da barra inferior, e validação no GitHub Actions. | **303 testes / 35 suítes JVM aprovados** e `:app:compileDebugAndroidTestKotlin` aprovado. |
| **Atualização Documental Pós-Etapa 5.4 (`DOC-008`)** | Inspeção integral do repositório atualizado e revisão dos 6 arquivos Markdown raiz (`README.md`, `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md`, `AI_USAGE.md`) nesta etapa. | Definição da nova identidade do projeto, exigência de precisão quanto aos limites da API IBAMA (dados territoriais e etnias associadas, sem perfis culturais completos) e auditoria final pelo responsável. | Documentação alinhada ao estado da Etapa 5.4. |
| **Revisão de Identidade e Saneamento Pós-Etapa 5.4 (`FUT-010`)** | Auditoria completa e remoção de referências ao protótipo *Acervo Provisório* em `strings.xml`, `themes.xml`, `AndroidManifest.xml`, `settings.gradle.kts`, `metadata.json`, `HomeScreen.kt`, `PreserveComponents.kt`, navegação (`Favorites`/`Archive`) e `ArchiveDtos.kt`/`ArchiveMappers.kt`, além da atualização de 7 suítes de teste e dos 8 documentos técnicos (`README.md`, `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md`, `AI_USAGE.md`, `docs/api-v1-contract.md`, `docs/arcgis-territories-api.md`). | Diretriz explícita de limpeza preservando Clean Architecture, Room v4, conteúdo Parintintin válido e escopo oficial IBAMA Camada 25, com conferência final dos testes e compilação. | Saneamento concluído com **303 testes / 35 suítes JVM** e `:app:compileDebugAndroidTestKotlin` aprovados após as alterações. |

---

## 4. Regras de Integridade, Verificação e Limites

1. **Nenhuma saída assistida por IA substitui execução real de testes ou revisão humana:** todas as afirmações técnicas nos documentos raiz foram conferidas diretamente nos arquivos Kotlin, Gradle, XML, schemas JSON do Room e relatórios XML de `app/build/test-results/testDebugUnitTest/` (**35 suítes / 303 testes**).
2. **Separação explícita entre validação local e confirmação externa:** testes executados neste container offline com `MockWebServer` e Robolectric são diferenciados de `:app:connectedDebugAndroidTest` (apenas compilado), dos testes manuais em hardware/TalkBack (pendentes) e da sincronização de **627 registros** e **GitHub Actions** (confirmados externamente pelo responsável).
3. **Integridade de dados:** nenhum dado cultural, histórico ou linguístico foi inventado para complementar os registros territoriais da API do IBAMA ou os 20 verbetes demonstrativos do Dicionário.
4. **Responsabilidade acadêmica e autoral:** todas as decisões de produto, curadoria documental, aprovação de código e gestão do repositório remoto pertencem integralmente ao responsável humano pelo projeto, sem atribuição de percentuais arbitrários de autoria.
