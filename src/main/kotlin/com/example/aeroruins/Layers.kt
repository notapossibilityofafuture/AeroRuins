package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.generator.ChunkGenerator.ChunkData
import org.bukkit.generator.WorldInfo
import java.util.Random
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private fun fmod(a: Int, b: Int): Int = Math.floorMod(a, b)
private fun fdiv(a: Int, b: Int): Int = Math.floorDiv(a, b)


class Tear(val cx: Int, val cz: Int, val q: Double, val depth: Double, val dmax: Double, val dist: Double)


class Site(val cx: Int, val cz: Int, val dmax: Int)






object Layers {
    fun hypoVariant(seed: Long, x: Int, z: Int): Int = mix(seed, fdiv(x, 192), fdiv(z, 192), 160) % 3
    fun muscleVariant(seed: Long, x: Int, z: Int): Int = mix(seed, fdiv(x, 192), fdiv(z, 192), 161) % 3

    

    private fun cellSize(layer: Int): Int = if (layer == 0) 1200 else 800

    
    fun siteIn(seed: Long, layer: Int, gx: Int, gz: Int): Site? {
        val cell = cellSize(layer)
        val h = mix(seed, gx, gz, 175 + layer * 11)
        if (h % 100 >= 62) return null
        return Site(gx * cell + 80 + (h shr 6) % (cell - 160), gz * cell + 80 + (h shr 16) % (cell - 160), 24 + (h shr 12) % 10)
    }

    
    fun siteNear(seed: Long, layer: Int, wx: Int, wz: Int, r: Int): Site? {
        val cell = cellSize(layer)
        val s = siteIn(seed, layer, fdiv(wx, cell), fdiv(wz, cell)) ?: return null
        return if (abs(wx - s.cx) <= r && abs(wz - s.cz) <= r) s else null
    }

    
    fun pad(data: ChunkData, x: Int, z: Int, dx: Int, dz: Int, y: Int, up: Boolean): Boolean {
        val ch = maxOf(abs(dx), abs(dz))
        if (ch > 4) return false
        val even = Math.floorMod(dx + dz, 2) == 0
        val m = when {
            ch <= 1 -> Material.CRYING_OBSIDIAN
            ch <= 3 -> if (up) (if (even) Material.QUARTZ_BLOCK else Material.SMOOTH_QUARTZ) else (if (even) Material.RED_NETHER_BRICKS else Material.NETHER_WART_BLOCK)
            else -> Material.SMOOTH_STONE
        }
        data.setBlock(x, y, z, m)
        if (abs(dx) == 4 && abs(dz) == 4) data.setBlock(x, y + 1, z, if (up) Material.SEA_LANTERN else Material.SHROOMLIGHT)
        return true
    }

    

    private fun fleshWall(h: Int): Material = when {
        h < 38 -> Material.NETHER_WART_BLOCK
        h < 56 -> Material.RED_NETHER_BRICKS
        h < 66 -> Material.PINK_TERRACOTTA
        h < 74 -> Material.BROWN_TERRACOTTA
        h < 82 -> Material.BONE_BLOCK
        h < 88 -> Material.MUSHROOM_STEM
        h < 94 -> Material.RED_MUSHROOM_BLOCK
        else -> Material.SHROOMLIGHT
    }

    
    fun muscleColumn(info: WorldInfo, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int) {
        val seed = info.seed
        rockColumn(info, data, x, z, wx, wz, MUSCLE_F)
        muscle(seed, data, x, z, wx, wz)
        data.setRegion(x, MUSCLE_C + 1, z, x + 1, MUSCLE_C + 6, z + 1, Material.STONE)
        data.setBlock(x, MUSCLE_C + 6, z, Material.BEDROCK)
        pads(seed, 3, data, x, z, wx, wz, MUSCLE_F)
    }

    private fun muscle(seed: Long, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int) {
        val f = MUSCLE_F
        val c = MUSCLE_C
        data.setRegion(x, f + 1, z, x + 1, c, z + 1, Material.AIR)

        
        val dist = mix(seed, fdiv(wx, 192), fdiv(wz, 192), 200) % 5
        val fh = mix(seed, wx, wz, 150) % 100
        val floor = if (dist == 2) {
            when { fh < 20 -> Material.RED_TERRACOTTA; fh < 35 -> Material.PINK_TERRACOTTA; fh < 45 -> Material.NETHER_WART_BLOCK; fh < 85 -> Material.COARSE_DIRT; else -> Material.MUD }
        } else {
            when { fh < 40 -> Material.RED_TERRACOTTA; fh < 60 -> Material.PINK_TERRACOTTA; fh < 74 -> Material.NETHER_WART_BLOCK; fh < 90 -> Material.COARSE_DIRT; else -> Material.MUD }
        }
        data.setBlock(x, f, z, floor)
        if (floor == Material.COARSE_DIRT && mix(seed, wx, wz, 151) % 100 < 40) data.setBlock(x, f + 1, z, Material.CRIMSON_ROOTS)

        
        val ch = mix(seed, wx, wz, 152) % 100
        data.setBlock(x, c, z, if (ch < 8) Material.SHROOMLIGHT else if (ch < 80) Material.NETHER_WART_BLOCK else Material.RED_NETHER_BRICKS)
        if (mix(seed, wx, wz, 153) % 100 < (if (dist == 2) 12 else 6)) {
            val len = 3 + mix(seed, wx, wz, 154) % 8
            for (i in 1..len) data.setBlock(x, c - i, z, if (i == len) Material.WEEPING_VINES else Material.WEEPING_VINES_PLANT)
        }

        
        if (dist == 2 || dist == 3) {
            val r = mix(seed, wx, wz, 210) % 1000
            if (dist == 2) {
                if (r < 14) {
                    val h = 4 + mix(seed, wx, wz, 211) % 24
                    for (i in 1..h) data.setBlock(x, f + i, z, Material.NETHER_WART_BLOCK)
                } else if (r < 18) {
                    val h = 10 + mix(seed, wx, wz, 212) % 20
                    data.setBlock(x, f + 1, z, Material.MUSHROOM_STEM)
                    for (i in 2..h) data.setBlock(x, f + i, z, Material.BONE_BLOCK)
                }
            } else {
                if (r < 25) {
                    val h = 6 + mix(seed, wx, wz, 213) % 28
                    for (i in 1..h) data.setBlock(x, f + i, z, if (i % 7 == 0) Material.MUSHROOM_STEM else Material.BONE_BLOCK)
                } else if (r < 29) {
                    for (i in 1..3) data.setBlock(x, f + i, z, Material.SHROOMLIGHT)
                }
            }
            return
        }

        val period = if (dist == 1) 32 else 40
        val street = if (dist == 1) 6 else 8
        val bxm = fmod(wx, period)
        val bzm = fmod(wz, period)
        if (bxm < street || bzm < street) { 
            val m = mix(seed, wx, wz, 155) % 100
            if (m < 5) data.setBlock(x, f + 1, z, Material.NETHER_WART_BLOCK)
            if (m < 2) data.setBlock(x, f + 2, z, Material.NETHER_WART_BLOCK)
            if (bzm == 3 && fmod(wx, 20) == 4) {
                for (i in 1..3) data.setBlock(x, f + i, z, Material.NETHER_BRICK_FENCE)
                data.setBlock(x, f + 4, z, Material.SHROOMLIGHT)
            }
            return
        }

        
        val gx = fdiv(wx, period)
        val gz = fdiv(wz, period)
        val bh = mix(seed, gx, gz, 156)
        val dens = when (dist) { 0 -> 80; 1 -> 70; else -> 55 }
        if (bh % 100 >= dens) return
        val bsz = period - street
        val lx = wx - (gx * period + street)
        val lz = wz - (gz * period + street)
        val inset = when (dist) { 0 -> 2; 1 -> 4; else -> 1 }
        val spread = when (dist) { 0 -> 3; 1 -> 5; else -> 3 }
        val w0 = inset + (bh shr 8) % spread
        val w1 = bsz - 1 - inset - (bh shr 12) % spread
        val d0 = inset + (bh shr 16) % spread
        val d1 = bsz - 1 - inset - (bh shr 20) % spread
        if (lx !in w0..w1 || lz !in d0..d1) return
        val hgt = when (dist) { 0 -> 18 + (bh shr 4) % 17; 1 -> 6 + (bh shr 4) % 11; else -> 8 + (bh shr 4) % 5 }
        val onX = lx == w0 || lx == w1
        val onZ = lz == d0 || lz == d1
        for (dy in 1..hgt) {
            val y = f + dy
            val hh = mix(seed, wx, wz * 64 + dy, 157) % 100
            if (onX || onZ) {
                val corner = onX && onZ
                val along = if (onX) lz else lx
                val door = dy <= 3 && !corner && fmod(along, 11) == 5
                val window = !corner && dy > 3 && dy % 4 in 1..2 && fmod(along, 3) == 1
                when {
                    door -> data.setBlock(x, y, z, Material.AIR)
                    window -> data.setBlock(x, y, z, if (hh < 18) Material.RED_STAINED_GLASS else Material.AIR)
                    else -> data.setBlock(x, y, z, fleshWall(hh))
                }
            } else if (dy % 5 == 0 && hh < 78) {
                data.setBlock(x, y, z, Material.RED_TERRACOTTA)
            }
        }
        val rh = mix(seed, wx, wz, 158) % 100
        val roof = when {
            dist == 4 && lx % 6 < 2 && lz % 6 < 2 -> Material.SHROOMLIGHT 
            dist == 4 && rh < 6 -> Material.MAGMA_BLOCK 
            rh < 15 -> Material.SHROOMLIGHT
            else -> Material.NETHER_WART_BLOCK
        }
        data.setBlock(x, f + hgt + 1, z, roof)
        if (dist == 0 && rh > 94 && lx == (w0 + w1) / 2 && lz == (d0 + d1) / 2) { 
            for (i in 2..6) data.setBlock(x, f + hgt + i, z, Material.NETHER_BRICK_FENCE)
        }
    }

    

    
    fun hospitalColumn(info: WorldInfo, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int) {
        val seed = info.seed
        rockColumn(info, data, x, z, wx, wz, HOSP_F)
        hypodermis(seed, data, x, z, wx, wz)
        data.setRegion(x, HOSP_C + 1, z, x + 1, HOSP_C + 6, z + 1, Material.STONE)
        data.setBlock(x, HOSP_C + 6, z, Material.BEDROCK)
        pads(seed, 2, data, x, z, wx, wz, HOSP_F)
    }

    
    private fun section(seed: Long, wx: Int, wz: Int): Int = mix(seed, fdiv(wx, 192), fdiv(wz, 192), 195) % 6

    private fun hypodermis(seed: Long, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int) {
        val f = HOSP_F
        val c = HOSP_C
        data.setRegion(x, f + 1, z, x + 1, c, z + 1, Material.AIR)
        val sect = section(seed, wx, wz)
        val bx = fmod(wx, 48)
        val bz = fmod(wz, 48)
        val gx = fdiv(wx, 48)
        val gz = fdiv(wz, 48)
        val corridor = bx < 6 || bz < 6
        val lx = bx - 6
        val lz = bz - 6
        val pitch = when (sect) { 2 -> 5; 5 -> 4; else -> 7 }
        val checker = ((fdiv(wx, 2) + fdiv(wz, 2)) and 1) == 0
        var floor = if (corridor) {
            if (checker) Material.WHITE_CONCRETE else Material.LIGHT_GRAY_CONCRETE
        } else {
            if (checker) Material.WHITE_TERRACOTTA else Material.LIGHT_GRAY_CONCRETE
        }
        if (sect == 1 && !corridor) floor = if (checker) Material.QUARTZ_BLOCK else Material.WHITE_CONCRETE 
        if (mix(seed, wx, wz, 162) % 100 < 3) floor = Material.RED_TERRACOTTA 
        data.setBlock(x, f, z, floor)

        var wall = false
        var door = false
        var pillar = false
        var strip = false
        var facing = "east"

        
        if (corridor && !(bx < 6 && bz < 6)) {
            if (bx < 6 && bz == 47 && mix(seed, gx, gz, 190) % 100 < 30) wall = true
            if (bz < 6 && bx == 47 && mix(seed, gx, gz, 191) % 100 < 30) wall = true
        }
        if (!corridor) {
            when (sect) {
                1 -> pillar = lx % 8 in 3..4 && lz % 8 in 3..4 
                3 -> { 
                    val sx = if (mix(seed, gx, gz, 193) % 100 < 60) 0 else 2 + mix(seed, gx, gz, 194) % 10
                    val ex = if (mix(seed, gx, gz, 197) % 100 < 40) 41 else 28 + mix(seed, gx, gz, 198) % 12
                    val sz = if (mix(seed, gx, gz, 199) % 100 < 60) 0 else 2 + mix(seed, gx, gz, 201) % 10
                    val ez = if (mix(seed, gx, gz, 202) % 100 < 40) 41 else 28 + mix(seed, gx, gz, 203) % 12
                    strip = (lz in 19..22 && lx in sx..ex) || (lx in 19..22 && lz in sz..ez)
                    wall = !strip
                }
                else -> { 
                    val wallX = lx % pitch == 0 || lx == 41
                    val wallZ = lz % pitch == 0 || lz == 41
                    val rx = minOf(lx / pitch, 41 / pitch)
                    val rz = minOf(lz / pitch, 41 / pitch)
                    val doorPct = when (sect) { 2 -> 55; 5 -> 70; else -> 82 }
                    val doorAt = pitch / 2
                    if (wallX && wallZ) {
                        wall = true
                    } else if (wallX) {
                        wall = true
                        if (lz % pitch == doorAt && mix(seed, gx * 16 + rx, gz * 16 + rz, 163) % 100 < doorPct) { door = true; facing = "east" }
                    } else if (wallZ) {
                        wall = true
                        if (lx % pitch == doorAt && mix(seed, gx * 16 + rx, gz * 16 + rz, 164) % 100 < doorPct) { door = true; facing = "north" }
                    }
                }
            }
        }
        if (wall) {
            for (y in f + 1 until c) data.setBlock(x, y, z, if (y <= f + 2) Material.CYAN_TERRACOTTA else Material.WHITE_CONCRETE)
            if (door) {
                data.setBlock(x, f + 1, z, Material.AIR)
                data.setBlock(x, f + 2, z, Material.AIR)
                if (mix(seed, wx, wz, 165) % 100 >= 15) { 
                    val open = mix(seed, wx, wz, 166) % 100 < 20
                    data.setBlock(x, f + 1, z, Material.BIRCH_DOOR.createBlockData("[half=lower,facing=$facing,open=$open]"))
                    data.setBlock(x, f + 2, z, Material.BIRCH_DOOR.createBlockData("[half=upper,facing=$facing,open=$open]"))
                }
            }
        } else if (pillar) {
            for (y in f + 1 until c) data.setBlock(x, y, z, Material.WHITE_CONCRETE)
        } else if (sect == 4 && mix(seed, wx, wz, 204) % 100 < 70) {
            data.setBlock(x, f + 1, z, Material.WATER) 
        }
        
        val lightRoom = when (sect) {
            1 -> !corridor && lx % 8 in 0..1 && lz % 8 in 0..1
            3 -> strip && (lx + lz) % 6 == 0
            else -> !corridor && fmod(lx, pitch) == pitch / 2 && fmod(lz, pitch) == pitch / 2
        }
        val lightCorr = corridor && ((bx in 2..3 && bz >= 6 && fmod(wz, 6) == 3) || (bz in 2..3 && bx >= 6 && fmod(wx, 6) == 3))
        val lit = (lightRoom || lightCorr) && mix(seed, wx, wz, 167) % 100 < 80
        data.setBlock(x, c, z, if (lit) Material.SEA_LANTERN else Material.WHITE_CONCRETE)
    }

    
    private fun pads(seed: Long, layer: Int, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int, f: Int) {
        val up = siteNear(seed, layer - 1, wx, wz, 5)
        if (up != null) {
            for (y in f + 1..f + 8) data.setBlock(x, y, z, Material.AIR)
            pad(data, x, z, wx - up.cx, wz - up.cz, f, true)
        }
        val down = if (layer <= 2) siteNear(seed, layer, wx, wz, 5) else null
        if (down != null) {
            for (y in f + 1..f + 8) data.setBlock(x, y, z, Material.AIR)
            pad(data, x, z, wx - down.cx, wz - down.cz, f, false)
        }
    }

    

    
    fun tearAt(seed: Long, wx: Int, wz: Int): Tear? {
        val gx = fdiv(wx, 1200)
        val gz = fdiv(wz, 1200)
        val s = siteIn(seed, 0, gx, gz) ?: return null
        val dx = (wx - s.cx).toDouble()
        val dz = (wz - s.cz).toDouble()
        val d = sqrt(dx * dx + dz * dz)
        if (d > 60.0) return null
        val h = mix(seed, gx, gz, 175)
        val base = 22.0 + (h shr 3) % 14
        val p1 = ((h shr 4) % 628) / 100.0
        val p2 = ((h shr 9) % 628) / 100.0
        val p3 = ((h shr 14) % 628) / 100.0
        val th = atan2(dz, dx)
        val r = base * (1.0 + 0.22 * sin(3 * th + p1) + 0.16 * sin(5 * th + p2) + 0.10 * sin(8 * th + p3))
        if (d >= r) return null
        val q = d / r
        return Tear(s.cx, s.cz, q, s.dmax * (1.0 - q).pow(0.8), s.dmax.toDouble(), d)
    }

    
    fun tearClear(seed: Long, x0: Int, z0: Int, w: Int, d: Int): Boolean {
        var u = -8
        while (u <= w + 8) {
            var v = -8
            while (v <= d + 8) {
                if (tearAt(seed, x0 + u, z0 + v) != null) return false
                v += 6
            }
            u += 6
        }
        return true
    }

    
    fun tear(seed: Long, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int, surface: Int) {
        val t = tearAt(seed, wx, wz) ?: return
        val coreFloor = Terrain.heightAt(seed, t.cx, t.cz) - t.dmax.toInt()
        val natural = surface - t.depth.toInt()
        val raw = when {
            t.dist <= 6.0 -> coreFloor
            t.dist < 14.0 -> Math.round(coreFloor + (natural - coreFloor) * ((t.dist - 6.0) / 8.0)).toInt()
            else -> natural
        }
        val floor = minOf(raw, surface)
        if (floor + 1 <= surface) data.setRegion(x, floor + 1, z, x + 1, surface + 1, z + 1, Material.AIR)
        val hh = mix(seed, wx, wz, 176) % 100
        data.setBlock(
            x, floor, z,
            if (t.q > 0.86) {
                if (hh < 40) Material.NETHER_WART_BLOCK else if (hh < 60) Material.RED_TERRACOTTA else Material.MOSS_BLOCK
            } else {
                if (hh < 40) Material.ROOTED_DIRT else if (hh < 70) Material.COARSE_DIRT else if (hh < 90) Material.MOSS_BLOCK else Material.NETHER_WART_BLOCK
            }
        )
        if (t.dist <= 4.9) pad(data, x, z, wx - t.cx, wz - t.cz, floor, false)
    }

    

    
    fun furnish(seed: Long, random: Random, p: Place, bx: Int, bz: Int) {
        val f = HOSP_F
        for (gx in fdiv(bx, 48)..fdiv(bx + 15, 48)) for (gz in fdiv(bz, 48)..fdiv(bz + 15, 48)) {
            for (rx in 0..5) for (rz in 0..5) {
                val x0 = gx * 48 + 6 + rx * 7 + 1
                val z0 = gz * 48 + 6 + rz * 7 + 1
                val cx = x0 + 2
                val cz = z0 + 2
                if (cx < bx || cx >= bx + 16 || cz < bz || cz >= bz + 16) continue
                val sec = section(seed, cx, cz)
                if (sec != 0 && sec != 4) continue 
                val kind = mix(seed, gx * 8 + rx, gz * 8 + rz, 168) % 100
                when {
                    kind < 35 -> { 
                        if (p.isAir(x0 + 1, f + 1, z0 + 3) && p.isAir(x0 + 1, f + 1, z0 + 2)) {
                            p.put(x0 + 1, f + 1, z0 + 3, Material.WHITE_BED, "[facing=north,part=foot]")
                            p.put(x0 + 1, f + 1, z0 + 2, Material.WHITE_BED, "[facing=north,part=head]")
                        }
                        if (p.isAir(x0 + 3, f + 1, z0 + 2)) p.put(x0 + 3, f + 1, z0 + 2, Material.BARREL, "[facing=up]")
                    }
                    kind < 50 -> { 
                        for (dx in 2..3) if (p.isAir(x0 + dx, f + 1, z0 + 2)) p.set(x0 + dx, f + 1, z0 + 2, Material.IRON_BLOCK)
                        if (p.isAir(x0 + 2, f + 1, z0 + 3)) p.set(x0 + 2, f + 1, z0 + 3, Material.LIGHT_GRAY_CARPET)
                    }
                    kind < 65 -> { 
                        for (dx in 0..4) {
                            if (p.isAir(x0 + dx, f + 1, z0)) p.set(x0 + dx, f + 1, z0, if (dx % 2 == 0) Material.BARREL else Material.BOOKSHELF)
                            if (dx % 2 == 1 && p.isAir(x0 + dx, f + 2, z0)) p.set(x0 + dx, f + 2, z0, Material.BOOKSHELF)
                        }
                        if (Loot.rare(random, 14.0)) Loot.chestAt(p, random, x0 + 2, f + 1, z0 + 3, "backrooms")
                    }
                    kind < 75 -> { 
                        for (dx in 1..3) {
                            if (p.isAir(x0 + dx, f + 1, z0 + 1)) p.put(x0 + dx, f + 1, z0 + 1, Material.SPRUCE_STAIRS, "[facing=south]")
                            if (p.isAir(x0 + dx, f + 1, z0 + 3)) p.put(x0 + dx, f + 1, z0 + 3, Material.SPRUCE_STAIRS, "[facing=north]")
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}
