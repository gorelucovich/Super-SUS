package com.example.model

import androidx.compose.ui.graphics.Color

enum class PlayerColor(
    val ruName: String,
    val colorHex: Long,
    val textColor: Long = 0xFFFFFFFF
) {
    RED("Красный", 0xFFE53935),
    BLUE("Синий", 0xFF1E88E5),
    GREEN("Зелёный", 0xFF43A047),
    YELLOW("Жёлтый", 0xFFFDD835, 0xFF212121),
    ORANGE("Оранжевый", 0xFFFB8C00),
    PURPLE("Фиолетовый", 0xFF8E24AA),
    CYAN("Голубой", 0xFF00ACC1),
    WHITE("Белый", 0xFFEEEEEE, 0xFF212121),
    BLACK("Чёрный", 0xFF212121),
    PINK("Розовый", 0xFFEC407A);

    val composeColor: Color
        get() = Color(colorHex)
}

enum class Faction(val ruName: String, val colorHex: Long) {
    SPACECREW("Космонавт", 0xFF4CAF50),
    IMPOSTOR("Предатель", 0xFFF44336),
    NEUTRAL("Нейтрал", 0xFFFF9800),
    UNKNOWN("Неизвестно", 0xFF9E9E9E)
}

data class SusRole(
    val id: String,
    val nameRu: String,
    val faction: Faction,
    val description: String,
    val keyBehaviors: List<String>,
    val canVent: Boolean = false,
    val canKill: Boolean = false
)

object SuperSusRoles {
    val allRoles = listOf(
        // Космонавты
        SusRole("spacecrew", "Космонавт", Faction.SPACECREW, "Обычный член экипажа, выполняет задания и ищет улики.", listOf("Делает визуалы", "Не прыгает в люки")),
        SusRole("sheriff", "Шериф", Faction.SPACECREW, "Может стрелять в подозреваемых. При выстреле в мирного погибает сам.", listOf("Охотится на импосторов", "Может убить"), canKill = true),
        SusRole("doctor", "Доктор", Faction.SPACECREW, "Может реанимировать погибших игроков на месте преступления.", listOf("Бегает к телам", "Оживляет игроков")),
        SusRole("detective", "Детектив", Faction.SPACECREW, "Видит следы игроков и знает, кто где проходил.", listOf("Следит за шагами", "Даёт точные тайминги")),
        SusRole("seer", "Пророк", Faction.SPACECREW, "Видит души погибших во время раунда.", listOf("Сразу знает о смертях", "Быстро репортит")),
        SusRole("engineer", "Инженер", Faction.SPACECREW, "Единственный мирный, способный прыгать в вентиляцию и чинить саботаж.", listOf("Прыгает в люк", "Чинит саботажи"), canVent = true),

        // Предатели
        SusRole("impostor", "Предатель", Faction.IMPOSTOR, "Устраняет членов экипажа и саботирует корабль.", listOf("Прыгает в люки", "Фейкает задания", "Алиби с напарником"), canVent = true, canKill = true),
        SusRole("chameleon", "Хамелеон", Faction.IMPOSTOR, "Становится невидимым на короткое время для незаметных устранений.", listOf("Внезапно появляется", "Убийства без свидетелей"), canVent = true, canKill = true),
        SusRole("spy", "Шпион", Faction.IMPOSTOR, "Маскируется под других игроков, чтобы подставить их.", listOf("Копирует внешность", "Подставляет мирных"), canVent = true, canKill = true),
        SusRole("undertaker", "Гробовщик", Faction.IMPOSTOR, "Может перетаскивать тела жертв в укромные места.", listOf("Прячет тела в люк", "Тела находят не там"), canVent = true, canKill = true),

        // Нейтралы
        SusRole("joker", "Джокер", Faction.NEUTRAL, "Побеждает, если его изгонят на голосовании! Главная цель — напроситься на кик.", listOf("Провоцирует голосование", "Странные отмазки", "Напрашивается на кик")),
        SusRole("arsonist", "Поджигатель", Faction.NEUTRAL, "Обливает всех игроков бензином и поджигает для соло-победы.", listOf("Держится рядом без убийств", "Прыгает в люк"), canVent = true),
        SusRole("phantom", "Фантом", Faction.NEUTRAL, "После смерти становится невидимым призраком и побеждает, выполнив задачи.", listOf("Умер первым", "Выполняет задачи"))
    )
}
