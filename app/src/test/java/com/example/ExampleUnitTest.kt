package com.example

import com.example.game.data.GameStatsEntity
import com.example.game.model.BESTIARY_ENTRIES
import com.example.game.model.EnemyType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun bestiary_containsAllMonsterTypes() {
    val types = BESTIARY_ENTRIES.map { it.type }.toSet()
    assertTrue(types.contains(EnemyType.ERRANTE))
    assertTrue(types.contains(EnemyType.CORREDOR))
    assertTrue(types.contains(EnemyType.TREPADOR))
    assertTrue(types.contains(EnemyType.BRUTO))
    assertTrue(types.contains(EnemyType.JEFE))
  }

  @Test
  fun statsEntity_hasSeenMonsterLogic() {
    val statsNew = GameStatsEntity(
      discoveredMonsters = "",
      highestLevel = 1,
      totalKills = 0
    )
    assertTrue("ERRANTE should always be seen from level 1", statsNew.hasSeenMonster(EnemyType.ERRANTE.name))
    assertFalse("JEFE should not be seen initially", statsNew.hasSeenMonster(EnemyType.JEFE.name))

    val statsWithBoss = statsNew.copy(discoveredMonsters = "ERRANTE,JEFE")
    assertTrue("JEFE should be seen once registered in discoveredMonsters", statsWithBoss.hasSeenMonster(EnemyType.JEFE.name))
  }

  @Test
  fun wardrobe_containsAllThreeSlots() {
    val slots = com.example.game.model.WARDROBE_SKINS.map { it.slot }.toSet()
    assertTrue(slots.contains(com.example.game.model.WardrobeSlot.CABEZA))
    assertTrue(slots.contains(com.example.game.model.WardrobeSlot.PECHO))
    assertTrue(slots.contains(com.example.game.model.WardrobeSlot.PIERNAS))
  }

  @Test
  fun wardrobe_defaultSkinsAreFree() {
    val defaultSkins = com.example.game.model.WARDROBE_SKINS.filter { it.id.endsWith("_DEFAULT") }
    assertEquals(3, defaultSkins.size)
    defaultSkins.forEach { skin ->
      assertEquals(0, skin.costGold)
    }
  }

  @Test
  fun statsEntity_skinOwnershipAndEquip() {
    val stats = GameStatsEntity(
      equippedHead = "HEAD_DEFAULT",
      equippedChest = "CHEST_DEFAULT",
      equippedLegs = "LEGS_DEFAULT",
      ownedSkins = "HEAD_DEFAULT,CHEST_DEFAULT,LEGS_DEFAULT,HEAD_CYBER"
    )

    assertTrue(stats.isSkinOwned("HEAD_DEFAULT"))
    assertTrue(stats.isSkinOwned("HEAD_CYBER"))
    assertFalse(stats.isSkinOwned("HEAD_TITAN"))

    assertTrue(stats.isSkinEquipped("HEAD_DEFAULT"))
    assertFalse(stats.isSkinEquipped("HEAD_CYBER"))

    val updatedStats = stats.copy(equippedHead = "HEAD_CYBER")
    assertTrue(updatedStats.isSkinEquipped("HEAD_CYBER"))
    assertFalse(updatedStats.isSkinEquipped("HEAD_DEFAULT"))
  }
}

