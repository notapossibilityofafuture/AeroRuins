package com.example.aeroruins

import org.bukkit.Material
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin


object Scatter {
    private fun ground(seed: Long, p: Place, gen: AeroGenerator, x: Int, z: Int): Int? {
        if (!gen.usesTerrain(seed, x, z)) return null
        val h = gen.heightAt(seed, x, z)
        if (h < SEA_LEVEL + 2 || p.type(x, h, z) != Material.GRASS_BLOCK || !p.isAir(x, h + 1, z)) return null
        return h
    }

    fun populate(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val mx = bx + 8
        val mz = bz + 8
        if (!gen.usesTerrain(seed, mx, mz) || zoneAt(seed, mx, mz) != Zone.JUNGLE) return
        val c = ClimateMap.at(seed, mx, mz)
        val r = random.nextInt(100)
        val x = bx + 3 + random.nextInt(10)
        val z = bz + 3 + random.nextInt(10)
        when {
            r < 6 -> boulder(seed, random, p, gen, x, z)
            r < 11 -> fallenLog(seed, random, p, gen, x, z, c)
            r < 14 -> fairyRing(seed, random, p, gen, x, z)
        }
    }

    private fun boulder(seed: Long, random: Random, p: Place, gen: AeroGenerator, x: Int, z: Int) {
        val h = ground(seed, p, gen, x, z) ?: return
        val r = 1 + random.nextInt(2)
        val mats = arrayOf(Material.MOSSY_COBBLESTONE, Material.COBBLESTONE, Material.ANDESITE, Material.STONE, Material.MOSSY_COBBLESTONE)
        for (dx in -r..r) for (dz in -r..r) for (dy in 0..r) {
            if (dx * dx + dz * dz + dy * dy * 2 <= r * r + 1) p.set(x + dx, h + 1 + dy, z + dz, mats[random.nextInt(mats.size)])
        }
    }

    private fun fallenLog(seed: Long, random: Random, p: Place, gen: AeroGenerator, x: Int, z: Int, c: Climate) {
        val alongX = random.nextBoolean()
        val len = 4 + random.nextInt(4)
        for (i in 0 until len) {
            val lx = if (alongX) x + i else x
            val lz = if (alongX) z else z + i
            val h = ground(seed, p, gen, lx, lz) ?: continue
            p.put(lx, h + 1, lz, c.log, if (alongX) "[axis=x]" else "[axis=z]")
            if (random.nextInt(100) < 40) p.set(lx, h + 2, lz, Material.MOSS_CARPET)
            if (random.nextInt(100) < 25) {
                val mx = if (alongX) lx else lx + 1
                val mz = if (alongX) lz + 1 else lz
                val mh = ground(seed, p, gen, mx, mz)
                if (mh != null) p.set(mx, mh + 1, mz, if (random.nextBoolean()) Material.RED_MUSHROOM else Material.BROWN_MUSHROOM)
            }
        }
    }

    
    private fun fairyRing(seed: Long, random: Random, p: Place, gen: AeroGenerator, x: Int, z: Int) {
        val ring = arrayOf(Material.RED_MUSHROOM, Material.OXEYE_DAISY, Material.BROWN_MUSHROOM, Material.LILY_OF_THE_VALLEY)
        for (k in 0 until 24) {
            val a = k * Math.PI * 2 / 24
            val rx = x + Math.round(cos(a) * 4).toInt()
            val rz = z + Math.round(sin(a) * 4).toInt()
            val h = ground(seed, p, gen, rx, rz) ?: continue
            p.set(rx, h + 1, rz, ring[k % ring.size])
        }
        val h = ground(seed, p, gen, x, z) ?: return
        p.set(x, h, z, Material.MOSS_BLOCK)
        p.set(x, h + 1, z, Material.FLOWERING_AZALEA)
    }
}
