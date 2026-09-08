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
    val spotlightLevel: Int = 0   // 0 = none, 1..3
)

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
    version = 2,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
