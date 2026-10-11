package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_records")
data class MatchRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val outcomeSummary: String = "Матч завершён",
    val accuracyPercent: Int = 80,
    val notes: String = "",
    val isCalibrated: Boolean = false
)

@Entity(tableName = "learned_weights")
data class LearnedWeightEntity(
    @PrimaryKey
    val featureKey: String,
    val weight: Float, // 0.0 .. 1.0
    val samplesCount: Int = 1,
    val descriptionRu: String
)

/**
 * Профиль игрока, привязанный СТРОГО к никнейму (так как цвет игрок меняет в каждом матче).
 * Хранит историю надёжности, склонность к блефу, ложь на голосованиях и замеченные модули.
 */
@Entity(tableName = "player_profiles")
data class PlayerProfileEntity(
    @PrimaryKey
    val nickname: String,
    val totalMatches: Int = 1,
    val timesImpostor: Int = 0,
    val timesCrew: Int = 0,
    val timesNeutral: Int = 0,
    val liesDetected: Int = 0,
    val favoriteRoles: String = "",
    val equippedModulesNotes: String = "",
    val trustScore: Int = 50, // 0 (полный лжец) .. 100 (всегда чист)
    val notes: String = "",
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

/**
 * База знаний ИИ-напарника Gemini Live: факты, записанные с голоса пользователя
 * или определённые визуально с экрана матча.
 */
@Entity(tableName = "copilot_knowledge")
data class CoPilotKnowledgeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // "ROLE_MODULE", "PLAYER_HABIT", "GAME_RULE", "SCREEN_OBSERVATION"
    val subject: String, // например, ник игрока, название роли или механика
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
