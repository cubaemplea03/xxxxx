package com.example.game.engine

import com.example.game.audio.GameAudioEngine
import com.example.game.model.Enemy
import com.example.game.model.EnemyState
import com.example.game.model.EnemyType
import com.example.game.model.FloatingText
import com.example.game.model.LevelPhase
import com.example.game.model.Particle
import com.example.game.model.Player
import com.example.game.model.Projectile
import com.example.game.model.ResourceItem
import com.example.game.model.ResourceType
import com.example.game.model.SlashEffect
import com.example.game.model.TrainState
import com.example.game.model.TrainWeaponState
import com.example.game.model.TurretProjectile
import com.example.game.model.Wagon
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class GameEngine(
    val audioEngine: GameAudioEngine,
    val onGameOver: (distance: Int, scrap: Int, gold: Int, kills: Int, survivalLevel: Int) -> Unit,
    val onLevelComplete: (level: Int, kills: Int, goldEarned: Int, scrapEarned: Int) -> Unit = { _, _, _, _ -> },
    val onPlayerLevelUp: ((newLevel: Int) -> Unit)? = null
) {
    var player = Player()
    val wagons = mutableListOf<Wagon>()
    val enemies = mutableListOf<Enemy>()
    val resources = mutableListOf<ResourceItem>()
    val projectiles = mutableListOf<Projectile>()
    val turretProjectiles = mutableListOf<TurretProjectile>()
    val slashEffects = mutableListOf<SlashEffect>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()
    val train = TrainState()
    val trainWeapons = TrainWeaponState()

    var cameraX = 400f
    var isPaused = false
    var totalKills = 0
    var enemyIdCounter = 1L
    var resourceIdCounter = 1L

    // Inputs
    var inputLeft = false
    var inputRight = false
    var inputCrouch = false

    // Survival Mode state
    var selectedMapId = 1
    var currentLevel = 1
    var playerSurvivalLevel = 0
    var levelPhase: LevelPhase = LevelPhase.ARRIVING
    var trainArrivalTimer = 2.8f
    var totalEnemiesTarget = 20
    var enemiesSpawned = 0
    var enemiesDefeated = 0
    var goldEarnedThisLevel = 0
    var scrapEarnedThisLevel = 0
    var trainWheelOffset = 0f

    private var spawnTimer = 0f
    private var weatherTimer = 0f
    var isRaining = false
    var isSnowing = false
    var graphicQuality = "ALTA" // "BAJA", "MEDIA", "ALTA"

    init {
        initTrain()
        resetGame()
    }

    fun applyUpgrades(stats: com.example.game.data.GameStatsEntity) {
        trainWeapons.turretLevel = stats.turretLevel
        trainWeapons.cannonLevel = stats.cannonLevel
        trainWeapons.armorLevel = stats.armorLevel
        trainWeapons.spotlightLevel = stats.spotlightLevel
    }

    fun resetGame(mapId: Int = 1) {
        selectedMapId = mapId
        currentLevel = mapId
        levelPhase = LevelPhase.ARRIVING
        trainArrivalTimer = 2.8f
        enemiesSpawned = 0
        enemiesDefeated = 0
        playerSurvivalLevel = 0
        goldEarnedThisLevel = 0
        scrapEarnedThisLevel = 0

        player = Player(
            x = 550f,
            y = 180f,
            health = 100f,
            ammo = 18,
            scrap = player.scrap.coerceAtLeast(10),
            gold = player.gold,
            medkits = 1,
            fuelCans = 1
        )
        player.invulnerableTimer = 1.8f
        enemies.clear()
        resources.clear()
        projectiles.clear()
        turretProjectiles.clear()
        slashEffects.clear()
        particles.clear()
        floatingTexts.clear()

        train.fuel = 95f
        train.distance = 0f
        train.speed = 220f
        train.environmentType = getMapNameForLevel(selectedMapId)
        isSnowing = (selectedMapId == 3)
        isRaining = (selectedMapId == 1)

        cameraX = player.x
        spawnTimer = 1.8f
        initTrain()

        audioEngine.playTrainBrake()

        // Pre-populate crates
        for (w in wagons) {
            if (w.hasCrate) {
                w.crateOpened = false
            }
        }
    }

    fun startNextLevel() {
        resetGame(currentLevel + 1)
    }

    fun getMapNameForLevel(lvl: Int): String {
        return when (lvl) {
            1 -> "Bosque Nocturno"
            2 -> "Cañón Carmesí"
            3 -> "Complejo Helado"
            else -> "Tierras Yermas Nivel $lvl"
        }
    }

    private fun initTrain() {
        wagons.clear()
        // Train moving to the right.
        // Caboose (rear): x = 40..260
        // Flatcar: x = 280..520 (Turret sits here at x = 400)
        // Armored Boxcar: x = 540..840 (Cannon sits here at x = 690)
        // Tender: x = 860..1080
        // Locomotive: x = 1100..1460 (Furnace at x = 1340, spikes at x = 1450)
        wagons.add(Wagon(index = 0, name = "Caboose", x = 40f, width = 220f, roofY = 160f, floorY = 220f, hasRoof = true, hasLadder = true, hasCrate = true))
        wagons.add(Wagon(index = 1, name = "Plataforma Artillada", x = 280f, width = 240f, roofY = 220f, floorY = 220f, hasRoof = false, hasCrate = true))
        wagons.add(Wagon(index = 2, name = "Vagón Blindado", x = 540f, width = 300f, roofY = 150f, floorY = 220f, hasRoof = true, hasLadder = true, hasCrate = true))
        wagons.add(Wagon(index = 3, name = "Tender de Carbón", x = 860f, width = 220f, roofY = 180f, floorY = 220f, hasRoof = false, hasCrate = true))
        wagons.add(Wagon(index = 4, name = "Locomotora", x = 1100f, width = 360f, roofY = 140f, floorY = 220f, hasRoof = true, hasLadder = true, hasFurnace = true, hasCrate = false))
    }

    fun update(dt: Float) {
        if (isPaused || player.isDead) return

        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // 1. Level Phase State Machine
        when (levelPhase) {
            LevelPhase.ARRIVING -> {
                trainArrivalTimer -= clampedDt
                // Decelerate smoothly to 0
                val progress = (trainArrivalTimer / 2.8f).coerceIn(0f, 1f)
                train.speed = 220f * progress
                trainWheelOffset += train.speed * clampedDt

                // Wheel braking sparks
                if (Random.nextInt(10) < 5) {
                    val wheelX = listOf(140f, 400f, 700f, 960f, 1320f).random()
                    particles.add(
                        Particle(
                            x = wheelX,
                            y = 232f,
                            vx = -train.speed * 0.5f - Random.nextFloat() * 60f,
                            vy = -Random.nextFloat() * 40f - 10f,
                            alpha = 1.0f,
                            size = 3.5f,
                            color = 0xFFFFD54F,
                            life = 0.35f,
                            maxLife = 0.35f
                        )
                    )
                }

                if (trainArrivalTimer <= 0f) {
                    train.speed = 0f
                    levelPhase = LevelPhase.DEFENDING
                    spawnTimer = 1.2f
                    floatingTexts.add(FloatingText("¡TREN EN ${train.environmentType.uppercase()}!", player.x, 90f, 0xFFFFD54F))
                    floatingTexts.add(FloatingText("MODO SUPERVIVENCIA: CADA 15 BAJAS SUBES DE NIVEL", player.x, 115f, 0xFFFF7043))
                }
            }
            LevelPhase.DEFENDING -> {
                train.speed = 0f
                // Spawning enemies progressively in endless survival
                updateEnemySpawning(clampedDt)

                // Train weapons automated defense
                updateTrainWeapons(clampedDt)
            }
            LevelPhase.VICTORY -> {
                train.speed = 0f
            }
            LevelPhase.DEPARTING -> {
                train.speed = (train.speed + 80f * clampedDt).coerceAtMost(260f)
                trainWheelOffset += train.speed * clampedDt
            }
        }

        // 2. Weather & Particles
        updateWeatherAndParticles(clampedDt)

        // 3. Player physics & movement
        updatePlayer(clampedDt)

        // 4. Projectiles & Slash effects
        updateProjectiles(clampedDt)
        updateTurretProjectiles(clampedDt)

        // 5. Enemies update
        updateEnemies(clampedDt)

        // 6. Resources update
        updateResources(clampedDt)

        // 7. Floating texts
        updateFloatingTexts(clampedDt)

        // 8. Camera follow
        val targetCamX = player.x + (if (player.facingRight) 35f else -35f)
        cameraX += (targetCamX - cameraX) * 5f * clampedDt
    }

    private fun updateEnemySpawning(dt: Float) {
        spawnTimer -= dt
        val maxAliveOnScreen = if (graphicQuality == "BAJA") 3 else (4 + playerSurvivalLevel.coerceAtMost(3))

        if (spawnTimer <= 0f && enemies.size < maxAliveOnScreen) {
            spawnEnemy()
            enemiesSpawned++
            // Spawn interval: steady, accelerates slightly as survival level increases
            val baseDelay = (2.2f - playerSurvivalLevel * 0.15f).coerceAtLeast(1.1f)
            spawnTimer = Random.nextFloat() * 0.8f + baseDelay
        }
    }

    private fun updateTrainWeapons(dt: Float) {
        // 1. Turret (Vagón Plataforma, x = 400f, y = 200f)
        if (trainWeapons.turretLevel > 0) {
            val turretX = 400f
            val turretY = 195f

            if (trainWeapons.turretMuzzleTimer > 0f) {
                trainWeapons.turretMuzzleTimer -= dt
            }
            trainWeapons.turretCooldown -= dt

            // Find nearest alive enemy in range (up to 450 units)
            var closestEnemy: Enemy? = null
            var closestDist = 450f

            for (e in enemies) {
                if (e.state == EnemyState.DYING) continue
                val dist = sqrt((e.x - turretX) * (e.x - turretX) + (e.y - turretY) * (e.y - turretY))
                if (dist < closestDist) {
                    closestDist = dist
                    closestEnemy = e
                }
            }

            if (closestEnemy != null) {
                val dx = closestEnemy.x - turretX
                val dy = (closestEnemy.y - 15f) - turretY
                trainWeapons.turretAngle = atan2(dy, dx)

                val fireRate = when (trainWeapons.turretLevel) {
                    1 -> 0.75f
                    2 -> 0.50f
                    else -> 0.32f
                }

                if (trainWeapons.turretCooldown <= 0f) {
                    trainWeapons.turretCooldown = fireRate
                    trainWeapons.turretMuzzleTimer = 0.12f

                    val bulletSpeed = 620f
                    val angle = trainWeapons.turretAngle
                    val vx = cos(angle) * bulletSpeed
                    val vy = sin(angle) * bulletSpeed
                    val bulletDamage = 16f + trainWeapons.turretLevel * 6f

                    turretProjectiles.add(
                        TurretProjectile(
                            x = turretX + cos(angle) * 20f,
                            y = turretY + sin(angle) * 20f,
                            vx = vx,
                            vy = vy,
                            damage = bulletDamage,
                            isCannon = false
                        )
                    )
                    audioEngine.playTrainTurretShoot()
                    spawnImpactParticles(turretX + cos(angle) * 20f, turretY + sin(angle) * 20f, 0xFFFFEE55, count = 4)
                }
            }
        }

        // 2. Heavy Cannon (Vagón Blindado, x = 690f, y = 138f)
        if (trainWeapons.cannonLevel > 0) {
            val cannonX = 690f
            val cannonY = 138f

            if (trainWeapons.cannonMuzzleTimer > 0f) {
                trainWeapons.cannonMuzzleTimer -= dt
            }
            trainWeapons.cannonCooldown -= dt

            // Find enemy at longer range
            var targetEnemy: Enemy? = null
            var targetDist = 550f

            for (e in enemies) {
                if (e.state == EnemyState.DYING) continue
                val dist = sqrt((e.x - cannonX) * (e.x - cannonX) + (e.y - cannonY) * (e.y - cannonY))
                if (dist < targetDist) {
                    targetDist = dist
                    targetEnemy = e
                }
            }

            if (targetEnemy != null) {
                val dx = targetEnemy.x - cannonX
                val dy = (targetEnemy.y - 15f) - cannonY
                trainWeapons.cannonAngle = atan2(dy, dx)

                val cannonInterval = when (trainWeapons.cannonLevel) {
                    1 -> 2.6f
                    2 -> 1.9f
                    else -> 1.4f
                }

                if (trainWeapons.cannonCooldown <= 0f) {
                    trainWeapons.cannonCooldown = cannonInterval
                    trainWeapons.cannonMuzzleTimer = 0.2f

                    val shellSpeed = 460f
                    val angle = trainWeapons.cannonAngle
                    val vx = cos(angle) * shellSpeed
                    val vy = sin(angle) * shellSpeed
                    val shellDamage = 45f + trainWeapons.cannonLevel * 18f

                    turretProjectiles.add(
                        TurretProjectile(
                            x = cannonX + cos(angle) * 26f,
                            y = cannonY + sin(angle) * 26f,
                            vx = vx,
                            vy = vy,
                            damage = shellDamage,
                            isCannon = true
                        )
                    )
                    audioEngine.playTrainCannonShoot()
                    spawnImpactParticles(cannonX + cos(angle) * 26f, cannonY + sin(angle) * 26f, 0xFFFF7043, count = 10)
                }
            }
        }

        // 3. Spikes and armor on locomotive front (x = 1450)
        if (trainWeapons.armorLevel > 0) {
            val spikeX = 1450f
            for (e in enemies) {
                if (e.state == EnemyState.DYING) continue
                if (abs(e.x - spikeX) < 25f && e.y >= 190f) {
                    damageEnemy(e, 30f + trainWeapons.armorLevel * 12f)
                    e.x += 40f // Knock back
                    spawnImpactParticles(spikeX, 210f, 0xFFFF7043, count = 8)
                }
            }
        }
    }

    private fun updateTurretProjectiles(dt: Float) {
        val it = turretProjectiles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt

            var hit = false
            for (enemy in enemies) {
                if (enemy.state == EnemyState.DYING) continue
                if (abs(p.x - enemy.x) < 24f && abs(p.y - (enemy.y - 16f)) < 24f) {
                    hit = true
                    damageEnemy(enemy, p.damage)

                    if (p.isCannon) {
                        // AOE explosion
                        audioEngine.playEnemyHit()
                        spawnImpactParticles(p.x, p.y, 0xFFFF5722, count = 16)
                        floatingTexts.add(FloatingText("¡BOOM!", p.x, p.y - 25f, 0xFFFF5722))

                        // Splash damage to nearby enemies
                        for (other in enemies) {
                            if (other != enemy && other.state != EnemyState.DYING) {
                                val d = abs(other.x - p.x)
                                if (d < 80f) {
                                    damageEnemy(other, p.damage * 0.6f)
                                }
                            }
                        }
                    } else {
                        spawnImpactParticles(p.x, p.y, 0xFFFFEE55, count = 5)
                    }
                    break
                }
            }

            if (hit || p.life <= 0f) {
                it.remove()
            }
        }
    }

    private fun updatePlayer(dt: Float) {
        if (player.isDead) return

        val walkSpeed = if (player.isCrouching) 115f else 215f
        player.vx = when {
            inputLeft -> {
                player.facingRight = false
                -walkSpeed
            }
            inputRight -> {
                player.facingRight = true
                walkSpeed
            }
            else -> 0f
        }
        player.isCrouching = inputCrouch

        // Gravity
        val gravity = 720f
        player.vy += gravity * dt
        player.x += player.vx * dt
        player.y += player.vy * dt

        // Keep player within train limits (x = 30..1460)
        player.x = player.x.coerceIn(30f, 1450f)

        // Platform collision checks
        var grounded = false
        for (w in wagons) {
            if (player.x >= w.x - 6f && player.x <= w.x + w.width + 6f) {
                val standY = if (w.hasRoof && !player.isCrouching) {
                    w.roofY
                } else {
                    w.floorY
                }

                if (player.vy >= 0f && player.y >= standY && player.y - player.vy * dt <= standY + 16f) {
                    player.y = standY
                    player.vy = 0f
                    grounded = true
                    player.currentWagonIndex = w.index
                    break
                }
            }
        }

        // Fallback ground: Train chassis floor level (220f)
        if (player.vy >= 0f && player.y >= 220f) {
            player.y = 220f
            player.vy = 0f
            grounded = true
        }

        player.isGrounded = grounded

        // Animation frame
        if (player.vx != 0f && player.isGrounded) {
            player.animFrame += dt * 10f
        } else {
            player.animFrame = 0f
        }

        // Timers
        if (player.attackTimer > 0f) {
            player.attackTimer -= dt
            if (player.attackTimer <= 0f) {
                player.isAttacking = false
            }
        }

        if (player.invulnerableTimer > 0f) {
            player.invulnerableTimer -= dt
        }
    }

    private fun updateEnemies(dt: Float) {
        val it = enemies.iterator()
        while (it.hasNext()) {
            val enemy = it.next()
            enemy.stateTimer += dt
            enemy.attackCooldown -= dt

            if (enemy.state == EnemyState.DYING) {
                if (enemy.stateTimer >= 0.4f) {
                    it.remove()
                    continue
                }
            } else {
                // AI behavior
                val distToPlayer = abs(player.x - enemy.x)
                enemy.facingRight = player.x > enemy.x

                // Spotlight slowing effect
                val speedMod = if (trainWeapons.spotlightLevel > 0 && abs(enemy.x - 1350f) < 400f) 0.75f else 1.0f

                when (enemy.type) {
                    EnemyType.ERRANTE -> {
                        val dir = if (enemy.facingRight) 1f else -1f
                        if (distToPlayer > 28f) {
                            enemy.vx = dir * enemy.type.speed * speedMod
                            enemy.x += enemy.vx * dt
                            enemy.state = EnemyState.CHASING
                            enemy.animFrame += dt * 5f
                        } else {
                            enemy.vx = 0f
                            if (enemy.attackCooldown <= 0f) {
                                enemyAttack(enemy)
                            }
                        }
                    }
                    EnemyType.CORREDOR -> {
                        val dir = if (enemy.facingRight) 1f else -1f
                        if (distToPlayer > 36f) {
                            enemy.vx = dir * enemy.type.speed * speedMod
                            enemy.x += enemy.vx * dt
                            enemy.state = EnemyState.CHASING
                            enemy.animFrame += dt * 9f
                        } else {
                            enemy.vx = 0f
                            if (enemy.attackCooldown <= 0f) {
                                enemyAttack(enemy)
                            }
                        }
                    }
                    EnemyType.TREPADOR -> {
                        if (enemy.isClimbing) {
                            enemy.y -= 75f * dt
                            if (enemy.y <= 165f) {
                                enemy.y = 165f
                                enemy.isClimbing = false
                                enemy.state = EnemyState.CHASING
                            }
                        } else {
                            val dir = if (enemy.facingRight) 1f else -1f
                            if (distToPlayer > 30f) {
                                enemy.vx = dir * enemy.type.speed * speedMod
                                enemy.x += enemy.vx * dt
                                enemy.state = EnemyState.CHASING
                                enemy.animFrame += dt * 7f
                            } else {
                                enemy.vx = 0f
                                if (enemy.attackCooldown <= 0f) {
                                    enemyAttack(enemy)
                                }
                            }
                        }
                    }
                }

                // Ground clamping
                var enemyGrounded = false
                for (w in wagons) {
                    if (enemy.x >= w.x - 10f && enemy.x <= w.x + w.width + 10f) {
                        val standY = if (w.hasRoof) w.roofY else w.floorY
                        if (!enemy.isClimbing && enemy.y >= standY - 10f && enemy.y <= standY + 20f) {
                            enemy.y = standY
                            enemyGrounded = true
                            break
                        }
                    }
                }
                if (!enemyGrounded && !enemy.isClimbing) {
                    enemy.y = (enemy.y + 400f * dt).coerceAtMost(220f)
                }
            }
        }
    }

    private fun spawnEnemy() {
        val type = when (Random.nextInt(100)) {
            in 0..45 -> EnemyType.ERRANTE
            in 46..75 -> EnemyType.CORREDOR
            else -> EnemyType.TREPADOR
        }

        // Spawn on left or right flanks of the train
        val spawnLeft = Random.nextBoolean()
        val spawnX = if (spawnLeft) {
            (player.x - Random.nextFloat() * 220f - 180f).coerceAtLeast(50f)
        } else {
            (player.x + Random.nextFloat() * 220f + 180f).coerceAtMost(1430f)
        }

        val isClimber = (type == EnemyType.TREPADOR)
        val spawnY = if (isClimber) 260f else 160f

        val enemy = Enemy(
            id = enemyIdCounter++,
            type = type,
            x = spawnX,
            y = spawnY,
            health = type.maxHp,
            isClimbing = isClimber,
            state = if (isClimber) EnemyState.SPAWNING else EnemyState.CHASING
        )
        enemies.add(enemy)
    }

    private fun enemyAttack(enemy: Enemy) {
        enemy.state = EnemyState.ATTACKING
        enemy.attackCooldown = 1.2f
        val hitDistance = 35f
        if (abs(player.x - enemy.x) <= hitDistance && abs(player.y - enemy.y) <= 25f) {
            var dmg = enemy.type.damage
            // Armor damage reduction
            if (trainWeapons.armorLevel > 0) {
                val reduction = trainWeapons.armorLevel * 0.12f // up to 36% reduction
                dmg *= (1.0f - reduction)
            }
            damagePlayer(dmg)
        }
    }

    fun damagePlayer(amount: Float) {
        if (player.invulnerableTimer > 0f || player.isDead) return
        player.health = (player.health - amount).coerceAtLeast(0f)
        player.invulnerableTimer = 0.8f
        audioEngine.playPlayerHurt()

        spawnImpactParticles(player.x, player.y - 20f, 0xFFE53935)
        floatingTexts.add(FloatingText("-${amount.toInt()}", player.x, player.y - 35f, 0xFFFF4444))

        if (player.health <= 0f) {
            player.isDead = true
            onGameOver(train.distance.toInt(), player.scrap, player.gold, totalKills, playerSurvivalLevel)
        }
    }

    fun performMeleeAttack() {
        if (player.isDead || player.attackTimer > 0f) return
        player.isAttacking = true
        player.attackTimer = 0.22f
        audioEngine.playAttackSwing()

        val slashX = if (player.facingRight) player.x + 24f else player.x - 24f
        val slashY = player.y - 22f
        slashEffects.add(SlashEffect(x = slashX, y = slashY, facingRight = player.facingRight))

        val hitBoxWidth = 55f
        for (enemy in enemies) {
            if (enemy.state == EnemyState.DYING) continue
            val inFront = if (player.facingRight) enemy.x >= player.x - 10f && enemy.x <= player.x + hitBoxWidth
            else enemy.x <= player.x + 10f && enemy.x >= player.x - hitBoxWidth
            val inY = abs(enemy.y - player.y) < 30f

            if (inFront && inY) {
                damageEnemy(enemy, 24f)
            }
        }
    }

    fun performRangedAttack() {
        if (player.isDead || player.ammo <= 0) return
        player.ammo--
        audioEngine.playGunshot()

        val bulletSpeed = 550f * (if (player.facingRight) 1f else -1f)
        val bulletX = if (player.facingRight) player.x + 20f else player.x - 20f
        val bulletY = player.y - 26f
        projectiles.add(Projectile(x = bulletX, y = bulletY, vx = bulletSpeed, vy = 0f))

        spawnImpactParticles(bulletX, bulletY, 0xFFFFEE55, count = 4)
    }

    fun performInteract() {
        if (player.isDead) return

        // 1. Furnace
        val furnaceX = 1340f
        if (abs(player.x - furnaceX) < 70f) {
            if (player.fuelCans > 0) {
                player.fuelCans--
                train.fuel = (train.fuel + 35f).coerceAtMost(100f)
                audioEngine.playInteractFurnace()
                floatingTexts.add(FloatingText("+35 Combustible", furnaceX, 120f, 0xFFFFB300))
                spawnImpactParticles(furnaceX, 120f, 0xFFFF7043, count = 12)
                return
            } else if (player.scrap >= 15) {
                player.scrap -= 15
                train.fuel = (train.fuel + 20f).coerceAtMost(100f)
                audioEngine.playInteractFurnace()
                floatingTexts.add(FloatingText("+20 Combustible (15 Chatarra)", furnaceX, 120f, 0xFFFFB300))
                spawnImpactParticles(furnaceX, 120f, 0xFFFF7043, count = 8)
                return
            } else {
                floatingTexts.add(FloatingText("¡Necesitas Combustible o 15 Chatarra!", furnaceX, 120f, 0xFFE0E0E0))
                return
            }
        }

        // 2. Crates
        for (w in wagons) {
            if (w.hasCrate && !w.crateOpened) {
                val crateX = w.x + w.width * 0.5f
                if (abs(player.x - crateX) < 55f) {
                    w.crateOpened = true
                    audioEngine.playItemPickup()
                    spawnCrateLoot(crateX, if (w.hasRoof) w.roofY else w.floorY)
                    floatingTexts.add(FloatingText("¡Caja Saqueada!", crateX, player.y - 35f, 0xFF81C784))
                    return
                }
            }
        }

        // 3. Medkit
        if (player.medkits > 0 && player.health < 95f) {
            player.medkits--
            player.health = (player.health + 45f).coerceAtMost(100f)
            audioEngine.playItemPickup()
            floatingTexts.add(FloatingText("+45 Salud", player.x, player.y - 35f, 0xFF4CAF50))
            spawnImpactParticles(player.x, player.y - 20f, 0xFF81C784, count = 8)
            return
        }
    }

    private fun spawnCrateLoot(crateX: Float, crateY: Float) {
        val types = listOf(ResourceType.CHATARRA, ResourceType.ORO, ResourceType.MUNICION, ResourceType.COMIDA, ResourceType.MEDICINA, ResourceType.COMBUSTIBLE)
        val count = Random.nextInt(2, 4)
        for (i in 0 until count) {
            val type = types.random()
            resources.add(
                ResourceItem(
                    id = resourceIdCounter++,
                    type = type,
                    x = crateX + (i - 1) * 24f,
                    y = crateY - 12f
                )
            )
        }
    }

    private fun damageEnemy(enemy: Enemy, amount: Float) {
        enemy.health -= amount
        enemy.state = EnemyState.HURT
        audioEngine.playEnemyHit()
        spawnImpactParticles(enemy.x, enemy.y - 15f, 0xFFFF7043, count = 6)
        floatingTexts.add(FloatingText("-${amount.toInt()}", enemy.x, enemy.y - 30f, 0xFFFFCA28))

        if (enemy.health <= 0f) {
            enemy.state = EnemyState.DYING
            enemy.stateTimer = 0f
            totalKills++
            enemiesDefeated++
            audioEngine.playEnemyDeath()

            // Survival Level Up: every 15 kills (15 -> Nivel 1, 30 -> Nivel 2, etc.)
            val newLevel = enemiesDefeated / 15
            if (newLevel > playerSurvivalLevel) {
                playerSurvivalLevel = newLevel
                audioEngine.playLevelVictory()
                floatingTexts.add(FloatingText("★ ¡SUBIDA DE NIVEL! NIVEL $playerSurvivalLevel ★", player.x, 70f, 0xFFFFD700))
                floatingTexts.add(FloatingText("+75 ORO  •  ¡MEJORAS DESBLOQUEADAS!", player.x, 95f, 0xFF76FF03))
                floatingTexts.add(FloatingText("+8 MUNICIÓN  +25 SALUD", player.x, 120f, 0xFF40C4FF))
                player.gold += 75
                player.ammo = (player.ammo + 8).coerceAtMost(35)
                player.health = (player.health + 25f).coerceAtMost(100f)
                goldEarnedThisLevel += 75
                spawnImpactParticles(player.x, player.y - 20f, 0xFFFFD700, count = 22)
                onPlayerLevelUp?.invoke(playerSurvivalLevel)
            }

            // Defeated enemy awards gold directly!
            val goldGained = Random.nextInt(8, 16)
            player.gold += goldGained
            goldEarnedThisLevel += goldGained
            audioEngine.playGoldPickup()
            floatingTexts.add(FloatingText("+$goldGained ORO", enemy.x, enemy.y - 42f, 0xFFFFD700))

            // Drop scrap, ammo, or extra gold coin on ground
            val dropType = when (Random.nextInt(100)) {
                in 0..45 -> ResourceType.CHATARRA
                in 46..75 -> ResourceType.MUNICION
                else -> ResourceType.ORO
            }
            resources.add(ResourceItem(id = resourceIdCounter++, type = dropType, x = enemy.x, y = enemy.y - 8f))
            spawnImpactParticles(enemy.x, enemy.y - 15f, 0xFF78909C, count = 12)
        }
    }

    private fun updateProjectiles(dt: Float) {
        val bulletIt = projectiles.iterator()
        while (bulletIt.hasNext()) {
            val b = bulletIt.next()
            b.x += b.vx * dt
            b.life -= dt

            var hit = false
            for (enemy in enemies) {
                if (enemy.state == EnemyState.DYING) continue
                if (abs(b.x - enemy.x) < 22f && abs(b.y - (enemy.y - 18f)) < 24f) {
                    damageEnemy(enemy, 35f)
                    hit = true
                    break
                }
            }

            if (hit || b.life <= 0f) {
                bulletIt.remove()
            }
        }

        val slashIt = slashEffects.iterator()
        while (slashIt.hasNext()) {
            val s = slashIt.next()
            s.life -= dt
            if (s.life <= 0f) slashIt.remove()
        }
    }

    private fun updateResources(dt: Float) {
        val it = resources.iterator()
        while (it.hasNext()) {
            val r = it.next()
            r.bobOffset = sin(System.currentTimeMillis() * 0.006f + r.id).toFloat() * 3f

            // Pickup by player
            if (abs(player.x - r.x) < 32f && abs(player.y - r.y) < 30f) {
                when (r.type) {
                    ResourceType.ORO -> {
                        val amount = Random.nextInt(10, 20)
                        player.gold += amount
                        goldEarnedThisLevel += amount
                        audioEngine.playGoldPickup()
                        floatingTexts.add(FloatingText("+$amount Oro", r.x, r.y - 20f, 0xFFFFD700))
                    }
                    ResourceType.CHATARRA -> {
                        val amount = Random.nextInt(5, 12)
                        player.scrap += amount
                        scrapEarnedThisLevel += amount
                        audioEngine.playItemPickup()
                        floatingTexts.add(FloatingText("+$amount Chatarra", r.x, r.y - 20f, 0xFFFFCA28))
                    }
                    ResourceType.MUNICION -> {
                        val amount = Random.nextInt(5, 10)
                        player.ammo += amount
                        audioEngine.playItemPickup()
                        floatingTexts.add(FloatingText("+$amount Munición", r.x, r.y - 20f, 0xFF42A5F5))
                    }
                    ResourceType.COMIDA -> {
                        player.health = (player.health + 18f).coerceAtMost(100f)
                        audioEngine.playItemPickup()
                        floatingTexts.add(FloatingText("+18 Salud", r.x, r.y - 20f, 0xFF66BB6A))
                    }
                    ResourceType.MEDICINA -> {
                        player.medkits++
                        audioEngine.playItemPickup()
                        floatingTexts.add(FloatingText("+1 Botiquín", r.x, r.y - 20f, 0xFF26A69A))
                    }
                    ResourceType.COMBUSTIBLE -> {
                        player.fuelCans++
                        audioEngine.playItemPickup()
                        floatingTexts.add(FloatingText("+1 Combustible", r.x, r.y - 20f, 0xFFFF7043))
                    }
                }
                it.remove()
            }
        }
    }

    private fun updateWeatherAndParticles(dt: Float) {
        weatherTimer += dt
        if (weatherTimer > 40f) {
            weatherTimer = 0f
            if (currentLevel == 1) isRaining = !isRaining
        }

        val maxParticles = when (graphicQuality) {
            "BAJA" -> 20
            "MEDIA" -> 45
            else -> 80
        }

        if (particles.size < maxParticles) {
            // Locomotive chimney
            val smokeX = 1390f
            val smokeY = 90f
            particles.add(
                Particle(
                    x = smokeX + Random.nextFloat() * 8f - 4f,
                    y = smokeY,
                    vx = -Random.nextFloat() * 40f - 20f,
                    vy = -Random.nextFloat() * 30f - 15f,
                    alpha = 0.7f,
                    size = Random.nextFloat() * 8f + 6f,
                    color = 0x88455A64,
                    life = 1.4f,
                    maxLife = 1.4f
                )
            )

            // Snowflakes in Level 3
            if (isSnowing) {
                particles.add(
                    Particle(
                        x = cameraX + Random.nextFloat() * 600f - 300f,
                        y = 10f,
                        vx = Random.nextFloat() * 30f - 20f,
                        vy = Random.nextFloat() * 40f + 30f,
                        alpha = 0.8f,
                        size = Random.nextFloat() * 3f + 1.5f,
                        color = 0xDDFFFFFF,
                        life = 2.5f,
                        maxLife = 2.5f
                    )
                )
            }
        }

        val pIt = particles.iterator()
        while (pIt.hasNext()) {
            val p = pIt.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt
            p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            if (p.life <= 0f) pIt.remove()
        }
    }

    private fun spawnImpactParticles(x: Float, y: Float, color: Long, count: Int = 8) {
        val limit = if (graphicQuality == "BAJA") (count / 2).coerceAtLeast(2) else count
        for (i in 0 until limit) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = Random.nextFloat() * 160f - 80f,
                    vy = Random.nextFloat() * 140f - 70f,
                    alpha = 1f,
                    size = Random.nextFloat() * 4f + 2f,
                    color = color,
                    life = 0.4f,
                    maxLife = 0.4f
                )
            )
        }
    }

    private fun updateFloatingTexts(dt: Float) {
        val it = floatingTexts.iterator()
        while (it.hasNext()) {
            val ft = it.next()
            ft.y -= 25f * dt
            ft.life -= dt
            if (ft.life <= 0f) it.remove()
        }
    }
}
