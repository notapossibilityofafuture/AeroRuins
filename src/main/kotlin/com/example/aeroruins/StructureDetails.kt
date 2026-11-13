package com.example.aeroruins

import org.bukkit.Material
import java.util.Random

object StructureDetails {
    fun facade(
        p: Place,
        random: Random,
        x0: Int,
        z0: Int,
        w: Int,
        d: Int,
        base: Int,
        height: Int,
        modern: Boolean
    ) {
        val trim = if (modern) Material.SMOOTH_QUARTZ else Material.STRIPPED_OAK_LOG
        val light = if (modern) Material.SEA_LANTERN else Material.LANTERN
        for (x in intArrayOf(x0, x0 + w - 1)) {
            for (y in base + 1..base + height) {
                if (y % 4 == 1 || y == base + height) p.set(x, y, z0, trim)
            }
        }
        for (x in x0 + 1 until x0 + w - 1) {
            if (random.nextInt(100) < 70 && p.isAir(x, base + 1, z0 - 1)) {
                p.set(x, base + 1, z0 - 1, Material.FLOWER_POT)
                p.set(x, base + 2, z0 - 1, if (modern) Material.FERN else Material.POTTED_OAK_SAPLING)
            }
        }
        if (p.isAir(x0 - 1, base + 3, z0 + d - 1)) p.set(x0 - 1, base + 3, z0 + d - 1, light)
        if (p.isAir(x0 + w, base + 3, z0 + d - 1)) p.set(x0 + w, base + 3, z0 + d - 1, light)
    }

    fun roofline(p: Place, x0: Int, z0: Int, w: Int, d: Int, y: Int, modern: Boolean) {
        val material = if (modern) Material.SMOOTH_QUARTZ_SLAB else Material.MOSS_CARPET
        for (x in x0 - 1..x0 + w) {
            p.set(x, y, z0 - 1, material)
            p.set(x, y, z0 + d, material)
        }
        for (z in z0 until z0 + d) {
            p.set(x0 - 1, y, z, material)
            p.set(x0 + w, y, z, material)
        }
    }

    fun plaza(p: Place, random: Random, x0: Int, z0: Int, w: Int, d: Int, y: Int) {
        val border = Material.POLISHED_ANDESITE
        for (x in x0 until x0 + w) for (z in z0 until z0 + d) {
            if (x == x0 || z == z0 || x == x0 + w - 1 || z == z0 + d - 1) p.set(x, y, z, border)
        }
        repeat(2) {
            val x = x0 + 2 + random.nextInt(maxOf(w - 4, 1))
            val z = z0 + 2 + random.nextInt(maxOf(d - 4, 1))
            if (p.type(x, y, z) == Material.AIR) p.set(x, y, z, Material.MOSS_BLOCK)
            if (p.isAir(x, y + 1, z)) p.set(x, y + 1, z, Material.FLOWER_POT)
        }
    }
}
