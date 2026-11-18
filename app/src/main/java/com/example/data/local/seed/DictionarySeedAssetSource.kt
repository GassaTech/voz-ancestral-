package com.example.data.local.seed

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstração de leitura dos bytes brutos do arquivo de seed do dicionário.
 * Permite substituir o acesso a `AssetManager` em testes unitários sem `Context`.
 */
fun interface DictionarySeedAssetSource {
    fun openSeedStream(): InputStream
}

@Singleton
class AndroidDictionarySeedAssetSource @Inject constructor(
    @ApplicationContext private val context: Context
) : DictionarySeedAssetSource {
    override fun openSeedStream(): InputStream =
        context.assets.open(DictionarySeedSpec.SEED_ASSET_FILE_NAME)
}
