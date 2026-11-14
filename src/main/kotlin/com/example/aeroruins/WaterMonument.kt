package com.example.aeroruins

import java.util.Random

object WaterMonument {
    private const val CELL = 3000
    private const val CHANCE = 1
    private val structure = SchematicStructure("/19709.schem", 59, 30, 61)

    fun center(seed: Long, gx: Int, gz: Int): IntArray? {
        val h = mix(seed, gx, gz, 230)
        if (h % 100 >= CHANCE) return null
        val cx = gx * CELL + 120 + (h ushr 6) % (CELL - 240)
        val cz = gz * CELL + 120 + (h ushr 16) % (CELL - 240)
        val x0 = cx - structure.width / 2
        val z0 = cz - structure.length / 2
        if (!isOcean(seed, x0, z0, structure.width, structure.length)) return null
        return intArrayOf(cx, cz, 38 + (h ushr 12) % 8)
    }

    fun populate(seed: Long, random: Random, p: Place, bx: Int, bz: Int): Boolean {
        val c = center(seed, Math.floorDiv(bx, CELL), Math.floorDiv(bz, CELL)) ?: return false
        val x0 = c[0] - structure.width / 2
        val z0 = c[1] - structure.length / 2
        if (bx + 15 < x0 || bx >= x0 + structure.width ||
            bz + 15 < z0 || bz >= z0 + structure.length
        ) return false
        structure.place(p, random, x0, c[2], z0)
        return true
    }

    private fun isOcean(seed: Long, x0: Int, z0: Int, width: Int, length: Int): Boolean {
        for (x in x0 until x0 + width step 4) for (z in z0 until z0 + length step 4) {
            if (Terrain.heightAt(seed, x, z) >= SEA_LEVEL) return false
        }
        return true
    }
}
