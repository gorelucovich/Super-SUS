package com.example.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.example.data.CoPilotKnowledgeEntity
import com.example.data.SusDatabase
import com.example.engine.VoiceSpeechParser
import com.example.service.OverlayRadarService
import com.example.util.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Полноценный аудио-модуль Hands-Free:
 * 1. TextToSpeech — озвучивает короткие тактические подсказки в наушники игрока.
 * 2. SpeechRecognizer — постоянно (без нажатия кнопок) слушает голос пользователя,
 *    распознает команды, реплики собрания, фиксирует роли и модули в базу данных.
 */
class CoPilotSpeechEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val database = SusDatabase.getInstance(context)

    // TTS
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    // STT (Continuous listening)
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningLoopActive = false
    private var restartJob: Job? = null

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow("Микрофон слушает (Hands-free)...")
    val lastRecognizedText = _lastRecognizedText.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("ru", "RU"))
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.getDefault()
                }
                isTtsReady = true
                AppLogger.i("CoPilotSpeech", "Голосовой синтезатор (TTS) инициализирован успешно")
            } else {
                AppLogger.e("CoPilotSpeech", "Ошибка инициализации TTS: $status")
            }
        }
    }

    /**
     * Озвучить реплику в наушники игрока
     */
    fun speak(text: String) {
        if (!isTtsReady || text.isBlank()) return
        AppLogger.i("CoPilotSpeech", "Голосовая подсказка: '$text'")
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "copilot_alert_${System.currentTimeMillis()}")
    }

    /**
     * Запустить постоянное прослушивание (Hands-free, без кнопок)
     */
    fun startContinuousListening() {
        if (isListeningLoopActive) return
        isListeningLoopActive = true
        _isListening.value = true
        AppLogger.i("CoPilotSpeech", "Запуск постоянного Hands-free прослушивания голоса")
        startListeningSession()
    }

    /**
     * Остановить прослушивание
     */
    fun stopContinuousListening() {
        isListeningLoopActive = false
        _isListening.value = false
        restartJob?.cancel()
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
        AppLogger.i("CoPilotSpeech", "Hands-free прослушивание остановлено")
    }

    private fun startListeningSession() {
        if (!isListeningLoopActive) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            AppLogger.w("CoPilotSpeech", "Распознавание речи недоступно на данном устройстве")
            _lastRecognizedText.value = "Распознавание речи системы недоступно"
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            AppLogger.e("CoPilotSpeech", "Ошибка запуска сессии распознавания", e)
            scheduleRestart(1500)
        }
    }

    private fun createListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                // SpeechRecognizer often gives NO_MATCH or SPEECH_TIMEOUT in quiet room - perfectly normal
                scheduleRestart(500)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognized = matches?.firstOrNull()?.trim()
                if (!recognized.isNullOrBlank()) {
                    handleRecognizedPhrase(recognized)
                }
                scheduleRestart(300)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let {
                    _lastRecognizedText.value = "Слышу: '$it'..."
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!isListeningLoopActive) return
        restartJob?.cancel()
        restartJob = scope.launch {
            delay(delayMs)
            if (isListeningLoopActive) {
                startListeningSession()
            }
        }
    }

    private fun handleRecognizedPhrase(phrase: String) {
        AppLogger.i("CoPilotSpeech", "Распознана фраза: '$phrase'")
        _lastRecognizedText.value = phrase

        scope.launch(Dispatchers.IO) {
            val lower = phrase.lowercase()

            // 1. Проверяем, говорит ли пользователь про СВОЮ роль или модуль
            // Например: "Я шериф 15 уровень взял уникальный модуль" или "Моя роль доктор"
            if (lower.startsWith("я ") || lower.contains("моя роль") || lower.contains("я играю за")) {
                database.susDao().insertKnowledge(
                    CoPilotKnowledgeEntity(
                        category = "MY_ROLE_INFO",
                        subject = "Моя роль",
                        content = phrase
                    )
                )
                speak("Зафиксировала вашу роль и модуль")
            }

            // 2. Быстрый локальный парсинг стандартных команд игры
            val parsed = VoiceSpeechParser.parse(phrase)
            if (parsed.targetColor != null) {
                OverlayRadarService.updateVoiceMessage(phrase)
            }

            // 3. Отправка в Gemini Live Co-Pilot для глубокой детекции лжи и обновления профилей по никнеймам
            val analysis = GeminiLiveCoPilotEngine.analyzeMeetingSpeechOrFact(phrase, database.susDao())
            if (analysis.adviceVoiceCallout.isNotBlank()) {
                speak(analysis.adviceVoiceCallout)
            }
        }
    }

    fun release() {
        stopContinuousListening()
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {}
    }
}
