package com.example.model

enum class EventType(val ruTitle: String, val icon: String) {
    VISUAL_TASK("Сделал визуал-таск", "🔬"),
    VENTED("Прыгнул в люк / вентиляцию", "🌀"),
    REPORTED_BODY("Сообщил о трупе", "🚨"),
    EMERGENCY_BUTTON("Нажал экстренную кнопку", "🔴"),
    REVIVED_PLAYER("Оживил игрока (Доктор)", "💉"),
    SHOT_PLAYER("Выстрелил в игрока (Шериф/Килл)", "🔫"),
    KILLED_PLAYER("Устранил игрока / Поедание (Гуль)", "💀"),
    ACCUSED_OTHERS("Агрессивно обвиняет", "👉"),
    PROVOKED_VOTE("Провоцирует голосовать против себя (Джокер?)", "🎭"),
    SUSPICIOUS_MOVEMENT("Фейк-таск / слежка", "👀"),
    CONFIRMED_ALIBI("Подтверждённое алиби", "🛡️"),
    VOICE_CONFESSION("Сказал в голосовой чат", "🎙️"),
    CHAT_MESSAGE("Написал в чат", "💬")
}

data class GameEvent(
    val id: Long = System.currentTimeMillis(),
    val type: EventType,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PlayerInGame(
    val color: PlayerColor,
    val nickname: String = color.ruName,
    val isAlive: Boolean = true,
    val isEjected: Boolean = false,
    val isClear: Boolean = false, // 100% подтверждённый мирный
    val impostorProbability: Int = 15,
    val jokerProbability: Int = 10,
    val spacecrewProbability: Int = 75,
    val mostLikelyFaction: Faction = Faction.UNKNOWN,
    val predictedRole: String = "Неизвестно",
    val events: List<GameEvent> = emptyList(),
    val isFlaggedAsJoker: Boolean = false
) {
    val overallDangerScore: Int
        get() = maxOf(impostorProbability, jokerProbability)
}
