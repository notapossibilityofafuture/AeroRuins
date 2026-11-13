package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.entity.EntityType
import java.util.Random






object Tokyo3 {
    private const val SLOT = 13
    private const val AREA = 2 * SLOT

    fun build(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 1
        val z0 = bz + 1
        val low = Sites.flatBase(seed, gen, x0, z0, AREA, AREA, 1, 6) ?: return

        
        for (dx in 0 until AREA) for (dz in 0 until AREA) {
            for (y in low + 1..low + 8) p.set(x0 + dx, y, z0 + dz, Material.AIR)
            val rx = dx % SLOT
            val rz = dz % SLOT
            val inTower = rx in 2..10 && rz in 2..10
            val m = when {
                inTower -> Material.GRAY_CONCRETE
                (rx == 0 && dz % 4 < 2) || (rz == 0 && dx % 4 < 2) -> Material.WHITE_CONCRETE
                rx == 11 || rx == 1 || rz == 11 || rz == 1 -> Material.LIGHT_GRAY_CONCRETE
                else -> Material.BLACK_CONCRETE
            }
            p.set(x0 + dx, low, z0 + dz, m)
        }

        val pyramid = random.nextInt(100) < 35
        val towers = ArrayList<IntArray>()
        for (i in 0..1) for (j in 0..1) {
            val tx = x0 + i * SLOT + 2
            val tz = z0 + j * SLOT + 2
            if (pyramid && i == 1 && j == 1) {
                pyramid(p, tx, tz, low)
                continue
            }
            if (random.nextInt(100) < 15) continue 
            val w = 7 + random.nextInt(3)
            val d = 7 + random.nextInt(3)
            val h = 50 + random.nextInt(65)
            tower(p, random, tx, tz, w, d, low, h)
            towers.add(intArrayOf(tx, tz, w, d, h))
        }
        StructureDetails.plaza(p, random, x0, z0, AREA, AREA, low)
        if (towers.isNotEmpty() && Loot.rare(random, 30.0)) {
            val t = towers[random.nextInt(towers.size)]
            Loot.fill(p, random, t[0], t[1], t[2], t[3], 2, listOf(low + 1, low + 5), "tower", listOf(EntityType.ZOMBIE, EntityType.SKELETON), 1, 3)
        }
    }

    private fun tower(p: Place, random: Random, tx: Int, tz: Int, w: Int, d: Int, low: Int, h: Int) {
        for (dy in 0 until h) {
            val y = low + 1 + dy
            val band = dy % 4 in 1..2
            for (dx in 0 until w) for (dz in 0 until d) {
                val onX = dx == 0 || dx == w - 1
                val onZ = dz == 0 || dz == d - 1
                if (onX || onZ) {
                    val pilaster = (onX && onZ) || (onZ && dx % 3 == 0) || (onX && dz % 3 == 0)
                    val m = when {
                        pilaster -> Material.GRAY_CONCRETE
                        band -> if (random.nextInt(100) < 8) null else if (random.nextInt(100) < 65) Material.BLUE_STAINED_GLASS else Material.CYAN_STAINED_GLASS
                        else -> Material.LIGHT_GRAY_CONCRETE
                    }
                    if (m != null) p.set(tx + dx, y, tz + dz, m)
                } else if (dy % 4 == 0 && random.nextInt(100) >= 8) {
                    p.set(tx + dx, y, tz + dz, Material.LIGHT_GRAY_CONCRETE)
                }
            }
        }
        
        val roof = low + 1 + h
        for (dx in 0 until w) for (dz in 0 until d) {
            val edge = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1
            p.set(tx + dx, roof, tz + dz, Material.GRAY_CONCRETE)
            if (edge && random.nextInt(100) < 80) p.set(tx + dx, roof + 1, tz + dz, Material.LIGHT_GRAY_CONCRETE)
        }
        val cx = tx + w / 2
        val cz = tz + d / 2
        val ant = 7 + random.nextInt(8)
        for (i in 1..ant) p.put(cx, roof + i, cz, Material.END_ROD, "[facing=up]")
        p.set(cx, roof + ant + 1, cz, Material.RED_CONCRETE)

        
        Interiors.furnish(p, random, tx, tz, w, d, 2, (1 until h / 4 step 3).map { low + 1 + it * 4 }, 22)

        
        if (random.nextInt(100) < 55) {
            val hy = low + 1 + (h * (0.45 + random.nextDouble() * 0.3)).toInt()
            val r = w / 2 + 2 + random.nextInt(2)
            val ox = cx + random.nextInt(3) - 1
            val oz = cz + random.nextInt(3) - 1
            for (dx in -r..r) for (dy in -r..r) for (dz in -r..r) {
                if (dx * dx + dy * dy + dz * dz > r * r) continue
                val x = ox + dx
                val z = oz + dz
                if (x < tx || x >= tx + w || z < tz || z >= tz + d) continue
                if (random.nextInt(100) < 92) p.set(x, hy + dy, z, Material.AIR)
            }
        }
    }

    
    private fun pyramid(p: Place, tx: Int, tz: Int, low: Int) {
        for (k in 0..4) {
            val inset = k
            val size = 9 - 2 * k
            for (dx in 0 until size) for (dz in 0 until size) {
                for (dy in 0 until 2) {
                    val m = if (k == 2 && dy == 1) Material.BLUE_STAINED_GLASS else Material.BLACK_CONCRETE
                    p.set(tx + inset + dx, low + 1 + k * 2 + dy, tz + inset + dz, m)
                }
            }
        }
        p.set(tx + 4, low + 11, tz + 4, Material.SEA_LANTERN)
    }
}
