package com.example.model

import androidx.compose.ui.graphics.Color

enum class PlayerColor(
    val ruName: String,
    val colorHex: Long,
    val textColor: Long = 0xFFFFFFFF
) {
    RED("Красный", 0xFFE53935),
    GARNET_RED("Гранатово-красный", 0xFF880E4F),
    BLUE("Синий", 0xFF1E88E5),
    CYAN("Голубой", 0xFF00BCD4, 0xFF212121),
    YELLOW("Жёлтый", 0xFFFDD835, 0xFF212121),
    PURPLE("Фиолетовый", 0xFF8E24AA),
    PINK("Розовый", 0xFFEC407A),
    SILVER("Серебряный", 0xFFCFD8DC, 0xFF212121),
    BROWN("Коричневый", 0xFF795548),
    WHITE("Белый", 0xFFF5F5F5, 0xFF212121);

    val composeColor: Color
        get() = Color(colorHex)
}

enum class Faction(val ruName: String, val colorHex: Long) {
    SPACECREW("Космонавт", 0xFF3B82F6), // Синяя полоса
    IMPOSTOR("Предатель", 0xFFEF4444), // Красная полоса
    NEUTRAL("Нейтрал", 0xFF9333EA), // Фиолетовая полоса
    UNKNOWN("Неизвестно", 0xFF6B7280)
}

data class SusRole(
    val id: String,
    val nameRu: String,
    val faction: Faction,
    val description: String,
    val iconBadge: String,
    val keyBehaviors: List<String>,
    val winCondition: String = "",
    val canVent: Boolean = false,
    val canKill: Boolean = false
)

object SuperSusRoles {
    val allRoles = listOf(
        // === КОСМОНАВТЫ (SPACECREW) ===
        SusRole(
            id = "sheriff",
            nameRu = "Шериф",
            faction = Faction.SPACECREW,
            description = "Может стрелять в подозреваемых. При выстреле в мирного погибает сам.",
            iconBadge = "⭐",
            keyBehaviors = listOf("Охотится на импосторов", "Может стрелять без собрания", "Погибает при ошибке"),
            winCondition = "Устранить всех предателей и нейтралов",
            canKill = true
        ),
        SusRole(
            id = "bomber_crew",
            nameRu = "Подрыватель",
            faction = Faction.SPACECREW,
            description = "Член экипажа: минирует вентиляционные люки ловушками; предатели, прыгнувшие в заминированный люк, погибают на месте.",
            iconBadge = "💣",
            keyBehaviors = listOf("Минирует люки против предателей", "Люк взрывается при прыжке импостора", "Член экипажа"),
            winCondition = "Устранить предателей через ловушки или задания"
        ),
        SusRole(
            id = "doctor",
            nameRu = "Доктор",
            faction = Faction.SPACECREW,
            description = "Видит показатели жизнедеятельности (пульс на плашках) и реанимирует погибших на месте.",
            iconBadge = "💓",
            keyBehaviors = listOf("Иконка пульса на плашке собрания", "Бежит к телам для оживления", "Оживляет погибших"),
            winCondition = "Выполнить все задания экипажа"
        ),
        SusRole(
            id = "seer",
            nameRu = "Пророк",
            faction = Faction.SPACECREW,
            description = "Видит души погибших в раунде и знает общее число живых членов экипажа (значок ока/пирамиды).",
            iconBadge = "👁️",
            keyBehaviors = listOf("Иконка пирамиды/глаза на плашке", "Мгновенно узнает об убийствах", "Быстро репортит"),
            winCondition = "Выполнить все задания экипажа"
        ),
        SusRole(
            id = "veteran",
            nameRu = "Ветеран",
            faction = Faction.SPACECREW,
            description = "Включает боевую готовность: любой, кто попытается напасть на него, погибает сам.",
            iconBadge = "🎖️",
            keyBehaviors = listOf("Иконка военного креста в итогах", "Защищает себя от килла", "Убивает нападающего"),
            winCondition = "Устранить всех предателей"
        ),
        SusRole(
            id = "mayor",
            nameRu = "Мэр",
            faction = Faction.SPACECREW,
            description = "Обладает правом дополнительного голоса на голосовании, решая исход собрания.",
            iconBadge = "🗳️",
            keyBehaviors = listOf("Двойной голос на собрании", "Решает судьбу ничьих"),
            winCondition = "Выгнать предателей голосованием"
        ),
        SusRole(
            id = "agent",
            nameRu = "Агент",
            faction = Faction.SPACECREW,
            description = "Выполняет скрытые миссии и отслеживает перемещения подозреваемых по отсекам.",
            iconBadge = "🕵️",
            keyBehaviors = listOf("Следит по карте датчиков", "Дает точные маршруты"),
            winCondition = "Завершить задания экипажа"
        ),
        SusRole(
            id = "engineer",
            nameRu = "Инженер",
            faction = Faction.SPACECREW,
            description = "Единственный мирный, способный прыгать в вентиляцию и чинить саботажи удаленно.",
            iconBadge = "🔧",
            keyBehaviors = listOf("Прыгает в люки легально", "Чинит саботажи на расстоянии"),
            winCondition = "Завершить задания экипажа",
            canVent = true
        ),
        SusRole(
            id = "detective",
            nameRu = "Детектив",
            faction = Faction.SPACECREW,
            description = "Видит цветные следы шагов игроков и вычисляет, кто проходил мимо места преступления.",
            iconBadge = "👣",
            keyBehaviors = listOf("Идет по следам ног", "Ловит убийц по горячим следам"),
            winCondition = "Найти и изгнать предателей"
        ),
        SusRole(
            id = "guardian",
            nameRu = "Хранитель",
            faction = Faction.SPACECREW,
            description = "Накладывает на выбранного игрока защитный щит от одного смертельного удара.",
            iconBadge = "🛡️",
            keyBehaviors = listOf("Вешает щит на мирных", "Спасает от первого нападения"),
            winCondition = "Защитить экипаж"
        ),
        SusRole(
            id = "baiter",
            nameRu = "Приманка",
            faction = Faction.SPACECREW,
            description = "При устранении принуждает убийцу автоматически сделать моментальный саморепорт!",
            iconBadge = "🪤",
            keyBehaviors = listOf("Убийца моментально репортит тело", "100% вычисление убийцы на собрании"),
            winCondition = "Подставить убийцу на саморепорте"
        ),

        // === ПРЕДАТЕЛИ (IMPOSTOR) ===
        SusRole(
            id = "spy",
            nameRu = "Шпион",
            faction = Faction.IMPOSTOR,
            description = "Маскируется под других игроков, копируя их внешний вид и имя (красная шляпа в итогах).",
            iconBadge = "🎩",
            keyBehaviors = listOf("Красная шляпа в профиле", "Копирует облик других", "Может взять приспособление Код для фейк-визуала"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "chameleon",
            nameRu = "Хамелеон",
            faction = Faction.IMPOSTOR,
            description = "Становится полностью невидимым на короткое время для скрытных нападений.",
            iconBadge = "🦎",
            keyBehaviors = listOf("Исчезает на глазах", "Убийства без свидетелей"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "undertaker",
            nameRu = "Гробовщик",
            faction = Faction.IMPOSTOR,
            description = "Перетаскивает тела жертв в укромные места или сбрасывает в вентиляцию (значок лопаты в итогах).",
            iconBadge = "⚰️",
            keyBehaviors = listOf("Значок лопаты в итогах", "Прячет тела в люк", "Тела находят не там"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "janitor",
            nameRu = "Уборщик",
            faction = Faction.IMPOSTOR,
            description = "Убирает тела жертв, не оставляя улик для репорта.",
            iconBadge = "🧹",
            keyBehaviors = listOf("Тела исчезают без следа", "Собрания без найденных тел"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "ninja",
            nameRu = "Ниндзя",
            faction = Faction.IMPOSTOR,
            description = "Помечает жертву на расстоянии и телепортируется к ней для мгновенного удара.",
            iconBadge = "🥷",
            keyBehaviors = listOf("Мгновенная телепортация", "Алиби на расстоянии"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),

        // === НЕЙТРАЛЫ (NEUTRALS) ===
        SusRole(
            id = "ghoul",
            nameRu = "Гуль",
            faction = Faction.NEUTRAL,
            description = "Нейтрал: поедает тела погибших игроков и устраняет экипаж для соло-победы (голова монстра в фиолетовой строке MVP).",
            iconBadge = "👾",
            keyBehaviors = listOf("Поедает тела жертв", "Устраняет игроков", "Фиолетовая строка MVP"),
            winCondition = "Поглотить нужное число игроков",
            canKill = true
        ),
        SusRole(
            id = "joker",
            nameRu = "Джокер",
            faction = Faction.NEUTRAL,
            description = "Побеждает, если его изгонят на голосовании! Главная цель — напроситься на кик на собрании.",
            iconBadge = "🃏",
            keyBehaviors = listOf("Провоцирует голосование", "Странные нелепые алиби", "Просит кикнуть себя"),
            winCondition = "Быть изгнанным на голосовании"
        ),
        SusRole(
            id = "arsonist",
            nameRu = "Поджигатель",
            faction = Faction.NEUTRAL,
            description = "Обливает всех игроков бензином при сближении, затем поджигает для мгновенной соло-победы.",
            iconBadge = "🔥",
            keyBehaviors = listOf("Держится рядом без убийств", "Прыгает в люки", "Не голосует агрессивно"),
            winCondition = "Облить всех и поджечь",
            canVent = true
        ),
        SusRole(
            id = "survivor",
            nameRu = "Выживший",
            faction = Faction.NEUTRAL,
            description = "Побеждает, если просто доживает до финала матча вместе с победившей стороной.",
            iconBadge = "🛡️",
            keyBehaviors = listOf("Активирует персональный щит", "Не вступает в конфликты", "Пассивная тактика"),
            winCondition = "Дожить до конца матча"
        ),
        SusRole(
            id = "phantom",
            nameRu = "Фантом",
            faction = Faction.NEUTRAL,
            description = "После смерти превращается в невидимого призрака и побеждает, если выполнит все свои задачи незамеченным.",
            iconBadge = "👻",
            keyBehaviors = listOf("Умирает в начале матча", "Выполняет задачи призраком"),
            winCondition = "Завершить задачи призрака"
        ),
        SusRole(
            id = "hypnotist",
            nameRu = "Гипнотизёр",
            faction = Faction.NEUTRAL,
            description = "Гипнотизирует других игроков, обращая их в своих приспешников для общей победы.",
            iconBadge = "🌀",
            keyBehaviors = listOf("Вербует союзников", "Тайная фракция"),
            winCondition = "Обратить экипаж"
        )
    )
}
