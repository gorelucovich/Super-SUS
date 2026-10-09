package com.example.engine

import com.example.model.EventType
import com.example.model.PlayerColor

data class ParsedSpeechResult(
    val targetColor: PlayerColor?,
    val eventType: EventType?,
    val rawText: String,
    val summary: String
)

object VoiceSpeechParser {

    private val colorMap = mapOf(
        "красн" to PlayerColor.RED,
        "син" to PlayerColor.BLUE,
        "зелен" to PlayerColor.GREEN,
        "зелён" to PlayerColor.GREEN,
        "желт" to PlayerColor.YELLOW,
        "жёлт" to PlayerColor.YELLOW,
        "оранж" to PlayerColor.ORANGE,
        "фиолет" to PlayerColor.PURPLE,
        "голуб" to PlayerColor.CYAN,
        "бел" to PlayerColor.WHITE,
        "черн" to PlayerColor.BLACK,
        "чёрн" to PlayerColor.BLACK,
        "розов" to PlayerColor.PINK
    )

    fun parse(text: String): ParsedSpeechResult {
        val lower = text.lowercase()

        // 1. Identify player color
        var detectedColor: PlayerColor? = null
        for ((keyword, color) in colorMap) {
            if (lower.contains(keyword)) {
                detectedColor = color
                break
            }
        }

        // 2. Identify action / suspicion
        var detectedEvent: EventType? = null
        var summary = ""

        when {
            lower.contains("люк") || lower.contains("вент") || lower.contains("вентиляц") || lower.contains("прыгн") -> {
                detectedEvent = EventType.VENTED
                summary = "Замечен в люке / вентиляции"
            }
            lower.contains("визуал") || lower.contains("скан") || lower.contains("медпункт") ||
            lower.contains("щит") || lower.contains("мусор") || lower.contains("чист") || lower.contains("100%") -> {
                detectedEvent = EventType.VISUAL_TASK
                summary = "Подтверждён визуальным заданием (Чистый)"
            }
            lower.contains("кикайте меня") || lower.contains("голосуйте за меня") ||
            lower.contains("сливайте меня") || lower.contains("я джокер") || lower.contains("выгоняйте") -> {
                detectedEvent = EventType.PROVOKED_VOTE
                summary = "Провоцирует голосование (Вероятный Джокер!)"
            }
            lower.contains("убил") || lower.contains("труп") || lower.contains("зарезал") || lower.contains("выстрел") -> {
                detectedEvent = EventType.SHOT_PLAYER
                summary = "Связан с устранением / выстрелом"
            }
            lower.contains("ожил") || lower.contains("поднял") || lower.contains("доктор") || lower.contains("воскрес") -> {
                detectedEvent = EventType.REVIVED_PLAYER
                summary = "Реанимация погибшего (Роль: Доктор)"
            }
            lower.contains("алиби") || lower.contains("вместе") || lower.contains("ходили вдвоем") -> {
                detectedEvent = EventType.CONFIRMED_ALIBI
                summary = "Подтверждённое алиби напарника"
            }
            lower.contains("фейк") || lower.contains("следит") || lower.contains("крутится") || lower.contains("преследует") -> {
                detectedEvent = EventType.SUSPICIOUS_MOVEMENT
                summary = "Подозрительное поведение / фейк-таск"
            }
            lower.contains("репорт") || lower.contains("нашел") || lower.contains("кнопк") -> {
                detectedEvent = EventType.REPORTED_BODY
                summary = "Сообщил о происшествии / собрание"
            }
            else -> {
                detectedEvent = EventType.VOICE_CONFESSION
                summary = "Реплика в голосовом чате"
            }
        }

        return ParsedSpeechResult(
            targetColor = detectedColor,
            eventType = detectedEvent,
            rawText = text,
            summary = summary
        )
    }
}
