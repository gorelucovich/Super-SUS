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

enum class SusGameMode(
    val id: String,
    val ruTitle: String,
    val badge: String,
    val description: String,
    val rules: List<String>,
    val hasVanillaCrew: Boolean
) {
    ALL_ROLES(
        id = "all_roles",
        ruTitle = "Все роли",
        badge = "✨",
        description = "Все игроки получают специальные роли из 3 фракций.",
        rules = listOf(
            "Никаких обычных космических экипажей (у всех игроков спецроли)",
            "3 фракции: Экипаж, Предатели, Нейтралы",
            "Распределение спецролей по правилам режима"
        ),
        hasVanillaCrew = false
    ),
    RANDOM_ROLES(
        id = "random_roles",
        ruTitle = "Рандомные роли",
        badge = "🎲",
        description = "Случайный выбор ролей из общего пула без ограничений.",
        rules = listOf(
            "Полностью случайное распределение ролей",
            "Случайный выбор из общего пула ролей",
            "Высокая непредсказуемость и разнообразие сетапов"
        ),
        hasVanillaCrew = false
    )
}

object SuperSusRoles {
    val allRoles = listOf(
        // =========================================================================
        // === КОСМОНАВТЫ (SPACECREW) ===
        // =========================================================================
        SusRole(
            id = "sheriff",
            nameRu = "Шериф",
            faction = Faction.SPACECREW,
            description = "Может стрелять в подозреваемых во время раунда. При выстреле в мирного погибает сам.",
            iconBadge = "⭐",
            keyBehaviors = listOf("Охотится на импосторов", "Может стрелять без собрания", "Погибает при ошибке"),
            winCondition = "Устранить всех предателей и нейтралов",
            canKill = true
        ),
        SusRole(
            id = "bomber_crew",
            nameRu = "Подрыватель",
            faction = Faction.SPACECREW,
            description = "Минирует вентиляционные люки ловушками; предатели, прыгнувшие в заминированный люк, погибают на месте.",
            iconBadge = "💣",
            keyBehaviors = listOf("Минирует люки против предателей", "Люк взрывается при прыжке импостора", "Член экипажа"),
            winCondition = "Устранить предателей через ловушки или задания"
        ),
        SusRole(
            id = "doctor",
            nameRu = "Доктор",
            faction = Faction.SPACECREW,
            description = "Видит показатели жизнедеятельности (пульс) на собрании и реанимирует погибших на месте в раунде.",
            iconBadge = "💓",
            keyBehaviors = listOf("Иконка пульса на плашке собрания", "Бежит к телам для оживления", "Оживляет погибших"),
            winCondition = "Выполнить все задания экипажа"
        ),
        SusRole(
            id = "seer",
            nameRu = "Пророк",
            faction = Faction.SPACECREW,
            description = "Видит души погибших на карте и знает точное количество живых членов экипажа в реальном времени.",
            iconBadge = "👁️",
            keyBehaviors = listOf("Иконка пирамиды/глаза на плашке", "Мгновенно узнает об убийствах", "Быстро репортит"),
            winCondition = "Выполнить все задания экипажа"
        ),
        SusRole(
            id = "veteran",
            nameRu = "Ветеран",
            faction = Faction.SPACECREW,
            description = "Включает боевую готовность: любой, кто попытается напасть на него во время режима, погибает сам.",
            iconBadge = "🎖️",
            keyBehaviors = listOf("Иконка военного креста в итогах", "Защищает себя от килла", "Убивает нападающего"),
            winCondition = "Устранить всех предателей"
        ),
        SusRole(
            id = "mayor",
            nameRu = "Мэр",
            faction = Faction.SPACECREW,
            description = "Обладает правом дополнительных голосов на голосовании, решая исход собрания и ничьих.",
            iconBadge = "🗳️",
            keyBehaviors = listOf("Двойной/дополнительный голос на собрании", "Решает судьбу ничьих"),
            winCondition = "Выгнать предателей голосованием"
        ),
        SusRole(
            id = "agent",
            nameRu = "Агент",
            faction = Faction.SPACECREW,
            description = "Выполняет скрытые миссии и отслеживает перемещения подозреваемых по отсекам корабля.",
            iconBadge = "🕵️",
            keyBehaviors = listOf("Следит по карте датчиков", "Дает точные маршруты"),
            winCondition = "Завершить задания экипажа"
        ),
        SusRole(
            id = "engineer",
            nameRu = "Инженер",
            faction = Faction.SPACECREW,
            description = "Единственный мирный, способный прыгать в вентиляционные люки и чинить саботажи удаленно.",
            iconBadge = "🔧",
            keyBehaviors = listOf("Прыгает в люки легально", "Чинит саботажи на расстоянии"),
            winCondition = "Завершить задания экипажа",
            canVent = true
        ),
        SusRole(
            id = "detective",
            nameRu = "Детектив",
            faction = Faction.SPACECREW,
            description = "Видит цветные следы шагов игроков и время с момента их появления, вычисляя маршруты убийц.",
            iconBadge = "👣",
            keyBehaviors = listOf("Идет по следам ног", "Ловит убийц по горячим следам"),
            winCondition = "Найти и изгнать предателей"
        ),
        SusRole(
            id = "guardian",
            nameRu = "Страж",
            faction = Faction.SPACECREW,
            description = "Накладывает на выбранного игрока защитный щит, блокирующий один смертельный удар.",
            iconBadge = "🛡️",
            keyBehaviors = listOf("Вешает щит на мирных", "Спасает от первого нападения"),
            winCondition = "Защитить экипаж"
        ),
        SusRole(
            id = "baiter",
            nameRu = "Приманщик",
            faction = Faction.SPACECREW,
            description = "Видит присутствие в люках. При устранении принуждает убийцу автоматически зарепортить тело через 1 секунду!",
            iconBadge = "🪤",
            keyBehaviors = listOf("Видит присутствие в люке", "Убийца сам репортит тело через 1 сек", "Не репортить раньше времени"),
            winCondition = "Подставить убийцу на авто-репорте"
        ),
        SusRole(
            id = "time_master",
            nameRu = "Повелитель времени",
            faction = Faction.SPACECREW,
            description = "Активирует перемотку времени назад на несколько секунд, возвращая позиции и отменяя недавние убийства союзников.",
            iconBadge = "⏳",
            keyBehaviors = listOf("Перемотка времени назад", "Спасение союзников от килла", "Возврат на позиции"),
            winCondition = "Выполнить задания экипажа"
        ),
        SusRole(
            id = "magician",
            nameRu = "Фокусник",
            faction = Faction.SPACECREW,
            description = "Оставляет своё физическое тело и становится призраком: свободно передвигается сквозь стены и видит в темноте.",
            iconBadge = "🎩",
            keyBehaviors = listOf("Оставляет тело и становится призраком", "Ходит сквозь стены", "Видит в темноте"),
            winCondition = "Сбор информации и выполнение задач"
        ),
        SusRole(
            id = "observer",
            nameRu = "Наблюдатель",
            faction = Faction.SPACECREW,
            description = "Устанавливает дополнительные портативные камеры наблюдения и датчики перемещения в слепых зонах.",
            iconBadge = "📹",
            keyBehaviors = listOf("Ставит переносные камеры", "Контролирует слепые зоны"),
            winCondition = "Зафиксировать перемещения предателей"
        ),
        SusRole(
            id = "scout",
            nameRu = "Разведчик",
            faction = Faction.SPACECREW,
            description = "Обладает расширенным радиусом обзора и может вести наблюдение через определённые препятствия.",
            iconBadge = "🔭",
            keyBehaviors = listOf("Увеличенный радиус обзора", "Заглядывает за углы"),
            winCondition = "Выполнить задания экипажа"
        ),
        SusRole(
            id = "judge",
            nameRu = "Судья",
            faction = Faction.SPACECREW,
            description = "На собрании начинает судебный процесс над двумя игроками: все обязаны голосовать только за одного из них, скипнуть нельзя!",
            iconBadge = "⚖️",
            keyBehaviors = listOf("Назначает суд двум игрокам", "Запрещает скип на собрании", "Принудительный выбор"),
            winCondition = "Изгнать предателей через судебный вердикт"
        ),
        SusRole(
            id = "hacker",
            nameRu = "Хакер",
            faction = Faction.SPACECREW,
            description = "Удаленно взламывает камеры видеонаблюдения и панель Admin через личный планшет из любой точки корабля.",
            iconBadge = "💻",
            keyBehaviors = listOf("Планшет с камерами и админкой", "Удаленный контроль комнат"),
            winCondition = "Выполнить задания экипажа"
        ),
        SusRole(
            id = "light_warrior",
            nameRu = "Воин света",
            faction = Faction.SPACECREW,
            description = "Призывает мощную вспышку света, рассеивает саботаж темноты и окружает себя защитной аурой от нападений.",
            iconBadge = "⚔️",
            keyBehaviors = listOf("Рассеивает саботаж тьмы", "Защитная аура света", "Противостоит Тёмному императору"),
            winCondition = "Защитить экипаж"
        ),
        SusRole(
            id = "heroine",
            nameRu = "Героиня",
            faction = Faction.SPACECREW,
            description = "Защитная боевая роль, прикрывающая союзников и создающая безопасное пространство для ключевых мирных.",
            iconBadge = "🦸‍♀️",
            keyBehaviors = listOf("Прикрытие союзников", "Противодействие нападениям"),
            winCondition = "Защитить экипаж"
        ),
        SusRole(
            id = "xiao_yao",
            nameRu = "Сяо Яо",
            faction = Faction.SPACECREW,
            description = "Совершает стремительный рывок (дэш) через препятствия и стены; во время рывка полностью неуязвим.",
            iconBadge = "⚡",
            keyBehaviors = listOf("Рывок сквозь стены и препятствия", "Неуязвимость во время рывка", "Быстрый уход от погони"),
            winCondition = "Выполнить задания экипажа"
        ),
        SusRole(
            id = "archaeologist",
            nameRu = "Археолог",
            faction = Faction.SPACECREW,
            description = "Раскапывает древние артефакты и улики на местах преступлений, выявляя детали и свойства убийцы.",
            iconBadge = "🏺",
            keyBehaviors = listOf("Раскопки артефактов", "Поиск скрытых улик на месте гибели"),
            winCondition = "Выполнить задания экипажа и раскрыть убийц"
        ),
        SusRole(
            id = "nezha",
            nameRu = "Нэчжа",
            faction = Faction.SPACECREW,
            description = "Использует священные огненные кольца, накладывая барьеры и подавляя атаки врагов вокруг себя.",
            iconBadge = "🔥",
            keyBehaviors = listOf("Огненные кольца защиты", "Блокирует покушения"),
            winCondition = "Защитить экипаж"
        ),
        SusRole(
            id = "lord",
            nameRu = "Лорд",
            faction = Faction.SPACECREW,
            description = "Обладает благородным статусом: сплачивает союзников, усиливает защиту доверенных лиц и командует на собраниях.",
            iconBadge = "👑",
            keyBehaviors = listOf("Лидерское влияние", "Защита соратников"),
            winCondition = "Объединить экипаж и изгнать предателей"
        ),
        SusRole(
            id = "hunter",
            nameRu = "Охотник",
            faction = Faction.SPACECREW,
            description = "Боевой член экипажа: накладывает метку преследования на подозреваемого и может устранить врага.",
            iconBadge = "🏹",
            keyBehaviors = listOf("Боевая охота", "Метка на подозреваемого", "Казнь предателя"),
            winCondition = "Устранить предателей и нейтралов",
            canKill = true
        ),
        SusRole(
            id = "whitejack",
            nameRu = "УайтДжек",
            faction = Faction.SPACECREW,
            description = "Мирный гроссмейстер дедукции: на собрании угадывает роль врага. При верной догадке цель выбывает, при ошибке — погибает сам!",
            iconBadge = "🃏",
            keyBehaviors = listOf("Угадывает роли на собрании", "Мгновенное устранение врага", "Смертельный риск ошибки"),
            winCondition = "Вычислить и перебить всех предателей дедукцией"
        ),

        // =========================================================================
        // === ПРЕДАТЕЛИ (IMPOSTOR) ===
        // =========================================================================
        SusRole(
            id = "collector",
            nameRu = "Коллектор",
            faction = Faction.IMPOSTOR,
            description = "Собирает «трофеи» и контракты с игроков; накопив нужное количество, получает сокрушительные бонусы к атаке.",
            iconBadge = "💼",
            keyBehaviors = listOf("Сбор трофеев с целей", "Усиление с каждым убийством", "Прыгает в люки"),
            winCondition = "Устранить весь экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "ghost_bride",
            nameRu = "Призрачная невеста",
            faction = Faction.IMPOSTOR,
            description = "Связывает жертв призрачной связью и может дистанционно атаковать любого наблюдаемого игрока издали!",
            iconBadge = "👰",
            keyBehaviors = listOf("Дистанционная атака наблюдаемых целей", "Призрачная невидимость", "Убийства без сближения"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "alternates",
            nameRu = "Подобие",
            faction = Faction.IMPOSTOR,
            description = "Создает пугающую точную копию другого игрока или себя, сбивая с толку всех свидетелей и фабрикуя ложные алиби.",
            iconBadge = "👥",
            keyBehaviors = listOf("Создает двойников-копий", "Ложные алиби", "Имитирует внешность"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "godfather",
            nameRu = "Крестный отец",
            faction = Faction.IMPOSTOR,
            description = "Мафиозный босс: один раз за матч может мгновенно ПРЕРВАТЬ экстренное собрание, спасая себя или напарника от голосования!",
            iconBadge = "🌹",
            keyBehaviors = listOf("Досрочно отменяет собрание (1 раз)", "Срывает голосование против себя", "Прыгает в люки"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "prankster",
            nameRu = "Проказница",
            faction = Faction.IMPOSTOR,
            description = "Расставляет ящики-ловушки с сюрпризами: ослепляет, пачкает экран коробками и блокирует обзор членам экипажа.",
            iconBadge = "🎁",
            keyBehaviors = listOf("Ловушки-сюрпризы", "Блокирует зрение мирным", "Сеет хаос перед киллом"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "puppeteer",
            nameRu = "Кукловод",
            faction = Faction.IMPOSTOR,
            description = "Берет под ментальный контроль другого игрока и принуждает его атаковать стоящего рядом союзника!",
            iconBadge = "🎭",
            keyBehaviors = listOf("Управляет чужими атаками", "Подставляет мирных игроков", "Дистанционное управление"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "dark_emperor",
            nameRu = "Тёмный император",
            faction = Faction.IMPOSTOR,
            description = "Владыка тьмы: погружает корабль в непроглядную темноту, получая огромную скорость и пониженный КД килла.",
            iconBadge = "🖤",
            keyBehaviors = listOf("Усиление в темноте", "Быстрый кулдаун килла", "Противоположность Воина Света"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "time_assassin",
            nameRu = "Ассасин времени",
            faction = Faction.IMPOSTOR,
            description = "Манипулирует временем: сбрасывает собственные таймеры перезарядки и совершает молниеносные скачки при нападении.",
            iconBadge = "⌛",
            keyBehaviors = listOf("Сброс КД атаки скачком времени", "Телепортация на точку убийства", "Прыгает в люки"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "vampire",
            nameRu = "Вампир",
            faction = Faction.IMPOSTOR,
            description = "Кусает жертву: укушенный погибает с задержкой через несколько секунд, что позволяет вампиру спокойно уйти с места преступления.",
            iconBadge = "🧛",
            keyBehaviors = listOf("Отложенное убийство после укуса", "Снижение КД при успешном укусе", "Идеальное алиби"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "monkey_king",
            nameRu = "Король обезьян",
            faction = Faction.IMPOSTOR,
            description = "Призывает стаю призрачных обезьян-клонов, которые разбегаются по карте, отвлекая и дезориентируя экипаж.",
            iconBadge = "🐒",
            keyBehaviors = listOf("Создает бегающих клонов", "Отвлекает внимание от трупов", "Мобилен в люках"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "sniper",
            nameRu = "Снайпер",
            faction = Faction.IMPOSTOR,
            description = "Атакует издали по лазерной траектории прицеливания через целые коридоры, не приближаясь к жертве вплотную.",
            iconBadge = "🎯",
            keyBehaviors = listOf("Лазерный прицел через коридоры", "Снайперский выстрел на расстоянии", "Не оставляет следов рядом с телом"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "trickster",
            nameRu = "Плутовка",
            faction = Faction.IMPOSTOR,
            description = "Создает фальшивые декорации, коробки и оптические иллюзии, искажая восприятие происходящего мирными игроками.",
            iconBadge = "🃏",
            keyBehaviors = listOf("Обманные иллюзии", "Сбивает свидетелей с толку"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "ninja",
            nameRu = "Ниндзя",
            faction = Faction.IMPOSTOR,
            description = "Помечает жертву на расстоянии и телепортируется к ней для мгновенного бесшумного удара.",
            iconBadge = "🥷",
            keyBehaviors = listOf("Мгновенная телепортация", "Алиби на расстоянии"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "spy",
            nameRu = "Шпион",
            faction = Faction.IMPOSTOR,
            description = "Маскируется под других игроков, копируя их внешний вид и имя.",
            iconBadge = "🎩",
            keyBehaviors = listOf("Копирует облик других", "Фейковое алиби"),
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
            description = "Перетаскивает тела жертв в укромные места или сбрасывает их в вентиляционные люки.",
            iconBadge = "⚰️",
            keyBehaviors = listOf("Таскает тела", "Прячет тела в люк"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),
        SusRole(
            id = "janitor",
            nameRu = "Уборщик",
            faction = Faction.IMPOSTOR,
            description = "Полностью растворяет тела жертв, не оставляя улик для проведения репорта.",
            iconBadge = "🧹",
            keyBehaviors = listOf("Тела исчезают без следа", "Собрания без найденных тел"),
            winCondition = "Устранить экипаж",
            canVent = true,
            canKill = true
        ),

        // =========================================================================
        // === НЕЙТРАЛЫ (NEUTRALS) ===
        // =========================================================================
        SusRole(
            id = "influencer",
            nameRu = "Стример",
            faction = Faction.NEUTRAL,
            description = "Устанавливает камеры трансляции, переключается между экранами игроков и набирает просмотры за опасные моменты. Побеждает при наборе нужного числа зрителей!",
            iconBadge = "📱",
            keyBehaviors = listOf("Стримит события матча", "Переключает камеры игроков", "Победа по набору зрителей", "Не прыгает в люки"),
            winCondition = "Собрать целевую аудиторию зрителей на стриме",
            canVent = false
        ),
        SusRole(
            id = "gluttony",
            nameRu = "Обжора",
            faction = Faction.NEUTRAL,
            description = "Пожирает лежащие тела и может проглотить живого игрока целиком! Побеждает соло, когда насытится нужным числом жертв.",
            iconBadge = "🍔",
            keyBehaviors = listOf("Пожирает тела погибших", "Проглатывает живых игроков", "Соло-победа по сытости", "Не прыгает в люки"),
            winCondition = "Проглотить нужное число игроков и насытиться",
            canVent = false,
            canKill = true
        ),
        SusRole(
            id = "greyjack",
            nameRu = "Грей Джек",
            faction = Faction.NEUTRAL,
            description = "Нейтральный угадыватель ролей на собраниях. Угадывает роль игрока: при верной догадке цель выбывает, при ошибке — погибает сам.",
            iconBadge = "🃏",
            keyBehaviors = listOf("Угадывает роли на собрании", "Охотится и на мирных, и на предателей", "Не прыгает в люки"),
            winCondition = "Устранить заданное число игроков своими догадками",
            canVent = false
        ),
        SusRole(
            id = "cupid",
            nameRu = "Купидон",
            faction = Faction.NEUTRAL,
            description = "Стреляет стрелами любви в двух игроков на старте матча, связывая их парой «Влюблённых». Влюблённые побеждают вместе, если выживут вдвоём!",
            iconBadge = "💘",
            keyBehaviors = listOf("Связывает пару влюбленных", "Если погибает один, второй умирает от горя", "Победа вместе с парой"),
            winCondition = "Довести связанную пару влюбленных до победного финала",
            canVent = false
        ),
        SusRole(
            id = "honey_bee",
            nameRu = "Пчела",
            faction = Faction.NEUTRAL,
            description = "Трудолюбиво собирает пыльцу и нектар с игроков и задач, наполняя медовый улей. Побеждает соло при заполнении всех сот!",
            iconBadge = "🐝",
            keyBehaviors = listOf("Собирает нектар с игроков", "Опыляет задания", "Мирная соло-победа через мёд", "Не прыгает в люки"),
            winCondition = "Заполнить улей мёдом до 100%",
            canVent = false
        ),
        SusRole(
            id = "kaitou",
            nameRu = "Кайто",
            faction = Faction.NEUTRAL,
            description = "Благородный вор: похищает роль у другого игрока! После кражи полностью перенимает его фракцию и способности, а жертва становится обычным космонавтом.",
            iconBadge = "🎩",
            keyBehaviors = listOf("Крадет роль и способности", "Меняет свою фракцию на фракцию жертвы", "Жертва лишается спецроли"),
            winCondition = "Украсть ключевую роль и победить с новой командой",
            canVent = false
        ),
        SusRole(
            id = "backstabber",
            nameRu = "Подлец",
            faction = Faction.NEUTRAL,
            description = "Коварный убийца: наносит смертельный удар ТОЛЬКО со спины (Backstab)! Побеждает соло, совершив серию удачных ударов в спину.",
            iconBadge = "🗡️",
            keyBehaviors = listOf("Убивает строго со спины", "Не может атаковать спереди", "Соло-победа по счетчику ударов", "Не прыгает в люки"),
            winCondition = "Совершить нужное количество успешных ударов в спину",
            canVent = false,
            canKill = true
        ),
        SusRole(
            id = "joker",
            nameRu = "Джокер",
            faction = Faction.NEUTRAL,
            description = "Побеждает, если его изгонят на голосовании! Главная цель — напроситься на кик на собрании.",
            iconBadge = "🃏",
            keyBehaviors = listOf("Провоцирует голосование", "Странные нелепые алиби", "Просит кикнуть себя", "Не прыгает в люки"),
            winCondition = "Быть изгнанным на голосовании",
            canVent = false
        ),
        SusRole(
            id = "arsonist",
            nameRu = "Поджигатель",
            faction = Faction.NEUTRAL,
            description = "Обливает всех игроков бензином при сближении, затем поджигает для мгновенной соло-победы. В люки прыгать не может.",
            iconBadge = "🔥",
            keyBehaviors = listOf("Держится рядом без убийств", "Обливает игроков бензином", "Не прыгает в люки"),
            winCondition = "Облить всех и поджечь",
            canVent = false
        ),
        SusRole(
            id = "survivor",
            nameRu = "Выживший",
            faction = Faction.NEUTRAL,
            description = "Побеждает, если просто доживает до финала матча вместе с победившей стороной.",
            iconBadge = "🛡️",
            keyBehaviors = listOf("Активирует персональный щит", "Не вступает в конфликты", "Пассивная тактика", "Не прыгает в люки"),
            winCondition = "Дожить до конца матча",
            canVent = false
        ),
        SusRole(
            id = "phantom",
            nameRu = "Фантом",
            faction = Faction.NEUTRAL,
            description = "После смерти превращается в невидимого призрака и побеждает, если выполнит все свои задачи незамеченным.",
            iconBadge = "👻",
            keyBehaviors = listOf("Умирает в начале матча", "Выполняет задачи призраком", "Не прыгает в люки"),
            winCondition = "Завершить задачи призрака",
            canVent = false
        ),
        SusRole(
            id = "hypnotist",
            nameRu = "Гипнотизёр",
            faction = Faction.NEUTRAL,
            description = "Гипнотизирует других игроков, обращая их в своих приспешников для общей победы.",
            iconBadge = "🌀",
            keyBehaviors = listOf("Вербует союзников", "Тайная фракция", "Не прыгает в люки"),
            winCondition = "Обратить экипаж",
            canVent = false
        ),
        SusRole(
            id = "ghoul",
            nameRu = "Гуль",
            faction = Faction.NEUTRAL,
            description = "Поедает тела погибших игроков и устраняет экипаж для соло-победы (голова монстра в фиолетовой строке MVP).",
            iconBadge = "👾",
            keyBehaviors = listOf("Поедает тела жертв", "Устраняет игроков", "Фиолетовая строка MVP", "Не прыгает в люки"),
            winCondition = "Поглотить нужное число игроков",
            canVent = false,
            canKill = true
        )
    )
}
