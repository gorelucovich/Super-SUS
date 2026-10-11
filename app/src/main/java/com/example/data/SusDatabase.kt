package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Dao
interface SusDao {
    @Query("SELECT * FROM match_records ORDER BY timestamp DESC")
    fun getAllMatches(): Flow<List<MatchRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchRecordEntity): Long

    @Query("SELECT * FROM learned_weights")
    fun getAllWeights(): Flow<List<LearnedWeightEntity>>

    @Query("SELECT * FROM learned_weights WHERE featureKey = :key LIMIT 1")
    suspend fun getWeight(key: String): LearnedWeightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeights(weights: List<LearnedWeightEntity>)

    @Update
    suspend fun updateWeight(weight: LearnedWeightEntity)

    @Query("SELECT COUNT(*) FROM match_records")
    fun getMatchesCount(): Flow<Int>

    // --- Профили игроков (привязка СТРОГО к никнеймам) ---
    @Query("SELECT * FROM player_profiles ORDER BY lastSeenTimestamp DESC")
    fun getAllPlayerProfiles(): Flow<List<PlayerProfileEntity>>

    @Query("SELECT * FROM player_profiles WHERE LOWER(nickname) = LOWER(:nickname) LIMIT 1")
    suspend fun getPlayerProfile(nickname: String): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayerProfile(profile: PlayerProfileEntity)

    @Query("DELETE FROM player_profiles WHERE nickname = :nickname")
    suspend fun deletePlayerProfile(nickname: String)

    // --- База знаний ИИ-напарника Gemini Live ---
    @Query("SELECT * FROM copilot_knowledge ORDER BY timestamp DESC")
    fun getAllKnowledge(): Flow<List<CoPilotKnowledgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(knowledge: CoPilotKnowledgeEntity): Long

    @Query("DELETE FROM copilot_knowledge WHERE id = :id")
    suspend fun deleteKnowledge(id: Long)
}

@Database(
    entities = [
        MatchRecordEntity::class,
        LearnedWeightEntity::class,
        PlayerProfileEntity::class,
        CoPilotKnowledgeEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SusDatabase : RoomDatabase() {
    abstract fun susDao(): SusDao

    companion object {
        @Volatile
        private var INSTANCE: SusDatabase? = null

        fun getInstance(context: Context): SusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SusDatabase::class.java,
                    "sus_radar_database"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                INSTANCE?.let { database ->
                    seedDefaultWeights(database.susDao())
                    seedSampleProfiles(database.susDao())
                }
            }
        }

        private suspend fun seedDefaultWeights(dao: SusDao) {
            val initialWeights = listOf(
                LearnedWeightEntity("VENT_IS_IMPOSTOR", 0.78f, 10, "Вероятность импостора при прыжке в люк"),
                LearnedWeightEntity("VENT_IS_ENGINEER", 0.15f, 10, "Вероятность инженера при прыжке в люк"),
                LearnedWeightEntity("PROVOKE_IS_JOKER", 0.88f, 8, "Вероятность Джокера при напрашивании на изгнание"),
                LearnedWeightEntity("VISUAL_IS_CLEAR", 0.99f, 20, "Вероятность мирного при визуальном задании"),
                LearnedWeightEntity("AGGRESSIVE_IS_IMPOSTOR", 0.65f, 6, "Вероятность импостора при агрессивном обвинении без улик"),
                LearnedWeightEntity("FIRST_REPORTER_IS_KILLER", 0.35f, 7, "Вероятность саморепорта (убийца сообщил о теле)")
            )
            dao.insertWeights(initialWeights)

            dao.insertMatch(
                MatchRecordEntity(
                    timestamp = System.currentTimeMillis() - 86400000,
                    outcomeSummary = "Победа экипажа (Калибровка 85%)",
                    accuracyPercent = 85,
                    notes = "Красный был Хамелеоном, Жёлтый — Джокером",
                    isCalibrated = true
                )
            )
        }

        private suspend fun seedSampleProfiles(dao: SusDao) {
            val sampleProfiles = listOf(
                PlayerProfileEntity(
                    nickname = "ShadowHunter",
                    totalMatches = 5,
                    timesImpostor = 3,
                    timesCrew = 2,
                    timesNeutral = 0,
                    liesDetected = 2,
                    favoriteRoles = "Шпион, Ниндзя",
                    equippedModulesNotes = "Модуль маскировки на 10 уровне",
                    trustScore = 35,
                    notes = "Часто фейкает задания в Электрике"
                ),
                PlayerProfileEntity(
                    nickname = "CosmoDoc",
                    totalMatches = 4,
                    timesImpostor = 0,
                    timesCrew = 4,
                    timesNeutral = 0,
                    liesDetected = 0,
                    favoriteRoles = "Доктор, Медик",
                    equippedModulesNotes = "Модуль щита реанимации",
                    trustScore = 95,
                    notes = "Всегда делает визуал в Медпункте, говорит правду"
                )
            )
            for (p in sampleProfiles) {
                dao.upsertPlayerProfile(p)
            }

            dao.insertKnowledge(
                CoPilotKnowledgeEntity(
                    category = "GAME_RULE",
                    subject = "Прокачка ролей",
                    content = "Прокачка до 20 уровня: на 10 уровне открывается модуль на выбор, на 20 уровне — уникальный модуль роли."
                )
            )
        }
    }
}
