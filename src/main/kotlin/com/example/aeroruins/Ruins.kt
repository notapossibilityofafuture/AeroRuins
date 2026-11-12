package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.entity.EntityType
import java.util.Random
import kotlin.math.abs
import kotlin.math.max






object Ruins {
    
    fun maybe(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator, gloom: Boolean): Boolean {
        val r = random.nextInt(1000)
        var acc = 0
        fun hit(normal: Int, dark: Int): Boolean {
            acc += if (gloom) dark else normal
            return r < acc
        }
        when {
            hit(14, 6) -> Overpass.build(seed, random, p, bx, bz, gen)
            hit(12, 8) -> FloodedBlock.build(seed, random, p, bx, bz, gen)
            hit(16, 10) -> Monolith.build(seed, random, p, bx, bz, gen)
            hit(10, 30) -> PowerLine.build(seed, random, p, bx, bz, gen)
            hit(6, 18) -> FencedLot.build(seed, random, p, bx, bz, gen)
            hit(8, 14) -> Outhouse.build(seed, random, p, bx, bz, gen)
            hit(20, 60) -> NoEntry.scatter(seed, random, p, bx, bz, gen)
            else -> return false
        }
        return true
    }
}


class Frame(private val x0: Int, private val z0: Int, val rot: Boolean) {
    fun x(u: Int, v: Int): Int = if (rot) x0 + v else x0 + u
    fun z(u: Int, v: Int): Int = if (rot) z0 + u else z0 + v

    
    fun dir(du: Int, dv: Int): String = if (!rot) {
        if (du > 0) "east" else if (du < 0) "west" else if (dv > 0) "south" else "north"
    } else {
        if (du > 0) "south" else if (du < 0) "north" else if (dv > 0) "east" else "west"
    }
}

private val GREENS = arrayOf(Material.MOSS_BLOCK, Material.MOSS_BLOCK, Material.MOSSY_COBBLESTONE)


object RuinTower {
    private val WALLS = arrayOf(
        Material.LIGHT_GRAY_CONCRETE, Material.STONE, Material.ANDESITE, Material.STONE_BRICKS, Material.CRACKED_STONE_BRICKS
    )

    fun build(
        p: Place, random: Random, x0: Int, z0: Int, w: Int, d: Int, baseY: Int, h: Int,
        moss: Int, vines: Int, hole: Boolean
    ) {
        for (dy in 0 until h) {
            val y = baseY + dy
            for (dx in 0 until w) for (dz in 0 until d) {
                val onX = dx == 0 || dx == w - 1
                val onZ = dz == 0 || dz == d - 1
                if (onX || onZ) {
                    val corner = onX && onZ
                    val window = !corner && dy > 2 && dy % 3 == 1 && (if (onZ) dx else dz) % 2 == 1
                    if (window && random.nextInt(100) < 88) continue
                    if (random.nextInt(100) < 3) continue 
                    val r = random.nextInt(100)
                    val m = when {
                        r < moss -> Material.MOSS_BLOCK
                        r < moss + 10 -> Material.MOSSY_STONE_BRICKS
                        else -> WALLS[random.nextInt(WALLS.size)]
                    }
                    p.set(x0 + dx, y, z0 + dz, m)
                } else if (dy % 3 == 0 && random.nextInt(100) >= 28) {
                    p.set(x0 + dx, y, z0 + dz, Material.LIGHT_GRAY_CONCRETE)
                }
            }
        }
        
        val roof = baseY + h
        for (dx in 0 until w) for (dz in 0 until d) {
            if (random.nextInt(100) < 12) continue
            p.set(x0 + dx, roof, z0 + dz, if (random.nextInt(100) < 70) Material.MOSS_BLOCK else Material.LIGHT_GRAY_CONCRETE)
            val r = random.nextInt(100)
            if (r < 35) p.leaf(x0 + dx, roof + 1, z0 + dz)
            if (r < 12) p.leaf(x0 + dx, roof + 2, z0 + dz)
        }
        
        val faces = arrayOf("south", "north", "east", "west")
        for (side in 0 until 4) {
            val len = if (side < 2) w else d
            for (i in 0 until len) {
                if (random.nextInt(100) >= vines) continue
                val x = when (side) { 0, 1 -> x0 + i; 2 -> x0 - 1; else -> x0 + w }
                val z = when (side) { 0 -> z0 - 1; 1 -> z0 + d; else -> z0 + i }
                val top = baseY + h - random.nextInt(maxOf(h / 3, 1))
                val n = 6 + random.nextInt(22)
                for (y in top downTo top - n) {
                    if (!p.isAir(x, y, z)) break
                    p.vine(x, y, z, faces[side])
                }
            }
        }
        
        if (hole && h > 24) {
            val hy = baseY + (h * (0.45 + random.nextDouble() * 0.3)).toInt()
            val r = w / 2 + 2
            val ox = x0 + w / 2
            val oz = z0 + d / 2
            for (dx in -r..r) for (dy in -r..r) for (dz in -r..r) {
                if (dx * dx + dy * dy + dz * dz > r * r) continue
                val x = ox + dx
                val z = oz + dz
                if (x < x0 || x >= x0 + w || z < z0 || z >= z0 + d) continue
                if (random.nextInt(100) < 90) p.set(x, hy + dy, z, Material.AIR)
            }
        }
    }
}


object Monolith {
    fun build(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val w = 9 + random.nextInt(3)
        val d = 8 + random.nextInt(3)
        val x0 = bx + 2
        val z0 = bz + 2
        val low = Sites.flatBase(seed, gen, x0, z0, w, d, 2, 5) ?: return
        val h = 60 + random.nextInt(45)
        RuinTower.build(p, random, x0, z0, w, d, low, h, 30, 75, true)
        
        for (dx in -3 until w + 3) for (dz in -3 until d + 3) {
            val inside = dx in 0 until w && dz in 0 until d
            if (inside || random.nextInt(100) >= 22) continue
            val x = x0 + dx
            val z = z0 + dz
            val gy = gen.heightAt(seed, x, z)
            p.set(x, gy + 1, z, if (random.nextBoolean()) Material.MOSS_BLOCK else Material.MOSSY_COBBLESTONE)
            if (random.nextInt(100) < 50) p.leaf(x, gy + 2, z)
        }
        Interiors.furnish(p, random, x0, z0, w, d, 2, (1 until h / 3 step 4).map { low + it * 3 }, 30)
        if (Loot.rare(random, 30.0)) {
            Loot.fill(p, random, x0, z0, w, d, 2, listOf(low + 1, low + 4), "tower", listOf(EntityType.ZOMBIE, EntityType.SKELETON), 1, 2)
        }
    }
}


object Overpass {
    private const val L = 28
    private const val W = 11

    fun build(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val f = Frame(bx + 1, bz + 1, random.nextBoolean())
        val fw = if (f.rot) W else L
        val fd = if (f.rot) L else W
        val low = Sites.flatBase(seed, gen, bx + 1, bz + 1, fw, fd, 1, 5) ?: return
        val deck = low + 8

        
        for (u in 0 until L) for (v in 0 until W) {
            val x = f.x(u, v)
            val z = f.z(u, v)
            for (y in low + 1..low + 12) p.set(x, y, z, Material.AIR)
            var yy = low - 2
            while (yy > low - 12 && p.isAir(x, yy, z)) {
                p.set(x, yy, z, Material.DIRT)
                yy--
            }
            p.set(x, low - 1, z, Material.WATER)
            p.set(x, low, z, Material.WATER)
        }
        
        repeat(2 + random.nextInt(2)) {
            val u = 3 + random.nextInt(L - 10)
            val v = 2 + random.nextInt(W - 5)
            for (du in 0..4) for (dv in 0..2) {
                val ring = du == 0 || du == 4 || dv == 0 || dv == 2
                if (ring && !(du == 4 && dv == 1)) p.set(f.x(u + du, v + dv), low + 1, f.z(u + du, v + dv), Material.SPRUCE_SLAB)
            }
        }
        
        for (u in 0 until L) for (v in 0 until W) {
            if (random.nextInt(100) < 32) {
                val x = f.x(u, v)
                val z = f.z(u, v)
                if (p.isAir(x, low + 1, z)) p.set(x, low + 1, z, Material.LILY_PAD)
            }
        }
        
        for (u in 0 until L) for (v in 0 until W) {
            val x = f.x(u, v)
            val z = f.z(u, v)
            p.set(x, deck, z, Material.LIGHT_GRAY_CONCRETE)
            val top = if (v == W / 2 && u % 4 < 2) Material.WHITE_CONCRETE else if (random.nextInt(100) < 24) Material.MOSS_BLOCK else Material.LIGHT_GRAY_CONCRETE
            p.set(x, deck + 1, z, top)
            if ((v == 0 || v == W - 1) && random.nextInt(100) < 80) p.set(x, deck + 2, z, Material.LIGHT_GRAY_CONCRETE)
            
            val r = random.nextInt(100)
            if (v in 1..W - 2) {
                if (r < 22) p.set(x, deck + 2, z, if (random.nextBoolean()) Material.SHORT_GRASS else Material.FERN)
                else if (r < 28) p.leaf(x, deck + 2, z)
            }
        }
        
        for (v in intArrayOf(2, 5, 8)) for (u in 0 until L) {
            if (random.nextInt(100) < 12) continue
            p.set(f.x(u, v), deck - 1, f.z(u, v), Material.POLISHED_ANDESITE)
        }
        for (u in 0 until L step 4) for (v in 0 until W) {
            p.set(f.x(u, v), deck - 1, f.z(u, v), Material.POLISHED_ANDESITE)
        }
        
        for (u in intArrayOf(3, 10, 17, 24)) {
            for (v in intArrayOf(2, W - 4)) {
                val broken = random.nextInt(100) < 15
                val top = if (broken) deck - 2 - random.nextInt(4) else deck - 1
                for (a in 0..1) for (b in 0..1) for (y in low - 1..top) {
                    val m = when (random.nextInt(8)) {
                        0 -> Material.MOSSY_STONE_BRICKS
                        1 -> Material.CRACKED_STONE_BRICKS
                        else -> Material.LIGHT_GRAY_CONCRETE
                    }
                    p.set(f.x(u + a, v + b), y, f.z(u + a, v + b), m)
                }
            }
        }
        
        repeat(2 + random.nextInt(2)) {
            val u = 2 + random.nextInt(L - 5)
            val v = 1 + random.nextInt(W - 4)
            for (a in 0..2) for (b in 0..2) for (y in deck - 1..deck + 2) p.set(f.x(u + a, v + b), y, f.z(u + a, v + b), Material.AIR)
        }
        
        for (u in 0 until L) for (v in 0 until W) {
            if (random.nextInt(100) >= 30) continue
            val x = f.x(u, v)
            val z = f.z(u, v)
            val top = if (p.type(x, deck - 1, z) != Material.AIR) deck - 2 else deck - 1
            if (p.type(x, top + 1, z) == Material.AIR) continue 
            p.hang(x, top, z, minOf(3 + random.nextInt(7), top - low - 1))
        }
    }
}


object FloodedBlock {
    private const val S = 28

    fun build(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 1
        val z0 = bz + 1
        val low = Sites.flatBase(seed, gen, x0, z0, S, S, 1, 5) ?: return

        for (dx in 2 until S - 2) for (dz in 2 until S - 2) {
            val x = x0 + dx
            val z = z0 + dz
            for (y in low + 1..low + 14) p.set(x, y, z, Material.AIR)
            var yy = low - 6
            while (yy > low - 16 && p.isAir(x, yy, z)) {
                p.set(x, yy, z, Material.DIRT)
                yy--
            }
            for (y in low - 5..low) p.set(x, y, z, Material.WATER)
        }
        val slots = arrayOf(intArrayOf(4, 4), intArrayOf(15, 4), intArrayOf(4, 15), intArrayOf(15, 15))
        val skip = random.nextInt(4)
        for ((i, s) in slots.withIndex()) {
            if (i == skip) continue
            val w = 7 + random.nextInt(3)
            val d = 7 + random.nextInt(3)
            val h = 16 + random.nextInt(18)
            RuinTower.build(p, random, x0 + s[0], z0 + s[1], w, d, low - 5, h + 5, 24, 40, random.nextInt(100) < 50)
        }
        
        repeat(6) {
            val ix = x0 + 3 + random.nextInt(S - 8)
            val iz = z0 + 3 + random.nextInt(S - 8)
            for (a in 0..1) for (b in 0..1) {
                if (p.type(ix + a, low, iz + b) == Material.WATER) {
                    p.set(ix + a, low, iz + b, Material.GRASS_BLOCK)
                    p.leaf(ix + a, low + 1, iz + b)
                }
            }
        }
        for (dx in 2 until S - 2) for (dz in 2 until S - 2) {
            if (random.nextInt(100) < 18 && p.isAir(x0 + dx, low + 1, z0 + dz) && p.type(x0 + dx, low, z0 + dz) == Material.WATER) {
                p.set(x0 + dx, low + 1, z0 + dz, Material.LILY_PAD)
            }
        }
        
        val pz = z0 + S / 2
        for (dx in 0 until 12) {
            if (dx in 5..6) continue
            for (b in 0..1) {
                if (random.nextInt(100) < 85) p.set(x0 + dx, low + 1, pz + b, Material.LIGHT_GRAY_CONCRETE)
            }
        }
    }
}


object PowerLine {
    fun build(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val f = Frame(bx + 1, bz + 1, random.nextBoolean())
        val v0 = 5 + random.nextInt(6)
        val us = intArrayOf(2, 9, 16, 23)
        val tops = ArrayList<IntArray>() 
        val arm = if (f.rot) "x" else "z"
        for (u in us) {
            val px = f.x(u, v0)
            val pz = f.z(u, v0)
            if (zoneAt(seed, px, pz) != Zone.JUNGLE) return
            val gy = gen.heightAt(seed, px, pz)
            if (gy < SEA_LEVEL + 1) continue
            val hgt = 9 + random.nextInt(3)
            val lx = random.nextInt(7) - 3
            val lz = random.nextInt(7) - 3
            val snap = if (random.nextInt(100) < 22) 4 + random.nextInt(hgt - 4) else hgt
            var tx = px
            var tz = pz
            for (dy in 1..snap) {
                tx = px + Math.round(lx * dy.toFloat() / hgt)
                tz = pz + Math.round(lz * dy.toFloat() / hgt)
                p.set(tx, gy + dy, tz, Material.DARK_OAK_LOG)
            }
            if (snap < hgt) { 
                for (i in 1..4) p.put(px + i, gy + 1, pz, Material.DARK_OAK_LOG, "[axis=x]")
                continue
            }
            
            for (s in -2..2) {
                val ax = tx + (if (f.rot) s else 0)
                val az = tz + (if (f.rot) 0 else s)
                p.put(ax, gy + hgt, az, Material.STRIPPED_DARK_OAK_LOG, "[axis=$arm]")
            }
            tops.add(intArrayOf(tx, gy + hgt, tz))
        }
        
        for (i in 0 until tops.size - 1) {
            val a = tops[i]
            val b = tops[i + 1]
            val n = max(abs(b[0] - a[0]), abs(b[2] - a[2])).coerceAtLeast(1)
            for (s in intArrayOf(-2, 0, 2)) {
                val sx = if (f.rot) s else 0
                val sz = if (f.rot) 0 else s
                val sag = 1 + random.nextInt(2)
                for (k in 0..n) {
                    val t = k.toDouble() / n
                    val x = Math.round(a[0] + sx + (b[0] - a[0]) * t).toInt()
                    val z = Math.round(a[2] + sz + (b[2] - a[2]) * t).toInt()
                    val y = Math.round(a[1] + 1 + (b[1] - a[1]) * t - sag * 4.0 * t * (1.0 - t)).toInt()
                    if (p.isAir(x, y, z)) p.set(x, y, z, Material.TRIPWIRE)
                }
            }
        }
    }
}


object FencedLot {
    private const val L = 20
    private const val W = 14

    fun build(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val f = Frame(bx + 2, bz + 2, random.nextBoolean())
        val fw = if (f.rot) W else L
        val fd = if (f.rot) L else W
        val low = Sites.flatBase(seed, gen, bx + 2, bz + 2, fw, fd, 1, 4) ?: return

        fun perimeter(u: Int, v: Int) = (u == 0 || u == L - 1 || v == 0 || v == W - 1) && !(v == 0 && u in 9..10)
        for (u in 0 until L) for (v in 0 until W) {
            val x = f.x(u, v)
            val z = f.z(u, v)
            for (y in low + 1..low + 6) p.set(x, y, z, Material.AIR)
            p.set(x, low, z, if (random.nextInt(100) < 80) Material.GRAVEL else Material.COARSE_DIRT)
        }
        for (u in 0 until L) for (v in 0 until W) {
            if (!perimeter(u, v)) continue
            val x = f.x(u, v)
            val z = f.z(u, v)
            val post = u % 6 == 0 || (u == L - 1) || (v == 0 || v == W - 1) && (u == 0 || u == L - 1)
            if (post) {
                for (y in low + 1..low + 4) p.put(x, y, z, Material.COBBLESTONE_WALL, "[up=true]")
                continue
            }
            val dirs = ArrayList<String>()
            for ((du, dv) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                if (perimeter(u + du, v + dv) && u + du in 0 until L && v + dv in 0 until W) dirs.add(f.dir(du, dv))
            }
            if (dirs.isEmpty()) continue
            val props = "[" + dirs.joinToString(",") { "$it=true" } + "]"
            for (y in low + 1..low + 3) p.put(x, y, z, Material.IRON_BARS, props)
            if (random.nextInt(100) < 85) p.set(x, low + 4, z, Material.TRIPWIRE) 
        }
        
        repeat(4 + random.nextInt(4)) {
            val x = f.x(2 + random.nextInt(L - 4), 2 + random.nextInt(W - 4))
            val z = f.z(2 + random.nextInt(L - 4), 2 + random.nextInt(W - 4))
            if (p.isAir(x, low + 1, z)) p.put(x, low + 1, z, Material.BARREL, "[facing=up]")
        }
        if (Loot.rare(random, 35.0)) {
            Loot.chestAt(p, random, f.x(L / 2, W / 2), low + 1, f.z(L / 2, W / 2), "frame")
        }
        NoEntry.post(p, random, f.x(9, -2), f.z(9, -2), low)
    }
}


object Outhouse {
    fun build(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 8 + random.nextInt(3)
        val z0 = bz + 8 + random.nextInt(3)
        val low = Sites.flatBase(seed, gen, x0 - 1, z0 - 1, 5, 5, 3, 3) ?: return
        for (dx in 0..2) for (dz in 0..2) {
            for (y in low + 1..low + 4) p.set(x0 + dx, y, z0 + dz, Material.AIR)
            p.set(x0 + dx, low, z0 + dz, Material.COARSE_DIRT)
        }
        for (dy in 1..3) for (dx in 0..2) for (dz in 0..2) {
            if (dx == 1 && dz == 1) continue
            if (dx == 1 && dz == 2 && dy <= 2) continue 
            p.set(x0 + dx, low + dy, z0 + dz, if (random.nextInt(100) < 20) Material.CYAN_TERRACOTTA else Material.LIGHT_BLUE_TERRACOTTA)
        }
        for (dx in -1..3) for (dz in -1..3) p.set(x0 + dx, low + 4, z0 + dz, Material.DARK_OAK_SLAB)
        p.put(x0 + 1, low + 1, z0 + 2, Material.WARPED_DOOR, "[half=lower,facing=south,hinge=left]")
        p.put(x0 + 1, low + 2, z0 + 2, Material.WARPED_DOOR, "[half=upper,facing=south,hinge=left]")
        if (Loot.rare(random, 55.0)) p.chest(x0 + 1, low + 1, z0, "south", "frame")
        
        val flowers = arrayOf(Material.LILY_OF_THE_VALLEY, Material.AZURE_BLUET, Material.OXEYE_DAISY, Material.WHITE_TULIP)
        for (dx in -7..7) for (dz in -7..7) {
            if (dx * dx + dz * dz > 49 || random.nextInt(100) >= 38) continue
            val x = x0 + 1 + dx
            val z = z0 + 1 + dz
            val gy = gen.heightAt(seed, x, z)
            if (p.type(x, gy, z) == Material.GRASS_BLOCK && p.isAir(x, gy + 1, z)) {
                p.set(x, gy + 1, z, flowers[random.nextInt(flowers.size)])
            }
        }
    }
}


object NoEntry {
    fun post(p: Place, random: Random, x: Int, z: Int, groundY: Int) {
        val facingX = random.nextBoolean()
        for (y in groundY + 1..groundY + 3) p.set(x, y, z, Material.OAK_FENCE)
        for (a in -1..1) for (b in -1..1) {
            if (abs(a) == 1 && abs(b) == 1) continue 
            val m = if (b == 0) Material.WHITE_CONCRETE else Material.RED_CONCRETE
            p.set(x + (if (facingX) a else 0), groundY + 5 + b, z + (if (facingX) 0 else a), m)
        }
    }

    fun scatter(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        repeat(1 + random.nextInt(3)) {
            val x = bx + 2 + random.nextInt(12)
            val z = bz + 2 + random.nextInt(12)
            val gy = gen.heightAt(seed, x, z)
            if (zoneAt(seed, x, z) == Zone.JUNGLE && gy >= SEA_LEVEL + 1 &&
                p.type(x, gy, z) == Material.GRASS_BLOCK && p.isAir(x, gy + 1, z)
            ) post(p, random, x, z, gy)
        }
    }
}
