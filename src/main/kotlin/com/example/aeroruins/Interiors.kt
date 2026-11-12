package com.example.aeroruins

import org.bukkit.Material
import java.util.Random


object Interiors {
    private val BEDS = listOf(Material.WHITE_BED, Material.LIGHT_BLUE_BED, Material.RED_BED, Material.GREEN_BED, Material.GRAY_BED)
    private val POTS = listOf(Material.POTTED_FERN, Material.POTTED_AZURE_BLUET, Material.POTTED_BLUE_ORCHID, Material.POTTED_CORNFLOWER, Material.POTTED_OXEYE_DAISY)
    private val CHAIRS = listOf(Material.SPRUCE_STAIRS, Material.OAK_STAIRS, Material.DARK_OAK_STAIRS)
    private val CARPETS = listOf(Material.WHITE_CARPET, Material.LIGHT_BLUE_CARPET, Material.MOSS_CARPET, Material.GRAY_CARPET)
    private val DIRS = listOf("north", "south", "east", "west")

    private fun free(p: Place, x: Int, y: Int, z: Int): Boolean = p.isAir(x, y, z)
    private fun floorOk(p: Place, x: Int, y: Int, z: Int): Boolean = p.type(x, y - 1, z).isSolid

    



    fun furnish(p: Place, random: Random, x0: Int, z0: Int, w: Int, d: Int, margin: Int, floors: List<Int>, density: Int) {
        val aw = w - 2 * margin
        val ad = d - 2 * margin
        if (aw < 3 || ad < 3) return
        val count = maxOf(2, aw * ad / density)
        for (fy in floors) {
            if (random.nextInt(100) >= 80) continue 
            val y = fy + 1
            repeat(count) {
                val x = x0 + margin + random.nextInt(aw)
                val z = z0 + margin + random.nextInt(ad)
                if (!free(p, x, y, z) || !floorOk(p, x, y, z)) return@repeat
                when (random.nextInt(16)) {
                    0, 1 -> bed(p, random, x, y, z)
                    2, 3 -> table(p, random, x, y, z)
                    4, 5 -> shelf(p, x, y, z)
                    6 -> sofa(p, random, x, y, z)
                    7 -> p.set(x, y, z, Material.CRAFTING_TABLE)
                    8 -> p.put(x, y, z, Material.FURNACE, "[facing=${DIRS[random.nextInt(4)]}]")
                    9 -> p.put(x, y, z, Material.BARREL, "[facing=up]")
                    10 -> p.set(x, y, z, POTS[random.nextInt(POTS.size)])
                    11 -> p.put(x, y, z, Material.CANDLE, "[candles=${1 + random.nextInt(4)},lit=true]")
                    12 -> p.set(x, y, z, Material.LANTERN)
                    13 -> for (dx in 0..1) for (dz in 0..1) {
                        if (free(p, x + dx, y, z + dz) && floorOk(p, x + dx, y, z + dz)) p.set(x + dx, y, z + dz, CARPETS[random.nextInt(CARPETS.size)])
                    }
                    14 -> p.put(x, y, z, Material.LECTERN, "[facing=${DIRS[random.nextInt(4)]}]")
                    else -> p.set(x, y, z, Material.COBWEB)
                }
            }
        }
    }

    private fun bed(p: Place, random: Random, x: Int, y: Int, z: Int) {
        
        if (!free(p, x, y, z - 1) || !floorOk(p, x, y, z - 1)) return
        val bed = BEDS[random.nextInt(BEDS.size)]
        p.put(x, y, z, bed, "[facing=north,part=foot]")
        p.put(x, y, z - 1, bed, "[facing=north,part=head]")
    }

    private fun table(p: Place, random: Random, x: Int, y: Int, z: Int) {
        p.set(x, y, z, Material.OAK_FENCE)
        p.set(x, y + 1, z, Material.OAK_PRESSURE_PLATE)
        if (random.nextInt(100) < 50) p.set(x, y + 2, z, Material.LANTERN)
        
        val spots = listOf(Triple(1, 0, "east"), Triple(-1, 0, "west"), Triple(0, 1, "south"), Triple(0, -1, "north")) 
        for ((dx, dz, face) in spots) {
            if (random.nextInt(100) < 55 && free(p, x + dx, y, z + dz) && floorOk(p, x + dx, y, z + dz)) {
                p.put(x + dx, y, z + dz, CHAIRS[random.nextInt(CHAIRS.size)], "[facing=$face]")
            }
        }
    }

    private fun shelf(p: Place, x: Int, y: Int, z: Int) {
        p.set(x, y, z, Material.BOOKSHELF)
        if (free(p, x, y + 1, z)) p.set(x, y + 1, z, Material.BOOKSHELF)
    }

    private fun sofa(p: Place, random: Random, x: Int, y: Int, z: Int) {
        val m = CHAIRS[random.nextInt(CHAIRS.size)]
        for (dx in 0..2) {
            if (!free(p, x + dx, y, z) || !floorOk(p, x + dx, y, z)) return
        }
        for (dx in 0..2) p.put(x + dx, y, z, m, "[facing=south]")
    }
}
