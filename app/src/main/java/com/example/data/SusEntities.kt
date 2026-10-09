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
