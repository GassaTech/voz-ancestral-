package com.example.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AudioPlaybackState {
    data object Idle : AudioPlaybackState
    data class Loading(val entryId: String) : AudioPlaybackState
    data class Playing(val entryId: String) : AudioPlaybackState
    data class Unavailable(val entryId: String, val reason: String) : AudioPlaybackState
}

interface AudioPlayer {
    val playbackState: StateFlow<AudioPlaybackState>
    fun playOrToggle(entryId: String, audioUrl: String?)
    fun stop()
}

@Singleton
class AndroidAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioPlayer {

    private val _playbackState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    override val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    override fun playOrToggle(entryId: String, audioUrl: String?) {
        val currentState = _playbackState.value
        if (currentState is AudioPlaybackState.Playing && currentState.entryId == entryId) {
            stop()
            return
        }

        stop()

        if (audioUrl.isNullOrBlank()) {
            _playbackState.value = AudioPlaybackState.Unavailable(
                entryId = entryId,
                reason = "Áudio de falante nativo pendente de sincronização com o acervo remoto."
            )
            return
        }

        _playbackState.value = AudioPlaybackState.Loading(entryId)
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(audioUrl))
                setOnPreparedListener {
                    it.start()
                    _playbackState.value = AudioPlaybackState.Playing(entryId)
                }
                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, _, _ ->
                    _playbackState.value = AudioPlaybackState.Unavailable(
                        entryId = entryId,
                        reason = "Não foi possível reproduzir o áudio no momento."
                    )
                    releasePlayerOnly()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            _playbackState.value = AudioPlaybackState.Unavailable(
                entryId = entryId,
                reason = e.localizedMessage ?: "Erro ao carregar faixa de áudio."
            )
            releasePlayerOnly()
        }
    }

    override fun stop() {
        releasePlayerOnly()
        _playbackState.value = AudioPlaybackState.Idle
    }

    private fun releasePlayerOnly() {
        mediaPlayer?.run {
            runCatching {
                if (isPlaying) stop()
                release()
            }
        }
        mediaPlayer = null
    }
}
