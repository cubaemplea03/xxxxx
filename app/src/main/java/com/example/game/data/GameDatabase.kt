package com.example.game.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_stats")
data class GameStatsEntity(
    @PrimaryKey val id: Int = 1,
    val highScoreDistance: Int = 0,
    val totalScrap: Int = 0,
    val totalGold: Int = 0,
    val totalKills: Int = 0,
    val gamesPlayed: Int = 0,
    val lastDistance: Int = 0,
    val highestLevel: Int = 1,
    // Train upgrades
    val turretLevel: Int = 0,     // 0 = none, 1..3
    val cannonLevel: Int = 0,     // 0 = none, 1..3
    val armorLevel: Int = 0,      // 0 = none, 1..3
    val spotlightLevel: Int = 0,   // 0 = none, 1..3
    val discoveredMonsters: String = "ERRANTE", // Comma-separated list of discovered EnemyType names
    // Player wardrobe & cosmetics
    val equippedHead: String = "HEAD_DEFAULT",
    val equippedChest: String = "CHEST_DEFAULT",
    val equippedLegs: String = "LEGS_DEFAULT",
    val ownedSkins: String = "HEAD_DEFAULT,CHEST_DEFAULT,LEGS_DEFAULT"
) {
    fun isSkinOwned(skinId: String): Boolean {
        if (skinId.endsWith("_DEFAULT")) return true
        val list = ownedSkins.split(",").map { it.trim().uppercase() }
        return list.contains(skinId.trim().uppercase())
    }

    fun isSkinEquipped(skinId: String): Boolean {
        val norm = skinId.trim().uppercase()
        return equippedHead.uppercase() == norm ||
               equippedChest.uppercase() == norm ||
               equippedLegs.uppercase() == norm
    }

    fun hasSeenMonster(typeName: String): Boolean {
        val list = discoveredMonsters.split(",").map { it.trim().uppercase() }
        if (list.contains(typeName.uppercase())) return true

        // Fallback progress check based on highest survival level or kills reached
        return when (typeName.uppercase()) {
            "ERRANTE" -> true
            "CORREDOR" -> highestLevel >= 1 && totalKills >= 3
            "TREPADOR" -> highestLevel >= 2 || totalKills >= 12
            "BRUTO" -> highestLevel >= 3 || totalKills >= 25
            "JEFE" -> highestLevel >= 10 || totalKills >= 90
            else -> false
        }
    }
}

@Entity(tableName = "game_settings")
data class GameSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val musicVolume: Float = 0.8f,
    val sfxVolume: Float = 0.9f,
    val graphicQuality: String = "ALTA", // "BAJA", "MEDIA", "ALTA"
    val vibrationEnabled: Boolean = true,
    val controlSize: String = "NORMAL", // "NORMAL", "GRANDE"
    val controlOpacity: Float = 0.7f
)

@Dao
interface GameDao {
    @Query("SELECT * FROM game_stats WHERE id = 1")
    fun getStats(): Flow<GameStatsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStats(stats: GameStatsEntity)

    @Query("SELECT * FROM game_settings WHERE id = 1")
    fun getSettings(): Flow<GameSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: GameSettingsEntity)
}

@Database(
    entities = [GameStatsEntity::class, GameSettingsEntity::class],
    version = 4,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
