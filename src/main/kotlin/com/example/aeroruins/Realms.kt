package com.example.aeroruins

import org.bukkit.util.noise.SimplexOctaveGenerator
import java.util.Random
import java.util.concurrent.ConcurrentHashMap


enum class Realm { WILD, GARDEN, FLOWERS, NEIGHBORHOOD }

object Realms {
    private class Gens(seed: Long) {
        val a = SimplexOctaveGenerator(Random(seed xor 0xA11L), 2).also { it.setScale(0.0006) }
        val b = SimplexOctaveGenerator(Random(seed xor 0xB22L), 2).also { it.setScale(0.0007) }
    }

    private val gens = ConcurrentHashMap<Long, Gens>()

    private class Memo { var seed = 0L; var x = Int.MIN_VALUE; var z = Int.MIN_VALUE; var value = Realm.WILD }
    private val memo = ThreadLocal.withInitial { Memo() }

    fun at(seed: Long, x: Int, z: Int): Realm {
        val m = memo.get()
        if (m.seed == seed && m.x == x && m.z == z) return m.value
        val g = gens.computeIfAbsent(seed) { Gens(it) }
        val fx = x.toDouble()
        val fz = z.toDouble()
        val v = g.a.noise(fx, fz, 0.5, 0.5, true)
        val value = if (v < 0.38) {
            Realm.WILD
        } else {
            val w = g.b.noise(fx, fz, 0.5, 0.5, true)
            if (w < -0.05) Realm.GARDEN else if (w < 0.35) Realm.FLOWERS else Realm.NEIGHBORHOOD
        }
        m.seed = seed; m.x = x; m.z = z; m.value = value
        return value
    }

    
    fun find(seed: Long, x: Int, z: Int, target: Realm): IntArray? {
        if (at(seed, x, z) == target) return intArrayOf(x, z)
        val step = 96
        var r = step
        while (r <= 6000) {
            var best: IntArray? = null
            var bd = Long.MAX_VALUE
            var i = -r
            while (i <= r) {
                val pts = arrayOf(intArrayOf(x + i, z - r), intArrayOf(x + i, z + r), intArrayOf(x - r, z + i), intArrayOf(x + r, z + i))
                for (pt in pts) {
                    if (at(seed, pt[0], pt[1]) != target) continue
                    val dx = (pt[0] - x).toLong()
                    val dz = (pt[1] - z).toLong()
                    if (dx * dx + dz * dz < bd) {
                        bd = dx * dx + dz * dz
                        best = pt
                    }
                }
                i += step
            }
            if (best != null) return best
            r += step
        }
        return null
    }
}
