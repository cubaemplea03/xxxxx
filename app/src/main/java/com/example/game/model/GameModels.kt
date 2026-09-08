package com.example.game.model

enum class GameScreen {
    SPLASH,
    MAIN_MENU,
    GAMEPLAY,
    SETTINGS,
    CREDITS,
    UPGRADES
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
    TREPADOR("Trepador", 55f, 28f, 15f)
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
    var isDead: Boolean = false
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
    val maxHealth: Float = type.maxHp,
    var stateTimer: Float = 0f,
    var attackCooldown: Float = 0f,
    var isClimbing: Boolean = false,
    var animFrame: Float = 0f
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
