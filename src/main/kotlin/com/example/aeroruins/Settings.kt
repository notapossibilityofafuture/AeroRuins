package com.example.aeroruins

import org.bukkit.configuration.file.FileConfiguration

object Settings {
    @Volatile var caves = true
    @Volatile var chests = true
    @Volatile var mobs = true
    @Volatile var vanillaStructures = false
    @Volatile var classics = false
    @Volatile var faunaDensity = 1.0
    @Volatile var chestScale = 1.0
    @Volatile var liminalChance = 12
    @Volatile var worldSize = 20000
    @Volatile var veil = true
    @Volatile var suburbCeilings = false
    @Volatile var domeSpacing = 7000
    @Volatile var roadSpacing = 3200
    @Volatile var townChance = 1
    @Volatile var villageChance = 1
    @Volatile var mobSpawnRate = 0.25
    @Volatile var structureMobRate = 0.4
    @Volatile var vanillaMixChance = 40
    @Volatile var datapackAutoDownload = false
    @Volatile var datapackUrl = ""
    @Volatile var datapackFileName = "aeroruins-datapack.zip"
    @Volatile var datapackTimeoutSeconds = 30
    @Volatile var datapackMaxSizeMb = 64

    fun load(c: FileConfiguration) {
        caves = c.getBoolean("caves", true)
        chests = c.getBoolean("chests", true)
        mobs = c.getBoolean("mobs", true)
        vanillaStructures = c.getBoolean("vanilla-structures", false)
        classics = c.getBoolean("classic-structures", false)
        faunaDensity = c.getDouble("fauna-density", 1.0).coerceIn(0.0, 5.0)
        liminalChance = c.getInt("liminal-chance", 12).coerceIn(0, 100)
        worldSize = c.getInt("world-size", 20000)
        veil = c.getBoolean("veil-dimensions", true)
        suburbCeilings = c.getBoolean("suburb-ceilings", false)
        domeSpacing = c.getInt("dome-spacing", 7000).coerceAtLeast(2 * CELL)
        villageChance = c.getInt("village-chance", 1).coerceIn(0, 100)
        townChance = c.getInt("town-chance", 1).coerceIn(0, 100)
        roadSpacing = c.getInt("road-spacing", 3200).coerceAtLeast(256)
        vanillaMixChance = c.getInt("vanilla-mix-chance", 40).coerceIn(0, 100)
        mobSpawnRate = c.getDouble("mob-spawn-rate", 0.25).coerceIn(0.0, 1.0)
        structureMobRate = c.getDouble("structure-mob-rate", 0.4).coerceIn(0.0, 1.0)
        chestScale = c.getDouble("chest-rarity", 1.0).coerceIn(0.0, 20.0)
        datapackAutoDownload = c.getBoolean("datapack-auto-download", false)
        datapackUrl = c.getString("datapack-url", "") ?: ""
        datapackFileName = c.getString("datapack-file-name", "aeroruins-datapack.zip")
            ?.takeIf { it.matches(Regex("[A-Za-z0-9._-]+\\.zip")) }
            ?: "aeroruins-datapack.zip"
        datapackTimeoutSeconds = c.getInt("datapack-timeout-seconds", 30).coerceIn(5, 300)
        datapackMaxSizeMb = c.getInt("datapack-max-size-mb", 64).coerceIn(1, 512)
    }
}
