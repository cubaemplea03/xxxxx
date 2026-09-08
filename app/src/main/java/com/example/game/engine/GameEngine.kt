package com.example.game.engine

import com.example.game.audio.GameAudioEngine
import com.example.game.model.Enemy
import com.example.game.model.EnemyState
import com.example.game.model.EnemyType
import com.example.game.model.FloatingText
import com.example.game.model.Particle
import com.example.game.model.Player
import com.example.game.model.Projectile
import com.example.game.model.ResourceItem
import com.example.game.model.ResourceType
import com.example.game.model.SlashEffect
import com.example.game.model.TrainState
import com.example.game.model.Wagon
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class GameEngine(
    val audioEngine: GameAudioEngine,
    val onGameOver: (distance: Int, scrap: Int, kills: Int) -> Unit
) {
    var player = Player()
    val wagons = mutableListOf<Wagon>()
    val enemies = mutableListOf<Enemy>()
    val resources = mutableListOf<ResourceItem>()
    val projectiles = mutableListOf<Projectile>()
    val slashEffects = mutableListOf<SlashEffect>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()
    val train = TrainState()

    var cameraX = 400f
    var isPaused = false
    var totalKills = 0
    var enemyIdCounter = 1L
    var resourceIdCounter = 1L

    // Inputs
    var inputLeft = false
    var inputRight = false
    var inputCrouch = false

    private var spawnTimer = 0f
    private var weatherTimer = 0f
    private var isRaining = false
    var graphicQuality = "ALTA" // "BAJA", "MEDIA", "ALTA"

    init {
        initTrain()
        resetGame()
    }

    fun resetGame() {
        player = Player(x = 550f, y = 180f, health = 100f, ammo = 14, scrap = 10, medkits = 1, fuelCans = 1)
        player.invulnerableTimer = 1.5f
        enemies.clear()
        resources.clear()
        projectiles.clear()
        slashEffects.clear()
        particles.clear()
        floatingTexts.clear()
        train.fuel = 90f
        train.distance = 0f
        train.speed = 180f
        train.environmentType = "Bosque Nocturno"
        totalKills = 0
        cameraX = player.x
        spawnTimer = 4.5f
        initTrain()

        // Pre-populate some crates
        for (w in wagons) {
            if (w.hasCrate) {
                w.crateOpened = false
            }
        }
    }

    private fun initTrain() {
        wagons.clear()
        // Train moving to the right. Locomotive in front (x = 1220..1540)
        // Caboose (rear): x = 40..260
        // Flatcar: x = 280..520
        // Boxcar 1: x = 540..840
        // Tender: x = 860..1080
        // Locomotive: x = 1100..1460
        wagons.add(Wagon(index = 0, name = "Caboose", x = 40f, width = 220f, roofY = 160f, floorY = 220f, hasRoof = true, hasLadder = true, hasCrate = true))
        wagons.add(Wagon(index = 1, name = "Vagón Plataforma", x = 280f, width = 240f, roofY = 220f, floorY = 220f, hasRoof = false, hasCrate = true))
        wagons.add(Wagon(index = 2, name = "Vagón Blindado", x = 540f, width = 300f, roofY = 150f, floorY = 220f, hasRoof = true, hasLadder = true, hasCrate = true))
        wagons.add(Wagon(index = 3, name = "Tender de Carbón", x = 860f, width = 220f, roofY = 180f, floorY = 220f, hasRoof = false, hasCrate = true))
        wagons.add(Wagon(index = 4, name = "Locomotora", x = 1100f, width = 360f, roofY = 140f, floorY = 220f, hasRoof = true, hasLadder = true, hasFurnace = true, hasCrate = false))
    }

    fun update(dt: Float) {
        if (isPaused || player.isDead) return

        val clampedDt = dt.coerceIn(0.001f, 0.05f)

        // 1. Train fuel and distance progression
        val fuelConsumptionRate = 0.45f
        train.fuel = (train.fuel - fuelConsumptionRate * clampedDt).coerceAtLeast(0f)
        val speedMultiplier = when {
            train.fuel <= 0f -> 0.3f
            train.fuel < 20f -> 0.65f
            else -> 1.0f
        }
        train.speed = 180f * speedMultiplier
        train.distance += train.speed * clampedDt * 0.1f // 1m per 10 units approx

        // Environment shifts based on distance
        train.environmentType = when {
            train.distance < 300f -> "Bosque Nocturno"
            train.distance < 800f -> "Gran Puente de Hierro"
            train.distance < 1400f -> "Túneles de la Mina"
            else -> "Complejo Industrial Abandonado"
        }

        // 2. Weather & Particles
        updateWeatherAndParticles(clampedDt)

        // 3. Player physics & movement
        updatePlayer(clampedDt)

        // 4. Projectiles & Slash effects
        updateProjectiles(clampedDt)

        // 5. Enemies update & spawning
        updateEnemies(clampedDt)

        // 6. Resources update
        updateResources(clampedDt)

        // 7. Floating texts
        updateFloatingTexts(clampedDt)

        // 8. Smooth Camera follow
        val targetCamX = player.x + (if (player.facingRight) 40f else -40f)
        cameraX += (targetCamX - cameraX) * 5f * clampedDt
        cameraX = cameraX.coerceIn(240f, 1340f)
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

        // Train boundaries clamping
        val minX = 45f
        val maxX = 1440f
        player.x = player.x.coerceIn(minX, maxX)

        // Wagon collision (grounding)
        var grounded = false
        for (w in wagons) {
            if (player.x >= w.x - 5f && player.x <= w.x + w.width + 5f) {
                // If wagon has roof and player not crouching through or falling from top
                val standY = if (w.hasRoof && (!player.isCrouching || player.y < w.roofY)) {
                    w.roofY
                } else {
                    w.floorY
                }

                // If player feet near standY while falling down
                if (player.vy >= 0f && player.y >= standY && player.y - player.vy * dt <= standY + 16f) {
                    player.y = standY
                    player.vy = 0f
                    grounded = true
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
        // Spawning
        spawnTimer -= dt
        val maxEnemies = if (graphicQuality == "BAJA") 4 else 7
        val spawnInterval = when {
            train.fuel < 20f -> 2.2f // swarm when train slowed
            train.distance > 800f -> 3.2f
            else -> 4.5f
        }

        if (spawnTimer <= 0f && enemies.size < maxEnemies) {
            spawnTimer = spawnInterval
            spawnEnemy()
        }

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

                when (enemy.type) {
                    EnemyType.ERRANTE -> {
                        // Slow zombie-like walker
                        val dir = if (enemy.facingRight) 1f else -1f
                        if (distToPlayer > 28f) {
                            enemy.vx = dir * enemy.type.speed
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
                        // Fast quadruped stalker / leaper
                        val dir = if (enemy.facingRight) 1f else -1f
                        if (distToPlayer > 36f) {
                            enemy.vx = dir * enemy.type.speed
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
                        // Climbs up side of wagon, then ambushes
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
                                enemy.vx = dir * enemy.type.speed
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

                // Wagon ground clamping for enemy
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

        // Spawn on random wagon or edges
        val spawnLeft = Random.nextBoolean()
        val spawnX = if (spawnLeft) {
            (player.x - Random.nextFloat() * 250f - 180f).coerceAtLeast(60f)
        } else {
            (player.x + Random.nextFloat() * 250f + 180f).coerceAtMost(1420f)
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
            damagePlayer(enemy.type.damage)
        }
    }

    fun damagePlayer(amount: Float) {
        if (player.invulnerableTimer > 0f || player.isDead) return
        player.health = (player.health - amount).coerceAtLeast(0f)
        player.invulnerableTimer = 0.8f
        audioEngine.playPlayerHurt()

        // Blood / spark particles
        spawnImpactParticles(player.x, player.y - 20f, 0xFFE53935)
        floatingTexts.add(FloatingText("-${amount.toInt()}", player.x, player.y - 35f, 0xFFFF4444))

        if (player.health <= 0f) {
            player.isDead = true
            onGameOver(train.distance.toInt(), player.scrap, totalKills)
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

        // Check hit against enemies
        val hitBoxWidth = 55f
        for (enemy in enemies) {
            if (enemy.state == EnemyState.DYING) continue
            val inFront = if (player.facingRight) enemy.x >= player.x - 10f && enemy.x <= player.x + hitBoxWidth
                          else enemy.x <= player.x + 10f && enemy.x >= player.x - hitBoxWidth
            val inY = abs(enemy.y - player.y) < 30f

            if (inFront && inY) {
                damageEnemy(enemy, 22f)
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

        // Muzzle flash particle
        spawnImpactParticles(bulletX, bulletY, 0xFFFFEE55, count = 4)
    }

    fun performInteract() {
        if (player.isDead) return

        // 1. Check Furnace interaction (Locomotive x = 1100..1460, furnace at ~1340)
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
                floatingTexts.add(FloatingText("¡Necesitas Carbón/Combustible o 15 Chatarra!", furnaceX, 120f, 0xFFE0E0E0))
                return
            }
        }

        // 2. Check Crate interaction
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

        // 3. Check Healing with Medkit
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
        // Spawns 2-3 items
        val types = listOf(ResourceType.CHATARRA, ResourceType.MUNICION, ResourceType.COMIDA, ResourceType.MEDICINA, ResourceType.COMBUSTIBLE)
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
            audioEngine.playEnemyDeath()

            // Drop scrap or ammo
            val dropType = if (Random.nextBoolean()) ResourceType.CHATARRA else ResourceType.MUNICION
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

        // Slash effects
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

            // Check pickup by player
            if (abs(player.x - r.x) < 32f && abs(player.y - r.y) < 30f) {
                audioEngine.playItemPickup()
                when (r.type) {
                    ResourceType.CHATARRA -> {
                        val amount = Random.nextInt(5, 12)
                        player.scrap += amount
                        floatingTexts.add(FloatingText("+$amount Chatarra", r.x, r.y - 20f, 0xFFFFCA28))
                    }
                    ResourceType.MUNICION -> {
                        val amount = Random.nextInt(4, 9)
                        player.ammo += amount
                        floatingTexts.add(FloatingText("+$amount Munición", r.x, r.y - 20f, 0xFF42A5F5))
                    }
                    ResourceType.COMIDA -> {
                        player.health = (player.health + 18f).coerceAtMost(100f)
                        floatingTexts.add(FloatingText("+18 Salud", r.x, r.y - 20f, 0xFF66BB6A))
                    }
                    ResourceType.MEDICINA -> {
                        player.medkits++
                        floatingTexts.add(FloatingText("+1 Botiquín", r.x, r.y - 20f, 0xFF26A69A))
                    }
                    ResourceType.COMBUSTIBLE -> {
                        player.fuelCans++
                        floatingTexts.add(FloatingText("+1 Combustible", r.x, r.y - 20f, 0xFFFF7043))
                    }
                }
                it.remove()
            }
        }
    }

    private fun updateWeatherAndParticles(dt: Float) {
        weatherTimer += dt
        if (weatherTimer > 35f) {
            weatherTimer = 0f
            isRaining = !isRaining
        }

        // Locomotive chimney smoke
        val maxParticles = when (graphicQuality) {
            "BAJA" -> 20
            "MEDIA" -> 45
            else -> 80
        }

        if (particles.size < maxParticles) {
            // Locomotive chimney at x ~ 1390, y ~ 100
            val smokeX = 1390f
            val smokeY = 90f
            particles.add(
                Particle(
                    x = smokeX + Random.nextFloat() * 8f - 4f,
                    y = smokeY,
                    vx = -train.speed * 0.85f - Random.nextFloat() * 40f,
                    vy = -Random.nextFloat() * 30f - 15f,
                    alpha = 0.8f,
                    size = Random.nextFloat() * 8f + 6f,
                    color = 0x88455A64,
                    life = 1.6f,
                    maxLife = 1.6f
                )
            )

            // Sparks on train wheels
            if (Random.nextInt(10) < 4) {
                val wheelX = listOf(140f, 400f, 700f, 960f, 1320f).random()
                particles.add(
                    Particle(
                        x = wheelX,
                        y = 232f,
                        vx = -Random.nextFloat() * 90f - 40f,
                        vy = -Random.nextFloat() * 45f - 10f,
                        alpha = 1.0f,
                        size = 3f,
                        color = 0xFFFFD54F,
                        life = 0.35f,
                        maxLife = 0.35f
                    )
                )
            }
        }

        // Update existing particles
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
