package com.example.aeroruins

import java.util.Random

object TrialChamber {
    private const val CELL = 3600
    private const val CHANCE = 1
    private val structure = SchematicStructure("/trial-chamber.schem", 110, 85, 91)

    fun center(seed: Long, gx: Int, gz: Int): IntArray? {
        val h = mix(seed, gx, gz, 240)
        if (h % 100 >= CHANCE) return null
        val cx = gx * CELL + 180 + (h ushr 6) % (CELL - 360)
        val cz = gz * CELL + 180 + (h ushr 16) % (CELL - 360)
        if (zoneAt(seed, cx, cz) != Zone.JUNGLE) return null
        val surface = Terrain.heightAt(seed, cx, cz)
        if (surface < SEA_LEVEL + 8) return null
        return intArrayOf(cx, cz, surface - 79)
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
}
