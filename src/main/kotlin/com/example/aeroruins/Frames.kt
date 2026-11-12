package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.entity.EntityType
import java.util.Random


object Frames {
    private const val S = 15

    fun build(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 2
        val z0 = bz + 2
        val base = Sites.flatBase(seed, gen, x0, z0, S, S, 3, 5) ?: return
        val floors = 3 + random.nextInt(4)
        val height = floors * 4
        val climate = ClimateMap.at(seed, x0, z0)
        val pil = intArrayOf(0, 1, 6, 7, 8, 13, 14) 

        for (dy in 0..height) {
            val y = base + dy
            val slab = dy % 4 == 0
            for (dx in 0 until S) for (dz in 0 until S) {
                val x = x0 + dx
                val z = z0 + dz
                val pillar = dx in pil && dz in pil
                if (pillar) {
                    if (random.nextInt(100) >= 5) {
                        val r = random.nextInt(100)
                        p.set(x, y, z, if (r < 80) Material.LIGHT_GRAY_CONCRETE else if (r < 90) Material.MOSS_BLOCK else Material.STONE_BRICKS)
                    }
                } else if (slab) {
                    if (dy == 0) {
                        p.set(x, y, z, if (random.nextInt(100) < 60) Material.MOSS_BLOCK else Material.COARSE_DIRT)
                    } else if (random.nextInt(100) >= 12 + dy / 2) {
                        p.set(x, y, z, Material.LIGHT_GRAY_CONCRETE)
                        val g = random.nextInt(100)
                        if (g < 40) p.set(x, y + 1, z, Material.MOSS_CARPET) else if (g < 55) p.leaf(x, y + 1, z)
                    }
                }
            }
        }

        
        val faces = arrayOf("south", "north", "east", "west")
        for (side in 0 until 4) for (i in 0 until S) for (f in 1..floors) {
            if (random.nextInt(100) >= 55) continue
            val ox: Int
            val oz: Int
            val wx: Int
            val wz: Int
            when (side) {
                0 -> { ox = x0 + i; oz = z0 - 1; wx = ox; wz = z0 }
                1 -> { ox = x0 + i; oz = z0 + S; wx = ox; wz = z0 + S - 1 }
                2 -> { ox = x0 - 1; oz = z0 + i; wx = x0; wz = oz }
                else -> { ox = x0 + S; oz = z0 + i; wx = x0 + S - 1; wz = oz }
            }
            val y = base + f * 4
            if (!p.type(wx, y, wz).isSolid) continue
            for (k in 0 until 1 + random.nextInt(4)) {
                if (!p.isAir(ox, y - k, oz)) break
                p.vine(ox, y - k, oz, faces[side])
            }
        }

        
        for (a in pil) for (b in pil) {
            val dirs = listOf(
                Triple(-1, 0, "east"), Triple(1, 0, "west"), Triple(0, -1, "south"), Triple(0, 1, "north")
            )
            for ((ddx, ddz, face) in dirs) {
                if (random.nextInt(100) >= 65) continue
                for (y in base + height downTo base + 1) {
                    if (random.nextInt(100) < 6) continue
                    val nx = x0 + a + ddx
                    val nz = z0 + b + ddz
                    if (p.isAir(nx, y, nz) && p.type(x0 + a, y, z0 + b).isSolid) p.vine(nx, y, nz, face)
                }
            }
        }

        
        if (random.nextInt(100) < 35 && p.type(x0 + 3, base + height, z0 + 3).isSolid) {
            Trees.bigTree(p, random, x0 + 3, base + height + 1, z0 + 3, climate, 7 + random.nextInt(5), 4)
        }
        Interiors.furnish(p, random, x0, z0, S, S, 2, (1 until floors).map { base + it * 4 }, 30)
        val ys = (0 until floors).map { base + it * 4 + 1 }
        Loot.fill(
            p, random, x0, z0, S, S, 2, ys, "frame", listOf(EntityType.SPIDER, EntityType.ZOMBIE),
            if (Loot.rare(random, 15.0)) 1 else 0, 2
        )
    }
}
