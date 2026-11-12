package com.example.aeroruins

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import java.util.Random
import java.util.UUID
import kotlin.math.abs


const val DERMIS_F = 60       
const val DERMIS_TOP = 130    
const val HOSP_F = 60         
const val HOSP_C = HOSP_F + 9 
const val MUSCLE_F = 60       
const val MUSCLE_C = MUSCLE_F + 36 

object Veil {
    val SUFFIX = arrayOf("", "_dermis", "_hypodermis", "_muscle")
    val NAMES = arrayOf("la epidermis (superficie)", "la dermis", "la hipodermis", "el musculo")

    
    fun layerOf(world: World): Int = when (val g = world.generator) {
        is VeilGenerator -> g.layer
        is AeroGenerator -> 0
        else -> -1
    }

    fun baseName(world: World): String {
        val n = world.name
        for (i in 1..3) if (n.endsWith(SUFFIX[i])) return n.removeSuffix(SUFFIX[i])
        return n
    }

    
    fun floorOf(layer: Int, seed: Long, s: Site): Int = when (layer) {
        0 -> Terrain.heightAt(seed, s.cx, s.cz) - s.dmax
        1 -> DERMIS_F
        2 -> HOSP_F
        else -> MUSCLE_F
    }
}


class VeilGenerator(val layer: Int) : ChunkGenerator() {
    private val aero = AeroGenerator(true) 

    override fun generateNoise(info: WorldInfo, random: Random, cx: Int, cz: Int, data: ChunkData) {
        if (layer == 1 && Settings.caves) aero.beginCaves(info, cx, cz)
        for (x in 0..15) for (z in 0..15) {
            val wx = cx * 16 + x
            val wz = cz * 16 + z
            data.setBlock(x, info.minHeight, z, Material.BEDROCK)
            when (layer) {
                1 -> dermisColumn(info, data, x, z, wx, wz)
                2 -> Layers.hospitalColumn(info, data, x, z, wx, wz)
                else -> Layers.muscleColumn(info, data, x, z, wx, wz)
            }
        }
        if (layer == 1 && Settings.caves) sweepOrphans(data, info.minHeight + 5, 120)
    }

    private fun dermisColumn(info: WorldInfo, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int) {
        val seed = info.seed
        rockColumn(info, data, x, z, wx, wz, DERMIS_TOP)
        data.setBlock(x, DERMIS_TOP, z, Material.BEDROCK)
        if (Settings.caves) aero.applyCaves(info, data, x, z, wx, wz, DERMIS_TOP, false)

        
        val up = Layers.siteNear(seed, 0, wx, wz, 10)
        val down = Layers.siteNear(seed, 1, wx, wz, 10)
        if (up != null) chamber(data, x, z, wx - up.cx, wz - up.cz, true)
        if (down != null) chamber(data, x, z, wx - down.cx, wz - down.cz, false)
        if (abs(wx) <= 10 && abs(wz) <= 10) chamber(data, x, z, wx, wz, null)
    }

    private fun chamber(data: ChunkData, x: Int, z: Int, dx: Int, dz: Int, up: Boolean?) {
        if (dx * dx + dz * dz > 100) return
        for (y in DERMIS_F - 4 until DERMIS_F) data.setBlock(x, y, z, Material.STONE)
        for (y in DERMIS_F + 1..DERMIS_F + 12) data.setBlock(x, y, z, Material.AIR)
        data.setBlock(x, DERMIS_F, z, if ((dx + dz) % 4 == 0) Material.MOSSY_STONE_BRICKS else Material.STONE_BRICKS)
        if (up != null) Layers.pad(data, x, z, dx, dz, DERMIS_F, up)
    }

    override fun getDefaultPopulators(world: World): List<BlockPopulator> = listOf(VeilPopulator(layer))

    override fun getFixedSpawnLocation(world: World, random: Random): Location = when (layer) {
        1 -> Location(world, 0.5, DERMIS_F + 1.0, 0.5)
        2 -> Location(world, 3.5, HOSP_F + 1.0, 3.5)
        else -> Location(world, 3.5, MUSCLE_F + 1.0, 5.5)
    }
}

class VeilPopulator(private val layer: Int) : BlockPopulator() {
    override fun populate(info: WorldInfo, random: Random, cx: Int, cz: Int, region: LimitedRegion) {
        val p = Place(region)
        val seed = info.seed
        val bx = cx * 16
        val bz = cz * 16
        when (layer) {
            1 -> Ores.populate(random, p, bx, bz, info.minHeight, Climate.OLD_OAK_FOREST) 
            2 -> Layers.furnish(seed, random, p, bx, bz) 
            else -> Unit
        }
    }
}


class VeilListener(private val plugin: AeroPlugin) : Listener {
    private val cooldown = HashMap<UUID, Long>()

    @EventHandler(ignoreCancelled = true)
    fun onMove(e: PlayerMoveEvent) {
        if (!Settings.veil) return
        val to = e.to
        val from = e.from
        if (to.blockX == from.blockX && to.blockY == from.blockY && to.blockZ == from.blockZ) return
        val player = e.player
        val world = player.world
        val layer = Veil.layerOf(world)
        if (layer < 0) return
        if ((cooldown[player.uniqueId] ?: 0L) > System.currentTimeMillis()) return
        val seed = world.seed
        if (layer <= 2) { 
            val s = Layers.siteNear(seed, layer, to.blockX, to.blockZ, 1)
            if (s != null && onPad(layer, seed, s, to.y)) {
                travel(player, layer + 1, s)
                return
            }
        }
        if (layer >= 1) { 
            val s = Layers.siteNear(seed, layer - 1, to.blockX, to.blockZ, 1)
            if (s != null && onPad(layer, seed, s, to.y)) travel(player, layer - 1, s)
        }
    }

    private fun onPad(layer: Int, seed: Long, s: Site, y: Double): Boolean =
        abs(y - (Veil.floorOf(layer, seed, s) + 1)) <= 1.6

    private fun travel(player: Player, target: Int, s: Site) {
        if (!player.hasPermission("aeroruins.veil.portal")) {
            cooldown[player.uniqueId] = System.currentTimeMillis() + 5000L
            player.sendActionBar(Component.text("El portal no responde: no tienes permiso (aeroruins.veil.portal)."))
            return
        }
        val seed = player.world.seed
        val dest = plugin.worldFor(Veil.baseName(player.world), target, seed) ?: return
        val y = Veil.floorOf(target, seed, s) + 1.0
        cooldown[player.uniqueId] = System.currentTimeMillis() + 5000L
        player.teleport(Location(dest, s.cx + 4.5, y, s.cz + 0.5)) 
        player.sendActionBar(Component.text("The Veil: ${Veil.NAMES[target]}"))
    }
}
