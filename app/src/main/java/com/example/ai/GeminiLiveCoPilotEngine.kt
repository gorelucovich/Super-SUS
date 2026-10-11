package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.CoPilotKnowledgeEntity
import com.example.data.PlayerProfileEntity
import com.example.data.SusDao
import com.example.model.PlayerColor
import com.example.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class ScreenObservation(
    val myDetectedRole: String? = null,
    val myRoleLevel: Int? = null,
    val observedNicknames: List<String> = emptyList(),
    val ventJumpers: List<String> = emptyList(),
    val visualTaskConfirmations: List<String> = emptyList(),
    val currentRoom: String? = null,
    val meetingActive: Boolean = false,
    val adviceShort: String = ""
)

data class MeetingSpeechAnalysis(
    val speakerNickname: String,
    val claimedRole: String? = null,
    val claimedLocation: String? = null,
    val claimedTasks: String? = null,
    val isSuspiciousOrLying: Boolean = false,
    val suspicionReason: String = "",
    val adviceVoiceCallout: String = ""
)

object GeminiLiveCoPilotEngine {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val _isKeyConfigured = MutableStateFlow(false)
    val isKeyConfigured = _isKeyConfigured.asStateFlow()

    private val _lastAiVerdict = MutableStateFlow("ИИ ожидает начала матча Super Sus")
    val lastAiVerdict = _lastAiVerdict.asStateFlow()

    private val _myRoleInfo = MutableStateFlow<String?>("Роль ещё не определена")
    val myRoleInfo = _myRoleInfo.asStateFlow()

    private val _activeAdvice = MutableStateFlow("Запустите захват экрана и оверлей для подсказок в реальном времени")
    val activeAdvice = _activeAdvice.asStateFlow()

    init {
        checkApiKey()
    }

    fun checkApiKey(): Boolean {
        val key = getApiKey()
        val configured = key.isNotBlank() && !key.contains("MY_GEMINI_API_KEY", ignoreCase = true)
        _isKeyConfigured.value = configured
        return configured
    }

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Системный контекст с полными знаниями об игре Super Sus:
     * - 32 роли (17 Экипаж, 10 Предатели, 5 Нейтралы)
     * - 2 режима (Все роли, Рандомные роли)
     * - Прокачка ролей 1..20 (модуль выбора на 10 ур, уникальный модуль на 20 ур)
     * - Визуальные задания (Щиты, Медпункт/Сканер, Сброс мусора)
     * - Вентиляция (Инженер может прыгать легально, но с кулдауном; Предатели без кулдауна)
     */
    private val SUPER_SUS_SYSTEM_PROMPT = """
        Ты — Gemini Live Co-Pilot, элитный тактический аналитик игры Super Sus в реальном времени.
        Ты помогаешь игроку побеждать, наблюдая за экраном матча и слушая его голос и собрания.
        
        ПРАВИЛА И ЗНАНИЯ О SUPER SUS:
        1. В игре строго 2 ключевых соревновательных режима: 'Все роли' и 'Рандомные роли'.
        2. Всего 32 уникальные роли в 3 фракциях:
           - Экипаж (17 ролей): Космонавт, Шериф, Доктор, Инженер, Детектив, Страж, Спецагент, Провидец, Хакер, Мэр, Санта, Дрессировщик, Капитан, Пчела, Ветеран, Оракул, Судья.
           - Предатели (10 ролей): Предатель, Хамелеон, Шпион, Ведьма, Поджигатель, Чернокнижник, Чистильщик, Охотник за головами, Гробовщик, Фантом.
           - Нейтралы (5 ролей): Джокер (хочет изгнания на голосовании), Арсонист (обливает бензином), Гуль (ест тела), Инквизитор (выслеживает), Выживший (просто выжить, 3 щита).
        3. Прокачка ролей:
           - Максимальный уровень прокачки роли — 20.
           - На 10 уровне открывается вариативный модуль на выбор.
           - На 20 уровне открывается уникальный модуль роли.
        4. Визуальные задания (100% подтверждают мирного):
           - Сканирование в Медпункте (зелёное кольцо)
           - Щиты (загораются лампочки снаружи)
           - Сброс мусора (вылетает мусор в космос)
           - Оружие/Астероиды (стрельба из пушек)
        5. Игроки идентифицируются СТРОГО ПО НИКНЕЙМАМ. Цвета — только временные маркеры текущего раунда.
    """.trimIndent()

    /**
     * Анализирует ключевой кадр экрана Super Sus с помощью Gemini Multimodal Vision.
     */
    suspend fun analyzeGameFrame(
        bitmap: Bitmap,
        userCurrentContext: String = ""
    ): ScreenObservation = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey.contains("MY_GEMINI_API_KEY", ignoreCase = true)) {
            AppLogger.w("GeminiLive", "GEMINI_API_KEY не задан в Secrets!")
            return@withContext ScreenObservation(
                adviceShort = "Настройте GEMINI_API_KEY в панели Secrets для активации зрения ИИ"
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val prompt = """
                $SUPER_SUS_SYSTEM_PROMPT
                
                Контекст от игрока: $userCurrentContext
                
                Проанализируй этот кадр экрана игры Super Sus.
                Ответь строго в формате JSON:
                {
                  "myDetectedRole": "название роли если виден экран роли или способностей, иначе null",
                  "myRoleLevel": null или число (1..20),
                  "observedNicknames": ["ники игроков над головами или в списке"],
                  "ventJumpers": ["ники или цвета тех, кто замечен в анимации люка"],
                  "visualTaskConfirmations": ["ники тех, кто выполняет анимацию визуал-таска"],
                  "currentRoom": "название комнаты если видно",
                  "meetingActive": true/false (идёт ли собрание/голосование),
                  "adviceShort": "очень краткая тактическая подсказка игроку (1 предложение)"
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray()
                    partsArray.put(JSONObject().apply { put("text", prompt) })
                    partsArray.put(JSONObject().apply {
                        val inlineData = JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        }
                        put("inlineData", inlineData)
                    })
                    put("parts", partsArray)
                }
                contentsArray.put(contentObj)
                put("contents", contentsArray)
                
                // Set generation config for deterministic JSON
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                AppLogger.e("GeminiLive", "Ошибка Gemini API: ${response.code} $responseBody")
                return@withContext ScreenObservation(adviceShort = "Ошибка связи с Gemini (${response.code})")
            }

            val parsedJson = JSONObject(responseBody)
            val candidates = parsedJson.optJSONArray("candidates")
            val textContent = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: "{}"

            val cleanJson = textContent.trim().removeSurrounding("```json", "```").trim()
            val resultObj = JSONObject(cleanJson)

            val role = resultObj.optString("myDetectedRole").takeIf { it.isNotBlank() && it != "null" }
            val level = if (resultObj.has("myRoleLevel") && !resultObj.isNull("myRoleLevel")) resultObj.getInt("myRoleLevel") else null
            val currentRoom = resultObj.optString("currentRoom").takeIf { it.isNotBlank() && it != "null" }
            val isMeeting = resultObj.optBoolean("meetingActive", false)
            val advice = resultObj.optString("adviceShort", "Внимательно следите за алиби")

            val observedNicknames = mutableListOf<String>()
            resultObj.optJSONArray("observedNicknames")?.let { arr ->
                for (i in 0 until arr.length()) observedNicknames.add(arr.getString(i))
            }

            val ventJumpers = mutableListOf<String>()
            resultObj.optJSONArray("ventJumpers")?.let { arr ->
                for (i in 0 until arr.length()) ventJumpers.add(arr.getString(i))
            }

            val visuals = mutableListOf<String>()
            resultObj.optJSONArray("visualTaskConfirmations")?.let { arr ->
                for (i in 0 until arr.length()) visuals.add(arr.getString(i))
            }

            if (role != null) {
                _myRoleInfo.value = if (level != null) "$role (Ур. $level)" else role
            }
            if (advice.isNotBlank()) {
                _activeAdvice.value = advice
                _lastAiVerdict.value = advice
            }

            ScreenObservation(
                myDetectedRole = role,
                myRoleLevel = level,
                observedNicknames = observedNicknames,
                ventJumpers = ventJumpers,
                visualTaskConfirmations = visuals,
                currentRoom = currentRoom,
                meetingActive = isMeeting,
                adviceShort = advice
            )
        } catch (e: Exception) {
            AppLogger.e("GeminiLive", "Исключение при анализе кадра Gemini", e)
            ScreenObservation(adviceShort = "Сбой анализа: ${e.message?.take(50)}")
        }
    }

    /**
     * Анализирует реплику игрока на собрании или голосовую фразу пользователя,
     * сопоставляя её с базой знаний и выявляя ложь / подозрительное поведение.
     */
    suspend fun analyzeMeetingSpeechOrFact(
        userSpokenText: String,
        dao: SusDao
    ): MeetingSpeechAnalysis = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank() || apiKey.contains("MY_GEMINI_API_KEY", ignoreCase = true)) {
            return@withContext MeetingSpeechAnalysis(
                speakerNickname = "Игрок",
                adviceVoiceCallout = "Подключите ключ Gemini в Secrets для детектора лжи"
            )
        }

        try {
            val prompt = """
                $SUPER_SUS_SYSTEM_PROMPT
                
                Пользователь передал фразу из игры / собрания Super Sus:
                "$userSpokenText"
                
                Проанализируй это высказывание:
                1. Кто говорит (никнейм)?
                2. Заявляет ли игрок какую-то роль?
                3. Указывает ли локацию или задания?
                4. Есть ли признаки лжи, блефа или противоречия?
                
                Ответь строго JSON:
                {
                  "speakerNickname": "никнейм игрока если назван, иначе Неизвестный",
                  "claimedRole": "названная роль или null",
                  "claimedLocation": "названная комната или null",
                  "claimedTasks": "названные задания или null",
                  "isSuspiciousOrLying": true/false,
                  "suspicionReason": "причина сомнения",
                  "adviceVoiceCallout": "короткая реплика (до 10 слов) для озвучивания игроку в наушники"
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray()
                    partsArray.put(JSONObject().apply { put("text", prompt) })
                    put("parts", partsArray)
                }
                contentsArray.put(contentObj)
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.2)
                    put("responseMimeType", "application/json")
                })
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            val parsedJson = JSONObject(responseBody)
            val candidates = parsedJson.optJSONArray("candidates")
            val textContent = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: "{}"

            val cleanJson = textContent.trim().removeSurrounding("```json", "```").trim()
            val resultObj = JSONObject(cleanJson)

            val nickname = resultObj.optString("speakerNickname", "Игрок")
            val claimedRole = resultObj.optString("claimedRole").takeIf { it.isNotBlank() && it != "null" }
            val claimedLocation = resultObj.optString("claimedLocation").takeIf { it.isNotBlank() && it != "null" }
            val claimedTasks = resultObj.optString("claimedTasks").takeIf { it.isNotBlank() && it != "null" }
            val isSuspicious = resultObj.optBoolean("isSuspiciousOrLying", false)
            val reason = resultObj.optString("suspicionReason", "")
            val voiceCallout = resultObj.optString("adviceVoiceCallout", "")

            // Обновляем долговременную базу профилей игроков по НИКНЕЙМУ
            if (nickname.isNotBlank() && nickname != "Неизвестный" && nickname != "Игрок") {
                val existing = dao.getPlayerProfile(nickname)
                if (existing != null) {
                    val updated = existing.copy(
                        totalMatches = existing.totalMatches + 1,
                        liesDetected = if (isSuspicious) existing.liesDetected + 1 else existing.liesDetected,
                        trustScore = if (isSuspicious) (existing.trustScore - 15).coerceIn(0, 100) else (existing.trustScore + 5).coerceIn(0, 100),
                        favoriteRoles = if (claimedRole != null && !existing.favoriteRoles.contains(claimedRole)) "${existing.favoriteRoles}, $claimedRole".trim(',', ' ') else existing.favoriteRoles,
                        lastSeenTimestamp = System.currentTimeMillis()
                    )
                    dao.upsertPlayerProfile(updated)
                } else {
                    dao.upsertPlayerProfile(
                        PlayerProfileEntity(
                            nickname = nickname,
                            totalMatches = 1,
                            liesDetected = if (isSuspicious) 1 else 0,
                            trustScore = if (isSuspicious) 35 else 60,
                            favoriteRoles = claimedRole ?: "",
                            notes = reason
                        )
                    )
                }
            }

            // Добавляем запись в базу знаний
            dao.insertKnowledge(
                CoPilotKnowledgeEntity(
                    category = if (isSuspicious) "SUSPICION" else "STATEMENT",
                    subject = nickname,
                    content = "Сказано: '$userSpokenText' | Роль: ${claimedRole ?: "-"} | Вывод: ${if (isSuspicious) "Подозрительно ($reason)" else "Убедительно"}"
                )
            )

            if (voiceCallout.isNotBlank()) {
                _activeAdvice.value = voiceCallout
                _lastAiVerdict.value = voiceCallout
            }

            MeetingSpeechAnalysis(
                speakerNickname = nickname,
                claimedRole = claimedRole,
                claimedLocation = claimedLocation,
                claimedTasks = claimedTasks,
                isSuspiciousOrLying = isSuspicious,
                suspicionReason = reason,
                adviceVoiceCallout = voiceCallout
            )
        } catch (e: Exception) {
            AppLogger.e("GeminiLive", "Ошибка анализа фразы Gemini", e)
            MeetingSpeechAnalysis(
                speakerNickname = "Игрок",
                adviceVoiceCallout = "Фраза зафиксирована"
            )
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Resize bitmap if very large to optimize latency
        val scaled = if (bitmap.width > 1280 || bitmap.height > 720) {
            val ratio = 720f / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        scaled.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
