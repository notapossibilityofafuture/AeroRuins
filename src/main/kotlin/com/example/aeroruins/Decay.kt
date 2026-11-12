package com.example.aeroruins

import org.bukkit.Material
import java.util.Random


object Decay {
    



    fun ruin(p: Place, random: Random, x0: Int, z0: Int, w: Int, d: Int, y0: Int, y1: Int, pct: Int) {
        for (x in x0 - 1..x0 + w) for (z in z0 - 1..z0 + d) for (y in y0..y1) {
            val t = p.type(x, y, z)
            if (t == Material.AIR || !t.isSolid) continue
            val roofish = y >= y0 + 3
            val r = random.nextInt(100)
            when {
                r < pct + (if (roofish) 10 else 0) -> p.set(x, y, z, Material.AIR)
                r < pct + 12 && !roofish -> p.set(x, y, z, Material.MOSS_BLOCK)
                r < pct + 15 && p.isAir(x, y + 1, z) -> p.set(x, y + 1, z, Material.COBWEB)
            }
        }
        
        repeat(4 + w * d / 20) {
            val x = x0 - 1 + random.nextInt(w + 2)
            val z = z0 - 1 + random.nextInt(d + 2)
            for (y in y1 downTo y0) {
                if (p.type(x, y, z).isSolid) {
                    if (p.isAir(x, y + 1, z)) p.leaf(x, y + 1, z)
                    break
                }
            }
        }
    }
}
