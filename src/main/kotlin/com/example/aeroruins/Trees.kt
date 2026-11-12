package com.example.aeroruins

import org.bukkit.Material
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin





object Trees {
    fun logFor(c: Climate): Material = c.log

    fun blob(p: Place, random: Random, cx: Int, cy: Int, cz: Int, r: Int, c: Climate, special: Material? = null) {
        for (dx in -r..r) for (dy in -r..r) for (dz in -r..r) {
            if (dx * dx + dy * dy * 2 + dz * dz > r * r) continue
            if (random.nextInt(100) >= (if (c.bare) 16 else 78)) continue
            val x = cx + dx
            val y = cy + dy
            val z = cz + dz
            if (special != null && random.nextInt(100) < 6 && p.isAir(x, y, z)) p.set(x, y, z, special)
            else p.leaf(x, y, z, Material.OAK_LEAVES)
        }
        
        val low = (r * 0.7).toInt()
        repeat(r * 2) {
            val dx = random.nextInt(2 * r + 1) - r
            val dz = random.nextInt(2 * r + 1) - r
            val x = cx + dx
            val z = cz + dz
            if (dx * dx + dz * dz <= r * r / 2 && p.type(x, cy - low, z) == Material.OAK_LEAVES) {
                p.hang(x, cy - low - 1, z, 2 + random.nextInt(5))
            }
        }
    }

    
    fun bigTree(p: Place, random: Random, x: Int, y: Int, z: Int, c: Climate, h: Int, r: Int, special: Material? = null) {
        val trunk = logFor(c)
        for (i in -2 until h) {
            p.set(x, y + i, z, trunk)
            p.set(x + 1, y + i, z, trunk)
            p.set(x, y + i, z + 1, trunk)
            p.set(x + 1, y + i, z + 1, trunk)
        }
        for ((dx, dz) in listOf(-1 to 0, 2 to 1, 0 to 2, 1 to -1)) {
            p.set(x + dx, y, z + dz, trunk)
            if (random.nextBoolean()) p.set(x + dx, y + 1, z + dz, trunk)
        }
        repeat(4 + random.nextInt(3)) {
            val ang = random.nextDouble() * 2 * PI
            val len = r + random.nextInt(3)
            var yy = y + (h * (0.5 + random.nextDouble() * 0.4)).toInt()
            var fx = x + 0.5
            var fz = z + 0.5
            for (s in 1..len) {
                fx += cos(ang)
                fz += sin(ang)
                if (s % 2 == 0) yy++
                p.set(fx.roundToInt(), yy, fz.roundToInt(), trunk)
            }
            blob(p, random, fx.roundToInt(), yy + 1, fz.roundToInt(), maxOf(2, r - 2), c, special)
        }
        blob(p, random, x, y + h, z, r, c, special)
    }
}
