package com.example.aeroruins

import org.bukkit.Material
import java.util.Random


object Garden {
    fun maybe(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        if (random.nextInt(100) >= 38) return
        when (random.nextInt(100)) {
            in 0..24 -> gazebo(seed, random, p, bx, bz, gen)
            in 25..49 -> greenhouse(seed, random, p, bx, bz, gen)
            in 50..69 -> fountain(seed, random, p, bx, bz, gen)
            in 70..89 -> hedgeGarden(seed, random, p, bx, bz, gen)
            else -> statue(seed, random, p, bx, bz, gen)
        }
    }

    private fun clear(p: Place, x0: Int, z0: Int, w: Int, d: Int, low: Int, floor: Material?) {
        for (dx in 0 until w) for (dz in 0 until d) {
            for (y in low + 1..low + 9) p.set(x0 + dx, y, z0 + dz, Material.AIR)
            if (floor != null) p.set(x0 + dx, low, z0 + dz, floor)
        }
    }

    private fun gazebo(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 3 + random.nextInt(3)
        val z0 = bz + 3 + random.nextInt(3)
        val low = Sites.flatBase(seed, gen, x0, z0, 9, 9, 1, 3) ?: return
        clear(p, x0, z0, 9, 9, low, null)
        for (dx in 1..7) for (dz in 1..7) p.set(x0 + dx, low, z0 + dz, if (random.nextInt(100) < 25) Material.MOSSY_STONE_BRICKS else Material.STONE_BRICKS)
        for ((px, pz) in listOf(1 to 1, 1 to 7, 7 to 1, 7 to 7, 4 to 1, 4 to 7, 1 to 4, 7 to 4)) {
            val h = if (random.nextInt(100) < 15) 2 + random.nextInt(2) else 4
            for (y in 1..h) p.set(x0 + px, low + y, z0 + pz, Material.QUARTZ_PILLAR)
        }
        for (dx in 0..8) for (dz in 0..8) if (random.nextInt(100) < 92) p.set(x0 + dx, low + 5, z0 + dz, Material.STONE_BRICK_SLAB)
        for (dx in 2..6) for (dz in 2..6) p.set(x0 + dx, low + 6, z0 + dz, Material.STONE_BRICK_SLAB)
        p.set(x0 + 4, low + 7, z0 + 4, Material.CHISELED_STONE_BRICKS)
        for (i in 0..8) {
            if (random.nextInt(100) < 40) p.hang(x0 + i, low + 4, z0, 2 + random.nextInt(3))
            if (random.nextInt(100) < 40) p.hang(x0 + i, low + 4, z0 + 8, 2 + random.nextInt(3))
        }
        p.put(x0 + 3, low + 1, z0 + 6, Material.SPRUCE_STAIRS, "[facing=north]")
        p.put(x0 + 5, low + 1, z0 + 6, Material.SPRUCE_STAIRS, "[facing=north]")
        if (Loot.rare(random, 25.0)) Loot.chestAt(p, random, x0 + 4, low + 1, z0 + 3, "frame")
    }

    private fun greenhouse(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val w = 12
        val d = 8
        val x0 = bx + 2 + random.nextInt(3)
        val z0 = bz + 3 + random.nextInt(3)
        val low = Sites.flatBase(seed, gen, x0, z0, w, d, 1, 3) ?: return
        clear(p, x0, z0, w, d, low, Material.PODZOL)
        for (dy in 1..4) for (dx in 0 until w) for (dz in 0 until d) {
            val edge = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1
            if (!edge) continue
            val door = dz == d - 1 && dx in 5..6 && dy <= 2
            if (door) continue
            val frame = (dx % 4 == 0 && (dz == 0 || dz == d - 1)) || dy == 1 || ((dx == 0 || dx == w - 1) && dz % 3 == 0)
            if (random.nextInt(100) < 18 && !frame) continue 
            p.set(x0 + dx, low + dy, z0 + dz, if (frame) Material.STONE_BRICKS else Material.GLASS)
        }
        for (dx in 0 until w) for (dz in 0 until d) if (random.nextInt(100) < 70) p.set(x0 + dx, low + 5, z0 + dz, Material.GLASS)
        for (dx in 1 until w - 1) for (dz in 1 until d - 1) {
            val r = random.nextInt(100)
            when {
                r < 30 -> p.set(x0 + dx, low + 1, z0 + dz, Material.FERN)
                r < 48 -> p.set(x0 + dx, low + 1, z0 + dz, Material.SHORT_GRASS)
                r < 60 -> p.set(x0 + dx, low + 1, z0 + dz, Material.AZALEA)
                r < 66 -> p.set(x0 + dx, low + 1, z0 + dz, Material.FLOWERING_AZALEA)
                r < 74 -> { p.leaf(x0 + dx, low + 1, z0 + dz); p.leaf(x0 + dx, low + 2, z0 + dz) }
            }
        }
        for (dx in 2..4) { 
            p.set(x0 + dx, low + 1, z0 + 2, Material.OAK_PLANKS)
            p.set(x0 + dx, low + 2, z0 + 2, if (random.nextBoolean()) Material.POTTED_FERN else Material.POTTED_AZURE_BLUET)
        }
        for (dx in 0 until w) if (random.nextInt(100) < 35) p.hang(x0 + dx, low + 4, z0 + 1, 2 + random.nextInt(2))
    }

    private fun fountain(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 4 + random.nextInt(4)
        val z0 = bz + 4 + random.nextInt(4)
        val low = Sites.flatBase(seed, gen, x0, z0, 7, 7, 1, 2) ?: return
        clear(p, x0, z0, 7, 7, low, null)
        for (dx in 0..6) for (dz in 0..6) {
            val ring = dx == 0 || dx == 6 || dz == 0 || dz == 6
            if (ring) {
                p.set(x0 + dx, low, z0 + dz, if (random.nextInt(100) < 30) Material.MOSSY_STONE_BRICKS else Material.STONE_BRICKS)
                if (random.nextInt(100) < 80) p.set(x0 + dx, low + 1, z0 + dz, Material.STONE_BRICK_SLAB)
            } else {
                p.set(x0 + dx, low, z0 + dz, Material.WATER)
                if (random.nextInt(100) < 20 && !(dx == 3 && dz == 3)) p.set(x0 + dx, low + 1, z0 + dz, Material.LILY_PAD)
            }
        }
        for (y in 1..3) p.put(x0 + 3, low + y, z0 + 3, Material.STONE_BRICK_WALL, "[up=true]")
        p.set(x0 + 3, low + 4, z0 + 3, Material.MOSS_BLOCK)
    }

    private fun hedgeGarden(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val s = 17
        val x0 = bx
        val z0 = bz
        val low = Sites.flatBase(seed, gen, x0, z0, s, s, 1, 3) ?: return
        for (dx in 0 until s) for (dz in 0 until s) {
            for (y in low + 1..low + 4) p.set(x0 + dx, y, z0 + dz, Material.AIR)
            val lineX = dx % 4 == 0
            val lineZ = dz % 4 == 0
            val hedge = if (lineX && lineZ) true else if (lineX) {
                mix(seed, x0 + dx, z0 + dz / 4, 140) % 100 >= 42
            } else if (lineZ) {
                mix(seed, x0 + dx / 4, z0 + dz, 141) % 100 >= 42
            } else false
            val onGrid = lineX || lineZ
            p.set(x0 + dx, low, z0 + dz, if (onGrid && !hedge) Material.GRAVEL else Material.GRASS_BLOCK)
            if (hedge) {
                p.leaf(x0 + dx, low + 1, z0 + dz)
                p.leaf(x0 + dx, low + 2, z0 + dz)
            } else if (!onGrid && random.nextInt(100) < 45) {
                p.set(x0 + dx, low + 1, z0 + dz, ClimateMap.at(seed, x0 + dx, z0 + dz).flowers.let { it[random.nextInt(it.size)] })
            }
        }
        for (y in 1..3) p.set(x0 + 8, low + y, z0 + 8, Material.STONE_BRICKS)
        p.set(x0 + 8, low + 4, z0 + 8, Material.CHISELED_STONE_BRICKS)
    }

    private fun statue(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 5 + random.nextInt(4)
        val z0 = bz + 5 + random.nextInt(4)
        val low = Sites.flatBase(seed, gen, x0, z0, 5, 5, 1, 2) ?: return
        clear(p, x0, z0, 5, 5, low, Material.GRAVEL)
        for (dx in 1..3) for (dz in 1..3) p.set(x0 + dx, low + 1, z0 + dz, Material.STONE_BRICKS)
        for (y in 2..4) p.set(x0 + 2, low + y, z0 + 2, if (random.nextInt(100) < 30) Material.MOSSY_STONE_BRICKS else Material.STONE_BRICKS)
        p.set(x0 + 2, low + 5, z0 + 2, Material.CHISELED_STONE_BRICKS)
        p.set(x0 + 1, low + 4, z0 + 2, Material.STONE_BRICK_SLAB)
        p.set(x0 + 3, low + 4, z0 + 2, Material.STONE_BRICK_SLAB)
        for (dy in 2..5) if (random.nextInt(100) < 50) p.vine(x0 + 1, low + dy, z0 + 2, "east")
    }
}
