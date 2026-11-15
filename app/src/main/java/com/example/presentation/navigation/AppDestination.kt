package com.example.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object EncyclopediaRoute

@Serializable
data object DictionaryRoute

@Serializable
data object FavoritesRoute

@Serializable
data object ArchiveStatusRoute

@Serializable
data class EntryDetailRoute(val entryId: String)

@Serializable
data class ArticleDetailRoute(val articleId: String)
