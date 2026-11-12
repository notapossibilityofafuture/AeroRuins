package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.entity.EntityType
import java.util.Random


object DomeFlora {
    private val flowers = listOf(Material.CORNFLOWER, Material.BLUE_ORCHID, Material.AZURE_BLUET, Material.OXEYE_DAISY)

    fun populate(seed: Long, random: Random, p: Place, bx: Int, bz: Int) {
        repeat(10) {
            val x = bx + random.nextInt(16)
            val z = bz + random.nextInt(16)
            val dome = domeAt(seed, x, z) ?: return@repeat
            if (dome.d < 14 || dome.d > dome.r - 10 || onDomePath(dome)) return@repeat
            if (p.type(x, FLOOR, z) != Material.GRASS_BLOCK || !p.isAir(x, FLOOR + 1, z)) return@repeat

            val extra = random.nextInt(100)
            if (Loot.rare(random, 0.4)) {
                Loot.chestAt(p, random, x, FLOOR + 1, z, "dome")
                return@repeat
            }
            if (extra < 6) {
                Loot.mobAt(p, x, FLOOR + 1, z, if (random.nextBoolean()) EntityType.ALLAY else EntityType.IRON_GOLEM)
                return@repeat
            }

            when (random.nextInt(10)) {
                in 0..1 -> palm(p, random, x, FLOOR + 1, z)
                in 2..3 -> {
                    if (p.type(x + 1, FLOOR, z + 1) == Material.GRASS_BLOCK) {
                        Trees.bigTree(p, random, x, FLOOR + 1, z, Climate.OLD_OAK_FOREST, 6 + random.nextInt(4), 3 + random.nextInt(2), Material.WARPED_WART_BLOCK)
                    }
                }
                in 4..6 -> p.set(x, FLOOR + 1, z, flowers[random.nextInt(flowers.size)])
                in 7..8 -> p.set(x, FLOOR + 1, z, Material.FERN)
                else -> p.set(x, FLOOR + 1, z, Material.SHORT_GRASS)
            }
        }
    }

    private fun palm(p: Place, random: Random, x: Int, y: Int, z: Int) {
        val h = 7 + random.nextInt(3)
        for (i in 0 until h) p.set(x, y + i, z, Material.JUNGLE_LOG)
        val top = y + h - 1
        p.leaf(x, top + 1, z)
        val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)
        for ((dx, dz) in dirs) {
            for (i in 1..3) p.leaf(x + dx * i, if (i == 3) top - 1 else top, z + dz * i)
        }
    }
}
