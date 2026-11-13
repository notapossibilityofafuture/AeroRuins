package com.example.aeroruins

import org.bukkit.Material
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin


class RoadHit(val dist: Double, val y: Int, val half: Int, val blend: Int, val local: Boolean, val axis: Int, val along: Int)







object Roads {
    private const val STEP = 16
    private const val HW_HIGH = 3
    private const val BL_HIGH = 7
    private const val HW_LOCAL = 2
    private const val BL_LOCAL = 4

    private val samples = ConcurrentHashMap<Long, Int>()
    private val townOk = ConcurrentHashMap<Long, Boolean>()

    private fun spacing(): Int = Settings.roadSpacing

    private fun offset(seed: Long, axis: Int, k: Int): Int = mix(seed, k, axis, 120) % 241 - 120

    
    private fun center(seed: Long, axis: Int, k: Int, t: Int): Double {
        val p1 = (mix(seed, k, axis, 123) % 628) / 100.0
        val p2 = (mix(seed, k, axis, 124) % 628) / 100.0
        return k.toDouble() * spacing() + offset(seed, axis, k) + sin(t * 0.0045 + p1) * 28.0 + sin(t * 0.013 + p2) * 9.0
    }

    private fun raw(seed: Long, local: Boolean, axis: Int, k: Int, i: Int): Int {
        val key = (((seed * 1000003L + (if (local) 2 else 0) + axis) * 1000003L) + k) * 1000003L + i
        val cached = samples[key]
        if (cached != null) return cached
        val t = i * STEP
        val c = if (local) k * 48 else Math.round(center(seed, axis, k, t)).toInt()
        val px = if (axis == 0) t else c
        val pz = if (axis == 0) c else t
        val v = Terrain.natural(seed, px, pz).toInt()
        samples[key] = v
        return v
    }

    private fun smoothY(seed: Long, local: Boolean, axis: Int, k: Int, i: Int): Double {
        var s = 0.0
        for (j in -2..2) s += maxOf(raw(seed, local, axis, k, i + j), SEA_LEVEL + 2)
        return s / 5.0
    }

    
    private fun roadY(seed: Long, local: Boolean, axis: Int, k: Int, t: Int): Int? {
        val i0 = Math.floorDiv(t, STEP)
        if (raw(seed, local, axis, k, i0) >= SEA_LEVEL + 22 || raw(seed, local, axis, k, i0 + 1) >= SEA_LEVEL + 22) return null
        val f = (t - i0 * STEP) / STEP.toDouble()
        val a = smoothY(seed, local, axis, k, i0)
        val b = smoothY(seed, local, axis, k, i0 + 1)
        return Math.round(a + (b - a) * f).toInt()
    }

    private inline fun forEachRoad(seed: Long, x: Int, z: Int, action: (RoadHit) -> Unit) {
        for (axis in 0..1) {
            val along = if (axis == 0) x else z
            val across = if (axis == 0) z else x
            val sp = spacing()
            val k0 = Math.floorDiv(across, sp)
            for (k in k0 - 1..k0 + 1) {
                val d = abs(across - center(seed, axis, k, along))
                if (d >= HW_HIGH + BL_HIGH) continue
                val y = roadY(seed, false, axis, k, along) ?: continue
                action(RoadHit(d, y, HW_HIGH, BL_HIGH, false, axis, along))
            }
            val kl = Math.floorDiv(across + 24, 48)
            val dl = abs(across - kl * 48.0)
            if (dl < HW_LOCAL + BL_LOCAL && townFactor(seed, x, z) > 0.30) {
                val y = roadY(seed, true, axis, kl, along) ?: continue
                action(RoadHit(dl, y, HW_LOCAL, BL_LOCAL, true, axis, along))
            }
        }
    }

    private fun townAt(seed: Long, a: Int, b: Int): IntArray? {
        val sp = spacing()
        if (mix(seed, a, b, 121) % 100 >= Settings.townChance) return null
        val iz0 = b * sp + offset(seed, 0, b)
        val ix = Math.round(center(seed, 1, a, iz0)).toInt()
        val iz = Math.round(center(seed, 0, b, ix)).toInt()
        val key = ((seed * 1000003L + a) * 1000003L) + b
        val ok = townOk.getOrPut(key) {
            val hgt = Terrain.natural(seed, ix, iz)
            hgt in (SEA_LEVEL + 3.0)..90.0 && domeBlend(seed, ix, iz) > 0.999
        }
        return if (ok) intArrayOf(ix, iz) else null
    }

    
    fun townCenters(seed: Long, x: Int, z: Int, r: Int): List<IntArray> {
        val sp = spacing()
        val ax = Math.floorDiv(x, sp)
        val bz = Math.floorDiv(z, sp)
        val out = ArrayList<IntArray>()
        for (a in ax - r..ax + r) for (b in bz - r..bz + r) townAt(seed, a, b)?.let { out.add(it) }
        return out
    }

    
    fun townFactor(seed: Long, x: Int, z: Int): Double {
        if (Realms.at(seed, x, z) == Realm.NEIGHBORHOOD) return 0.6 
        val sp = spacing()
        var best = 0.0
        val ax = Math.floorDiv(x, sp)
        val bz = Math.floorDiv(z, sp)
        for (a in ax - 1..ax + 1) for (b in bz - 1..bz + 1) {
            if (mix(seed, a, b, 121) % 100 >= Settings.townChance) continue
            val iz0 = (b * sp + offset(seed, 0, b))
            val ix = Math.round(center(seed, 1, a, iz0)).toInt()
            val iz = Math.round(center(seed, 0, b, ix)).toInt()
            val r = 100 + mix(seed, a, b, 122) % 60
            val d = hypot((x - ix).toDouble(), (z - iz).toDouble())
            if (d >= r) continue
            val key = ((seed * 1000003L + a) * 1000003L) + b
            val ok = townOk.getOrPut(key) {
                val hgt = Terrain.natural(seed, ix, iz)
                hgt in (SEA_LEVEL + 3.0)..90.0 && domeBlend(seed, ix, iz) > 0.999
            }
            if (ok) best = maxOf(best, 1.0 - d / r)
        }
        return best
    }

    
    fun grade(seed: Long, x: Int, z: Int, h0: Double): Double {
        var h = h0
        forEachRoad(seed, x, z) { hit ->
            if (hit.dist <= hit.half) {
                h = hit.y.toDouble()
            } else if (hit.dist < hit.half + hit.blend) {
                val t = (hit.dist - hit.half) / hit.blend
                val s = t * t * (3.0 - 2.0 * t)
                h = hit.y + (h - hit.y) * s
            }
        }
        return h
    }

    
    fun nearest(seed: Long, x: Int, z: Int): RoadHit? {
        if (domeBlend(seed, x, z) < 0.999) return null
        var best: RoadHit? = null
        forEachRoad(seed, x, z) { hit ->
            val b = best
            if (b == null || hit.dist - hit.half < b.dist - b.half) best = hit
        }
        return best
    }

    
    fun surface(seed: Long, x: Int, z: Int): Material? {
        val h = nearest(seed, x, z) ?: return null
        val wear = mix(seed, x, z, 130) % 100
        if (h.dist <= h.half) {
            if (!h.local) {
                if (h.dist <= 0.5 && Math.floorMod(h.along, 8) < 5) return Material.YELLOW_CONCRETE
                if (h.dist >= h.half - 0.5) return Material.WHITE_CONCRETE
            }
            val k = if (h.local) 3 else 1 
            return when {
                wear < 4 * k -> Material.ANDESITE
                wear < 7 * k -> Material.GRAVEL
                wear < 9 * k -> Material.MOSS_BLOCK
                else -> Material.GRAY_CONCRETE
            }
        }
        if (h.dist <= h.half + (if (h.local) 2.0 else 1.5)) return if (h.local) Material.LIGHT_GRAY_CONCRETE else Material.GRAVEL
        return null
    }

    
    fun footprintClear(seed: Long, x0: Int, z0: Int, w: Int, d: Int, margin: Int): Boolean {
        var u = -margin
        while (u <= w + margin) {
            var v = -margin
            while (v <= d + margin) {
                val hit = nearest(seed, x0 + u, z0 + v)
                if (hit != null && hit.dist - hit.half < margin) return false
                v += 4
            }
            u += 4
        }
        return true
    }

    

    
    fun decorate(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        
        if (random.nextInt(100) < 20) {
            for (attempt in 0 until 8) {
                val x = bx + 1 + random.nextInt(14)
                val z = bz + 1 + random.nextInt(14)
                val hit = nearest(seed, x, z) ?: continue
                if (hit.dist > hit.half - 1.5) continue
                if (!gen.usesTerrain(seed, x, z)) continue
                val gy = gen.heightAt(seed, x, z)
                City.car(p, random, x, gy + 1, z, hit.axis == 0)
                break
            }
        }
        
        for (x in bx..bx + 15) for (z in bz..bz + 15) {
            val gx = if (x % 16 == 0 || z % 16 == 0) nearest(seed, x, z) else null
            val hit = gx ?: continue
            if (!hit.local || hit.dist < hit.half + 0.9 || hit.dist > hit.half + 1.6) continue
            if (Math.floorMod(hit.along, 16) != 0 || random.nextInt(100) >= 80) continue
            City.lamp(p, random, x, gen.heightAt(seed, x, z), z)
        }
        
        for (kx in Math.floorDiv(bx, 48)..Math.floorDiv(bx + 15, 48)) for (kz in Math.floorDiv(bz, 48)..Math.floorDiv(bz + 15, 48)) {
            val sx = kx * 48 + 4
            val sz = kz * 48 + 4
            if (sx < bx || sx >= bx + 16 || sz < bz || sz >= bz + 16) continue
            if (townFactor(seed, sx, sz) < 0.35 || !gen.usesTerrain(seed, sx, sz)) continue
            City.trafficLight(p, random, sx, gen.heightAt(seed, sx, sz), sz)
        }
    }
}
