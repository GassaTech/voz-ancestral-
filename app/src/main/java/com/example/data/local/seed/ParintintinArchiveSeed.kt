package com.example.data.local.seed

import com.example.core.common.DispatcherProvider
import com.example.data.local.dao.DictionaryDao
import com.example.data.local.dao.EncyclopediaDao
import com.example.data.local.dao.SyncMetadataDao
import com.example.data.local.entity.CulturalArticleEntity
import com.example.data.local.entity.DictionaryEntryEntity
import com.example.data.local.entity.SyncMetadataEntity
import com.example.domain.model.CulturalTopic
import com.example.domain.model.GrammaticalClass
import com.example.domain.model.SemanticCategory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class ArchiveSeeder @Inject constructor(
    private val dictionaryDao: DictionaryDao,
    private val encyclopediaDao: EncyclopediaDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val dispatchers: DispatcherProvider
) {
    private val mutex = Mutex()

    suspend fun ensureSeeded() {
        mutex.withLock {
            withContext(dispatchers.io) {
                val now = System.currentTimeMillis()
                if (dictionaryDao.countOnce() == 0) {
                    dictionaryDao.upsertAll(ParintintinArchiveSeed.dictionaryEntries(now))
                }
                if (encyclopediaDao.countOnce() == 0) {
                    encyclopediaDao.upsertAll(ParintintinArchiveSeed.culturalArticles(now))
                }
                syncMetadataDao.upsert(
                    SyncMetadataEntity(
                        lastSyncEpochMillis = now,
                        remoteVersion = "seed-v1",
                        lastStatus = "LOCAL_SEED"
                    )
                )
            }
        }
    }
}

object ParintintinArchiveSeed {

    fun dictionaryEntries(timestamp: Long): List<DictionaryEntryEntity> = listOf(
        DictionaryEntryEntity(
            id = "par_kagwahiva",
            parintintinWord = "Kagwahiva",
            phoneticIpa = "/kaŋwaˈhiʋa/",
            portugueseTranslation = "Nossa gente; nós mesmos (autodenominação do povo Parintintin)",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.FAMILY_SOCIETY.name,
            exampleParintintin = "Kagwahiva nhande.",
            examplePortuguese = "Nós somos Kagwahiva.",
            culturalNote = "Termo de autoidentificação compartilhado pelos povos Kagwahiva da bacia do Rio Madeira, entre eles os Parintintin.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_y",
            parintintinWord = "Y",
            phoneticIpa = "/ɨ/",
            portugueseTranslation = "Água, rio, igarapé",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.NATURE.name,
            exampleParintintin = "Y oho.",
            examplePortuguese = "O rio corre / há água.",
            culturalNote = "A vogal central fechada /ɨ/ é característica das línguas Tupi-Guarani e frequentemente grafada como 'y'.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_ywy",
            parintintinWord = "Ywy",
            phoneticIpa = "/ɨˈβɨ/",
            portugueseTranslation = "Terra, chão, território",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.NATURE.name,
            exampleParintintin = "Ywy porã.",
            examplePortuguese = "Terra boa / fértil.",
            culturalNote = "Designa tanto o solo físico quanto o espaço de moradia e manejo tradicional na floresta.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_kaa",
            parintintinWord = "Ka'a",
            phoneticIpa = "/kaˈʔa/",
            portugueseTranslation = "Mata, floresta",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.NATURE.name,
            exampleParintintin = "Ka'a pe oho.",
            examplePortuguese = "Foi para a mata.",
            culturalNote = "O apóstrofo marca a oclusiva glotal /ʔ/, consoante distintiva entre sílabas vocálicas em Parintintin.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_tata",
            parintintinWord = "Tata",
            phoneticIpa = "/taˈta/",
            portugueseTranslation = "Fogo, brasa",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.NATURE.name,
            exampleParintintin = "Tata rendy.",
            examplePortuguese = "Chama do fogo.",
            culturalNote = "Elemento central na preparação da farinha de mandioca e nos encontros noturnos na aldeia.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_kwarahy",
            parintintinWord = "Kwarahy",
            phoneticIpa = "/kwaɾaˈhɨ/",
            portugueseTranslation = "Sol, luz do dia",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.NATURE.name,
            exampleParintintin = "Kwarahy osẽ.",
            examplePortuguese = "O sol nasce.",
            culturalNote = "Relacionado à medição tradicional do tempo ao longo do dia e das estações amazônicas (seca e cheia).",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_jasy",
            parintintinWord = "Jasy",
            phoneticIpa = "/dʒaˈsɨ/",
            portugueseTranslation = "Lua, mês",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.NATURE.name,
            exampleParintintin = "Jasy pyahu.",
            examplePortuguese = "Lua nova.",
            culturalNote = "As fases de Jasy orientam atividades de plantio, pesca e narrativas tradicionais noturnas.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_nheenga",
            parintintinWord = "Nhe'enga",
            phoneticIpa = "/ɲeˈʔeŋa/",
            portugueseTranslation = "Língua, fala, palavra, idioma",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.EVERYDAY.name,
            exampleParintintin = "Kagwahiva nhe'enga.",
            examplePortuguese = "A língua do povo Kagwahiva.",
            culturalNote = "Expressa tanto a faculdade da fala quanto o idioma comunitário transmitido pelos anciãos.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_oka",
            parintintinWord = "Oka",
            phoneticIpa = "/ˈoka/",
            portugueseTranslation = "Casa, habitação",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.FAMILY_SOCIETY.name,
            exampleParintintin = "Oka pe apyta.",
            examplePortuguese = "Fico em casa.",
            culturalNote = "Refere-se à moradia familiar e ao núcleo de convivência cotidiana da aldeia.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_kunha",
            parintintinWord = "Kunhã",
            phoneticIpa = "/kuˈɲã/",
            portugueseTranslation = "Mulher",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.FAMILY_SOCIETY.name,
            exampleParintintin = "Kunhã oma'ẽ.",
            examplePortuguese = "A mulher observa.",
            culturalNote = "O til indica nasalização vocálica, traço fonológico essencial para diferenciar significados.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_kurumi",
            parintintinWord = "Kurumi",
            phoneticIpa = "/kuɾuˈmi/",
            portugueseTranslation = "Menino, criança",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.FAMILY_SOCIETY.name,
            exampleParintintin = "Kurumi oñani.",
            examplePortuguese = "O menino corre.",
            culturalNote = "Usado no cotidiano familiar e em iniciativas bilíngues escolares nas Terras Indígenas.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_pira",
            parintintinWord = "Pira",
            phoneticIpa = "/piˈɾa/",
            portugueseTranslation = "Peixe",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.ANIMALS.name,
            exampleParintintin = "Pira heta y pe.",
            examplePortuguese = "Há muitos peixes no rio.",
            culturalNote = "Base alimentar fundamental dos povos ribeirinhos do médio e baixo Rio Madeira.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_guyra",
            parintintinWord = "Guyra",
            phoneticIpa = "/ɡwɨˈɾa/",
            portugueseTranslation = "Pássaro, ave",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.ANIMALS.name,
            exampleParintintin = "Guyra oveve.",
            examplePortuguese = "O pássaro voa.",
            culturalNote = "Presente em cantos e histórias que associam o canto das aves às mudanças do tempo.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_mandio",
            parintintinWord = "Mandi'o",
            phoneticIpa = "/mandiˈʔo/",
            portugueseTranslation = "Mandioca",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.FOOD.name,
            exampleParintintin = "Mandi'o ro'o.",
            examplePortuguese = "Polpa / massa de mandioca.",
            culturalNote = "Cultivo essencial nas roças tradicionais para produção de farinha, beiju e bebidas fermentadas.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_pora",
            parintintinWord = "Porã",
            phoneticIpa = "/poˈɾã/",
            portugueseTranslation = "Bom, bonito, belo, saudável",
            grammaticalClass = GrammaticalClass.ADJECTIVE.name,
            semanticCategory = SemanticCategory.EVERYDAY.name,
            exampleParintintin = "Ára porã.",
            examplePortuguese = "Bom dia / tempo bonito.",
            culturalNote = "Qualifica beleza estética, bem-estar comunitário e harmonia.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        ),
        DictionaryEntryEntity(
            id = "par_maraka",
            parintintinWord = "Maraká",
            phoneticIpa = "/maɾaˈka/",
            portugueseTranslation = "Chocalho ritual, canto acompanhado",
            grammaticalClass = GrammaticalClass.NOUN.name,
            semanticCategory = SemanticCategory.CULTURE_SPIRIT.name,
            exampleParintintin = "Maraká opurahéi.",
            examplePortuguese = "O maracá acompanha o canto.",
            culturalNote = "Instrumento percussivo ligado às celebrações, narrativas sagradas e transmissão de memória ancestral.",
            audioUrl = null,
            updatedAtEpochMillis = timestamp
        )
    )

    fun culturalArticles(timestamp: Long): List<CulturalArticleEntity> = listOf(
        CulturalArticleEntity(
            id = "art_kagwahiva_identity",
            title = "O Povo Kagwahiva-Parintintin",
            subtitle = "História, autodenominação e memória no Rio Madeira",
            summary = "Conheça a origem do etnônimo Parintintin, a autodenominação Kagwahiva e a trajetória de resistência no sul do Amazonas.",
            bodyParagraphs = listOf(
                "Os Parintintin autodenominam-se Kagwahiva ('nossa gente'), integrando um conjunto histórico de povos falantes de variantes muito próximas da família linguística Tupi-Guarani que habitam a região entre os rios Madeira e Tapajós.",
                "O nome 'Parintintin' foi atribuído externamente por povos vizinhos e registrado na documentação colonial e sertanista a partir do século XIX. No cotidiano das aldeias, preservar a autodenominação e os termos próprios de parentesco é parte central da afirmação cultural.",
                "Atualmente, as comunidades Parintintin vivem principalmente nas Terras Indígenas Nove de Janeiro e Ipixuna, nos municípios de Humaitá e Manicoré (Amazonas), articulando educação escolar indígena bilíngue e projetos de documentação oral."
            ),
            topic = CulturalTopic.HISTORY.name,
            readingTimeMinutes = 4,
            sourceReference = "Documentação Etnolinguística Kagwahiva • Acervo Aberto",
            updatedAtEpochMillis = timestamp
        ),
        CulturalArticleEntity(
            id = "art_phonology_orthography",
            title = "Fonologia e Ortografia Prática",
            subtitle = "Vogais nasais, oclusiva glotal e a vogal /ɨ/",
            summary = "Guia introdutório para compreender os principais sons da língua Parintintin e sua representação escrita nos verbetes.",
            bodyParagraphs = listOf(
                "Como língua do ramo Tupi-Guarani (ramo VI), o Parintintin apresenta um sistema vocálico simétrico que opõe vogais orais e nasais, incluindo a vogal central alta não arredondada /ɨ/, tradicionalmente grafada como 'y'.",
                "A oclusiva glotal /ʔ/, representada pelo apóstrofo ('), atua como consoante plena capaz de distinguir palavras semelhantes, como em 'ka'a' (mata). Ignorar essa pausa altera o sentido lexical.",
                "Neste acervo digital, cada entrada lexical apresenta a forma ortográfica de referência acompanhada pela transcrição fonética no Alfabeto Fonético Internacional (IPA) e preparação para sincronização de áudios gravados por falantes."
            ),
            topic = CulturalTopic.LINGUISTICS.name,
            readingTimeMinutes = 5,
            sourceReference = "Estudos Fonológicos Kagwahiva • Guia do Acervo",
            updatedAtEpochMillis = timestamp - 1000L
        ),
        CulturalArticleEntity(
            id = "art_madeira_territory",
            title = "Território, Rios e Ciclos da Floresta",
            subtitle = "Hidrografia e saberes ecológicos na bacia do Madeira",
            summary = "Como a relação com igarapés, terra firme e várzea molda o vocabulário ambiental e as práticas sazonais Parintintin.",
            bodyParagraphs = listOf(
                "A paisagem amazônica entre Humaitá e Manicoré estrutura-se pelo pulso das águas do Rio Madeira e seus afluentes. O léxico ambiental distingue tipos de mata, cursos d'água, peixes migradores e períodos de cheia e vazante.",
                "O manejo das roças de mandioca, a coleta da castanha-do-brasil e a pesca sazonal sustentam não apenas a segurança alimentar, mas também o vocabulário especializado transmitido entre gerações durante o trabalho coletivo.",
                "Documentar termos geográficos e ecológicos em consulta offline permite que estudantes, professores indígenas e pesquisadores acessem o acervo mesmo em áreas ribeirinhas sem conectividade."
            ),
            topic = CulturalTopic.TERRITORY.name,
            readingTimeMinutes = 4,
            sourceReference = "Cartografia Cultural e Léxico Ambiental • Acervo Aberto",
            updatedAtEpochMillis = timestamp - 2000L
        ),
        CulturalArticleEntity(
            id = "art_oral_tradition",
            title = "Tradição Oral, Cantos e Revitalização",
            subtitle = "O papel dos anciãos e da escola bilíngue na transmissão da língua",
            summary = "Estratégias comunitárias de registro em áudio, narrativas míticas e fortalecimento linguístico entre os jovens.",
            bodyParagraphs = listOf(
                "Na tradição Kagwahiva, narrativas cosmológicas e cantos acompanhados por maracá preservam normas sociais, memória histórica e vínculos entre grupos de metades clânicas (como Mutum e Arara/Taravé).",
                "Com a expansão do português na região amazônica, lideranças e educadores Parintintin têm priorizado oficinas de registro fonográfico, materiais didáticos bilíngues e dicionários comunitários.",
                "A arquitetura Offline-First deste aplicativo foi pensada justamente para apoiar esse trabalho: todo o conteúdo salvo permanece acessível em sala de aula e em campo, sincronizando novas gravações sempre que houver rede."
            ),
            topic = CulturalTopic.ORAL_TRADITION.name,
            readingTimeMinutes = 5,
            sourceReference = "Memória Oral e Educação Bilíngue • Acervo Aberto",
            updatedAtEpochMillis = timestamp - 3000L
        )
    )
}
