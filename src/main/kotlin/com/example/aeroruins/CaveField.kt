package com.example.aeroruins

import org.bukkit.util.noise.SimplexOctaveGenerator
import kotlin.math.abs











class CaveSampler(private val seed: Long, private val g: Array<SimplexOctaveGenerator>, private val wide: Boolean = false) {

    
    fun exact(x: Int, y: Int, z: Int): Double {
        val fx = x.toDouble()
        val fy = y.toDouble()
        val fz = z.toDouble()

        
        val regU = ((g[5].noise(fx, fz, 0.5, 0.5, true) + 1.0) * 0.5).coerceIn(0.0, 1.0)

        
        val wa = g[4].noise(fx, fy, fz, 0.5, 0.5, true)
        val wb = g[4].noise(fx + 31.37, fy - 17.21, fz + 23.91, 0.5, 0.5, true)
        val px = fx + wa * WARP
        val py = fy + wb * WARP * 0.6
        val pz = fz + (wa - wb) * 0.5 * WARP

        
        var tw = 0.075 + 0.07 * regU
        if (y < -5) tw *= 1.0 + 0.4 * minOf(1.0, (-5 - y) / 15.0) 
        var tube = abs(g[0].noise(px, py * 1.4, pz, 0.5, 0.5, true)) - tw
        if (tube < 0.25) {
            tube = maxOf(tube, abs(g[1].noise(px, py * 1.4, pz, 0.5, 0.5, true)) - tw)
        }

        
        val dShallow = maxOf(0, maxOf(12 - y, y - 50))
        val dDeep = maxOf(0, maxOf(-56 - y, y + 10))
        val band = if (wide) 0.0 else minOf(dShallow, dDeep) * 0.04 
        var cavern = 9.0
        if (band < 0.3) {
            cavern = (0.58 - 0.12 * regU) - g[2].noise(px, py, pz, 0.5, 0.5, true) + band
        }

        
        var canyon = 9.0
        if (mix(seed, Math.floorDiv(x, 160), Math.floorDiv(z, 160), 25) % 100 < 45) {
            val t = ((y - 14) / 50.0).coerceIn(0.0, 1.0)
            canyon = abs(g[3].noise(fx, fz, 0.5, 0.5, true)) - (0.018 + 0.040 * t)
            if (y < 14) canyon += (14 - y) * 0.05
        }

        return minOf(tube, minOf(cavern, canyon))
    }

    companion object {
        const val WARP = 6.0
    }
}





class CaveGrid {
    private lateinit var sampler: CaveSampler
    private var baseX = 0
    private var baseZ = 0
    private var y0 = 0
    private var ny = 0
    private var nodes = DoubleArray(0)

    
    fun begin(sampler: CaveSampler, baseX: Int, baseZ: Int, minY: Int) {
        this.sampler = sampler
        this.baseX = baseX
        this.baseZ = baseZ
        y0 = Math.floorDiv(minY, 4) * 4
        ny = ((GRID_TOP - y0) shr 2) + 2
        val size = 5 * 5 * ny
        if (nodes.size != size) nodes = DoubleArray(size)
        java.util.Arrays.fill(nodes, Double.NaN)
    }

    private fun node(ix: Int, iy: Int, iz: Int): Double {
        val i = (ix * 5 + iz) * ny + iy
        var v = nodes[i]
        if (v.isNaN()) {
            v = sampler.exact(baseX + ix * 4, y0 + iy * 4, baseZ + iz * 4)
            nodes[i] = v
        }
        return v
    }

    



    fun carved(lx: Int, y: Int, lz: Int, pen: Double): Boolean {
        val dy = y - y0
        val cy = dy shr 2
        if (cy + 1 >= ny) return sampler.exact(baseX + lx, y, baseZ + lz) + pen < 0.0
        val cx = lx shr 2
        val cz = lz shr 2

        val a = node(cx, cy, cz)
        val b = node(cx + 1, cy, cz)
        val c = node(cx, cy, cz + 1)
        val d = node(cx + 1, cy, cz + 1)
        val e = node(cx, cy + 1, cz)
        val f = node(cx + 1, cy + 1, cz)
        val g = node(cx, cy + 1, cz + 1)
        val h = node(cx + 1, cy + 1, cz + 1)

        val lo = minOf(minOf(minOf(a, b), minOf(c, d)), minOf(minOf(e, f), minOf(g, h))) + pen
        if (lo > MARGIN) return false 
        val hi = maxOf(maxOf(maxOf(a, b), maxOf(c, d)), maxOf(maxOf(e, f), maxOf(g, h))) + pen
        if (hi < -MARGIN) return true 

        
        val fx = (lx and 3) * 0.25
        val fz = (lz and 3) * 0.25
        val fy = (dy and 3) * 0.25
        val x00 = a + (b - a) * fx
        val x01 = c + (d - c) * fx
        val x10 = e + (f - e) * fx
        val x11 = g + (h - g) * fx
        val lower = x00 + (x01 - x00) * fz
        val upper = x10 + (x11 - x10) * fz
        val v = lower + (upper - lower) * fy + pen
        if (abs(v) > MARGIN) return v < 0.0
        return sampler.exact(baseX + lx, y, baseZ + lz) + pen < 0.0
    }

    companion object {
        
        const val GRID_TOP = 176
        const val MARGIN = 0.10
    }
}







private const val MAX_POCKET = 24

internal fun sweepOrphans(data: org.bukkit.generator.ChunkGenerator.ChunkData, minY: Int, maxY: Int) {
    val h = maxY - minY + 1
    if (h <= 2) return
    
    val state = ByteArray(16 * 16 * h)
    val queue = IntArray(MAX_POCKET + 2)
    val dxs = intArrayOf(1, -1, 0, 0, 0, 0)
    val dys = intArrayOf(0, 0, 1, -1, 0, 0)
    val dzs = intArrayOf(0, 0, 0, 0, 1, -1)
    for (x in 0..15) for (z in 0..15) for (y in minY..maxY) {
        val start = (x * 16 + z) * h + (y - minY)
        if (state[start].toInt() != 0 || data.getType(x, y, z) != org.bukkit.Material.AIR) continue
        var head = 0
        var tail = 1
        queue[0] = start
        state[start] = 1
        var open = false
        while (head < tail && !open) {
            val cur = queue[head++]
            val cy = cur % h + minY
            val t = cur / h
            val cz = t % 16
            val cx = t / 16
            if (cx == 0 || cx == 15 || cz == 0 || cz == 15 || cy == minY || cy == maxY) {
                open = true
                break
            }
            for (d in 0..5) {
                val ni = ((cx + dxs[d]) * 16 + (cz + dzs[d])) * h + (cy + dys[d] - minY)
                val st = state[ni].toInt()
                if (st == 1) continue
                if (st == 2) { 
                    open = true
                    break
                }
                if (data.getType(cx + dxs[d], cy + dys[d], cz + dzs[d]) != org.bukkit.Material.AIR) continue
                if (tail > MAX_POCKET) {
                    open = true
                    break
                }
                state[ni] = 1
                queue[tail++] = ni
            }
        }
        if (open) {
            for (i in 0 until tail) state[queue[i]] = 2
        } else {
            for (i in 0 until tail) {
                val cur = queue[i]
                val cy = cur % h + minY
                val t = cur / h
                data.setBlock(t / 16, cy, t % 16, if (cy < 0) org.bukkit.Material.DEEPSLATE else org.bukkit.Material.STONE)
            }
        }
    }
}
