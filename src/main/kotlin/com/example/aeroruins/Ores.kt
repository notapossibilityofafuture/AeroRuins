package com.example.aeroruins

import org.bukkit.Material
import java.util.Random


object Ores {
    private class Ore(val stone: Material?, val deep: Material?, val minY: Int, val maxY: Int, val tries: Int, val size: Int)

    private val ORES = listOf(
        Ore(Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, 0, 120, 20, 14),
        Ore(Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, -60, 70, 18, 10),
        Ore(Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, -16, 70, 12, 11),
        Ore(Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, -60, 30, 6, 9),
        Ore(Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, -60, 16, 10, 9),
        Ore(Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, -60, 50, 4, 8),
        Ore(Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE, -60, 10, 6, 7),
        Ore(Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, -40, 60, 1, 1),
        Ore(null, Material.NETHERRACK, -52, -20, 2, 3) 
    )

    
    private fun biomeFactor(o: Ore, c: Climate): Double {
        return when (o.stone) {
            Material.COAL_ORE -> if (c.gloom) 1.5 else 1.0
            Material.IRON_ORE -> 1.0
            Material.COPPER_ORE, Material.GOLD_ORE -> if (c.warm) 1.5 else 1.0
            Material.LAPIS_ORE -> if (c.cold) 2.0 else 1.0
            Material.DIAMOND_ORE -> if (c.gloom) 1.3 else 1.0
            Material.EMERALD_ORE -> 1.0
            else -> 1.0
        }
    }

    fun populate(random: Random, p: Place, bx: Int, bz: Int, minHeight: Int, climate: Climate) {
        for (o in ORES) {
            val lo = maxOf(o.minY, minHeight + 6)
            if (o.maxY <= lo) continue
            val tries = maxOf(1, Math.round(o.tries * biomeFactor(o, climate)).toInt())
            repeat(tries) {
                var x = bx + random.nextInt(16)
                var y = lo + random.nextInt(o.maxY - lo + 1)
                var z = bz + random.nextInt(16)
                for (i in 0 until o.size) {
                    val m: Material? = when (p.type(x, y, z)) {
                        Material.STONE -> o.stone
                        Material.DEEPSLATE -> o.deep
                        else -> null
                    }
                    if (m != null) p.set(x, y, z, m)
                    when (random.nextInt(6)) {
                        0 -> x++
                        1 -> x--
                        2 -> y++
                        3 -> y--
                        4 -> z++
                        else -> z--
                    }
                }
            }
        }
    }
}
