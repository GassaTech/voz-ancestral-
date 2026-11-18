package com.example.data.local.seed

/**
 * Especificação centralizada do arquivo de seed local do dicionário.
 *
 * O incremento de [CURRENT_SEED_VERSION] sinaliza que houve alteração estrutural
 * ou editorial relevante no seed empacotado em `assets/dictionary_seed.json`.
 */
object DictionarySeedSpec {
    const val CURRENT_SEED_VERSION: Int = 2
    const val SEED_ASSET_FILE_NAME: String = "dictionary_seed.json"
}
