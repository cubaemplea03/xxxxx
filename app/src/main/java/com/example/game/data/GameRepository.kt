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

    suspend fun saveGameRun(distance: Int, scrapGained: Int, kills: Int) {
        val current = dao.getStats().firstOrNull() ?: GameStatsEntity()
        val updated = current.copy(
            highScoreDistance = maxOf(current.highScoreDistance, distance),
            totalScrap = current.totalScrap + scrapGained,
            totalKills = current.totalKills + kills,
            gamesPlayed = current.gamesPlayed + 1,
            lastDistance = distance
        )
        dao.saveStats(updated)
    }
}
