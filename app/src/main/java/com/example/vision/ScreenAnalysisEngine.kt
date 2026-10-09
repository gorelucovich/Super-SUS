package com.example.vision

import android.graphics.Bitmap
import com.example.engine.VoiceSpeechParser
import com.example.model.PlayerColor
import com.example.service.OverlayRadarService
import com.example.util.AppLogger
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GameScreenScanResult(
    val activePlayers: List<PlayerColor>,
    val deadPlayers: List<PlayerColor>,
    val detectedChatMessages: List<String>,
    val detectedRoles: Map<PlayerColor, String>,
    val summary: String
)

object ScreenAnalysisEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val allPlayerColors = listOf(
        PlayerColor.RED,
        PlayerColor.GARNET_RED,
        PlayerColor.BLUE,
        PlayerColor.CYAN,
        PlayerColor.YELLOW,
        PlayerColor.SILVER,
        PlayerColor.WHITE,
        PlayerColor.BROWN,
        PlayerColor.PINK,
        PlayerColor.PURPLE
    )

    /**
     * Analyzes game screen bitmap using on-device ML Kit OCR.
     * Extracts player cards, dead indicators, and chat messages in real time.
     */
    suspend fun analyzeGameScreen(bitmap: Bitmap): GameScreenScanResult = withContext(Dispatchers.Default) {
        AppLogger.i("VisionEngine", "Анализ кадра экрана Super Sus (${bitmap.width}x${bitmap.height})")

        val inputImage = InputImage.fromBitmap(bitmap, 0)

        val visionText = try {
            com.google.android.gms.tasks.Tasks.await(recognizer.process(inputImage))
        } catch (e: Exception) {
            AppLogger.e("VisionEngine", "Ошибка распознавания текста ML Kit", e)
            throw e
        }

        val fullText = visionText.text
        AppLogger.d("VisionEngine", "Распознан текст экрана: ${fullText.take(150)}...")

        val detectedActive = mutableSetOf<PlayerColor>()
        val detectedDead = mutableSetOf<PlayerColor>()
        val detectedRoles = mutableMapOf<PlayerColor, String>()
        val detectedChatMessages = mutableListOf<String>()

        val lines = visionText.textBlocks.flatMap { it.lines.map { line -> line.text } }

        for (line in lines) {
            val lower = line.lowercase()

            // 1. Detect player names on cards
            for (color in allPlayerColors) {
                if (lower.contains(color.ruName.lowercase())) {
                    detectedActive.add(color)

                    // Check if line indicates elimination
                    if (lower.contains("х") || lower.contains("x") || lower.contains("погиб") || lower.contains("убит")) {
                        detectedDead.add(color)
                    }
                }
            }

            // 2. Detect chat messages: "Имя: Сообщение"
            if (line.contains(":") || lower.contains("пчела") || lower.contains("голосуйте") || lower.contains("я был")) {
                detectedChatMessages.add(line)

                val parsed = VoiceSpeechParser.parse(line)
                if (parsed.targetColor != null) {
                    val color = parsed.targetColor
                    detectedActive.add(color)

                    when {
                        lower.contains("пчела") -> {
                            detectedRoles[color] = "Пчела 🐝"
                            OverlayRadarService.updatePlayerRole(color, "Пчела 🐝")
                            OverlayRadarService.updatePlayerSuspicion(color, "Мирный")
                            AppLogger.i("VisionEngine", "Чат: ${color.ruName} заявил роль Пчела 🐝")
                        }
                        lower.contains("голосуйте за") || lower.contains("предатель") || lower.contains("кик") -> {
                            OverlayRadarService.updatePlayerSuspicion(color, "Предатель")
                            AppLogger.i("VisionEngine", "Чат: подозрение на ${color.ruName} (призыв к кику)")
                        }
                        lower.contains("шериф") -> {
                            detectedRoles[color] = "Шериф ⭐"
                            OverlayRadarService.updatePlayerRole(color, "Шериф ⭐")
                        }
                        lower.contains("доктор") -> {
                            detectedRoles[color] = "Доктор 💓"
                            OverlayRadarService.updatePlayerRole(color, "Доктор 💓")
                        }
                        lower.contains("инженер") -> {
                            detectedRoles[color] = "Инженер 🔧"
                            OverlayRadarService.updatePlayerRole(color, "Инженер 🔧")
                        }
                        lower.contains("пророк") -> {
                            detectedRoles[color] = "Пророк 👁️"
                            OverlayRadarService.updatePlayerRole(color, "Пророк 👁️")
                        }
                        lower.contains("гуль") -> {
                            detectedRoles[color] = "Гуль 👾"
                            OverlayRadarService.updatePlayerRole(color, "Гуль 👾")
                        }
                    }
                }
            }
        }

        // Apply dead marks to overlay
        for (dead in detectedDead) {
            OverlayRadarService.togglePlayerDead(dead)
        }

        val summary = buildString {
            append("Найдено игроков: ${detectedActive.size}. ")
            if (detectedDead.isNotEmpty()) {
                append("Погибших: ${detectedDead.size}. ")
            }
            if (detectedRoles.isNotEmpty()) {
                append("Роли: ${detectedRoles.entries.joinToString { "${it.key.ruName}=${it.value}" }}. ")
            }
            if (detectedChatMessages.isNotEmpty()) {
                append("Реплик чата: ${detectedChatMessages.size}.")
            }
        }

        AppLogger.i("VisionEngine", "Итог сканирования экрана: $summary")
        return@withContext GameScreenScanResult(
            activePlayers = detectedActive.toList(),
            deadPlayers = detectedDead.toList(),
            detectedChatMessages = detectedChatMessages,
            detectedRoles = detectedRoles,
            summary = summary
        )
    }
}
