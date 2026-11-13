package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.entity.EntityType
import java.util.Random





object Towers {
    private const val FLOOR_H = 5

    fun build(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val size = 9 + 2 * random.nextInt(4) 
        val x0 = bx + 2 + random.nextInt(3)
        val z0 = bz + 2 + random.nextInt(3)
        val low = Sites.flatBase(seed, gen, x0, z0, size, size, 1, 5) ?: return

        val height = 45 + random.nextInt(65)
        shell(p, random, x0, z0, size, low, height)
        overgrowth(p, random, x0, z0, size, low, height)
        StructureDetails.facade(p, random, x0, z0, size, size, low, height, modern = true)
        StructureDetails.roofline(p, x0, z0, size, size, low + height, modern = true)

        val ry = low + height
        val climate = ClimateMap.at(seed, x0, z0)
        val mid = size / 2
        if (random.nextInt(100) < 45) {
            Trees.bigTree(p, random, x0 + mid - 1, ry + 1, z0 + mid - 1, climate, 8 + random.nextInt(6), 4 + random.nextInt(2))
        } else if (random.nextInt(100) < 50) {
            Chimney.build(p, random, x0 + mid - 1, ry + 1, z0 + mid)
            Chimney.build(p, random, x0 + mid + 1, ry + 1, z0 + mid)
        }
        
        val ys = (1 until height / FLOOR_H).map { low + it * FLOOR_H + 1 }
        val guard = if (climate.cold) EntityType.STRAY else EntityType.SKELETON
        val chests = if (Loot.rare(random, 35.0)) 1 else 0
        Interiors.furnish(p, random, x0, z0, size, size, 3, (0 until height / FLOOR_H).map { low + it * FLOOR_H }, 14)
        Loot.fill(p, random, x0, z0, size, size, 3, ys, "tower", listOf(guard, EntityType.ZOMBIE), chests, 3)
    }

    private fun pillar(random: Random): Material = when (random.nextInt(10)) {
        in 0..4 -> Material.QUARTZ_PILLAR
        in 5..6 -> Material.MOSSY_STONE_BRICKS
        in 7..8 -> Material.STONE_BRICKS
        else -> Material.CRACKED_STONE_BRICKS
    }

    private fun shell(p: Place, random: Random, x0: Int, z0: Int, size: Int, baseY: Int, height: Int) {
        val last = size - 1
        for (dy in 0 until height) {
            val y = baseY + dy
            val slab = dy % FLOOR_H == 0
            for (dx in 0 until size) for (dz in 0 until size) {
                val ring = minOf(minOf(dx, last - dx), minOf(dz, last - dz)) 
                val cornerX = dx < 2 || dx > last - 2
                val cornerZ = dz < 2 || dz > last - 2
                val mat: Material? = when {
                    
                    ring <= 1 && cornerX && cornerZ -> pillar(random)
                    
                    ring == 0 ->
                        if (slab) Material.QUARTZ_BLOCK
                        else if ((dx % 4 == 0 && (dz == 0 || dz == last)) || (dz % 4 == 0 && (dx == 0 || dx == last))) Material.QUARTZ_PILLAR
                        else if (random.nextInt(100) < 12) null
                        else Material.LIGHT_GRAY_STAINED_GLASS
                    
                    ring == 1 ->
                        if (slab) Material.QUARTZ_BLOCK
                        else if ((dx + dz) % 3 == 0 && random.nextInt(100) < 55) null
                        else if (random.nextInt(100) < 20) null
                        else if (random.nextInt(100) < 25) Material.MOSSY_STONE_BRICKS else Material.STONE_BRICKS
                    
                    dx % 6 == 3 && dz % 6 == 3 ->
                        if (random.nextInt(100) < 6) null else Material.QUARTZ_PILLAR
                    
                    slab ->
                        if (ring >= 3 && (dx - size / 2) in -1..1 && (dz - size / 2) in -1..1) null
                        else if (random.nextInt(100) < 10) null else Material.QUARTZ_BLOCK
                    else -> null
                }
                if (mat != null) p.set(x0 + dx, y, z0 + dz, mat)
            }
        }
        val roofY = baseY + height
        for (dx in 0 until size) for (dz in 0 until size) {
            p.set(x0 + dx, roofY, z0 + dz, if (random.nextInt(10) < 7) Material.MOSS_BLOCK else Material.QUARTZ_BLOCK)
        }
    }

    private fun overgrowth(p: Place, random: Random, x0: Int, z0: Int, size: Int, baseY: Int, height: Int) {
        val roofY = baseY + height
        val vineChance = 35 + random.nextInt(35)

        for (dx in -1..size) for (dz in -1..size) {
            if (random.nextInt(100) < 45) {
                for (i in 0 until 1 + random.nextInt(4)) p.leaf(x0 + dx, roofY + 1 + i, z0 + dz)
            }
        }

        val faces = arrayOf("south", "north", "east", "west")
        for (side in 0 until 4) for (i in 0 until size) {
            val x: Int
            val z: Int
            when (side) {
                0 -> { x = x0 + i; z = z0 - 1 }
                1 -> { x = x0 + i; z = z0 + size }
                2 -> { x = x0 - 1; z = z0 + i }
                else -> { x = x0 + size; z = z0 + i }
            }

            if (random.nextInt(100) < vineChance) {
                val top = baseY + 2 + random.nextInt(height)
                val len = 4 + random.nextInt(22)
                for (y in top downTo top - len) {
                    if (!p.isAir(x, y, z)) break
                    p.vine(x, y, z, faces[side])
                }
            }

            if (random.nextInt(100) < 9) {
                val cy = baseY + 3 + random.nextInt(height - 3)
                for (dx in -2..2) for (dy in -2..2) for (dz in -2..2) {
                    if (dx * dx + dy * dy + dz * dz <= 5 && random.nextInt(100) < 85) p.leaf(x + dx, cy + dy, z + dz)
                }
            }
        }
    }
}
