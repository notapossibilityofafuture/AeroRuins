package com.example.aeroruins

import org.bukkit.Material
import java.util.Random

object Sites {
    


    fun flatBase(seed: Long, gen: AeroGenerator, x0: Int, z0: Int, w: Int, d: Int, margin: Int, maxDelta: Int, inVillage: Boolean = false): Int? {
        if (!Roads.footprintClear(seed, x0, z0, w, d, 3)) return null 
        if (!Layers.tearClear(seed, x0, z0, w, d)) return null 
        if (!inVillage && Village.near(seed, x0, z0, w, d)) return null 
        for (x in intArrayOf(x0 - margin, x0 + w - 1 + margin)) {
            for (z in intArrayOf(z0 - margin, z0 + d - 1 + margin)) {
                if (zoneAt(seed, x, z) != Zone.JUNGLE) return null
            }
        }
        val pts = listOf(
            x0 to z0, (x0 + w - 1) to z0, x0 to (z0 + d - 1), (x0 + w - 1) to (z0 + d - 1), (x0 + w / 2) to (z0 + d / 2)
        )
        val hs = pts.map { gen.heightAt(seed, it.first, it.second) }
        val low = hs.min()
        if (low < SEA_LEVEL + 2 || hs.max() - low > maxDelta) return null
        return low
    }
}


object Chimney {
    fun build(p: Place, random: Random, x: Int, y: Int, z: Int) {
        val h = 10 + random.nextInt(7)
        for (i in 0 until h) {
            p.set(x, y + i, z, if ((i / 3) % 2 == 0) Material.WHITE_CONCRETE else Material.RED_CONCRETE)
        }
        p.set(x, y + h, z, Material.BLACK_CONCRETE)
    }
}
