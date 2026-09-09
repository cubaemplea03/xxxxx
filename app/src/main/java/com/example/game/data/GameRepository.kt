package com.example.game.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class GameRepository(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        GameDatabase::class.java,
        "ultimo_tren.db"
    ).fallbackToDestructiveMigration().build()

    private val dao = db.gameDao()

    val statsFlow: Flow<GameStatsEntity?> = dao.getStats()
    val settingsFlow: Flow<GameSettingsEntity?> = dao.getSettings()

    suspend fun getInitialSettings(): GameSettingsEntity {
        return dao.getSettings().firstOrNull() ?: GameSettingsEntity()
    }

    suspend fun getInitialStats(): GameStatsEntity {
        return dao.getStats().firstOrNull() ?: GameStatsEntity()
    }

    suspend fun saveSettings(settings: GameSettingsEntity) {
        dao.saveSettings(settings)
    }

    suspend fun saveGameRun(distance: Int, scrapGained: Int, goldGained: Int, kills: Int, levelReached: Int = 1) {
        val current = dao.getStats().firstOrNull() ?: GameStatsEntity()
        val updated = current.copy(
            highScoreDistance = maxOf(current.highScoreDistance, distance),
            totalScrap = current.totalScrap + scrapGained,
            totalKills = current.totalKills + kills,
            gamesPlayed = current.gamesPlayed + 1,
            lastDistance = distance,
            highestLevel = maxOf(current.highestLevel, levelReached)
        )
        dao.saveStats(updated)
    }

    suspend fun addGold(amount: Int) {
        if (amount <= 0) return
        val current = dao.getStats().firstOrNull() ?: GameStatsEntity()
        dao.saveStats(current.copy(totalGold = current.totalGold + amount))
    }

    suspend fun savePlayerLevel(level: Int) {
        val current = dao.getStats().firstOrNull() ?: GameStatsEntity()
        if (level > current.highestLevel) {
            dao.saveStats(current.copy(highestLevel = level))
        }
    }

    suspend fun markMonsterSeen(typeName: String) {
        val current = dao.getStats().firstOrNull() ?: GameStatsEntity()
        val currentSet = current.discoveredMonsters.split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .toMutableSet()
        
        val normalized = typeName.trim().uppercase()
        if (!currentSet.contains(normalized)) {
            currentSet.add(normalized)
            val updatedString = currentSet.joinToString(",")
            dao.saveStats(current.copy(discoveredMonsters = updatedString))
        }
    }

    suspend fun purchaseTrainUpgrade(upgradeType: String, cost: Int): Boolean {
        val current = dao.getStats().firstOrNull() ?: GameStatsEntity()
        if (current.totalGold < cost) return false

        val updated = when (upgradeType) {
            "TURRET" -> current.copy(
                totalGold = current.totalGold - cost,
                turretLevel = (current.turretLevel + 1).coerceAtMost(3)
            )
            "CANNON" -> current.copy(
                totalGold = current.totalGold - cost,
                cannonLevel = (current.cannonLevel + 1).coerceAtMost(3)
            )
            "ARMOR" -> current.copy(
                totalGold = current.totalGold - cost,
                armorLevel = (current.armorLevel + 1).coerceAtMost(3)
            )
            "SPOTLIGHT" -> current.copy(
                totalGold = current.totalGold - cost,
                spotlightLevel = (current.spotlightLevel + 1).coerceAtMost(3)
            )
            else -> current
        }
        dao.saveStats(updated)
        return true
    }

    suspend fun purchaseSkin(skinId: String, cost: Int, slotName: String): Boolean {
        val current = dao.getStats().firstOrNull() ?: GameStatsEntity()
        val normalizedId = skinId.trim().uppercase()
        val owned = current.ownedSkins.split(",")
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .toMutableSet()

        if (owned.contains(normalizedId)) {
            equipSkin(normalizedId, slotName)
            return true
        }

        if (current.totalGold < cost) return false

        owned.add(normalizedId)
        val newGold = current.totalGold - cost
        val newOwnedString = owned.joinToString(",")

        val updated = when (slotName.trim().uppercase()) {
            "CABEZA" -> current.copy(totalGold = newGold, ownedSkins = newOwnedString, equippedHead = normalizedId)
            "PECHO" -> current.copy(totalGold = newGold, ownedSkins = newOwnedString, equippedChest = normalizedId)
            "PIERNAS" -> current.copy(totalGold = newGold, ownedSkins = newOwnedString, equippedLegs = normalizedId)
            else -> current.copy(totalGold = newGold, ownedSkins = newOwnedString)
        }
        dao.saveStats(updated)
        return true
    }

    suspend fun equipSkin(skinId: String, slotName: String) {
        val current = dao.getStats().firstOrNull() ?: GameStatsEntity()
        val normalizedId = skinId.trim().uppercase()
        val updated = when (slotName.trim().uppercase()) {
            "CABEZA" -> current.copy(equippedHead = normalizedId)
            "PECHO" -> current.copy(equippedChest = normalizedId)
            "PIERNAS" -> current.copy(equippedLegs = normalizedId)
            else -> current
        }
        dao.saveStats(updated)
    }
}

