package com.example.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LearnedWeightEntity
import com.example.data.MatchRecordEntity
import com.example.data.SusDao
import com.example.engine.DeductionEngine
import com.example.engine.VoiceSpeechParser
import com.example.model.EventType
import com.example.model.GameEvent
import com.example.model.PlayerColor
import com.example.model.PlayerInGame
import com.example.service.OverlayRadarService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class SusRadarViewModel(
    private val dao: SusDao
) : ViewModel() {

    private val _players = MutableStateFlow<List<PlayerInGame>>(initInitialPlayers())
    val players: StateFlow<List<PlayerInGame>> = _players.asStateFlow()

    private val _eventsFeed = MutableStateFlow<List<GameEvent>>(emptyList())
    val eventsFeed: StateFlow<List<GameEvent>> = _eventsFeed.asStateFlow()

    private val _isVoiceListening = MutableStateFlow(false)
    val isVoiceListening: StateFlow<Boolean> = _isVoiceListening.asStateFlow()

    private val _isScreenCapturing = MutableStateFlow(false)
    val isScreenCapturing: StateFlow<Boolean> = _isScreenCapturing.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("Ожидание реплик голосового чата...")
    val lastRecognizedText: StateFlow<String> = _lastRecognizedText.asStateFlow()

    val learnedWeights: StateFlow<List<LearnedWeightEntity>> = dao.getAllWeights()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matchHistory: StateFlow<List<MatchRecordEntity>> = dao.getAllMatches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isOverlayActive: StateFlow<Boolean> = OverlayRadarService.isServiceRunning

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        // Sync with overlay service
        viewModelScope.launch {
            _players.collect { updatedPlayers ->
                OverlayRadarService.updatePlayers(updatedPlayers)
            }
        }
    }

    private fun initInitialPlayers(): List<PlayerInGame> {
        return PlayerColor.entries.map { color ->
            PlayerInGame(color = color)
        }
    }

    fun addPlayerEvent(color: PlayerColor, eventType: EventType, description: String? = null) {
        val eventDesc = description ?: eventType.ruTitle
        val newEvent = GameEvent(type = eventType, description = eventDesc)

        _eventsFeed.value = listOf(newEvent) + _eventsFeed.value

        val weightsMap = learnedWeights.value.associate { it.featureKey to it.weight }

        _players.value = _players.value.map { player ->
            if (player.color == color) {
                val updatedEvents = player.events + newEvent
                val updatedPlayer = player.copy(events = updatedEvents)
                DeductionEngine.recalculatePlayerProbabilities(updatedPlayer, weightsMap)
            } else {
                player
            }
        }
    }

    fun togglePlayerAlive(color: PlayerColor) {
        _players.value = _players.value.map { player ->
            if (player.color == color) {
                val nextAlive = !player.isAlive
                player.copy(isAlive = nextAlive)
            } else player
        }
    }

    fun markPlayerClear(color: PlayerColor) {
        addPlayerEvent(color, EventType.VISUAL_TASK, "100% подтверждённый мирный")
    }

    fun onSpeechRecognized(text: String) {
        _lastRecognizedText.value = text
        OverlayRadarService.updateVoiceMessage(text)

        val parsed = VoiceSpeechParser.parse(text)
        if (parsed.targetColor != null && parsed.eventType != null) {
            addPlayerEvent(parsed.targetColor, parsed.eventType, parsed.summary)
        }
    }

    fun startVoiceListening(context: Context) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _lastRecognizedText.value = "Распознавание речи недоступно на данном устройстве"
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isVoiceListening.value = true
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        _isVoiceListening.value = false
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let { spoken ->
                            onSpeechRecognized(spoken)
                        }
                        _isVoiceListening.value = false
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let { spoken ->
                            _lastRecognizedText.value = spoken
                            OverlayRadarService.updateVoiceMessage(spoken)
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
            _isVoiceListening.value = true
        } catch (e: Exception) {
            _isVoiceListening.value = false
        }
    }

    fun stopVoiceListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _isVoiceListening.value = false
    }

    fun setScreenCapturing(active: Boolean) {
        _isScreenCapturing.value = active
    }

    fun resetMatch() {
        _players.value = initInitialPlayers()
        _eventsFeed.value = emptyList()
        _lastRecognizedText.value = "Новый матч Super Sus начат"
        OverlayRadarService.updatePlayers(_players.value)
    }

    fun calibrateMatch(
        actualRoles: Map<PlayerColor, String>,
        outcomeSummary: String = "Матч откалиброван"
    ) {
        viewModelScope.launch {
            val (accuracy, updatedWeights) = DeductionEngine.computeCalibrationWeights(
                players = _players.value,
                actualRoles = actualRoles,
                currentWeights = learnedWeights.value
            )

            // Save updated weights
            dao.insertWeights(updatedWeights)

            // Save match record
            dao.insertMatch(
                MatchRecordEntity(
                    outcomeSummary = outcomeSummary,
                    accuracyPercent = accuracy,
                    notes = "Точность детекции: $accuracy%. Ролей проверено: ${actualRoles.size}",
                    isCalibrated = true
                )
            )
        }
    }

    // Quick demo simulation of Super Sus match events
    fun simulateDemoEvent(step: Int) {
        when (step) {
            1 -> onSpeechRecognized("Синий сделал сканирование в медпункте, он чистый")
            2 -> onSpeechRecognized("Красный прыгнул в вентиляцию у реактора!")
            3 -> onSpeechRecognized("Жёлтый кричит: давайте кикайте меня, я джокер!")
            4 -> onSpeechRecognized("Зеленый и Белый вместе чинили свет")
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }
}

class SusRadarViewModelFactory(
    private val dao: SusDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SusRadarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SusRadarViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
