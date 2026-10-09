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

    private val colorMap = listOf(
        "гранатов" to PlayerColor.GARNET_RED, // must check before "красн"
        "красн" to PlayerColor.RED,
        "голуб" to PlayerColor.CYAN,
        "син" to PlayerColor.BLUE,
        "желт" to PlayerColor.YELLOW,
        "жёлт" to PlayerColor.YELLOW,
        "серебр" to PlayerColor.SILVER,
        "бел" to PlayerColor.WHITE,
        "фиолет" to PlayerColor.PURPLE,
        "розов" to PlayerColor.PINK,
        "коричн" to PlayerColor.BROWN
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

        // 2. Identify exact game phrase from Super Sus
        var detectedEvent: EventType? = null
        var summary = ""

        when {
            // Direct Super Sus accusation phrases
            lower.contains("предатель") || lower.contains("импостор") || lower.contains("голосуйте за") || lower.contains("кик") -> {
                detectedEvent = EventType.ACCUSED_OTHERS
                summary = "Прямое обвинение в Предательстве"
            }
            // Verified / clear
            lower.contains("проверили") || lower.contains("визуал") || lower.contains("скан") ||
            lower.contains("чист") || lower.contains("проверен") || lower.contains("100%") -> {
                detectedEvent = EventType.VISUAL_TASK
                summary = "Проверен / подтверждён мирный"
            }
            // Joker provocation
            lower.contains("кикайте меня") || lower.contains("голосуйте за меня") ||
            lower.contains("сливайте меня") || lower.contains("я джокер") -> {
                detectedEvent = EventType.PROVOKED_VOTE
                summary = "Провоцирует голосование (Вероятный Джокер!)"
            }
            // Ghoul / monster eating
            lower.contains("гуль") || lower.contains("сожрал") || lower.contains("съел") -> {
                detectedEvent = EventType.KILLED_PLAYER
                summary = "Поедание тел / нападение (Роль: Гуль)"
            }
            // Code gadget caveat
            lower.contains("код") || lower.contains("фейк скан") -> {
                detectedEvent = EventType.SUSPICIOUS_MOVEMENT
                summary = "Упоминание приспособления Код (фейк-визуал)"
            }
            // Vent
            lower.contains("люк") || lower.contains("вент") || lower.contains("прыгн") -> {
                detectedEvent = EventType.VENTED
                summary = "Замечен в вентиляции / люке"
            }
            // Body report query
            lower.contains("где ты нашёл") || lower.contains("где ты нашел") || lower.contains("где тело") || lower.contains("репорт") -> {
                detectedEvent = EventType.REPORTED_BODY
                summary = "Вопрос / отчёт о месте нахождения тела"
            }
            // Alibi location
            lower.contains("я был") || lower.contains("я была") || lower.contains("медотсек") ||
            lower.contains("электрическ") || lower.contains("кафетерий") || lower.contains("навигаци") -> {
                detectedEvent = EventType.CONFIRMED_ALIBI
                summary = "Заявление об алиби и локации"
            }
            // Revive / Doctor
            lower.contains("ожил") || lower.contains("поднял") || lower.contains("доктор") -> {
                detectedEvent = EventType.REVIVED_PLAYER
                summary = "Реанимация погибшего (Роль: Доктор)"
            }
            else -> {
                detectedEvent = EventType.CHAT_MESSAGE
                summary = "Реплика собрания: '$text'"
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
