package com.example.aeroruins

import org.bukkit.Material
import java.util.Random
import kotlin.math.abs

object AncientCity {
    private const val CELL = 5200
    private const val CHANCE = 1
    private const val HALF = 34

    fun center(seed: Long, gx: Int, gz: Int): IntArray? {
        val h = mix(seed, gx, gz, 250)
        if (h % 100 >= CHANCE) return null
        val cx = gx * CELL + 180 + (h ushr 6) % (CELL - 360)
        val cz = gz * CELL + 180 + (h ushr 16) % (CELL - 360)
        if (zoneAt(seed, cx, cz) != Zone.JUNGLE) return null
        val surface = Terrain.heightAt(seed, cx, cz)
        if (surface < SEA_LEVEL + 12) return null
        return intArrayOf(cx, cz, -34)
    }

    fun populate(seed: Long, random: Random, p: Place, bx: Int, bz: Int): Boolean {
        val c = center(seed, Math.floorDiv(bx, CELL), Math.floorDiv(bz, CELL)) ?: return false
        val x0 = c[0] - HALF
        val z0 = c[1] - HALF
        if (bx + 15 < x0 || bx >= x0 + HALF * 2 + 1 ||
            bz + 15 < z0 || bz >= z0 + HALF * 2 + 1
        ) return false
        build(p, random, x0, c[1] - HALF)
        return true
    }

    private fun build(p: Place, random: Random, x0: Int, z0: Int) {
        val floor = z0
        for (x in x0 - 2..x0 + HALF * 2 + 2) for (z in floor - 2..floor + HALF * 2 + 2) {
            if ((x - x0) % 4 == 0 || (z - floor) % 4 == 0) p.set(x, -35, z, Material.DEEPSLATE_BRICKS)
        }
        for (x in x0 + 2..x0 + HALF * 2 - 2) {
            p.set(x, -34, floor + HALF, Material.POLISHED_DEEPSLATE)
            p.set(x, -33, floor + HALF, Material.SOUL_LANTERN)
        }
        for (z in floor + 2..floor + HALF * 2 - 2) {
            p.set(x0 + HALF, -34, z, Material.POLISHED_DEEPSLATE)
            p.set(x0 + HALF, -33, z, Material.SOUL_LANTERN)
        }
        for (dx in listOf(-25, -13, 13, 25)) for (dz in listOf(-25, -13, 13, 25)) {
            building(p, random, x0 + HALF + dx, floor + HALF + dz)
        }
        for (dx in -3..3) for (dz in -3..3) {
            val ring = abs(dx) == 3 || abs(dz) == 3
            p.set(x0 + HALF + dx, -34, floor + HALF + dz, if (ring) Material.REINFORCED_DEEPSLATE else Material.SCULK)
        }
    }

    private fun building(p: Place, random: Random, cx: Int, cz: Int) {
        for (dx in -4..4) for (dz in -4..4) {
            val edge = dx == -4 || dx == 4 || dz == -4 || dz == 4
            if (edge) {
                for (y in -34..-28) p.set(cx + dx, y, cz + dz, if (random.nextInt(5) == 0) Material.POLISHED_DEEPSLATE else Material.DEEPSLATE_BRICKS)
            } else {
                p.set(cx + dx, -34, cz + dz, Material.DEEPSLATE_TILES)
            }
        }
        p.set(cx, -33, cz, Material.SOUL_LANTERN)
        p.set(cx, -32, cz, Material.SCULK_SENSOR)
        if (random.nextBoolean()) p.set(cx + 2, -33, cz, Material.CHEST)
    }
}
