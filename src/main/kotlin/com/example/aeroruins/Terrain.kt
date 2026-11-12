package com.example.aeroruins

import org.bukkit.util.noise.SimplexOctaveGenerator
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.pow


const val HIGH_Y = 100





object Terrain {
    private class Gens(seed: Long) {
        val cont = SimplexOctaveGenerator(Random(seed xor 0x7A1L), 3).also { it.setScale(0.0007) }
        val rough = SimplexOctaveGenerator(Random(seed xor 0x7B2L), 2).also { it.setScale(0.0022) }
        val mountains = SimplexOctaveGenerator(Random(seed xor 0x7C3L), 2).also { it.setScale(0.0011) }
        val ridge = SimplexOctaveGenerator(Random(seed xor 0x7D4L), 4).also { it.setScale(0.0016) }
        val detail = SimplexOctaveGenerator(Random(seed xor 0x7E5L), 5).also { it.setScale(0.011) }
        val riverOn = SimplexOctaveGenerator(Random(seed xor 0x7F6L), 2).also { it.setScale(0.0009) }
        val river = SimplexOctaveGenerator(Random(seed xor 0x807L), 2).also { it.setScale(0.0026) }
        val lake = SimplexOctaveGenerator(Random(seed xor 0x918L), 2).also { it.setScale(0.0045) }
    }

    private val gens = ConcurrentHashMap<Long, Gens>()

    private fun smooth(t: Double): Double {
        val c = t.coerceIn(0.0, 1.0)
        return c * c * (3.0 - 2.0 * c)
    }

    
    fun natural(seed: Long, x: Int, z: Int): Double {
        val g = gens.computeIfAbsent(seed) { Gens(it) }
        val fx = x.toDouble()
        val fz = z.toDouble()

        
        val c = g.cont.noise(fx, fz, 0.5, 0.5, true)
        val r = g.rough.noise(fx, fz, 0.5, 0.5, true)
        val d = g.detail.noise(fx, fz, 0.5, 0.5, true)
        val amp = 2.5 + 9.5 * smooth((r + 0.25) / 0.9)
        var h = SEA_LEVEL + 9 + c * 8 + d * amp

        
        val region = smooth((g.mountains.noise(fx, fz, 0.5, 0.5, true) - 0.12) / 0.35)
        if (region > 0.0) {
            val crest = 1.0 - abs(g.ridge.noise(fx, fz, 0.5, 0.5, true))
            h += region * (7.0 + 58.0 * crest.pow(1.9))
        }
        h = maxOf(h, SEA_LEVEL + 2.0)

        
        if (region < 0.25) {
            val on = smooth((g.riverOn.noise(fx, fz, 0.5, 0.5, true) - 0.30) / 0.2)
            if (on > 0.0) {
                val rv = abs(g.river.noise(fx, fz, 0.5, 0.5, true))
                val width = 0.012 * on
                if (rv < width * 3.0) {
                    val t = smooth((rv - width) / (width * 2.0))
                    val bed = SEA_LEVEL - 2.0
                    h = bed + (h - bed) * t
                }
            }
            val ln = g.lake.noise(fx, fz, 0.5, 0.5, true)
            if (ln > 0.62) h -= smooth((ln - 0.62) / 0.12) * 16.0
        }

        return h
    }

    fun heightAt(seed: Long, x: Int, z: Int): Int {
        var h = natural(seed, x, z)
        
        val blend = domeBlend(seed, x, z)
        h = if (blend < 1.0) FLOOR + (h - FLOOR) * blend else Roads.grade(seed, x, z, h)
        return h.toInt()
    }
}
