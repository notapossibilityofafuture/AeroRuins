package com.example.aeroruins

import org.bukkit.Material
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

enum class Zone { JUNGLE, AERO, BACKROOMS, MALL, QUARRY }

const val SEA_LEVEL = 62
const val FLOOR = 64     
const val CELL = 224     


fun mix(seed: Long, a: Int, b: Int, salt: Int): Int {
    var h = seed xor (a.toLong() * -7046029254386353131L) xor
        (b.toLong() * -4658895280553007687L) xor (salt.toLong() * 1442695040888963407L)
    h = h xor (h ushr 33)
    h *= -49064778989728563L
    h = h xor (h ushr 33)
    h *= -4265267296055464877L
    h = h xor (h ushr 33)
    return (h ushr 33).toInt()
}

fun cellOf(v: Int): Int = Math.floorDiv(v + CELL / 2, CELL)

private class ZoneMemo { var seed = 0L; var cx = Int.MIN_VALUE; var cz = Int.MIN_VALUE; var zone = Zone.JUNGLE }
private val zoneMemo = ThreadLocal.withInitial { ZoneMemo() }

private fun liminalCandidate(seed: Long, cx: Int, cz: Int): Boolean =
    !(cx == 0 && cz == 0) && mix(seed, cx, cz, 1) % 100 < Settings.liminalChance

private fun liminalType(seed: Long, cx: Int, cz: Int): Zone = when (mix(seed, cx, cz, 3) % 3) {
    0 -> Zone.BACKROOMS
    1 -> Zone.MALL
    else -> Zone.QUARRY
}


private fun beats(seed: Long, ax: Int, az: Int, bx: Int, bz: Int): Boolean {
    val pa = mix(seed, ax, az, 70)
    val pb = mix(seed, bx, bz, 70)
    return if (pa != pb) pa > pb else (ax.toLong() * 65537L + az) > (bx.toLong() * 65537L + bz)
}





fun zoneOfCell(seed: Long, cx: Int, cz: Int): Zone {
    val m = zoneMemo.get()
    if (m.seed == seed && m.cx == cx && m.cz == cz) return m.zone
    val zone = computeZone(seed, cx, cz)
    m.seed = seed; m.cx = cx; m.cz = cz; m.zone = zone
    return zone
}


private fun domeCellsPerSide(): Int = maxOf(2, Settings.domeSpacing / CELL)


private fun domeCellIn(seed: Long, sx: Int, sz: Int): IntArray {
    val n = domeCellsPerSide()
    val h = mix(seed, sx, sz, 5)
    return intArrayOf(sx * n + h % n, sz * n + (h shr 10) % n)
}

private fun isDomeCell(seed: Long, cx: Int, cz: Int): Boolean {
    if (cx == 0 && cz == 0) return false
    val n = domeCellsPerSide()
    val c = domeCellIn(seed, Math.floorDiv(cx, n), Math.floorDiv(cz, n))
    return cx == c[0] && cz == c[1]
}


fun domeBlend(seed: Long, x: Int, z: Int): Double {
    val n = domeCellsPerSide()
    val scx = Math.floorDiv(cellOf(x), n)
    val scz = Math.floorDiv(cellOf(z), n)
    var best = 1.0
    for (sx in scx - 1..scx + 1) for (sz in scz - 1..scz + 1) {
        val c = domeCellIn(seed, sx, sz)
        if (c[0] == 0 && c[1] == 0) continue
        val dx = (x - c[0] * CELL).toDouble()
        val dz = (z - c[1] * CELL).toDouble()
        val d = sqrt(dx * dx + dz * dz)
        val r = domeRadius(seed, c[0], c[1])
        if (d < r + 90) {
            val t = ((d - r) / 90.0).coerceIn(0.0, 1.0)
            best = minOf(best, t * t * (3.0 - 2.0 * t))
        }
    }
    return best
}

private fun computeZone(seed: Long, cx: Int, cz: Int): Zone {
    if (cx == 0 && cz == 0) return Zone.JUNGLE
    if (isDomeCell(seed, cx, cz)) return Zone.AERO
    if (liminalCandidate(seed, cx, cz)) {
        val mine = liminalType(seed, cx, cz)
        var wins = true
        loop@ for (dx in -1..1) for (dz in -1..1) {
            if (dx == 0 && dz == 0) continue
            val nx = cx + dx
            val nz = cz + dz
            if (liminalCandidate(seed, nx, nz) && liminalType(seed, nx, nz) != mine && beats(seed, nx, nz, cx, cz)) {
                wins = false
                break@loop
            }
        }
        if (wins) return mine
    }
    return Zone.JUNGLE
}

fun zoneAt(seed: Long, x: Int, z: Int): Zone = zoneOfCell(seed, cellOf(x), cellOf(z))

fun domeRadius(seed: Long, cx: Int, cz: Int): Int = 84 + mix(seed, cx, cz, 2) % 12


class DomeInfo(val dx: Int, val dz: Int, val d: Double, val a: Double, val r: Int)

fun domeAt(seed: Long, x: Int, z: Int): DomeInfo? {
    val cx = cellOf(x)
    val cz = cellOf(z)
    if (zoneOfCell(seed, cx, cz) != Zone.AERO) return null
    val dx = x - cx * CELL
    val dz = z - cz * CELL
    val d = sqrt((dx * dx + dz * dz).toDouble())
    val r = domeRadius(seed, cx, cz)
    if (d >= r) return null
    return DomeInfo(dx, dz, d, atan2(dz.toDouble(), dx.toDouble()), r)
}


fun onDomePath(dome: DomeInfo): Boolean {
    if (dome.d < 11.5) return true
    val k = (dome.a / (PI / 4)).roundToInt()
    val perp = dome.d * abs(sin(dome.a - k * PI / 4))
    val ring = dome.d >= dome.r * 0.55 && dome.d <= dome.r * 0.55 + 2.5
    return perp < 1.6 || ring
}


fun spiralHit(a: Double, rel: Int): Boolean {
    val t = a - rel * 0.5
    return abs(atan2(sin(t), cos(t))) < 0.45
}


const val BF = 26 
const val BC = 35 
const val QF = 22 
const val QC = 38 
const val MF = 16 
const val MC = 26 

const val DEEP_Y = 0      
const val LAVA_Y = -50    
const val LAKE_BOTTOM = 8 
const val LAKE_TOP = 16


const val SHALLOW_THEMES = 15
const val DEEP_THEMES = 4


fun caveTheme(seed: Long, x: Int, y: Int, z: Int): Int =
    mix(seed, Math.floorDiv(x, 128), Math.floorDiv(z, 128) + Math.floorDiv(y, 40) * 137, 21) % CaveStyles.ALL.size


fun hallVariant(seed: Long, x: Int, z: Int, salt: Int, n: Int): Int =
    mix(seed, Math.floorDiv(x, 128), Math.floorDiv(z, 128), salt) % n


fun mallVariant(seed: Long, x: Int, z: Int): Int = hallVariant(seed, x, z, 61, 4)


fun quarryVariant(seed: Long, x: Int, z: Int): Int = hallVariant(seed, x, z, 62, 3)


fun domeVariant(seed: Long, x: Int, z: Int): Int = mix(seed, cellOf(x), cellOf(z), 63) % 3


fun domeCenters(seed: Long, x: Int, z: Int): List<IntArray> {
    val n = domeCellsPerSide()
    val scx = Math.floorDiv(cellOf(x), n)
    val scz = Math.floorDiv(cellOf(z), n)
    val out = ArrayList<IntArray>()
    for (sx in scx - 2..scx + 2) for (sz in scz - 2..scz + 2) {
        val c = domeCellIn(seed, sx, sz)
        if (c[0] == 0 && c[1] == 0) continue
        out.add(intArrayOf(c[0] * CELL, c[1] * CELL))
    }
    return out
}
