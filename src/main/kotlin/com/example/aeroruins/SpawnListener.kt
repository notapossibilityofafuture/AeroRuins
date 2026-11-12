package com.example.aeroruins

import org.bukkit.entity.Enemy
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import java.util.concurrent.ThreadLocalRandom


class SpawnListener : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onSpawn(e: CreatureSpawnEvent) {
        if (e.entity !is Enemy) return
        if (e.spawnReason != SpawnReason.NATURAL && e.spawnReason != SpawnReason.CHUNK_GEN && e.spawnReason != SpawnReason.PATROL) return
        if (e.location.world?.generator !is AeroGenerator) return
        if (ThreadLocalRandom.current().nextDouble() >= Settings.mobSpawnRate) e.isCancelled = true
    }
}
