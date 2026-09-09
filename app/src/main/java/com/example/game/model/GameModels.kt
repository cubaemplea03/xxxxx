package com.example.game.model

enum class GameScreen {
    SPLASH,
    MAIN_MENU,
    GAMEPLAY,
    SETTINGS,
    CREDITS,
    UPGRADES,
    MONSTER_GUIDE,
    WARDROBE
}

data class GameMapInfo(
    val id: Int,
    val name: String,
    val subtitle: String,
    val description: String,
    val iconTag: String,
    val themeColorHex: Long
)

val AVAILABLE_MAPS = listOf(
    GameMapInfo(
        id = 1,
        name = "Bosque Sombrío",
        subtitle = "Lluvia Ácida & Niebla",
        description = "Pinos gigantes y ruinas ferroviarias bajo una tormenta nocturna continua.",
        iconTag = "🌲",
        themeColorHex = 0xFF4CAF50
    ),
    GameMapInfo(
        id = 2,
        name = "Cañón Carmesí",
        subtitle = "Atardecer & Polvo Rojo",
        description = "Mesetas áridas y calor abrasador bajo un cielo crepuscular sangriento.",
        iconTag = "🏜️",
        themeColorHex = 0xFFFF7043
    ),
    GameMapInfo(
        id = 3,
        name = "Tundra Glacial",
        subtitle = "Ventisca & Silos Helados",
        description = "Complejo industrial abandonado con vías congeladas a temperaturas extremas.",
        iconTag = "❄️",
        themeColorHex = 0xFF40C4FF
    )
)

enum class LevelPhase {
    ARRIVING,   // Tren llegando y frenando a la estación
    DEFENDING,  // Combate activo: eliminar a los 20 enemigos
    VICTORY,    // Nivel completado, todos los enemigos eliminados
    DEPARTING   // Tren acelerando hacia la siguiente estación
}

enum class EnemyType(val displayName: String, val speed: Float, val maxHp: Float, val damage: Float) {
    ERRANTE("Errante", 45f, 35f, 12f),
    CORREDOR("Corredor", 90f, 20f, 8f),
    TREPADOR("Trepador", 55f, 28f, 15f),
    BRUTO("Bruto Carmesí", 36f, 85f, 24f),
    JEFE("Titán del Páramo", 28f, 420f, 32f)
}

enum class EnemyState {
    SPAWNING,
    IDLE,
    CHASING,
    ATTACKING,
    HURT,
    DYING
}

enum class ResourceType(val label: String) {
    CHATARRA("Chatarra"),
    ORO("Oro"),
    MUNICION("Munición"),
    COMIDA("Comida"),
    MEDICINA("Medicamentos"),
    COMBUSTIBLE("Combustible")
}

data class Player(
    var x: Float = 350f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val width: Float = 28f,
    val height: Float = 44f,
    var facingRight: Boolean = true,
    var isGrounded: Boolean = true,
    var isCrouching: Boolean = false,
    var isAttacking: Boolean = false,
    var attackTimer: Float = 0f,
    var health: Float = 100f,
    val maxHealth: Float = 100f,
    var invulnerableTimer: Float = 0f,
    var ammo: Int = 12,
    var scrap: Int = 0,
    var gold: Int = 0,
    var medkits: Int = 1,
    var fuelCans: Int = 1,
    var currentWagonIndex: Int = 1,
    var animFrame: Float = 0f,
    var isDead: Boolean = false,
    var headSkin: String = "HEAD_DEFAULT",
    var chestSkin: String = "CHEST_DEFAULT",
    var legsSkin: String = "LEGS_DEFAULT"
)

data class Enemy(
    val id: Long,
    val type: EnemyType,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var facingRight: Boolean = false,
    var state: EnemyState = EnemyState.CHASING,
    var health: Float = type.maxHp,
    var maxHealth: Float = type.maxHp,
    var stateTimer: Float = 0f,
    var attackCooldown: Float = 0f,
    var isClimbing: Boolean = false,
    var animFrame: Float = 0f,
    var isBoss: Boolean = false,
    var bossTier: Int = 1,
    var bossName: String = "Titán del Páramo",
    var shootCooldown: Float = 2.2f,
    var chargeShootTimer: Float = 0f
)

data class EnemyProjectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    var life: Float = 3.0f,
    val color: Long = 0xFFFF1744,
    val radius: Float = 7f
)

data class Wagon(
    val index: Int,
    val name: String,
    val x: Float,
    val width: Float,
    val roofY: Float,
    val floorY: Float,
    val hasRoof: Boolean = true,
    val hasLadder: Boolean = false,
    val hasFurnace: Boolean = false,
    var hasCrate: Boolean = false,
    var crateOpened: Boolean = false
)

data class ResourceItem(
    val id: Long,
    val type: ResourceType,
    var x: Float,
    var y: Float,
    var collected: Boolean = false,
    var bobOffset: Float = 0f
)

data class Projectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    var life: Float = 1.0f
)

data class SlashEffect(
    val x: Float,
    val y: Float,
    val facingRight: Boolean,
    var life: Float = 0.15f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float = 1.0f,
    val size: Float = 4f,
    val color: Long = 0xFFCCCCCC,
    var life: Float = 1.0f,
    val maxLife: Float = 1.0f
)

data class FloatingText(
    val text: String,
    var x: Float,
    var y: Float,
    val color: Long,
    var life: Float = 1.2f
)

data class TurretProjectile(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    val isCannon: Boolean = false,
    var life: Float = 1.2f
)

data class TrainWeaponState(
    var turretLevel: Int = 0, // 0 = not owned, 1..3
    var cannonLevel: Int = 0, // 0 = not owned, 1..3
    var armorLevel: Int = 0,  // 0 = not owned, 1..3
    var spotlightLevel: Int = 0, // 0 = not owned, 1..3
    var turretAngle: Float = 0f,
    var cannonAngle: Float = 0f,
    var turretCooldown: Float = 0f,
    var cannonCooldown: Float = 0f,
    var turretMuzzleTimer: Float = 0f,
    var cannonMuzzleTimer: Float = 0f
)

data class TrainState(
    var speed: Float = 180f,
    var fuel: Float = 85f,
    var distance: Float = 0f,
    var stokeNotificationTimer: Float = 0f,
    var environmentType: String = "Bosque Nocturno"
)

data class MonsterGuideEntry(
    val type: EnemyType,
    val name: String,
    val title: String,
    val appearanceUnlockLevel: String,
    val baseHp: Int,
    val baseSpeed: Int,
    val baseDamage: Int,
    val threatLevel: String,
    val threatColorHex: Long,
    val lore: String,
    val attackPattern: String,
    val weaknesses: String,
    val survivalTip: String
)

val BESTIARY_ENTRIES = listOf(
    MonsterGuideEntry(
        type = EnemyType.ERRANTE,
        name = "ERRANTE",
        title = "Infectado Biomecánico Común",
        appearanceUnlockLevel = "Nivel 1+",
        baseHp = 35,
        baseSpeed = 45,
        baseDamage = 12,
        threatLevel = "BAJA",
        threatColorHex = 0xFF81C784,
        lore = "Antiguos maquinistas y pobladores del ferrocarril infectados por esporas radiactivas y nanomáquinas parásitas. Sus brazos mutaron en afiladas garras de chatarra oxidada. Conservan una marcha pesada pero incesante hacia el tren.",
        attackPattern = "Marcha constante hacia el convoy. Al alcanzar el vagón lanza zarpazos directos con sus cuchillas biomecánicas.",
        weaknesses = "Cabeza vulnerable y movilidad limitada. Disparos directos de pistola o una ráfaga de perdigones los neutralizan con facilidad.",
        survivalTip = "No permitas que se acumulen en el mismo vagón. Usa ataques cuerpo a cuerpo para ahorrar munición en los primeros niveles."
    ),
    MonsterGuideEntry(
        type = EnemyType.CORREDOR,
        name = "CORREDOR",
        title = "Quimera Cuadrúpeda Voraz",
        appearanceUnlockLevel = "Nivel 1+",
        baseHp = 20,
        baseSpeed = 90,
        baseDamage = 8,
        threatLevel = "MEDIA",
        threatColorHex = 0xFFFFB74D,
        lore = "Depredador cuadrúpedo nacido del mestizaje biológico forzado en el páramo. Su columna vertebral está reforzada con placas de espinas óseas y una cola punzante que le otorga un equilibrio y velocidad temibles.",
        attackPattern = "Embiste a más de 90 km/h saltando entre vagones para cerrar distancias rápidamente. Muerde continuamente provocando daño constante.",
        weaknesses = "Poca resistencia física (20 HP base). Caerá con un único disparo certero de escopeta o dos impactos de rifle.",
        survivalTip = "Agáchate o dispara apuntando bajo apenas lo veas aproximarse. Si estás recargando, salta hacia otro vagón para ganar tiempo."
    ),
    MonsterGuideEntry(
        type = EnemyType.TREPADOR,
        name = "TREPADOR",
        title = "Acechador Aracnomorfo de Techo",
        appearanceUnlockLevel = "Nivel 2+",
        baseHp = 28,
        baseSpeed = 55,
        baseDamage = 15,
        threatLevel = "ALTA",
        threatColorHex = 0xFFBA68C8,
        lore = "Organismo parásito que anida bajo los bogies y ruedas del tren. Cuenta con 6 extremidades articuladas con garras de escalada y un abdomen bioluminiscente lleno de secreciones neurotóxicas.",
        attackPattern = "Emerge desde los bajos del tren o trepa por las paredes laterales de los vagones, saltando sobre el jugador desde ángulos elevados inesperados.",
        weaknesses = "Mientras trepa por el lateral del vagón no puede esquivar ni contraatacar. Es el momento perfecto para eliminarlo.",
        survivalTip = "Monitorea la parte inferior y los bordes del tren. Elimínalos antes de que alcancen la cubierta superior del convoy."
    ),
    MonsterGuideEntry(
        type = EnemyType.BRUTO,
        name = "BRUTO CARMESÍ",
        title = "Behemoth Acorazado de Asalto",
        appearanceUnlockLevel = "Nivel 3+",
        baseHp = 85,
        baseSpeed = 36,
        baseDamage = 24,
        threatLevel = "MUY ALTA",
        threatColorHex = 0xFFFF5252,
        lore = "Un coloso biomecánico reforzado con planchas de acero industrial remachadas y un generador nuclear fundido en el pecho. Su masa corporal absorbe impactos sin inmutarse y sus nudillos con púas destrozan el blindaje.",
        attackPattern = "Avanza como un tanque imparable. Cada puñetazo derriba al jugador e inflige 24 puntos de daño masivo con sacudida de pantalla.",
        weaknesses = "Lento al girar y avanzar. El cañón pesado del tren y los disparos a quemarropa en su reactor pectoral infligen daño crítico.",
        survivalTip = "Mantén la distancia en todo momento. Instala y mejora la torreta y el cañón del tren para que lo desgasten antes de que te alcance."
    ),
    MonsterGuideEntry(
        type = EnemyType.JEFE,
        name = "TITÁN DEL PÁRAMO",
        title = "Jefe Colosal de Asalto (Cada 10 Niveles)",
        appearanceUnlockLevel = "Nivel 10, 20, 30...",
        baseHp = 420,
        baseSpeed = 28,
        baseDamage = 32,
        threatLevel = "☠️ CATASTRÓFICA ☠️",
        threatColorHex = 0xFFFF1744,
        lore = "La culminación del horror de las vías. Una aberración titánica de más de 4 metros construida sobre maquinaria de locomotora pesada, chimeneas que expulsan humo tóxico y un cañón dorsal de bio-plasma devastador.",
        attackPattern = "A corta distancia aplasta con garras hidráulicas (32 HP). A media y larga distancia carga su cañón dorsal y dispara esferas incandescentes de bio-plasma con dispersión y calor infernal.",
        weaknesses = "Durante la carga de su cañón (cuando la boca brilla en blanco y amarillo) queda fijo durante un instante. Es el momento de descargar tu rifle y llamar el fuego de artillería del tren.",
        survivalTip = "¡Moverse es sobrevivir! Salta entre vagones para esquivar las ráfagas de bio-plasma. Mejora el blindaje y el cañón del tren antes de alcanzar el Nivel 10."
    )
)

enum class WardrobeSlot(val displayName: String, val iconTag: String) {
    CABEZA("Cabeza", "🪖"),
    PECHO("Pecho", "🦺"),
    PIERNAS("Piernas", "👖")
}

data class WardrobeSkinItem(
    val id: String,
    val slot: WardrobeSlot,
    val name: String,
    val subtitle: String,
    val description: String,
    val costGold: Int,
    val rarity: String, // "BÁSICO", "RARO", "ÉPICO", "LEGENDARIO", "MÍTICO"
    val rarityColorHex: Long,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val accentColorHex: Long
)

val WARDROBE_SKINS: List<WardrobeSkinItem> = listOf(
    // ==========================================
    // FASE 1: CABEZA (Cascos, Máscaras, Visores)
    // ==========================================
    WardrobeSkinItem(
        id = "HEAD_DEFAULT",
        slot = WardrobeSlot.CABEZA,
        name = "Máscara de Gas Estándar",
        subtitle = "Equipo Básico de Superviviente",
        description = "Máscara militar de caucho reforzado con doble filtro de carbón y visor de cuarzo cian para filtrar el polvo radiactivo.",
        costGold = 0,
        rarity = "BÁSICO",
        rarityColorHex = 0xFF90A4AE,
        primaryColorHex = 0xFF37474F,
        secondaryColorHex = 0xFF263238,
        accentColorHex = 0xFF00E5FF
    ),
    WardrobeSkinItem(
        id = "HEAD_TACTICAL",
        slot = WardrobeSlot.CABEZA,
        name = "Casco Balístico S.W.A.T.",
        subtitle = "Asalto Militar Táctico",
        description = "Casco de kevlar con linterna táctica frontal de haz concentrado, visor polarizado ámbar y blindaje auditivo.",
        costGold = 300,
        rarity = "RARO",
        rarityColorHex = 0xFF42A5F5,
        primaryColorHex = 0xFF263238,
        secondaryColorHex = 0xFF37474F,
        accentColorHex = 0xFFFFB300
    ),
    WardrobeSkinItem(
        id = "HEAD_CYBER",
        slot = WardrobeSlot.CABEZA,
        name = "Visor Neural Cyberpunk",
        subtitle = "Tecnología de Vanguardia",
        description = "Visor panorámico con escaneo térmico holográfico, leds cian y magenta, y antenas de telemetría de combate.",
        costGold = 750,
        rarity = "ÉPICO",
        rarityColorHex = 0xFFAB47BC,
        primaryColorHex = 0xFF0D1B2A,
        secondaryColorHex = 0xFF1A237E,
        accentColorHex = 0xFFE040FB
    ),
    WardrobeSkinItem(
        id = "HEAD_SKULL",
        slot = WardrobeSlot.CABEZA,
        name = "Cráneo de Acero Infernal",
        subtitle = "Terror del Páramo",
        description = "Máscara forjada con placa craneal de hierro negro, cuernos curvos de acero y cuencas con llamas rojas incandescentes.",
        costGold = 1500,
        rarity = "LEGENDARIO",
        rarityColorHex = 0xFFFF7043,
        primaryColorHex = 0xFF212121,
        secondaryColorHex = 0xFF424242,
        accentColorHex = 0xFFFF1744
    ),
    WardrobeSkinItem(
        id = "HEAD_TITAN",
        slot = WardrobeSlot.CABEZA,
        name = "Corona Blindada del Coloso",
        subtitle = "Forja del Titán Jefe",
        description = "Yelmo sagrado fundido a partir de las placas del Titán Colosal. Corona de agujas de oro y visor de plasma solar deslumbrante.",
        costGold = 3000,
        rarity = "MÍTICO",
        rarityColorHex = 0xFFFFD700,
        primaryColorHex = 0xFF263238,
        secondaryColorHex = 0xFFFFD700,
        accentColorHex = 0xFFFF5722
    ),

    // ==========================================
    // FASE 2: PECHO (Abrigos, Chalecos, Armaduras)
    // ==========================================
    WardrobeSkinItem(
        id = "CHEST_DEFAULT",
        slot = WardrobeSlot.PECHO,
        name = "Guardapolvo de Cuero",
        subtitle = "Ropajes Clásicos",
        description = "Abrigo de cuero marrón curtido con pañuelo rojo de superviviente al cuello, gastado pero resistente al viento y lluvia ácida.",
        costGold = 0,
        rarity = "BÁSICO",
        rarityColorHex = 0xFF90A4AE,
        primaryColorHex = 0xFF4E342E,
        secondaryColorHex = 0xFF3E2723,
        accentColorHex = 0xFFD32F2F
    ),
    WardrobeSkinItem(
        id = "CHEST_TACTICAL",
        slot = WardrobeSlot.PECHO,
        name = "Chaleco Kevlar de Asalto",
        subtitle = "Protección Modular Ligera",
        description = "Chaleco balístico militar con placas de titanio, cananas de munición, radio de onda corta y arnés táctico reforzado.",
        costGold = 400,
        rarity = "RARO",
        rarityColorHex = 0xFF42A5F5,
        primaryColorHex = 0xFF1E2D2F,
        secondaryColorHex = 0xFF37474F,
        accentColorHex = 0xFF76FF03
    ),
    WardrobeSkinItem(
        id = "CHEST_CYBER",
        slot = WardrobeSlot.PECHO,
        name = "Exo-Pechera Nanocelular",
        subtitle = "Bio-Mecánica Neón",
        description = "Armadura pectoral de nanofibra con circuitos luminiscentes azul cian y microgenerador de energía cinética central.",
        costGold = 900,
        rarity = "ÉPICO",
        rarityColorHex = 0xFFAB47BC,
        primaryColorHex = 0xFF0B132B,
        secondaryColorHex = 0xFF1C2541,
        accentColorHex = 0xFF00E5FF
    ),
    WardrobeSkinItem(
        id = "CHEST_PUNKER",
        slot = WardrobeSlot.PECHO,
        name = "Chaqueta Berserker de Púas",
        subtitle = "Furia de los Rieles",
        description = "Chaqueta de cuero negro tachonada de pinchos de acero, cadenas pesadas cruzadas en el pecho y hombreras con cuchillas.",
        costGold = 1800,
        rarity = "LEGENDARIO",
        rarityColorHex = 0xFFFF7043,
        primaryColorHex = 0xFF3E1218,
        secondaryColorHex = 0xFF212121,
        accentColorHex = 0xFFFF5252
    ),
    WardrobeSkinItem(
        id = "CHEST_TITAN",
        slot = WardrobeSlot.PECHO,
        name = "Reactor Blindado del Titán",
        subtitle = "Coraza de Combustión",
        description = "Chasis acorazado de caldera con engranajes de locomotora y un núcleo de reactor volcánico pulsante en el corazón.",
        costGold = 3500,
        rarity = "MÍTICO",
        rarityColorHex = 0xFFFFD700,
        primaryColorHex = 0xFF192227,
        secondaryColorHex = 0xFF263238,
        accentColorHex = 0xFFFF3D00
    ),

    // ==========================================
    // FASE 3: PIERNAS (Pantalones, Grebas, Exo-Patas)
    // ==========================================
    WardrobeSkinItem(
        id = "LEGS_DEFAULT",
        slot = WardrobeSlot.PIERNAS,
        name = "Pantalón de Superviviente",
        subtitle = "Prenda Utilitaria Básica",
        description = "Pantalones de trabajo gris oscuro con remaches de lona y botas de cuero negro desgastadas por las vías.",
        costGold = 0,
        rarity = "BÁSICO",
        rarityColorHex = 0xFF90A4AE,
        primaryColorHex = 0xFF263238,
        secondaryColorHex = 0xFF1E282D,
        accentColorHex = 0xFF78909C
    ),
    WardrobeSkinItem(
        id = "LEGS_TACTICAL",
        slot = WardrobeSlot.PIERNAS,
        name = "Pantalón de Combate Urbano",
        subtitle = "Uniforme de Asalto",
        description = "Pantalones cargo camuflados en verde oliva con rodilleras de acero balístico y pistoleras laterales de extracción rápida.",
        costGold = 350,
        rarity = "RARO",
        rarityColorHex = 0xFF42A5F5,
        primaryColorHex = 0xFF2B3A33,
        secondaryColorHex = 0xFF1B2620,
        accentColorHex = 0xFFFFCA28
    ),
    WardrobeSkinItem(
        id = "LEGS_CYBER",
        slot = WardrobeSlot.PIERNAS,
        name = "Grebas Exo-Hidráulicas",
        subtitle = "Movilidad Aumentada",
        description = "Exo-esqueleto en piernas con pistones neumáticos de amortiguación, fibra de carbono y filamentos leds azul eléctrico.",
        costGold = 800,
        rarity = "ÉPICO",
        rarityColorHex = 0xFFAB47BC,
        primaryColorHex = 0xFF101C24,
        secondaryColorHex = 0xFF00E5FF,
        accentColorHex = 0xFF00B0FF
    ),
    WardrobeSkinItem(
        id = "LEGS_PUNKER",
        slot = WardrobeSlot.PIERNAS,
        name = "Botas Acorazadas con Cadenas",
        subtitle = "Estilo Demoledor",
        description = "Grebas pesadas de chapa oxidada con cadenas enrolladas en las pantorrillas y punteras de acero reforzadas con clavos.",
        costGold = 1600,
        rarity = "LEGENDARIO",
        rarityColorHex = 0xFFFF7043,
        primaryColorHex = 0xFF2C1B18,
        secondaryColorHex = 0xFFCFD8DC,
        accentColorHex = 0xFFFF1744
    ),
    WardrobeSkinItem(
        id = "LEGS_TITAN",
        slot = WardrobeSlot.PIERNAS,
        name = "Andadores Servo-Colosales",
        subtitle = "Poder Titánico",
        description = "Extremidades servo-asistidas con amortiguadores de bogie ferroviario, pistones pesados de vapor y placas blindadas ignífugas.",
        costGold = 3200,
        rarity = "MÍTICO",
        rarityColorHex = 0xFFFFD700,
        primaryColorHex = 0xFF1A2226,
        secondaryColorHex = 0xFF37474F,
        accentColorHex = 0xFFFFB300
    )
)

