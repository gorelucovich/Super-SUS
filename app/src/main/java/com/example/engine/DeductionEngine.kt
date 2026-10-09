package com.example.engine

import com.example.data.LearnedWeightEntity
import com.example.model.EventType
import com.example.model.Faction
import com.example.model.PlayerColor
import com.example.model.PlayerInGame

object DeductionEngine {

    fun recalculatePlayerProbabilities(
        player: PlayerInGame,
        weights: Map<String, Float>
    ): PlayerInGame {
        if (!player.isAlive || player.isEjected) {
            return player
        }

        // Default weights if not loaded
        val ventImpostorWeight = weights["VENT_IS_IMPOSTOR"] ?: 0.78f
        val ventEngineerWeight = weights["VENT_IS_ENGINEER"] ?: 0.15f
        val provokeJokerWeight = weights["PROVOKE_IS_JOKER"] ?: 0.88f
        val visualClearWeight = weights["VISUAL_IS_CLEAR"] ?: 0.99f
        val aggressiveWeight = weights["AGGRESSIVE_IS_IMPOSTOR"] ?: 0.65f

        var impostorScore = 15f
        var jokerScore = 8f
        var spacecrewScore = 77f
        var isClear = player.isClear
        var isFlaggedJoker = false

        for (event in player.events) {
            when (event.type) {
                EventType.VISUAL_TASK -> {
                    isClear = true
                    spacecrewScore = visualClearWeight * 100f
                    impostorScore = (1f - visualClearWeight) * 100f
                    jokerScore = 0f
                }
                EventType.VENTED -> {
                    if (!isClear) {
                        impostorScore = maxOf(impostorScore, ventImpostorWeight * 100f)
                        spacecrewScore = ventEngineerWeight * 100f
                    }
                }
                EventType.PROVOKED_VOTE -> {
                    jokerScore = maxOf(jokerScore, provokeJokerWeight * 100f)
                    isFlaggedJoker = true
                }
                EventType.REVIVED_PLAYER -> {
                    // Clearly Doctor (Spacecrew)
                    isClear = true
                    spacecrewScore = 96f
                    impostorScore = 3f
                    jokerScore = 1f
                }
                EventType.SHOT_PLAYER -> {
                    // Sheriff or killer
                    impostorScore = maxOf(impostorScore, 50f)
                }
                EventType.ACCUSED_OTHERS -> {
                    if (!isClear) {
                        impostorScore += (aggressiveWeight * 20f)
                    }
                }
                EventType.SUSPICIOUS_MOVEMENT -> {
                    if (!isClear) {
                        impostorScore += 25f
                    }
                }
                EventType.CONFIRMED_ALIBI -> {
                    impostorScore = maxOf(5f, impostorScore - 30f)
                    spacecrewScore = minOf(95f, spacecrewScore + 25f)
                }
                else -> {}
            }
        }

        // Clamp
        val impClamped = impostorScore.toInt().coerceIn(1, 99)
        val jokClamped = jokerScore.toInt().coerceIn(0, 99)
        val spaceClamped = spacecrewScore.toInt().coerceIn(1, 99)

        // Faction & Role deduction
        val mostLikelyFaction = when {
            isFlaggedJoker || jokClamped >= 70 -> Faction.NEUTRAL
            impClamped >= 60 -> Faction.IMPOSTOR
            spaceClamped >= 70 || isClear -> Faction.SPACECREW
            else -> Faction.UNKNOWN
        }

        val predictedRole = when {
            isFlaggedJoker || jokClamped >= 70 -> "Джокер"
            player.events.any { it.type == EventType.REVIVED_PLAYER } -> "Доктор"
            isClear -> "Космонавт (Чистый)"
            impClamped >= 80 && player.events.any { it.type == EventType.VENTED } -> "Хамелеон / Импостор"
            player.events.any { it.type == EventType.VENTED } && impClamped < 60 -> "Инженер"
            impClamped >= 60 -> "Предатель"
            else -> "Космонавт"
        }

        return player.copy(
            isClear = isClear,
            impostorProbability = impClamped,
            jokerProbability = jokClamped,
            spacecrewProbability = spaceClamped,
            mostLikelyFaction = mostLikelyFaction,
            predictedRole = predictedRole,
            isFlaggedAsJoker = isFlaggedJoker
        )
    }

    /**
     * Self-learning calibration: compares predictions with ground truth roles,
     * computes accuracy and adjusts Bayesian weights.
     */
    fun computeCalibrationWeights(
        players: List<PlayerInGame>,
        actualRoles: Map<PlayerColor, String>,
        currentWeights: List<LearnedWeightEntity>
    ): Pair<Int, List<LearnedWeightEntity>> {
        var correctPredictions = 0
        var totalJudged = 0

        for (player in players) {
            val actual = actualRoles[player.color] ?: continue
            totalJudged++
            val predicted = player.predictedRole.lowercase()
            val actualLower = actual.lowercase()

            if (predicted.contains(actualLower) || actualLower.contains(predicted)) {
                correctPredictions++
            }
        }

        val accuracy = if (totalJudged > 0) {
            (correctPredictions * 100 / totalJudged).coerceIn(20, 100)
        } else 80

        // Adaptive gradient updates to weights
        val updatedWeights = currentWeights.map { weight ->
            val samples = weight.samplesCount + 1
            // Small learning rate nudging towards empirical feedback
            val learningRate = 1.0f / samples.coerceAtMost(30)
            val delta = if (accuracy >= 80) 0.02f else -0.02f
            val newWeight = (weight.weight + delta * learningRate).coerceIn(0.1f, 0.99f)
            weight.copy(
                weight = newWeight,
                samplesCount = samples
            )
        }

        return Pair(accuracy, updatedWeights)
    }
}
