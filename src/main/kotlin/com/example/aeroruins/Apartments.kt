package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.entity.EntityType
import java.util.Random


object Apartments {
    private const val W = 15
    private const val D = 11

    fun build(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 2
        val z0 = bz + 3
        val base = Sites.flatBase(seed, gen, x0, z0, W, D + 2, 3, 5) ?: return
        val floors = 5 + random.nextInt(5)
        val height = floors * 4
        val climate = ClimateMap.at(seed, x0, z0)

        shell(p, random, x0, z0, base, height)
        balconies(p, random, x0, z0, base, floors)
        ivy(p, random, x0, z0, base, height)
        roof(p, random, x0, z0, base, height, climate)
        StructureDetails.facade(p, random, x0, z0, W, D, base, height, modern = true)
        StructureDetails.roofline(p, x0, z0, W, D, base + height + 1, modern = true)
        Interiors.furnish(p, random, x0, z0, W, D, 2, (0 until floors).map { base + it * 4 }, 9)
        val ys = (0 until floors).map { base + it * 4 + 1 }
        Loot.fill(
            p, random, x0, z0, W, D, 3, ys, "apartment",
            listOf(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER, EntityType.VINDICATOR),
            if (Loot.rare(random, 25.0)) 1 else 0, 3
        )
    }

    private fun wall(random: Random): Material {
        val r = random.nextInt(100)
        return if (r < 70) Material.WHITE_CONCRETE else if (r < 92) Material.LIGHT_GRAY_CONCRETE else Material.MOSS_BLOCK
    }

    private fun shell(p: Place, random: Random, x0: Int, z0: Int, base: Int, height: Int) {
        for (dy in 0..height) {
            val y = base + dy
            val rel = dy % 4
            val holeChance = 5 + dy * 12 / maxOf(height, 1)
            for (dx in 0 until W) for (dz in 0 until D) {
                
                val perimeter = dx < 2 || dx > W - 3 || dz < 2 || dz > D - 3
                if (rel == 0) {
                    if (dy == 0) {
                        val r = random.nextInt(100)
                        p.set(
                            x0 + dx, y, z0 + dz,
                            if (r < 55) Material.MOSS_BLOCK else if (r < 80) Material.COARSE_DIRT else Material.LIGHT_GRAY_CONCRETE
                        )
                    } else {
                        val stairwell = dx in (W - 5)..(W - 4) && dz in 2..3
                        if (!stairwell && random.nextInt(100) >= holeChance) {
                            p.set(x0 + dx, y, z0 + dz, Material.LIGHT_GRAY_CONCRETE)
                        }
                    }
                } else if (perimeter) {
                    val corner = (dx < 2 || dx > W - 3) && (dz < 2 || dz > D - 3)
                    val longWall = dz < 2 || dz > D - 3
                    val window = !corner && rel >= 2 && (if (longWall) dx % 4 in 1..2 else dz % 4 in 1..2)
                    if (!window && random.nextInt(100) >= 7) p.set(x0 + dx, y, z0 + dz, wall(random))
                } else if (dx % 5 == 2 && dz % 5 == 2) {
                    
                    p.set(x0 + dx, y, z0 + dz, Material.LIGHT_GRAY_CONCRETE)
                }
            }
        }
    }

    private fun balconies(p: Place, random: Random, x0: Int, z0: Int, base: Int, floors: Int) {
        for (f in 1 until floors) {
            val y = base + f * 4
            for (bay in 0..2) {
                if (random.nextInt(100) >= 75) continue
                val bs = 1 + bay * 5
                for (dx in bs..bs + 2) for (dz in D..D + 1) {
                    if (random.nextInt(100) >= 10) p.set(x0 + dx, y, z0 + dz, Material.LIGHT_GRAY_CONCRETE)
                }
                for (dx in bs..bs + 2) {
                    p.set(x0 + dx, y + 1, z0 + D + 1, Material.WHITE_CONCRETE)
                    p.set(x0 + dx, y + 2, z0 + D + 1, Material.RED_CONCRETE)
                }
                p.set(x0 + bs, y + 1, z0 + D, Material.WHITE_CONCRETE)
                p.set(x0 + bs + 2, y + 1, z0 + D, Material.WHITE_CONCRETE)
                if (random.nextInt(100) < 70) p.leaf(x0 + bs + 1, y + 1, z0 + D)
                if (random.nextInt(100) < 50) p.leaf(x0 + bs + 1, y + 3, z0 + D + 1)
            }
        }
    }

    private fun ivy(p: Place, random: Random, x0: Int, z0: Int, base: Int, height: Int) {
        val faces = arrayOf("south", "north", "east", "west")
        for (side in 0 until 4) {
            val cover = 45 + random.nextInt(50)
            val n = if (side < 2) W else D
            for (i in 0 until n) {
                if (random.nextInt(100) >= cover) continue
                val ox: Int
                val oz: Int
                val wx: Int
                val wz: Int
                when (side) {
                    0 -> { ox = x0 + i; oz = z0 - 1; wx = ox; wz = z0 }
                    1 -> { ox = x0 + i; oz = z0 + D; wx = ox; wz = z0 + D - 1 }
                    2 -> { ox = x0 - 1; oz = z0 + i; wx = x0; wz = oz }
                    else -> { ox = x0 + W; oz = z0 + i; wx = x0 + W - 1; wz = oz }
                }
                val top = base + height - random.nextInt(5)
                for (y in top downTo base + 1) {
                    if (random.nextInt(100) < 8) continue
                    if (p.isAir(ox, y, oz) && p.type(wx, y, wz).isSolid) p.vine(ox, y, oz, faces[side])
                }
            }
        }
        
        repeat(10) {
            val cx = x0 + random.nextInt(W)
            val cz = if (random.nextBoolean()) z0 - 1 else z0 + D + 1
            val cy = base + 3 + random.nextInt(maxOf(height - 3, 1))
            for (dx in -2..2) for (dy in -2..2) for (dz in -2..2) {
                if (dx * dx + dy * dy + dz * dz <= 5 && random.nextInt(100) < 85) p.leaf(cx + dx, cy + dy, cz + dz)
            }
        }
    }

    private fun roof(p: Place, random: Random, x0: Int, z0: Int, base: Int, height: Int, climate: Climate) {
        val ry = base + height
        for (dx in 0 until W) for (dz in 0 until D) {
            if (random.nextInt(100) < 50) {
                for (i in 1..1 + random.nextInt(3)) p.leaf(x0 + dx, ry + i, z0 + dz)
            }
        }
        if (random.nextInt(100) < 35 && p.type(x0 + 4, ry, z0 + 3).isSolid) {
            Trees.bigTree(p, random, x0 + 4, ry + 1, z0 + 3, climate, 7 + random.nextInt(5), 4)
        }
        if (random.nextInt(100) < 30 && p.type(x0 + 11, ry, z0 + 6).isSolid) {
            Chimney.build(p, random, x0 + 11, ry + 1, z0 + 6)
            Chimney.build(p, random, x0 + 12, ry + 1, z0 + 6)
        }
    }
}
