package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.generator.ChunkGenerator.ChunkData
import org.bukkit.generator.WorldInfo
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private fun fmod(a: Int, b: Int): Int = Math.floorMod(a, b)
private fun fdiv(a: Int, b: Int): Int = Math.floorDiv(a, b)

private val MALL_FLOWERS = listOf(Material.CORNFLOWER, Material.AZURE_BLUET, Material.BLUE_ORCHID)




internal fun rockColumn(info: WorldInfo, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int, top: Int) {
    val lo = info.minHeight + 1
    if (top <= lo) return
    val deepTop = minOf(0, top)
    if (deepTop > lo) data.setRegion(x, lo, z, x + 1, deepTop, z + 1, Material.DEEPSLATE)
    for (y in maxOf(0, lo) until minOf(8, top)) {
        data.setBlock(x, y, z, if (mix(info.seed, wx, wz * 31 + y, 51) % 8 >= y) Material.DEEPSLATE else Material.STONE)
    }
    if (top > 8) data.setRegion(x, maxOf(8, lo), z, x + 1, top, z + 1, Material.STONE)
}


internal fun AeroGenerator.baseGround(info: WorldInfo, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int): Int {
    val h = heightAt(info.seed, wx, wz)
    rockColumn(info, data, x, z, wx, wz, h - 3)
    val beach = h <= SEA_LEVEL + 1
    val rocky = h >= HIGH_Y + 12
    data.setRegion(x, h - 3, z, x + 1, h, z + 1, if (beach) Material.SAND else if (rocky) Material.STONE else Material.DIRT)
    val roadTop = if (h > SEA_LEVEL) Roads.surface(info.seed, wx, wz) else null
    data.setBlock(
        x, h, z,
        when {
            roadTop != null -> roadTop
            beach -> Material.SAND
            h >= HIGH_Y + 42 -> Material.SNOW_BLOCK
            rocky -> if (mix(info.seed, wx, wz, 52) % 5 == 0) Material.ANDESITE else Material.STONE
            else -> Material.GRASS_BLOCK
        }
    )
    if (h < SEA_LEVEL) data.setRegion(x, h + 1, z, x + 1, SEA_LEVEL + 1, z + 1, Material.WATER)
    return h
}

internal fun AeroGenerator.terrainColumn(info: WorldInfo, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int) {
    val h = baseGround(info, data, x, z, wx, wz)
    if (Settings.caves) {
        applyCaves(info, data, x, z, wx, wz, h, true)
        applyGeodes(info, data, x, z, wx, wz)
    }
    Portals.carve(data, x, z, wx, wz, h, false) 
    Layers.tear(info.seed, data, x, z, wx, wz, h) 
}



internal fun AeroGenerator.domeColumn(
    info: WorldInfo, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int, dome: DomeInfo
) {
    val seed = info.seed
    val d = dome.d
    val r = dome.r
    val a = dome.a

    rockColumn(info, data, x, z, wx, wz, FLOOR - 3)
    data.setRegion(x, FLOOR - 3, z, x + 1, FLOOR, z + 1, Material.DIRT)

    if (Settings.caves) {
        applyCaves(info, data, x, z, wx, wz, FLOOR, false)
        applyGeodes(info, data, x, z, wx, wz)
    }

    
    var pond = false
    val top: Material = when {
        d < 5.0 -> Material.SMOOTH_QUARTZ
        d < 6.5 -> Material.SEA_LANTERN
        d < 10.0 -> Material.WHITE_CONCRETE
        d < 11.5 -> Material.LIGHT_BLUE_CONCRETE
        onDomePath(dome) -> if (((wx + wz) and 1) == 0) Material.SMOOTH_QUARTZ else Material.QUARTZ_BLOCK
        else -> {
            if (d > 16 && d < r - 6 && pondNoise(seed, wx, wz) > 0.45) pond = true
            Material.GRASS_BLOCK
        }
    }
    if (pond) {
        data.setBlock(x, FLOOR - 2, z, Material.SAND)
        data.setRegion(x, FLOOR - 1, z, x + 1, FLOOR + 1, z + 1, Material.WATER)
    } else {
        data.setBlock(x, FLOOR, z, top)
    }

    
    val ytop = floor(sqrt(r.toDouble() * r - d * d)).toInt()
    val inner = (r - 2).toDouble()
    val ybot = if (d < inner) floor(sqrt(inner * inner - d * d)).toInt() else 0
    val k = (a / (PI / 6)).roundToInt()
    val rib = d * abs(sin(a - k * PI / 6)) < 1.0
    for (y in FLOOR + ybot + 1..FLOOR + ytop) {
        if (!rib && mix(seed, wx + y * 31, wz, 7) % 100 < 2) continue
        data.setBlock(x, y, z, if (rib) Material.BLACK_CONCRETE else Material.LIGHT_BLUE_STAINED_GLASS)
    }

    
    if (d <= 3.6) {
        val tubeTop = FLOOR + r - 3
        for (y in FLOOR + 1..tubeTop) {
            val rel = y - FLOOR
            if (d >= 2.6) {
                val door = rel <= 3 && dome.dx > 0 && abs(dome.dz) <= 1
                if (!door) data.setBlock(x, y, z, if (rel % 6 == 0) Material.SEA_LANTERN else Material.GLASS)
            } else if (d >= 0.6 && spiralHit(a, rel)) {
                data.setBlock(x, y, z, Material.QUARTZ_BLOCK)
            }
        }
    }
}




internal fun AeroGenerator.mallColumn(info: WorldInfo, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int) {
    val seed = info.seed
    val h = baseGround(info, data, x, z, wx, wz)
    if (Settings.caves) applyCaves(info, data, x, z, wx, wz, h, false, MF - 5) 

    val edge = zoneAt(seed, wx - 1, wz) != Zone.MALL || zoneAt(seed, wx + 1, wz) != Zone.MALL ||
        zoneAt(seed, wx, wz - 1) != Zone.MALL || zoneAt(seed, wx, wz + 1) != Zone.MALL
    if (edge) {
        Portals.carve(data, x, z, wx, wz, h, true) 
        return 
    }

    val variant = mallVariant(seed, wx, wz)
    data.setRegion(x, MF + 1, z, x + 1, MC, z + 1, Material.AIR)

    val px = fmod(wx, 16)
    val pz = fmod(wz, 16)
    val pillar = px in 0..1 && pz in 0..1
    val bz = fmod(wz, 14)
    val bx = fmod(wx, 24)
    val planter = bz in 4..8 && bx < 18
    if (pillar) {
        data.setRegion(x, MF, z, x + 1, MC, z + 1, Material.LIGHT_GRAY_CONCRETE)
        if (px == 0 && pz == 0) data.setBlock(x, MC - 2, z, Material.LIME_CONCRETE) 
    } else if (planter) {
        val kerb = bz == 4 || bz == 8 || bx == 0 || bx == 17
        if (kerb) {
            data.setBlock(x, MF, z, Material.STONE_BRICKS)
            data.setBlock(x, MF + 1, z, Material.STONE_BRICK_SLAB)
        } else if (variant == 1) { 
            data.setBlock(x, MF, z, Material.WATER)
        } else {
            data.setBlock(x, MF, z, Material.GRASS_BLOCK)
            val roll = mix(seed, wx, wz, 6) % 100
            when {
                roll < 38 -> {
                    val hgt = 1 + mix(seed, wx, wz, 7) % 3
                    val leaves = if (roll % 2 == 0) Material.JUNGLE_LEAVES else Material.AZALEA_LEAVES
                    for (i in 1..hgt) data.setBlock(x, MF + i, z, leafData(leaves))
                }
                roll < 70 -> data.setBlock(x, MF + 1, z, Material.FERN)
                roll < 85 -> data.setBlock(x, MF + 1, z, Material.SHORT_GRASS)
                roll < 90 -> data.setBlock(x, MF + 1, z, MALL_FLOWERS[roll % MALL_FLOWERS.size])
            }
        }
    } else {
        val checker = ((fdiv(wx, 2) + wz) and 1) == 0
        val floor = when (variant) {
            2 -> when (mix(seed, wx, wz, 12) % 6) {
                0 -> Material.CRACKED_STONE_BRICKS
                1 -> Material.COBBLESTONE
                2 -> Material.MOSSY_COBBLESTONE
                else -> if (checker) Material.STONE_BRICKS else Material.POLISHED_ANDESITE
            }
            3 -> if (checker) Material.QUARTZ_BLOCK else Material.LIGHT_BLUE_TERRACOTTA
            else -> if (checker) Material.STONE_BRICKS else Material.POLISHED_ANDESITE
        }
        data.setBlock(x, MF, z, floor)
    }

    
    if (variant == 3 && fmod(wx, 8) in 2..5 && fmod(wz, 8) in 2..5) {
        
        data.setBlock(x, MC, z, Material.LIGHT_BLUE_STAINED_GLASS)
        data.setBlock(x, MC + 1, z, Material.SEA_LANTERN)
    } else {
        val lamp = fmod(wx, 6) in 2..3 && fmod(wz, 6) in 2..3
        val dead = variant == 2 && mix(seed, fdiv(wx, 6), fdiv(wz, 6), 13) % 100 >= 12
        val beam = fmod(wx, 8) == 0
        data.setBlock(
            x, MC, z,
            if (lamp && !dead) Material.SEA_LANTERN else if (beam) Material.BLACK_CONCRETE else Material.GRAY_CONCRETE
        )
        data.setBlock(x, MC + 1, z, Material.GRAY_CONCRETE)
    }
}




internal fun AeroGenerator.quarryColumn(info: WorldInfo, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int) {
    val seed = info.seed
    val h = baseGround(info, data, x, z, wx, wz) 
    if (Settings.caves) applyCaves(info, data, x, z, wx, wz, h, false, QF - 5) 
    val variant = quarryVariant(seed, wx, wz)

    val edge = zoneAt(seed, wx - 1, wz) != Zone.QUARRY || zoneAt(seed, wx + 1, wz) != Zone.QUARRY ||
        zoneAt(seed, wx, wz - 1) != Zone.QUARRY || zoneAt(seed, wx, wz + 1) != Zone.QUARRY
    if (edge) {
        Portals.carve(data, x, z, wx, wz, h, true) 
        return 
    }

    
    data.setRegion(x, QF + 1, z, x + 1, QC, z + 1, Material.AIR)

    val pillar = fmod(wx, 16) in 6..9 && fmod(wz, 16) in 6..9
    if (pillar) {
        val m = if (mix(seed, wx, wz, 11) % 4 == 0) Material.STONE else Material.LIGHT_GRAY_CONCRETE
        data.setRegion(x, QF, z, x + 1, QC, z + 1, m)
        return
    }

    val pondLimit = when (variant) {
        1 -> -0.05 
        2 -> 0.45
        else -> 0.30
    }
    if (pondNoise(seed, wx, wz) > pondLimit) {
        data.setBlock(x, QF - 1, z, Material.CLAY)
        data.setBlock(x, QF, z, Material.WATER)
    } else {
        data.setBlock(x, QF, z, Material.GRASS_BLOCK)
        val roll = mix(seed, wx, wz, 6) % 100
        val daisy = if (variant == 2) 55 else 38
        when {
            roll < daisy -> data.setBlock(x, QF + 1, z, Material.OXEYE_DAISY)
            roll < daisy + 24 -> data.setBlock(x, QF + 1, z, Material.SHORT_GRASS)
            roll < daisy + 28 -> data.setBlock(x, QF + 1, z, Material.AZURE_BLUET)
            variant == 2 && roll < daisy + 40 -> data.setBlock(x, QF + 1, z, Material.CORNFLOWER)
        }
    }

    
    if (fmod(wx, 8) in 3..4 && fmod(wz, 8) in 3..4) data.setBlock(x, QC, z, Material.GLOWSTONE)

    
    if (mix(seed, wx, wz, 9) % 40 == 0) {
        val len = 2 + mix(seed, wx, wz, 10) % 3
        for (i in 1..len) {
            val y = QC - i
            if (i == len) data.setBlock(x, y, z, Material.CAVE_VINES.createBlockData("[berries=true]"))
            else data.setBlock(x, y, z, Material.CAVE_VINES_PLANT.createBlockData("[berries=true]"))
        }
    }
}
