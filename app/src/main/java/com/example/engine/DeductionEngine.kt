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

        // Bayesian learned weights
        val ventImpostorWeight = weights["VENT_IS_IMPOSTOR"] ?: 0.78f
        val ventEngineerWeight = weights["VENT_IS_ENGINEER"] ?: 0.18f
        val provokeJokerWeight = weights["PROVOKE_IS_JOKER"] ?: 0.88f
        // Note: Visual task weight adjusted because of gadget "Код", which allows Impostors to fake visual tasks
        val visualClearWeight = weights["VISUAL_IS_CLEAR"] ?: 0.88f
        val aggressiveWeight = weights["AGGRESSIVE_IS_IMPOSTOR"] ?: 0.65f

        var impostorScore = 15f
        var jokerScore = 8f
        var spacecrewScore = 77f
        var hasVisualProof = false
        var isFlaggedJoker = false

        for (event in player.events) {
            when (event.type) {
                EventType.VISUAL_TASK -> {
                    hasVisualProof = true
                    spacecrewScore = visualClearWeight * 100f
                    // Не 0%, так как с приспособлением "Код" предатель/нейтрал может сделать визуал!
                    impostorScore = ((1f - visualClearWeight) * 100f).coerceAtLeast(10f)
                    jokerScore = 5f
                }
                EventType.VENTED -> {
                    // Люк: может быть Инженер или Подрыватель (экипаж), либо Предатель/Поджигатель
                    if (!hasVisualProof) {
                        impostorScore = maxOf(impostorScore, ventImpostorWeight * 100f)
                        spacecrewScore = ventEngineerWeight * 100f
                    } else {
                        // Был визуал, но полез в люк -> Инженер или Подрыватель, либо предатель с "Кодом"
                        impostorScore = maxOf(impostorScore, 35f)
                    }
                }
                EventType.PROVOKED_VOTE -> {
                    jokerScore = maxOf(jokerScore, provokeJokerWeight * 100f)
                    isFlaggedJoker = true
                }
                EventType.REVIVED_PLAYER -> {
                    // Точно Доктор (Космонавт)
                    spacecrewScore = 98f
                    impostorScore = 2f
                    jokerScore = 0f
                }
                EventType.SHOT_PLAYER -> {
                    // Шериф или убийца
                    impostorScore = maxOf(impostorScore, 45f)
                }
                EventType.ACCUSED_OTHERS -> {
                    impostorScore += (aggressiveWeight * 16f)
                }
                EventType.SUSPICIOUS_MOVEMENT -> {
                    impostorScore += 20f
                }
                EventType.CONFIRMED_ALIBI -> {
                    impostorScore = maxOf(5f, impostorScore - 20f)
                    spacecrewScore = minOf(95f, spacecrewScore + 18f)
                }
                else -> {}
            }
        }

        // Clamp
        val impClamped = impostorScore.toInt().coerceIn(1, 99)
        val jokClamped = jokerScore.toInt().coerceIn(0, 99)
        val spaceClamped = spacecrewScore.toInt().coerceIn(1, 99)

        // Faction deduction
        val mostLikelyFaction = when {
            isFlaggedJoker || jokClamped >= 70 -> Faction.NEUTRAL
            impClamped >= 60 -> Faction.IMPOSTOR
            spaceClamped >= 70 || hasVisualProof -> Faction.SPACECREW
            else -> Faction.UNKNOWN
        }

        // Predicted Role matching Super Sus
        val predictedRole = when {
            isFlaggedJoker || jokClamped >= 70 -> "Джокер"
            player.events.any { it.type == EventType.REVIVED_PLAYER } -> "Доктор"
            hasVisualProof && impClamped < 30 -> "Космонавт (Визуал/Код)"
            hasVisualProof && impClamped >= 30 -> "Подозрение (Возможен Код!)"
            player.events.any { it.type == EventType.VENTED } && impClamped < 50 -> "Инженер / Подрыватель"
            impClamped >= 80 -> "Шпион / Предатель"
            impClamped >= 60 -> "Предатель"
            else -> "Космонавт"
        }

        return player.copy(
            isClear = hasVisualProof && impClamped < 30,
            impostorProbability = impClamped,
            jokerProbability = jokClamped,
            spacecrewProbability = spaceClamped,
            mostLikelyFaction = mostLikelyFaction,
            predictedRole = predictedRole,
            isFlaggedAsJoker = isFlaggedJoker
        )
    }

    /**
     * Self-learning calibration
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

        val updatedWeights = currentWeights.map { weight ->
            val samples = weight.samplesCount + 1
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
