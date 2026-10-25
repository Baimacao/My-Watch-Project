package com.example.lddc.presentation.screens.search

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.example.lddc.common.models.enums.LyricsFormat
import com.example.lddc.common.models.enums.Source
import com.example.lddc.common.models.info.SongInfo
import com.example.lddc.common.models.lyrics.Lyrics
import com.example.lddc.core.utils.LyricsSaver
import com.example.lddc.domain.convert.ConvertLyricsUseCase
import com.example.lddc.domain.search.GetLyricsUseCase
import com.example.lddc.domain.settings.GetSettingsUseCase
import com.example.lddc.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsDetailViewModel @Inject constructor(
    private val getLyricsUseCase: GetLyricsUseCase,
    private val convertLyricsUseCase: ConvertLyricsUseCase,
    private val getSettingsUseCase: GetSettingsUseCase,
    @ApplicationContext private val context: Context // 注入 Context 用于自动保存
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(LyricsDetailUiState())
    val uiState: StateFlow<LyricsDetailUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collectLatest { settings ->
                _uiState.update { state ->
                    val newFormat = settings.lyricsFormat

                    val newLanguages = mutableListOf("orig")
                    if (settings.autoTranslate) newLanguages.add("ts")
                    if (settings.showRomaji) newLanguages.add("roma")

                    state.copy(
                        selectedFormat = newFormat,
                        selectedLanguages = newLanguages,
                        downloadTreeUri = settings.downloadTreeUri,
                        autoSave = settings.saveLyricsFile // 同步自动保存开关
                    )
                }
            }
        }
    }

    fun loadLyrics(songInfo: SongInfo) {
        _uiState.update { it.copy(currentSong = songInfo) }

        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = getLyricsUseCase(songInfo, useCache = true)

            result.onSuccess { lyrics ->
                var stateToSave: LyricsDetailUiState? = null

                _uiState.update { state ->
                    val availableFormats = getAvailableFormatsForLyrics(lyrics)
                    val format = if (availableFormats.contains(state.selectedFormat)) {
                        state.selectedFormat
                    } else {
                        availableFormats.firstOrNull() ?: state.selectedFormat
                    }
                    val languages = state.selectedLanguages
                    val convertedText = convertLyrics(lyrics, format, languages)

                    val newState = state.copy(
                        isLoading = false,
                        lyrics = lyrics,
                        selectedFormat = format,
                        convertedLyrics = convertedText
                    )
                    stateToSave = newState
                    newState
                }

                // ====== 自动保存逻辑 ======
                val currentState = stateToSave
                if (currentState != null && currentState.autoSave && !currentState.convertedLyrics.isNullOrBlank()) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val fileName = LyricsSaver.buildFileName(
                            title = currentState.currentSong?.title ?: lyrics.title,
                            artist = currentState.currentSong?.artist?.toString() ?: lyrics.artist?.toString(),
                            format = currentState.selectedFormat
                        )
                        LyricsSaver.save(
                            context = context,
                            fileName = fileName,
                            content = currentState.convertedLyrics!!,
                            format = currentState.selectedFormat,
                            treeUriString = currentState.downloadTreeUri
                        )
                    }
                }
                // ==========================

            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
            }
        }
    }

    fun switchSource(source: Source) {
        val lyrics = _uiState.value.allLyrics[source]
        _uiState.update { state ->
            val format = lyrics?.let { lyricsData ->
                val availableFormats = getAvailableFormatsForLyrics(lyricsData)
                if (availableFormats.contains(state.selectedFormat)) {
                    state.selectedFormat
                } else {
                    availableFormats.firstOrNull() ?: state.selectedFormat
                }
            } ?: state.selectedFormat

            state.copy(
                currentSource = source,
                lyrics = lyrics,
                selectedFormat = format,
                convertedLyrics = lyrics?.let {
                    convertLyrics(
                        it,
                        format,
                        state.selectedLanguages
                    )
                }
            )
        }
    }

    fun onFormatChange(format: LyricsFormat) {
        _uiState.update { state ->
            state.copy(
                selectedFormat = format,
                convertedLyrics = state.lyrics?.let {
                    convertLyrics(
                        it,
                        format,
                        state.selectedLanguages
                    )
                }
            )
        }
    }

    fun onLanguageToggle(language: String) {
        _uiState.update { state ->
            val newLanguages = if (language in state.selectedLanguages) {
                state.selectedLanguages - language
            } else {
                state.selectedLanguages + language
            }
            state.copy(
                selectedLanguages = newLanguages,
                convertedLyrics = state.lyrics?.let {
                    convertLyrics(
                        it,
                        state.selectedFormat,
                        newLanguages
                    )
                }
            )
        }
    }

    fun getAvailableLanguages(): List<String> {
        return _uiState.value.lyrics?.data?.keys?.toList() ?: emptyList()
    }

    private fun isVerbatimLyrics(lyrics: Lyrics): Boolean {
        for ((_, lines) in lyrics.data) {
            for (line in lines) {
                if (line.words.size > 1) {
                    return true
                } else if (line.words.size == 1) {
                    val word = line.words.first()
                    if (word.start != line.start || word.end != null && word.end != line.end) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun getAvailableFormatsForLyrics(lyrics: Lyrics): List<LyricsFormat> {
        val allFormats = convertLyricsUseCase.getAvailableFormats()

        if (!isVerbatimLyrics(lyrics)) {
            return allFormats.filter { it != LyricsFormat.VERBATIMLRC }
        }

        return allFormats
    }

    fun getAvailableFormats(): List<LyricsFormat> {
        val lyrics = _uiState.value.lyrics
        return if (lyrics != null) {
            getAvailableFormatsForLyrics(lyrics)
        } else {
            convertLyricsUseCase.getAvailableFormats()
        }
    }

    private fun convertLyrics(
        lyrics: Lyrics,
        format: LyricsFormat,
        languages: List<String>
    ): String {
        return if (languages.isEmpty()) {
            lyrics.toFormat(format, listOf("orig"))
        } else {
            lyrics.toFormat(format, languages)
        }
    }

    data class LyricsDetailUiState(
        val currentSong: SongInfo? = null,
        val lyrics: Lyrics? = null,
        val allLyrics: Map<Source, Lyrics> = emptyMap(),
        val currentSource: Source? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val selectedFormat: LyricsFormat = LyricsFormat.LINEBYLINELRC,
        val selectedLanguages: List<String> = listOf("orig"),
        val convertedLyrics: String? = null,
        val downloadTreeUri: String? = null,
        val autoSave: Boolean = false // 新增：自动保存标记
    )
}