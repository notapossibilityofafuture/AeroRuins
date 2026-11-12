package com.example.aeroruins

import org.bukkit.Material
import java.util.Random


object Neighborhood {
    fun lot(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        when (random.nextInt(100)) {
            in 0..79 -> {
                val h = Suburb.house(seed, random, p, bx, bz, gen, decay = 28) ?: return 
                val x0 = h[0]
                val z0 = h[1]
                val w = h[2]
                val d = h[3]
                fence(p, random, seed, gen, x0 - 2, z0 - 2, w + 4, d + 7, x0 + w / 2 - 1..x0 + w / 2 + 2)
                
                val mx = x0 + w / 2 + 3
                val mz = z0 + d + 5
                val my = gen.heightAt(seed, mx, mz)
                if (random.nextInt(100) < 55) { 
                    p.set(mx, my + 1, mz, Material.OAK_FENCE)
                    p.put(mx, my + 2, mz, Material.BARREL, "[facing=south]")
                }
                
                for (z in z0 + d until z0 + d + 6) {
                    val gy = gen.heightAt(seed, x0 + w - 1, z)
                    for (a in 0..2) p.set(x0 + w - 2 + a, gy, z, Material.LIGHT_GRAY_CONCRETE)
                }
                if (random.nextInt(100) < 40) City.car(p, random, x0 + w - 2, gen.heightAt(seed, x0 + w - 2, z0 + d + 1) + 1, z0 + d + 1, false)
            }
            in 80..90 -> City.park(seed, random, p, bx, bz, gen)
            in 91..95 -> City.busStop(seed, random, p, bx, bz, gen)
            else -> City.shop(seed, random, p, bx, bz, gen)
        }
    }

    
    private fun fence(p: Place, random: Random, seed: Long, gen: AeroGenerator, x0: Int, z0: Int, w: Int, d: Int, gate: IntRange) {
        fun perim(u: Int, v: Int): Boolean {
            if (u !in 0 until w || v !in 0 until d) return false
            if (!(u == 0 || u == w - 1 || v == 0 || v == d - 1)) return false
            return !(v == d - 1 && (x0 + u) in gate)
        }
        for (u in 0 until w) for (v in 0 until d) {
            if (!perim(u, v)) continue
            val dirs = ArrayList<String>()
            if (perim(u + 1, v)) dirs.add("east")
            if (perim(u - 1, v)) dirs.add("west")
            if (perim(u, v + 1)) dirs.add("south")
            if (perim(u, v - 1)) dirs.add("north")
            val x = x0 + u
            val z = z0 + v
            val gy = gen.heightAt(seed, x, z)
            if (p.type(x, gy + 1, z) != Material.AIR) continue
            if (random.nextInt(100) < 40) continue 
            if (dirs.isEmpty()) p.set(x, gy + 1, z, Material.OAK_FENCE)
            else p.put(x, gy + 1, z, Material.OAK_FENCE, "[" + dirs.joinToString(",") { "$it=true" } + "]")
        }
    }
}
