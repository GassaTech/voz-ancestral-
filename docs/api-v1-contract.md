# Especificação Proposta do Contrato da API v1 — Acervo Parintintin

> **Status do documento:** Especificação técnica **proposta** para o contrato da API remota do acervo.  
> O cliente Android está configurado para o endereço remoto `https://archive.parintintin.org/` (`NetworkModule`), porém **não foi possível confirmar a disponibilidade operacional do servidor remoto no ambiente de auditoria** e os endpoints descritos abaixo ainda não foram validados contra um ambiente em produção. Este contrato documenta exatamente o que o cliente Android atual espera receber e processar (`ParintintinApiService`, `ArchiveDtos`, `ArchiveMappers`, `RemoteArchiveDataSource`), cuja desserialização e comportamento HTTP foram verificados exclusivamente em ambiente controlado com `MockWebServer` (`ParintintinApiContractTest`).
>
> **Nota sobre exemplos:** Todos os payloads de exemplo neste documento utilizam dados **exclusivamente ilustrativos/fictícios**, sem representar vocabulário, pronúncia ou conteúdo cultural Parintintin autêntico.

---

## 1. Visão Geral e Configuração do Cliente

- **Base URL configurada no cliente (`NetworkModule`):** `https://archive.parintintin.org/`
- **Protocolo:** HTTPS obrigatório (o aplicativo não habilita tráfego cleartext HTTP).
- **Timeouts atuais do cliente (`OkHttpClient`):**
  - `connectTimeout`: `5` segundos
  - `readTimeout`: `10` segundos
- **Serialização:** JSON UTF-8 desserializado por **Moshi** com adaptadores gerados em tempo de compilação (`@JsonClass(generateAdapter = true)` via KSP).

### Endpoints Previstos

| Método | Caminho | Finalidade | Status de integração no cliente (v1.0) |
|---|---|---|---|
| `GET` | `/v1/manifest.json` | Metadados de versão e contagens esperadas do snapshot | **Integrado** via `RemoteArchiveDataSource.fetchArchiveManifest()` e chamado em `ArchiveSyncRepositoryImpl.syncArchive()` antes de `/v1/dictionary.json`, validando `version` não vazia e contagens `>= 0` e gravando `lastSuccessfulRemoteVersion` |
| `GET` | `/v1/dictionary.json` | Lista completa de verbetes do dicionário | Implementado em `OfflineFirstDictionaryRepository` e **integrado** ao fluxo efetivo de `ArchiveSyncRepositoryImpl.syncArchive()` após a leitura do manifesto |
| `GET` | `/v1/articles.json` | Lista completa de artigos culturais da enciclopédia | Documentado no contrato, declarado no Retrofit (`getCulturalArticles()`) e implementado em `OfflineFirstEncyclopediaRepository`, porém **não integrado** ao fluxo de `ArchiveSyncRepositoryImpl.syncArchive()` na versão 1.0 |

### 1.1. Distinção Obrigatória de Estado Real (Cliente vs. Produção)

| Nível Verificado | Situação Comprovada na Versão 1.0 |
|---|---|
| **1. Cliente Retrofit/OkHttp/Moshi implementado** | **SIM.** `NetworkModule`, `ParintintinApiService`, `ArchiveDtos` e `RemoteArchiveDataSource` estão implementados e apontam para `https://archive.parintintin.org/`. |
| **2. Requisições realmente executadas** | **APENAS EM TESTES LOCAIS (`MockWebServer`).** Não há evidência de execução contra o domínio externo em ambiente real. |
| **3. Respostas remotas recebidas** | **APENAS RESPOSTAS SIMULADAS.** As respostas `HTTP 200`, `HTTP 200 []`, `404`/`500` e timeouts para `/v1/manifest.json` e `/v1/dictionary.json` foram obtidas exclusivamente via `MockWebServer` (`ParintintinApiContractTest`, `RemoteArchiveDataSourceTest`, `DictionaryOfflineIntegrationTest`); a disponibilidade operacional de `https://archive.parintintin.org/` **não foi confirmada**. |
| **4. Sincronização realmente concluída** | **NÃO COMPROVADA EM PRODUÇÃO.** Em ambiente controlado (`MockWebServer`), o fluxo completo executa sequencialmente `GET /v1/manifest.json` e `GET /v1/dictionary.json`, gravando `version` em `sync_metadata`; `/v1/articles.json` permanece fora da sincronização na v1.0. |

---

## 2. Especificação dos Endpoints e Estrutura JSON

### 2.1. `GET /v1/manifest.json`

Retorna um único objeto JSON descrevendo a versão publicada do acervo e os totais esperados nos demais arquivos.

#### Campos

| Campo JSON | Tipo JSON | Tipo Kotlin (`ArchiveManifestDto`) | Obrigatório | Permite `null` | Descrição |
|---|---|---|---|---|---|
| `version` | `string` | `String` | Sim | Não | Identificador de versão do snapshot publicado (ex.: SemVer `"1.0.0"`). |
| `entries_count` | `number` (inteiro) | `Int` | Sim | Não | Quantidade exata de objetos presentes em `/v1/dictionary.json`. |
| `articles_count` | `number` (inteiro) | `Int` | Sim | Não | Quantidade exata de objetos presentes em `/v1/articles.json`. |

#### Exemplo de resposta (`HTTP 200 OK`)

```json
{
  "version": "1.0.0",
  "entries_count": 150,
  "articles_count": 25
}
```

---

### 2.2. `GET /v1/dictionary.json`

Retorna um array JSON contendo todos os verbetes disponíveis no snapshot atual.

#### Campos por item do array

| Campo JSON | Tipo JSON | Tipo Kotlin (`DictionaryEntryDto`) | Obrigatório | Permite `null` | Descrição |
|---|---|---|---|---|---|
| `id` | `string` | `String` | Sim | Não | Identificador único e estável do verbete. |
| `parintintin_word` | `string` | `String` | Sim | Não | Termo/expressão principal exibido no verbete. |
| `phonetic_ipa` | `string` | `String` | Sim | Não | Transcrição fonética (IPA). |
| `portuguese_translation` | `string` | `String` | Sim | Não | Tradução ou glosa em português. |
| `grammatical_class` | `string` | `String` | Sim | Não | Identificador do enum `GrammaticalClass` (ver seção 3). |
| `semantic_category` | `string` | `String` | Sim | Não | Identificador do enum `SemanticCategory` (ver seção 3). |
| `example_parintintin` | `string` | `String` | Sim | Não | Frase de exemplo na língua (pode ser `""` quando não aplicável, nunca `null`). |
| `example_portuguese` | `string` | `String` | Sim | Não | Tradução da frase de exemplo (pode ser `""` quando não aplicável, nunca `null`). |
| `cultural_note` | `string` | `String` | Sim | Não | Nota contextual/cultural (pode ser `""` quando não aplicável, nunca `null`). |
| `audio_url` | `string \| null` | `String? = null` | Não | **Sim** | URL do áudio de pronúncia. Pode ser omitida ou enviada como `null` (cliente assume `null`). |
| `updated_at_epoch_millis` | `number` (inteiro 64-bit) | `Long` | Sim | Não | Data/hora da última atualização em milissegundos desde a Unix Epoch (UTC). |

#### Exemplo de resposta (`HTTP 200 OK`, dados ilustrativos)

```json
[
  {
    "id": "entry_001",
    "parintintin_word": "EXEMPLO",
    "phonetic_ipa": "exemplo",
    "portuguese_translation": "Tradução ilustrativa",
    "grammatical_class": "NOUN",
    "semantic_category": "NATURE",
    "example_parintintin": "",
    "example_portuguese": "",
    "cultural_note": "",
    "audio_url": null,
    "updated_at_epoch_millis": 1794787200000
  }
]
```

---

### 2.3. `GET /v1/articles.json`

Retorna um array JSON contendo todos os artigos culturais da enciclopédia disponíveis no snapshot atual.

#### Campos por item do array

| Campo JSON | Tipo JSON | Tipo Kotlin (`CulturalArticleDto`) | Obrigatório | Permite `null` | Descrição |
|---|---|---|---|---|---|
| `id` | `string` | `String` | Sim | Não | Identificador único e estável do artigo. |
| `title` | `string` | `String` | Sim | Não | Título do artigo. |
| `subtitle` | `string` | `String` | Sim | Não | Subtítulo do artigo. |
| `summary` | `string` | `String` | Sim | Não | Resumo curto exibido em listagens e pré-visualizações. |
| `body_paragraphs` | `array<string>` | `List<String>` | Sim | Não | Lista ordenada de parágrafos do corpo do artigo. Nenhum elemento deve ser `null`. |
| `topic` | `string` | `String` | Sim | Não | Identificador do enum `CulturalTopic` (ver seção 3). |
| `reading_time_minutes` | `number` (inteiro) | `Int` | Sim | Não | Tempo estimado de leitura em minutos. |
| `source_reference` | `string` | `String` | Sim | Não | Referência bibliográfica, documental ou comunitária. |
| `updated_at_epoch_millis` | `number` (inteiro 64-bit) | `Long` | Sim | Não | Data/hora da última atualização em milissegundos desde a Unix Epoch (UTC). |

#### Exemplo de resposta (`HTTP 200 OK`, dados ilustrativos)

```json
[
  {
    "id": "article_001",
    "title": "Título ilustrativo",
    "subtitle": "Subtítulo ilustrativo",
    "summary": "Resumo ilustrativo",
    "body_paragraphs": [
      "Primeiro parágrafo ilustrativo.",
      "Segundo parágrafo ilustrativo."
    ],
    "topic": "HISTORY",
    "reading_time_minutes": 5,
    "source_reference": "Referência ilustrativa",
    "updated_at_epoch_millis": 1794787200000
  }
]
```

---

## 3. Enums Aceitos

Os campos categóricos chegam como `string` no JSON para evitar quebra de desserialização caso novos valores sejam introduzidos no futuro. O backend deve enviar **exatamente** os identificadores em maiúsculas abaixo:

### 3.1. `grammatical_class` (`GrammaticalClass`)

| Valor no JSON | Rótulo no aplicativo |
|---|---|
| `NOUN` | Substantivo |
| `VERB` | Verbo |
| `ADJECTIVE` | Adjetivo |
| `PRONOUN` | Pronome |
| `ADVERB` | Advérbio |
| `EXPRESSION` | Expressão |
| `TOPONYM` | Topônimo |

- **Fallback atual no cliente (`toDomain()`):** valores desconhecidos exibem `NOUN` na UI.

### 3.2. `semantic_category` (`SemanticCategory`)

| Valor no JSON | Rótulo no aplicativo |
|---|---|
| `NATURE` | Natureza e Território |
| `FAMILY_SOCIETY` | Pessoas e Parentesco |
| `ANIMALS` | Fauna Amazônica |
| `FOOD` | Alimentação e Roça |
| `CULTURE_SPIRIT` | Cultura e Espiritualidade |
| `EVERYDAY` | Comunicação Cotidiana |

- **Fallback atual no cliente (`toDomain()`):** valores desconhecidos exibem `EVERYDAY` na UI.

### 3.3. `topic` (`CulturalTopic`)

| Valor no JSON | Rótulo no aplicativo |
|---|---|
| `HISTORY` | História e Identidade |
| `LINGUISTICS` | Língua e Fonologia |
| `TERRITORY` | Território e Rios |
| `ORAL_TRADITION` | Tradição Oral e Cantos |
| `MATERIAL_CULTURE` | Cultura Material |

- **Fallback atual no cliente (`toDomain()`):** valores desconhecidos exibem `HISTORY` na UI.

> **Observação importante de implementação atual:** em `ArchiveMappers.kt`, o valor de `grammatical_class`, `semantic_category` e `topic` é persistido literalmente no banco local Room (`toEntity()`), aplicando o fallback apenas na conversão para domínio (`toDomain()`). Portanto, enviar strings fora do padrão (como minúsculas `"noun"`) impede o funcionamento correto de filtros SQL por categoria/tópico no estado atual do cliente.

---

## 4. Regras de Integridade dos Dados

Para evitar ambiguidades entre o que o aplicativo **já valida hoje** e o que é **exigência contratual proposta** para o futuro backend, as regras estão divididas em dois grupos:

### 4.1. Comportamento efetivamente implementado no cliente

1. **Desserialização estrita de campos e tipos dos DTOs:**
   - Os adaptadores Moshi gerados via KSP validam a correspondência dos nomes (`@Json`) e dos tipos definidos em `ArchiveDtos` (`String`, `Int`, `Long`, `List<String>`).
2. **Rejeição de campos obrigatórios ausentes ou `null`:**
   - Com exceção de `audio_url` em `DictionaryEntryDto` (`String? = null`), a ausência ou envio de `null` em qualquer campo obrigatório dos DTOs gera `JsonDataException` e rejeita toda a resposta daquele endpoint, conforme validado em `ParintintinApiContractTest`.
   - *Ressalva técnica conhecida:* o adaptador KSP gerado para `CulturalArticleDto` (`Types.newParameterizedType(List::class.java, String::class.java)`) rejeita `body_paragraphs: null`, mas não bloqueia automaticamente um elemento `null` dentro do array em tempo de parse.
3. **Preservação de favoritos e marcadores durante os upserts:**
   - Em cada repositório (`OfflineFirstDictionaryRepository` e `OfflineFirstEncyclopediaRepository`), a leitura de `getFavoriteIds()` / `getBookmarkedIds()`, o mapeamento para entidades Room e o `upsertAll(...)` executam atomicamente dentro de `database.withTransaction`, preservando os flags locais `is_favorite` e `is_bookmarked`.
4. **Tolerância a campos JSON desconhecidos:**
   - Propriedades JSON adicionais não mapeadas nos DTOs são ignoradas automaticamente (`reader.skipName(); reader.skipValue()`), permitindo evolução aditiva retrocompatível dos payloads da v1.
5. **Validação estrutural básica do manifesto e tratamento de falhas HTTP/parse:**
   - `RemoteArchiveDataSource.fetchArchiveManifest()` valida que `version` não é vazia (`isNotBlank()`) e que `entriesCount >= 0` e `articlesCount >= 0` (rejeitando violações com `RemoteErrorKind.DESERIALIZATION_ERROR`).
   - Exceções HTTP (`HttpException` para `4xx`/`5xx`) e falhas de leitura/desserialização (`JsonDataException`, `JsonEncodingException`, `EOFException`) são capturadas em `RemoteArchiveDataSource` e convertidas em `AppResult.Error`, propagando normalmente `CancellationException`.
6. **Persistência independente por repositório:**
   - Cada repositório realiza sua própria transação Room isoladamente para seu respectivo conjunto de dados (dicionário ou enciclopédia).

### 4.2. Requisitos propostos para o futuro contrato (ainda não validados pelo cliente)

Os itens abaixo definem regras esperadas do servidor e dos dados publicados; **o cliente atual ainda não executa validações automáticas para eles**:

1. **Padronização semântica de `version` e comparação incremental no cliente:**
   - Propõe-se que `version` siga um formato acordado (ex.: SemVer `"1.0.0"` ou identificador monotônico) e que futuras versões possam evitar downloads redundantes comparando `remoteVersion` com `lastSuccessfulRemoteVersion`. Atualmente o cliente valida que `version` não está em branco e persiste o valor recebido, mas ainda não realiza *skip* condicional de `/v1/dictionary.json`.
2. **IDs únicos e estáveis entre versões (`id`):**
   - Propõe-se que cada registro em `/v1/dictionary.json` e `/v1/articles.json` tenha `id` único no array e estável entre publicações. Como o Room usa `@PrimaryKey` com `OnConflictStrategy.REPLACE`, IDs duplicados na mesma resposta sobrescrevem uns aos outros silenciosamente e alterações de `id` entre versões fazem o usuário perder `is_favorite` / `is_bookmarked` — não há verificação explícita de duplicidade pelo cliente.
3. **Contagens do manifesto iguais ao tamanho dos arrays:**
   - Propõe-se que `manifest.entries_count == dictionary.json.length` e `manifest.articles_count == articles.json.length`. Atualmente o cliente valida apenas que `entries_count >= 0` e `articles_count >= 0`, sem comparar esses valores contra o tamanho do array recebido.
4. **Timestamps semanticamente válidos (`updated_at_epoch_millis`):**
   - Propõe-se que `updated_at_epoch_millis` represente o instante real da última atualização em UTC, expresso em **milissegundos** desde `1970-01-01T00:00:00Z` (não em segundos nem em strings ISO-8601). O cliente valida apenas que o valor JSON é numérico e compatível com `Long`, sem validação semântica de intervalo ou unidade.
5. **Campos textuais vazios vs nulos e elementos de `body_paragraphs`:**
   - Propõe-se que textos opcionais ausentes (`example_parintintin`, `example_portuguese`, `cultural_note`) sejam enviados como `""` (nunca `null`) e que o servidor nunca emita `null` dentro de `body_paragraphs`.
6. **Publicação consistente e incremento de versão:**
   - Propõe-se que os três arquivos (`/v1/manifest.json`, `/v1/dictionary.json`, `/v1/articles.json`) sejam publicados como um snapshot consistente e que `version` seja incrementada em qualquer alteração de conteúdo (detalhado na seção 6).

---

## 5. Comportamento HTTP Esperado

1. **Resposta de sucesso:**
   - Status **`200 OK`** com corpo JSON válido correspondente ao schema do endpoint.
2. **`Content-Type` e codificação:**
   - Header `Content-Type: application/json; charset=utf-8`.
   - Codificação obrigatória em **UTF-8** para preservação fiel de acentuação portuguesa e símbolos fonéticos IPA.
3. **Respostas de erro HTTP (`4xx` / `5xx`):**
   - Códigos como `404 Not Found` ou `500 Internal Server Error` produzem `HttpException` no Retrofit.
   - O `RemoteArchiveDataSource` captura a exceção e retorna `AppResult.Error`, impedindo a gravação de dados para o endpoint que falhou e mantendo intactos seus registros locais.
   - Recomenda-se que o servidor retorne corpo JSON estruturado em erros (mesmo que o cliente atualmente apenas registre a exceção HTTP sem parsear o corpo de erro).
4. **JSON malformado e campos inválidos:**
   - JSON sintaticamente inválido (`JsonEncodingException`), resposta truncada (`EOFException`), campo obrigatório ausente/nulo ou tipo incompatível (`JsonDataException`) invalidam a desserialização do **endpoint inteiro** (processamento atômico "tudo ou nada" por arquivo).
   - O `RemoteArchiveDataSource` converte essas falhas em `AppResult.Error`, preservando os dados locais do endpoint afetado.
   - `CancellationException` nunca é suprimida pelo cliente e continua propagando cancelamento estruturado.

---

## 6. Regras de Sincronização

### 6.1. Relação entre versão do manifesto e conteúdo

- **Requisito proposto para o futuro backend:** os arquivos `/v1/manifest.json`, `/v1/dictionary.json` e `/v1/articles.json` devem ser publicados de forma consistente entre si, representando um mesmo snapshot do acervo.
- **Comportamento atual no cliente (versão 1.0):** `ArchiveSyncRepositoryImpl.syncArchive()` consulta primeiramente `GET /v1/manifest.json` (via `RemoteArchiveDataSource.fetchArchiveManifest()`) e, após validação estrutural básica (`version` não em branco e contagens `>= 0`), executa `GET /v1/dictionary.json` (via `dictionaryRepository.syncFromRemote()`), gravando `lastSuccessfulRemoteVersion = manifest.version` nos metadados locais quando há verbetes sincronizados. A enciclopédia (`/v1/articles.json`) **não participa** desse fluxo na versão 1.0 (a presença de `RemoteArchiveDataSource.fetchCulturalArticles()` e `OfflineFirstEncyclopediaRepository` constitui apenas estrutura preparatória).

### 6.2. Quando a versão do manifesto deve ser incrementada *(Requisito proposto)*

Como parte do contrato proposto para o servidor, a propriedade `version` em `/v1/manifest.json` deve ser incrementada/alterada **sempre que houver qualquer mudança** nos dados servidos por `/v1/dictionary.json` ou `/v1/articles.json`, incluindo:
- Adição de novos verbetes ou artigos;
- Edição de conteúdo, traduções, exemplos, notas, parágrafos, `audio_url` ou metadados;
- Correções de categorias/tópicos;
- Eventual remoção de registros (conforme política a ser definida).

Publicações sem alteração de conteúdo devem manter o mesmo valor de `version`. *(O cliente já registra `manifest.version` em `sync_metadata`, mas ainda não pula o download de `/v1/dictionary.json` quando `version` permanece igual.)*

### 6.3. Comportamento esperado diante de falhas e execução da sincronização

- **Comportamento efetivamente implementado no aplicativo na versão 1.0 (Offline-First):**
  - `ArchiveSyncRepositoryImpl.syncArchive()` executa sob `syncMutex.withLock` em `dispatchers.io`, colocando `syncState` em `SyncState.Syncing`.
  - Primeiro executa `runSeedUpdateSafely()` (`dictionarySeedUpdater.updateIfNeeded()`), isolando eventuais falhas locais do seed para não bloquear a tentativa de sincronização remota.
  - Em seguida, consulta `remoteArchiveDataSource.fetchArchiveManifest()` (`GET /v1/manifest.json`). Se o manifesto falhar ou for inválido (`AppResult.Error`), a sincronização é encerrada com `OfflineFallback` sem chamar `/v1/dictionary.json` e preservando dados e histórico locais.
  - Com manifesto válido, invoca **exclusivamente** `dictionaryRepository.syncFromRemote()` (`OfflineFirstDictionaryRepository`), **não** disparando `encyclopediaRepository.syncFromRemote()` na versão 1.0 (`encyclopediaRepository` é injetado em `ArchiveSyncRepositoryImpl` e utilizado atualmente apenas em `observeArchiveStats()`).
  - Em caso de sucesso com verbetes (`AppResult.Success(RemoteSyncOutcome.Synced(entriesCount))`):
    - `OfflineFirstDictionaryRepository.syncFromRemote()` persiste os verbetes dentro de transação Room própria (`database.withTransaction`), preservando `is_favorite`.
    - `ArchiveSyncRepositoryImpl` grava `SyncMetadataEntity` via `syncMetadataDao.upsert(...)` com `lastAttemptStatus = STATUS_SYNCED`, `lastSuccessfulSyncEpochMillis = now`, `lastSuccessfulEntriesCount = outcome.entriesCount`, `lastSuccessfulArticlesCount = null` (mantido `null` explicitamente na v1.0 por ausência de sincronização da enciclopédia) e `lastSuccessfulRemoteVersion = manifest.version`, transitando `syncState` para `SyncState.Synced`.
  - Em caso de lista remota vazia (`AppResult.Success(RemoteSyncOutcome.Empty)`), aplica o tratamento detalhado na Seção 6.4 (`SyncState.RemoteEmpty`).
  - Em caso de falha remota (`AppResult.Error`) no manifesto ou no dicionário, ou exceção não canceladora:
    - Nenhum dado é gravado em `dictionary_entries`, preservando integralmente os registros e favoritos locais existentes.
    - `ArchiveSyncRepositoryImpl` classifica a conectividade atual via `SyncFailureClassifier.classify(networkConnectivityObserver.connectivityState.value)` (`NO_INTERNET`, `UNVALIDATED_NETWORK` ou `SERVER_UNAVAILABLE`), persiste `STATUS_OFFLINE_FALLBACK` via `syncMetadataDao.recordAttemptFailure(...)` preservando intactos `lastSuccessfulSyncEpochMillis`, `lastSuccessfulEntriesCount`, `lastSuccessfulArticlesCount` e `lastSuccessfulRemoteVersion`, e transita `syncState` para `SyncState.OfflineFallback` (ou mantém fallback em memória caso a escrita no Room falhe).
    - `CancellationException` nunca é convertida em falha offline: o estado transitório é restaurado e o cancelamento é propagado.
  - *Nota para evolução futura (pós-v1.0):* embora `OfflineFirstDictionaryRepository` e `OfflineFirstEncyclopediaRepository` possuam cada qual sua própria transação `database.withTransaction` independente (sem transação global envolvendo ambos), qualquer sincronização futura conjunta de dicionário e enciclopédia ainda precisará ser integrada e ter sua semântica de falha parcial definida antes da ativação.

### 6.4. Respostas vazias (`[]`)

- **Dicionário (`GET /v1/dictionary.json` — comportamento atual explícito):**
  - `OfflineFirstDictionaryRepository.syncFromRemote()` retorna `AppResult.Success(RemoteSyncOutcome.Empty)` imediatamente **sem abrir transação** e **sem apagar registros locais ou favoritos existentes**.
  - `ArchiveSyncRepositoryImpl.syncArchive()` diferencia explicitamente `RemoteSyncOutcome.Empty` de uma sincronização oficial com dados (`RemoteSyncOutcome.Synced(entriesCount)` com `entriesCount > 0`):
    - Registra `lastAttemptStatus = SyncMetadataEntity.STATUS_REMOTE_EMPTY` via `SyncMetadataDao.recordRemoteEmptyAttempt(...)` e transita `syncState` para `SyncState.RemoteEmpty`.
    - **Não** grava `STATUS_SYNCED`, **não** atualiza `lastSuccessfulSyncEpochMillis`, `lastSuccessfulEntriesCount`, `lastSuccessfulArticlesCount` nem `lastSuccessfulRemoteVersion` (preservando eventual histórico oficial anterior) e **não** trata a resposta vazia como falha de rede (`OfflineFallback`).
    - Enquanto nenhuma sincronização oficial com verbetes válidos (`entriesCount > 0`) tiver sido concluída (`lastSuccessfulSyncEpochMillis == null`), futuras atualizações controladas do Seed demonstrativo continuam desbloqueadas.
- **Enciclopédia (`GET /v1/articles.json` — comportamento atual explícito):**
  - Na versão 1.0 a enciclopédia permanece fora do fluxo de sincronização de `ArchiveSyncRepositoryImpl.syncArchive()`. Caso chamado diretamente, `OfflineFirstEncyclopediaRepository.syncFromRemote()` retorna `AppResult.Success(0)` imediatamente **sem abrir transação** e **sem apagar registros locais existentes**.

### 6.5. Política de exclusão de registros (**Decisão Pendente**)

- **Comportamento atual:** o cliente utiliza `@Insert(onConflict = OnConflictStrategy.REPLACE)` apenas para os IDs presentes na resposta remota e **não remove registros locais que deixaram de existir no servidor**.
- Como a disponibilidade operacional do servidor remoto não foi confirmada e o contrato definitivo de reconciliação ainda não foi fechado, **não há política de exclusão definida neste momento**. Essa regra permanece como **decisão pendente** antes da integração completa de reconciliação no cliente.

---

## 7. Decisões de Contrato Pendentes para Definição Futura

Antes de homologar o backend operacional em produção, precisam ser definidas:

1. **Política de exclusão de registros removidos no servidor:**
   - Definir se a API trabalhará por **snapshot completo** (onde ausência na lista significa remoção no cliente, respeitando regra explícita para respostas `[]` acidentais) ou por **tombstones** (ex.: campo `is_deleted: Boolean` / `deleted_at_epoch_millis`), além do tratamento para itens excluídos que estavam favoritados pelo usuário.
2. **Formato definitivo e otimização incremental por `version` no manifesto:**
   - Confirmar se `version` será SemVer (`"1.0.0"`), timestamp/revision string ou hash de conteúdo, e se o cliente passará a pular o download completo quando `remoteVersion == lastSuccessfulRemoteVersion`.
3. **Validação cruzada de contagens (`entries_count` / `articles_count`):**
   - Definir se uma divergência entre o manifesto e o tamanho real das listas retornadas deve ser tratada como erro de integridade ou apenas alerta/telemetria (considerando risco de inconsistência transitória durante deploy de arquivos estáticos).
4. **Suporte a cache condicional HTTP (`ETag` / `If-None-Match` / `304 Not Modified`):**
   - Definir se o servidor servirá headers `ETag`/`Cache-Control` e compressão `gzip` para reduzir tráfego quando o acervo crescer.
5. **Política para campos textuais vazios vs opcionais e URLs de áudio:**
   - Confirmar se `example_parintintin`, `example_portuguese` e `cultural_note` continuarão obrigatórios (aceitando `""`) e qual será o contrato de hospedagem/formato de `audio_url` (ex.: MP3/AAC/OGG, cache offline, expiração).
6. **Paginação ou versionamento incremental futuro:**
   - Confirmar se a v1 permanecerá com arquivos estáticos completos (`dictionary.json` / `articles.json`) e qual o limite de tamanho esperado compatível com `readTimeout = 10s`.
7. **Estrutura padronizada de payload de erro HTTP:**
   - Definir o schema JSON retornado em `4xx`/`5xx` (caso o cliente passe a ler mensagens/códigos estruturados de erro no futuro).
