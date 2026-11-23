# AI_USAGE.md — Projeto Acadêmico Voz Ancestral (Aplicativo Acervo Provisório)

> **Nota de Natureza Documental:** Este documento registra **retrospectivamente**, de maneira transparente e fundamentada nas evidências disponíveis, a utilização de ferramentas de Inteligência Artificial (IA) ao longo do planejamento, desenvolvimento, revisão técnica e regularização documental da **Versão 1.0 Demonstrativa** do projeto acadêmico **Voz Ancestral** (aplicativo **Acervo Provisório**). Nenhuma ferramenta sem comprovação, nome/versão de modelo especulativo, data não verificada ou percentual arbitrário de contribuição humana ou automatizada foi inventado.

---

## 1. Identificação do Projeto e Versão

| Atributo | Definição Oficial / Verificada | Referência no Repositório |
|---|---|---|
| **Nome do Projeto Acadêmico** | **Voz Ancestral** | `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md` |
| **Nome do Aplicativo** | **Acervo Provisório** (`AcervoProvisorio`) | `app/src/main/res/values/strings.xml`, `metadata.json`, `settings.gradle.kts` |
| **Tema** | Preservação digital e documentação da língua Parintintin | `SPECIFICATION.md`, `strings.xml` |
| **Versão Documentada** | **1.0 Demonstrativa** (`versionCode = 1`, `versionName = "1.0"`) | `app/build.gradle.kts`, `TEST_RESULTS.md` |
| **Identificador Android (`applicationId`)** | `com.aistudio.parintintin.kagwah` (`namespace = "com.example"`) | `app/build.gradle.kts` |
| **Escopo Funcional Visível (v1.0)** | Telas **Início** e **Dicionário** com 20 registros demonstrativos fictícios em português (`DEMO-001` a `DEMO-020`) | `SPECIFICATION.md`, `app/src/main/assets/dictionary_seed.json` |

*Observação técnica sobre o produto:* A utilização de IA documentada neste arquivo refere-se ao **processo de engenharia, revisão e documentação do projeto**. O aplicativo **Acervo Provisório (v1.0 Demonstrativa)** em si **não executa chamadas a modelos generativos ou APIs de IA em tempo de execução** para o usuário final (as dependências opcionais de `firebase-ai` em `app/build.gradle.kts` permanecem comentadas/inativas, e o campo `majorCapabilities` em `metadata.json` pertence exclusivamente à configuração de plataforma do ambiente Google AI Studio).

---

## 2. Objetivo do Documento

Estabelecer um registro claro, auditável e academicamente íntegro sobre:
1. Quais ferramentas de Inteligência Artificial foram efetivamente utilizadas no ciclo do projeto;
2. Para quais finalidades distintas cada ferramenta foi empregada, diferenciando-as das ferramentas convencionais de desenvolvimento;
3. Como as sugestões assistidas por IA se articularam com as decisões, revisões e validações conduzidas pelo responsável humano; e
4. Quais são as limitações, riscos e implicações de transparência e responsabilidade acadêmica associados a esse processo.

---

## 3. Ferramentas de Inteligência Artificial Utilizadas

Com base no histórico de trabalho confirmado pelo responsável e pelas evidências do ambiente, documentam-se exclusivamente duas ferramentas de Inteligência Artificial:

| Ferramenta de IA | Contexto de Uso Confirmado | Limite Explícito do Registro |
|---|---|---|
| **Google AI Studio** | Ambiente integrado de assistência ao desenvolvimento Android conectado aos arquivos do projeto, utilizado para análise técnica, auxílio à implementação, correções, suporte a testes, auditoria, verificação de artefatos e elaboração/revisão dos documentos SDD, com fluxo de sincronização ao repositório via **GitHub Sync**. | Não são declarados nome ou versão específicos do modelo utilizado, nem se afirma que todas as alterações do repositório tenham sido produzidas exclusivamente por essa ferramenta. |
| **ChatGPT** | Ferramenta externa de apoio analítico e organizacional utilizada pelo responsável para planejamento, estruturação das etapas, formulação de instruções (prompts) direcionadas ao Google AI Studio, análise de relatórios técnicos e revisão crítica de código/documentação. | O ChatGPT não teve acesso direto ao sistema de arquivos, não executou operações Git, não modificou diretamente o repositório e não executou compilações ou testes no ambiente Android. |

### Distinção em Relação a Ferramentas Convencionais de Engenharia
Ferramentas tradicionais de desenvolvimento e controle de versão empregadas no projeto — como **Kotlin**, **Jetpack Compose**, **Android Gradle Plugin / Gradle**, **KSP**, **Room**, **Hilt**, **Retrofit/OkHttp/Moshi**, **JUnit/Robolectric/MockWebServer**, **Git** e **GitHub** (incluindo o mecanismo de integração **GitHub Sync**) — são tecnologias convencionais de engenharia de software e infraestrutura, **não devendo ser confundidas com ferramentas de Inteligência Artificial**.

Não há evidência de utilização de outras ferramentas de IA além de **Google AI Studio** e **ChatGPT**.

---

## 4. Finalidades de Utilização de Cada Ferramenta

| Ferramenta | Finalidades Efetivamente Desempenhadas | Atividades Fora do Escopo da Ferramenta |
|---|---|---|
| **Google AI Studio** | - Assistência na implementação e evolução de módulos Kotlin/Compose e camada de dados;<br>- Inspeção de estrutura, dependências, configurações Gradle e artefatos de build;<br>- Análise de erros de compilação, falhas técnicas e inconsistências contratuais;<br>- Apoio na criação, manutenção e execução verificada das suítes em `app/src/test` e compilação de `app/src/androidTest`;<br>- Auditoria técnica geral do estado real do projeto, execução das correções `TEC-001`–`TEC-004`, integração cliente de `/v1/manifest.json` e geração verificada do APK Debug único `Voz-Ancestral-debug.apk`;<br>- Redação e atualização consistente do conjunto documental SDD (`SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md`, `README.md`, `docs/api-v1-contract.md` e este `AI_USAGE.md`). | - Definição autônoma de escopo acadêmico, requisitos éticos ou autorização de conteúdo linguístico;<br>- Execução de testes em hardware Android físico ou emulador com ADB (indisponível no container);<br>- Substituição de revisão e aprovação humanas. |
| **ChatGPT** | - Apoio ao planejamento das etapas de trabalho e organização metodológica (fluxo SDD);<br>- Elaboração e refinamento de instruções e critérios técnicos encaminhados pelo responsável ao Google AI Studio;<br>- Análise interpretativa de relatórios de auditoria, logs e resultados apresentados pelo responsável;<br>- Revisão técnica e documental que originou correções pontuais de precisão (como terminologia de registros demonstrativos, escopo da v1.0, ausência de afirmação absoluta sobre servidor externo e distinção entre testes unitários e demais testes automatizados na JVM). | - Leitura ou edição direta de arquivos do repositório;<br>- Execução de comandos Gradle, compilação de APK ou execução de testes automatizados;<br>- Execução de commits, pushes ou quaisquer operações Git. |

---

## 5. Participação da IA nas Etapas do Projeto

A tabela abaixo mapeia a participação das ferramentas de IA nas etapas identificáveis do projeto **Voz Ancestral**, em coerência com `PLANNING.md` e `TASKS.md`, diferenciando o papel de cada ferramenta:

| Etapa / Frente do Projeto | Participação do Google AI Studio | Participação do ChatGPT | Estado Técnico Associado |
|---|---|---|---|
| **Arquitetura e Implementação Android (Fases 1 a 8 em `PLANNING.md`)** | Apoio à estruturação em Clean Architecture + MVVM, persistência Room v3, engine de seed SHA-256, cliente Retrofit/Moshi (incluindo integração de `/v1/manifest.json`), observador de conectividade, desacoplamento `data`/`di` (`TEC-002`) e UI Compose M3 (`TEC-004`). | Apoio na organização de etapas, refinamento de requisitos técnicos e revisão conceitual de diretrizes enviadas pelo responsável. | Implementação da v1.0 concluída nas telas **Início** e **Dicionário** (`SPECIFICATION.md`). |
| **Testes, Integração e Regressão (Fase 9)** | Apoio na escrita e revisão de testes JVM/Room/`MockWebServer`/Robolectric/Compose (`app/src/test`) e atualização/recompilação de `app/src/androidTest`, além da execução verificada de `gradle :app:testDebugUnitTest`. | Análise de relatórios de execução e verificação crítica para impedir confusão entre execução JVM, compilação `androidTest` e validação em dispositivo físico. | Evolução auditada: **233/24** (marco inicial) → **238/25** (`TEC-001`) → **252 testes aprovados em 27 suítes JVM** (`VAL-001`); `androidTest` **recompilado, não executado** (`VAL-002` / `VAL-003`). |
| **Preparação do Artefato v1.0 Demonstrativo (Fase 10)** | Execução de higiene (`chmod +x gradlew`, `.gitignore`, remoção de `.build-outputs/` antigos via `TEC-003`), geração/verificação do APK Debug único `Voz-Ancestral-debug.apk` (`22.692.801` bytes) e diagnóstico da pendência de assinatura Release sem keystore. | Orientação sobre preservação do artefato Debug v1.0 para testes externos em aparelho físico e delimitação da pendência Release. | APK Debug v1.0 único verificado (`VAL-005`); assinatura Release bloqueada externamente (`VAL-006`). |
| **Auditoria Geral e Regularização Documental SDD (Fase 11 / `DOC-001`–`DOC-007`)** | Inspeção técnica completa do repositório, redação retrospectiva e revisão final de consistência de `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md`, `AI_USAGE.md`, `README.md` e `docs/api-v1-contract.md` ao fechamento v1.0. | Planejamento sequencial das etapas documentais, formulação dos roteiros de auditoria/documentação/consistência e revisão crítica que identificou ajustes de precisão terminológica, contratual e de escopo. | Sete tarefas documentais concluídas (`DOC-001`–`DOC-007`): documentos SDD, `README.md` e `docs/api-v1-contract.md` alinhados ao código real, aos testes (`252/27`) e ao APK único. |

*Nota de transparência:* Não são atribuídos percentuais numéricos de participação entre trabalho humano, Google AI Studio e ChatGPT, por inexistir métrica objetiva confiável para essa divisão.

---

## 6. Responsabilidades e Decisões Humanas

Durante todo o ciclo do projeto, o **responsável humano** manteve o controle decisório, técnico e acadêmico sobre a condução do trabalho, incluindo:

1. **Definição de objetivos e escopo:** estabelecer a finalidade do projeto acadêmico **Voz Ancestral**, optar pela **Versão 1.0 Demonstrativa** (*Acervo Provisório*) focada nas telas **Início** e **Dicionário**, e restringir o conteúdo inicial a registros fictícios em português (`DEMO-001` a `DEMO-020`) para evitar exposição de vocabulário Parintintin sem validação e autorização adequadas;
2. **Seleção e aprovação de funcionalidades:** decidir o que deveria integrar a v1.0 e o que permaneceria fora da navegação ou como estrutura preparatória (`Encyclopedia`, `Favorites`, detalhes de verbetes, áudio e integração efetiva com servidor remoto);
3. **Direcionamento e curadoria de instruções:** formular objetivos, restrições e regras estritas para as etapas executadas com apoio do ChatGPT e do Google AI Studio;
4. **Avaliação das sugestões fornecidas pelas ferramentas:** aceitar, rejeitar ou solicitar reformulação de propostas técnicas, arquiteturais e redacionais;
5. **Autorização de alterações no projeto:** controlar quais modificações poderiam ser aplicadas no código-fonte ou exclusivamente na documentação (incluindo etapas restritas apenas à leitura ou exclusivamente documentais);
6. **Revisão dos relatórios e documentos produzidos:** realizar revisões críticas que levaram a ajustes corretivos concretos — por exemplo, refinar a descrição dos termos demonstrativos e da disponibilidade do servidor em `SPECIFICATION.md`, eliminar afirmações absolutas sobre isolamento arquitetural em `PLANNING.md`, esclarecer que `TEC-001` não é requisito funcional da v1.0 em `TASKS.md` e corrigir a nomenclatura de "testes unitários" para "testes automatizados na JVM" em `TEST_RESULTS.md`;
7. **Execução e solicitação de validações:** acionar verificações de build, suítes de testes na JVM, recuperação do APK Debug v1.0 para instalação externa em dispositivo Android e gestão das alterações no repositório remoto (via GitHub Sync / fluxo de controle do responsável);
8. **Decisão final de retenção:** determinar quais códigos, configurações e artefatos documentais permaneceriam no projeto.

---

## 7. Processo de Revisão e Validação das Contribuições

Nenhuma saída gerada ou sugerida por ferramenta de IA foi considerada automaticamente válida pelo simples fato de ter sido produzida. O fluxo adotado distingue cinco estágios independentes:

| Estágio | Significado no Processo do Projeto | Exemplo / Referência Concreta |
|---|---|---|
| **1. Sugestão produzida por IA** | Proposta de código, arquitetura, teste ou texto redigido pelo Google AI Studio ou estruturado como orientação com apoio do ChatGPT antes de verificação efetiva. | Esboços iniciais de documentos SDD ou propostas de implementação/refatoração antes de inspeção. |
| **2. Alteração efetivamente implementada** | Modificação gravada nos arquivos do repositório após autorização e aplicação no ambiente de desenvolvimento. | Código em `app/src/main/`, suítes em `app/src/test/` e arquivos `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md`. |
| **3. Alteração revisada** | Conteúdo conferido frente aos arquivos reais do repositório e submetido à revisão humana para correção de excessos, generalizações ou imprecisões. | Etapas pontuais de revisão aplicadas em `SPECIFICATION.md` (1.1.1), `PLANNING.md`, `TASKS.md` (Seção 8.2) e `TEST_RESULTS.md`. |
| **4. Teste efetivamente executado** | Verificação automatizada de fato executada em ambiente de compilação/JVM com registro em relatório gerado pela ferramenta de build. | Execução de `gradle :app:testDebugUnitTest` evoluindo de **24 XMLs** (`233` testes) para **25 XMLs** (`238` testes em `TEC-001`) e alcançando **27 XMLs** (`252` testes) no fechamento v1.0 em `app/build/test-results/testDebugUnitTest/`. |
| **5. Resultado comprovado** | Fato verificável e auditável registrado conforme `TEST_RESULTS.md`, separando aprovações reais de itens pendentes ou não executados. | Evolução auditada (**233/24** → **238/25** → **252 testes aprovados em 27 suítes JVM — `APROVADO`**); `androidTest` **`RECOMPILADO, NÃO EXECUTADO`**; APK Debug v1.0 único **`APROVADO`** (`22.692.801` bytes); testes em hardware físico, TalkBack, servidor oficial em produção e Release **`PENDENTE` / `BLOQUEADO`**. |

### Regras Práticas de Verificação Aplicadas
- **Testes sugeridos ou escritos com apoio de IA não contam como testes executados:** a suíte instrumentada (`app/src/androidTest`), mesmo recompilada via `:app:assembleDebugAndroidTest` (`1.056.983` bytes), permanece classificada estritamente como `RECOMPILADO, NÃO EXECUTADO` em `TEST_RESULTS.md` por não haver emulador/ADB no container.
- **Compilação não substitui validação funcional em dispositivo:** a geração bem-sucedida do APK Debug v1.0 não comprova desempenho, comportamento real de rede celular/Wi-Fi ou acessibilidade física via TalkBack — motivo pelo qual foi incluído um checklist explícito para validação manual externa em `TEST_RESULTS.md` (Seção 12).
- **Inspeção direta do código prevalece sobre memória ou suposições:** a reconstrução documental retrospectiva utilizou verificação direta dos arquivos Kotlin, XML, JSON, Gradle e relatórios XML de teste para evitar divergências entre texto e código.

---

## 8. Limitações e Riscos do Uso de IA

O emprego de ferramentas de IA no ciclo de desenvolvimento e documentação envolve riscos técnicos conhecidos, considerados e mitigados neste projeto:

1. **Código incorreto ou incompatível com o projeto:** sugestões automatizadas podem introduzir APIs incompatíveis com versões específicas configuradas no projeto (AGP `9.1.1`, Kotlin `2.2.10`, KSP `2.3.5`, Room `2.7.0`, Hilt `2.59`), erros de concorrência ou quebra de regras de negócio (como o tratamento de `CancellationException` inicialmente pendente em `EncyclopediaViewModel.kt`, mapeado como `TEC-001` em `TASKS.md` e posteriormente corrigido e validado com testes unitários).
2. **Sugestões arquiteturais inadequadas ou generalizações excessivas:** modelos podem propor abstrações desnecessárias ou descrever camadas com rigor maior do que o real (motivo pelo qual `PLANNING.md` havia registrado inicialmente o acoplamento pontual de classes de `data` ao qualificador `@ApplicationScope` de `di`, posteriormente eliminado por meio da tarefa `TEC-002` ao mover `@ApplicationScope` para `core.common`).
3. **Testes incompletos ou com escopo restrito:** testes gerados na JVM com `MockWebServer` e Robolectric validam contratos e lógica local, mas não garantem comportamento idêntico em hardware Android real, transições reais de conectividade, leitura com TalkBack ou resposta de servidor oficial em produção.
4. **Informações técnicas imprecisas:** na elaboração textual ou análise técnica podem ocorrer classificações imprecisas (como rotular um conjunto misto de testes JVM — domínio, Room, `MockWebServer`, Robolectric e Compose — apenas como "testes unitários", ou extrapolar o estado de um servidor externo sem evidência operacional direta).
5. **Documentação desalinhada ao estado real do aplicativo:** documentos gerados sem conferência direta contra o código podem apresentar funcionalidades futuras (`Encyclopedia`, `Favorites`, áudio, backend) como já entregues na v1.0.

Esses riscos demonstram que **a revisão humana contínua e a verificação por evidências técnicas concretas** (código-fonte, relatórios XML do Gradle, metadados de APK e testes em dispositivo real quando aplicável) são indispensáveis e insubstituíveis.

---

## 9. Transparência Acadêmica e Integridade

Para fins de integridade acadêmica, declara-se explicitamente que:

- O uso do **Google AI Studio** e do **ChatGPT** ocorreu como **ferramenta de apoio técnico, organizacional e documental** ao processo de engenharia de software e documentação do projeto **Voz Ancestral**, **não substituindo** o discernimento intelectual, a responsabilidade técnica nem a condução acadêmica do responsável pelo projeto.
- **Não se declara** que o projeto foi desenvolvido integralmente por Inteligência Artificial, assim como **não se declara** que foi desenvolvido sem o auxílio de Inteligência Artificial: trata-se de um processo assistido por IA sob direção, seleção, revisão crítica e decisão do responsável humano.
- Este registro busca exclusivamente **transparência metodológica e rastreabilidade** sobre como o software e sua documentação foram produzidos, sem invocar políticas institucionais, normas específicas de faculdade ou exigências de docentes que não constam no repositório.
- De igual modo, reforça-se a integridade cultural e linguística do projeto: nenhum conteúdo autêntico da língua Parintintin foi gerado ou simulado por IA como verbete oficial; todos os 20 registros atuais (`DEMO-001` a `DEMO-020`) são termos e expressões demonstrativos fictícios em português reservados exclusivamente para validação técnica da v1.0.

---

## 10. Considerações sobre Autoria e Responsabilidade

- **Não reivindicação de autoria exclusiva de código onde há assistência documentada:** reconhece-se abertamente que partes da implementação, da estrutura de testes e da redação técnica contaram com geração e assistência do **Google AI Studio**, bem como com apoio analítico e de revisão via **ChatGPT**.
- **Ausência de percentuais arbitrários:** não é apresentada nenhuma estimativa percentual de autoria humana versus automatizada, pois não existe medição auditável que sustente tal quantificação.
- **Responsabilidade integral pelo artefato entregue:** independentemente da ferramenta de apoio utilizada na geração de sugestões de código ou texto, a **responsabilidade intelectual, técnica, ética e acadêmica** pelas decisões de escopo, pela validação das funcionalidades mantidas, pela integridade dos dados demonstrativos e pelo conteúdo final submetido pertence integralmente ao **responsável humano pelo projeto**.

---

## 11. Histórico e Observações

- **Natureza e versão do documento:** registro retrospectivo atualizado no fechamento técnico da **Versão 1.0 Demonstrativa** do projeto acadêmico **Voz Ancestral** (`Acervo Provisório`), coerente com `SPECIFICATION.md`, `PLANNING.md`, `TASKS.md`, `TEST_RESULTS.md`, `README.md` e `docs/api-v1-contract.md`.
- **Atualizações futuras recomendadas:** caso novas ferramentas de IA venham a ser incorporadas após a v1.0, caso ocorram novas rodadas significativas de implementação (como ativação do backend oficial, módulo `Encyclopedia`, `Favorites` ou áudio), ou caso sejam executadas validações em dispositivo físico (`VAL-003`, `VAL-004`), este documento deverá ser atualizado para manter a rastreabilidade.
- **Preservação do ambiente e histórico de rodadas:** rodadas exclusivamente documentais mantiveram código-fonte e builds inalterados; já a rodada final de fechamento técnico executou `TEC-002`–`TEC-004`, integração cliente de `/v1/manifest.json`, ampliação para `252` testes / `27` suítes JVM, recompilação de `androidTest`, empacotamento único de `Voz-Ancestral-debug.apk` e atualização integral dos 7 documentos Markdown sem execução de comandos Git neste container (onde `.git` não está presente).
