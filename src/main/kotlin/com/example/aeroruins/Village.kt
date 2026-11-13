package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.entity.Villager
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private fun fdiv(a: Int, b: Int): Int = Math.floorDiv(a, b)






object Village {
    private const val CELL = 512
    private const val HOUSE = 0
    private const val CHURCH = 1
    private const val BARN = 2
    private const val FARM = 3
    private const val MILL = 4
    private const val GRAVE = 5
    private const val KEEP = 6
    private const val MARKET = 7
    private const val WELL = -1

    private const val RURAL = 0
    private const val FORTIFIED = 1
    private const val DESERT = 2
    private const val WALL_R = 44
    private val DRY = setOf("yellow_plains", "scrub_savanna", "dust_flats", "dry_acacia_plain", "sun_bleached_grove", "rust_meadow", "dandelion_plain")
    private val COLD = setOf("frostbitten_steppe", "paper_birch_barrens", "hoarfrost_taiga", "silver_birch_vale", "fogbound_spruce", "grey_fen")

    class Piece(val kind: Int, val x: Int, val z: Int)

    private fun center(seed: Long, gx: Int, gz: Int): IntArray? {
        val h = mix(seed, gx, gz, 180)
        if (h % 100 >= Settings.villageChance) return null
        return intArrayOf(gx * CELL + 110 + (h shr 8) % 292, gz * CELL + 110 + (h shr 16) % 292)
    }

    
    private fun valid(seed: Long, gx: Int, gz: Int): IntArray? {
        val c = center(seed, gx, gz) ?: return null
        if (Realms.at(seed, c[0], c[1]) != Realm.WILD) return null
        if (zoneAt(seed, c[0], c[1]) != Zone.JUNGLE) return null
        if (Roads.townFactor(seed, c[0], c[1]) > 0.2) return null
        val h = Terrain.heightAt(seed, c[0], c[1])
        if (h < SEA_LEVEL + 3 || h > 92) return null
        return c
    }

    
    fun centersAround(seed: Long, x: Int, z: Int, r: Int): List<IntArray> {
        val out = ArrayList<IntArray>()
        for (gx in fdiv(x, CELL) - r..fdiv(x, CELL) + r) for (gz in fdiv(z, CELL) - r..fdiv(z, CELL) + r) {
            valid(seed, gx, gz)?.let { out.add(it) }
        }
        return out
    }

    
    fun near(seed: Long, x0: Int, z0: Int, w: Int, d: Int): Boolean {
        val mx = x0 + w / 2
        val mz = z0 + d / 2
        val c = valid(seed, fdiv(mx, CELL), fdiv(mz, CELL)) ?: return false
        val dx = mx - c[0]
        val dz = mz - c[1]
        return dx * dx + dz * dz < 70 * 70
    }

    
    private fun style(seed: Long, gx: Int, gz: Int, c: IntArray): Int {
        if (ClimateMap.at(seed, c[0], c[1]).id in DRY) return DESERT
        return if (mix(seed, gx, gz, 181) % 100 < 35) FORTIFIED else RURAL
    }

    private fun pieces(seed: Long, gx: Int, gz: Int, c: IntArray, style: Int): List<Piece> {
        val r = Random(seed xor (gx.toLong() * 341873128712L) xor (gz.toLong() * 132897987541L))
        val list = ArrayList<Piece>()
        list.add(Piece(WELL, c[0] - 1, c[1] - 1))
        val fort = style == FORTIFIED
        fun add(kind: Int, minD: Int) {
            for (t in 0 until 14) {
                val a = r.nextDouble() * Math.PI * 2
                val rad = if (fort) 12.0 + r.nextDouble() * 22.0 else 14.0 + r.nextDouble() * 34.0
                val x = c[0] + (cos(a) * rad).toInt()
                val z = c[1] + (sin(a) * rad).toInt()
                if (list.none { abs(it.x - x) < minD && abs(it.z - z) < minD }) {
                    list.add(Piece(kind, x, z))
                    return
                }
            }
        }
        when (style) {
            FORTIFIED -> {
                list.add(Piece(KEEP, c[0] - 4, c[1] - 20)) 
                repeat(5 + r.nextInt(4)) { add(HOUSE, 12) }
                if (r.nextInt(100) < 70) add(CHURCH, 15)
                if (r.nextInt(100) < 50) add(MARKET, 11)
                if (r.nextInt(100) < 40) add(BARN, 14)
            }
            DESERT -> {
                repeat(6 + r.nextInt(5)) { add(HOUSE, 13) }
                repeat(1 + r.nextInt(2)) { add(MARKET, 12) }
                repeat(1 + r.nextInt(2)) { add(FARM, 13) }
                if (r.nextInt(100) < 40) add(BARN, 15)
                if (r.nextInt(100) < 30) add(MILL, 12)
            }
            else -> {
                repeat(6 + r.nextInt(5)) { add(HOUSE, 13) }
                if (r.nextInt(100) < 60) add(CHURCH, 16)
                if (r.nextInt(100) < 50) add(BARN, 15)
                repeat(2 + r.nextInt(2)) { add(FARM, 13) }
                if (r.nextInt(100) < 35) add(MILL, 12)
                if (r.nextInt(100) < 30) add(GRAVE, 10)
            }
        }
        return list
    }

    fun populate(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val gx = fdiv(bx, CELL)
        val gz = fdiv(bz, CELL)
        val c = valid(seed, gx, gz) ?: return
        if (c[0] < bx - 70 || c[0] > bx + 85 || c[1] < bz - 70 || c[1] > bz + 85) return
        val st = style(seed, gx, gz, c)
        val desert = st == DESERT
        val ruined = mix(seed, gx, gz, 182) % 100 < 18
        val list = pieces(seed, gx, gz, c, st)
        if (st == FORTIFIED) wall(seed, random, p, bx, bz, gen, c)

        
        for (pc in list) {
            if (pc.kind == WELL) continue
            var x = c[0]
            var z = c[1]
            while (x != pc.x) { path(seed, p, gen, bx, bz, x, z); x += if (pc.x > x) 1 else -1 }
            while (z != pc.z) { path(seed, p, gen, bx, bz, x, z); z += if (pc.z > z) 1 else -1 }
        }
        for (pc in list) {
            if (pc.x < bx || pc.x >= bx + 16 || pc.z < bz || pc.z >= bz + 16) continue
            when (pc.kind) {
                WELL -> {
                    well(seed, random, p, pc.x, pc.z, gen)
                    wellFolk(seed, random, p, pc.x, pc.z, gen, ruined)
                }
                HOUSE -> Rustic.cottage(seed, random, p, pc.x, pc.z, gen, desert, ruined)
                CHURCH -> church(seed, random, p, pc.x, pc.z, gen)
                BARN -> barn(seed, random, p, pc.x, pc.z, gen, desert)
                FARM -> farm(seed, random, p, pc.x, pc.z, gen)
                MILL -> windmill(seed, random, p, pc.x, pc.z, gen, desert)
                GRAVE -> graveyard(seed, random, p, pc.x, pc.z, gen)
                KEEP -> keep(seed, random, p, pc.x, pc.z, gen)
                MARKET -> market(seed, random, p, pc.x, pc.z, gen, desert)
            }
            if (ruined && (pc.kind == CHURCH || pc.kind == BARN || pc.kind == MILL || pc.kind == KEEP || pc.kind == MARKET)) {
                val gy = gen.heightAt(seed, pc.x, pc.z)
                Decay.ruin(p, random, pc.x, pc.z, 12, 12, gy + 1, gy + 14, 20)
            }
        }
    }

    private fun path(seed: Long, p: Place, gen: AeroGenerator, bx: Int, bz: Int, x: Int, z: Int) {
        if (x < bx || x >= bx + 16 || z < bz || z >= bz + 16) return
        val h = gen.heightAt(seed, x, z)
        val t = p.type(x, h, z)
        if (t == Material.GRASS_BLOCK || t == Material.DIRT || t == Material.COARSE_DIRT || t == Material.PODZOL) {
            p.set(x, h, z, Material.DIRT_PATH)
            // Vanilla village paths are two blocks wide at intersections and
            // never turn into a continuous fence line.
            if ((x + z) % 5 == 0) {
                val sideX = x + 1
                val sideY = gen.heightAt(seed, sideX, z)
                if (sideX in bx until bx + 16) {
                    val side = p.type(sideX, sideY, z)
                    if (side == Material.GRASS_BLOCK || side == Material.DIRT || side == Material.COARSE_DIRT || side == Material.PODZOL)
                        p.set(sideX, sideY, z, Material.DIRT_PATH)
                }
            }
            if (!p.type(x, h + 1, z).isSolid) p.set(x, h + 1, z, Material.AIR) 
        }
    }

    private fun clear(p: Place, x0: Int, z0: Int, w: Int, d: Int, low: Int, h: Int) {
        for (dx in 0 until w) for (dz in 0 until d) for (y in low + 1..low + h) p.set(x0 + dx, y, z0 + dz, Material.AIR)
    }

    
    internal fun gable(p: Place, x0: Int, z0: Int, w: Int, d: Int, baseY: Int, alongX: Boolean, stairs: Material, slab: Material, fill: Material) {
        val span = if (alongX) d else w
        val len = if (alongX) w else d
        val mid = (span + 1) / 2
        for (idx in 0..span + 1) {
            val level = if (idx <= mid) idx else span + 1 - idx
            for (j in -1..len) {
                val x = if (alongX) x0 + j else x0 + idx - 1
                val z = if (alongX) z0 + idx - 1 else z0 + j
                val y = baseY + level
                when {
                    idx < mid -> p.put(x, y, z, stairs, "[facing=${if (alongX) "south" else "east"}]")
                    idx > mid -> p.put(x, y, z, stairs, "[facing=${if (alongX) "north" else "west"}]")
                    else -> p.set(x, y, z, slab)
                }
            }
        }
        for (idx in 1..span) {
            val level = if (idx <= mid) idx else span + 1 - idx
            for (dy in 0 until level) {
                if (alongX) {
                    p.set(x0, baseY + dy, z0 + idx - 1, fill)
                    p.set(x0 + len - 1, baseY + dy, z0 + idx - 1, fill)
                } else {
                    p.set(x0 + idx - 1, baseY + dy, z0, fill)
                    p.set(x0 + idx - 1, baseY + dy, z0 + len - 1, fill)
                }
            }
        }
    }

    
    private fun wellFolk(seed: Long, random: Random, p: Place, x0: Int, z0: Int, gen: AeroGenerator, ruined: Boolean) {
        val jobs = arrayOf(Villager.Profession.FARMER, Villager.Profession.NITWIT, Villager.Profession.NONE, Villager.Profession.LIBRARIAN)
        repeat(2 + random.nextInt(3)) {
            val x = x0 + random.nextInt(7) - 2
            val z = z0 + random.nextInt(7) - 2
            p.villager(x, gen.heightAt(seed, x, z) + 1, z, jobs[random.nextInt(jobs.size)], ruined)
        }
        if (!ruined && random.nextInt(100) < 50) p.animal(x0 + 3, gen.heightAt(seed, x0 + 3, z0) + 1, z0, org.bukkit.entity.EntityType.IRON_GOLEM)
    }

    private fun well(seed: Long, random: Random, p: Place, x0: Int, z0: Int, gen: AeroGenerator) {
        val low = Sites.flatBase(seed, gen, x0, z0, 3, 3, 1, 2, true) ?: return
        clear(p, x0, z0, 3, 3, low, 5)
        for (dx in 0..2) for (dz in 0..2) {
            if (dx == 1 && dz == 1) {
                for (y in low - 2..low) p.set(x0 + 1, y, z0 + 1, Material.WATER)
            } else {
                p.set(x0 + dx, low, z0 + dz, Material.COBBLESTONE)
                p.set(x0 + dx, low + 1, z0 + dz, Material.COBBLESTONE_SLAB)
            }
        }
        for ((px, pz) in listOf(0 to 0, 2 to 0, 0 to 2, 2 to 2)) for (y in 2..4) p.put(x0 + px, low + y, z0 + pz, Material.COBBLESTONE_WALL, "[up=true]")
        for (dx in 0..2) for (dz in 0..2) p.set(x0 + dx, low + 5, z0 + dz, Material.OAK_SLAB)
    }

    private fun church(seed: Long, random: Random, p: Place, x0: Int, z0: Int, gen: AeroGenerator) {
        val w = 7
        val d = 13
        val low = Sites.flatBase(seed, gen, x0, z0, w, d, 1, 3, true) ?: return
        clear(p, x0, z0, w, d, low, 12)
        for (dx in 0 until w) for (dz in 0 until d) p.set(x0 + dx, low, z0 + dz, Material.STONE_BRICKS)
        for (dy in 1..5) for (dx in 0 until w) for (dz in 0 until d) {
            val edge = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1
            if (!edge) continue
            val door = dz == d - 1 && dx == w / 2 && dy <= 2
            val window = dy in 3..4 && ((dx == 0 || dx == w - 1) && dz % 3 == 1)
            if (door) continue
            p.set(x0 + dx, low + dy, z0 + dz, if (window) Material.GLASS else if (random.nextInt(100) < 25) Material.MOSSY_COBBLESTONE else Material.COBBLESTONE)
        }
        gable(p, x0, z0, w, d, low + 6, false, Material.STONE_BRICK_STAIRS, Material.STONE_BRICK_SLAB, Material.COBBLESTONE)
        for (dz in 2..9 step 2) for (dx in intArrayOf(1, 2, 4, 5)) {
            if (random.nextInt(100) < 80) p.put(x0 + dx, low + 1, z0 + dz, Material.SPRUCE_STAIRS, "[facing=south]")
        }
        for (dx in 2..4) p.set(x0 + dx, low + 1, z0 + 1, Material.QUARTZ_SLAB)
        p.put(x0 + w / 2, low + 2, z0 + 1, Material.CANDLE, "[candles=3,lit=true]")
        if (Loot.rare(random, 35.0)) Loot.chestAt(p, random, x0 + 1, low + 1, z0 + 1, "salon")
    }

    private fun barn(seed: Long, random: Random, p: Place, x0: Int, z0: Int, gen: AeroGenerator, desert: Boolean) {
        val w = 11
        val d = 8
        val low = Sites.flatBase(seed, gen, x0, z0, w, d, 1, 3, true) ?: return
        clear(p, x0, z0, w, d, low, 9)
        for (dx in 0 until w) for (dz in 0 until d) p.set(x0 + dx, low, z0 + dz, Material.COARSE_DIRT)
        val climate = ClimateMap.at(seed, x0, z0).id
        val timber = if (climate in COLD) Material.SPRUCE_PLANKS else Material.OAK_PLANKS
        for (dy in 1..4) for (dx in 0 until w) for (dz in 0 until d) {
            val edge = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1
            if (!edge) continue
            val door = dz == d - 1 && dx in 4..6 && dy <= 3
            if (door) continue
            if (random.nextInt(100) < 6) continue
            p.set(x0 + dx, low + dy, z0 + dz, if (desert) Material.SANDSTONE else if (dy == 1 || (dx % 5 == 0 && dz % 7 == 0)) Material.DARK_OAK_PLANKS else timber)
        }
        if (desert) gable(p, x0, z0, w, d, low + 5, true, Material.SANDSTONE_STAIRS, Material.SANDSTONE_SLAB, Material.SANDSTONE)
        else gable(p, x0, z0, w, d, low + 5, true, Material.SPRUCE_STAIRS, Material.SPRUCE_SLAB, timber)
        repeat(10) {
            val x = x0 + 1 + random.nextInt(w - 2)
            val z = z0 + 1 + random.nextInt(d - 3)
            if (p.isAir(x, low + 1, z)) p.put(x, low + 1, z, Material.HAY_BLOCK, "[axis=y]")
        }
        repeat(3) {
            val x = x0 + 1 + random.nextInt(w - 2)
            val z = z0 + 1 + random.nextInt(d - 3)
            if (p.isAir(x, low + 1, z)) p.put(x, low + 1, z, Material.BARREL, "[facing=up]")
        }
    }

    private fun farm(seed: Long, random: Random, p: Place, x0: Int, z0: Int, gen: AeroGenerator) {
        val w = 9
        val d = 7
        val low = Sites.flatBase(seed, gen, x0, z0, w, d, 1, 2, true) ?: return
        clear(p, x0, z0, w, d, low, 3)
        for (dx in 0 until w) for (dz in 0 until d) {
            if (dz == 3) {
                p.set(x0 + dx, low, z0 + dz, Material.WATER)
                continue
            }
            p.put(x0 + dx, low, z0 + dz, Material.FARMLAND, "[moisture=7]")
            val r = random.nextInt(100)
            when {
                r < 60 -> p.put(x0 + dx, low + 1, z0 + dz, Material.WHEAT, "[age=7]")
                r < 78 -> p.put(x0 + dx, low + 1, z0 + dz, Material.CARROTS, "[age=7]")
                r < 88 -> p.set(x0 + dx, low + 1, z0 + dz, Material.DEAD_BUSH)
            }
        }
        for ((px, pz) in listOf(-1 to -1, w to -1, -1 to d, w to d)) p.set(x0 + px, low + 1, z0 + pz, Material.OAK_FENCE)
    }

    private fun windmill(seed: Long, random: Random, p: Place, x0: Int, z0: Int, gen: AeroGenerator, desert: Boolean) {
        val low = Sites.flatBase(seed, gen, x0, z0, 5, 6, 1, 2, true) ?: return
        clear(p, x0, z0, 6, 6, low, 14)
        for (dy in 1..8) for (dx in 0..4) for (dz in 0..4) {
            val edge = dx == 0 || dx == 4 || dz == 0 || dz == 4
            if (!edge) continue
            val door = dz == 4 && dx == 2 && dy <= 2
            if (door) continue
            p.set(x0 + dx, low + dy, z0 + dz, if (desert) (if (dy % 4 == 0) Material.CUT_SANDSTONE else Material.SANDSTONE) else if (dy % 4 == 0) Material.STONE_BRICKS else Material.COBBLESTONE)
        }
        for (dx in 0..4) for (dz in 0..4) p.set(x0 + dx, low + 9, z0 + dz, Material.OAK_PLANKS)
        for (dx in 1..3) for (dz in 1..3) p.set(x0 + dx, low + 10, z0 + dz, Material.OAK_PLANKS)
        p.set(x0 + 2, low + 11, z0 + 2, Material.OAK_PLANKS)
        
        val bz = z0 + 5
        for (k in -4..4) {
            if (k == 0) continue
            p.set(x0 + 2, low + 7 + k, bz, Material.SPRUCE_PLANKS)
            p.set(x0 + 2 + k, low + 7, bz, Material.SPRUCE_PLANKS)
        }
        p.set(x0 + 2, low + 7, bz, Material.OAK_LOG)
    }

    private fun graveyard(seed: Long, random: Random, p: Place, x0: Int, z0: Int, gen: AeroGenerator) {
        val low = Sites.flatBase(seed, gen, x0, z0, 6, 5, 1, 2, true) ?: return
        clear(p, x0, z0, 6, 5, low, 4)
        for (dx in 0..5) for (dz in 0..4) if (random.nextInt(100) < 40) p.set(x0 + dx, low + 1, z0 + dz, Material.MOSS_CARPET)
        for (dz in intArrayOf(1, 3)) for (dx in intArrayOf(1, 3, 5)) {
            if (random.nextInt(100) < 85) {
                p.put(x0 + dx, low + 1, z0 + dz, Material.COBBLESTONE_WALL, "[up=true]")
                if (random.nextBoolean()) p.set(x0 + dx, low + 2, z0 + dz, Material.STONE_BRICK_SLAB)
            }
        }
    }

    

    
    private fun wall(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator, c: IntArray) {
        val r = WALL_R
        for (x in bx..bx + 15) for (z in bz..bz + 15) {
            val dx = x - c[0]
            val dz = z - c[1]
            val ax = abs(dx)
            val az = abs(dz)
            if (maxOf(ax, az) > r + 2 || maxOf(ax, az) < r - 2) continue
            val corner = ax >= r - 2 && az >= r - 2
            val onRing = (ax == r && az <= r) || (az == r && ax <= r)
            if (!corner && !onRing) continue
            if (zoneAt(seed, x, z) != Zone.JUNGLE) continue
            val h = gen.heightAt(seed, x, z)
            if (corner) { 
                val ch = gen.heightAt(seed, c[0] + (if (dx > 0) r else -r), c[1] + (if (dz > 0) r else -r))
                val base = minOf(h, ch) - 2
                val edge = ax == r - 2 || ax == r + 2 || az == r - 2 || az == r + 2
                for (y in base..ch + 9) {
                    val solid = y <= ch + 8
                    if (edge) {
                        if (y == ch + 9) { if ((x + z) % 2 == 0) p.set(x, y, z, Material.STONE_BRICKS) }
                        else if (solid) p.set(x, y, z, if (random.nextInt(100) < 20) Material.MOSSY_STONE_BRICKS else Material.STONE_BRICKS)
                    } else if (y <= ch) {
                        p.set(x, y, z, Material.COBBLESTONE)
                    }
                }
                continue
            }
            val gate = (ax == r && az <= 2) || (az == r && ax <= 2) 
            for (y in h - 3..h) p.set(x, y, z, Material.COBBLESTONE)
            if (gate) {
                p.set(x, h, z, Material.DIRT_PATH)
                for (y in h + 1..h + 3) p.set(x, y, z, Material.AIR)
                p.set(x, h + 4, z, Material.SPRUCE_PLANKS)
                continue
            }
            for (y in h + 1..h + 3) p.set(x, y, z, if (random.nextInt(100) < 18) Material.MOSSY_STONE_BRICKS else Material.STONE_BRICKS)
            if ((x + z) % 2 == 0) p.set(x, h + 4, z, Material.STONE_BRICKS)
        }
    }

    
    private fun keep(seed: Long, random: Random, p: Place, x0: Int, z0: Int, gen: AeroGenerator) {
        val w = 9
        val low = Sites.flatBase(seed, gen, x0, z0, w, w, 1, 3, true) ?: return
        clear(p, x0, z0, w, w, low, 14)
        for (dx in 0 until w) for (dz in 0 until w) p.set(x0 + dx, low, z0 + dz, Material.STONE_BRICKS)
        for (dy in 1..11) for (dx in 0 until w) for (dz in 0 until w) {
            val edge = dx == 0 || dx == w - 1 || dz == 0 || dz == w - 1
            if (edge) {
                val door = dz == w - 1 && dx == w / 2 && dy <= 2
                val slit = dy in 4..5 && ((dx == 0 || dx == w - 1) && dz % 4 == 2 || (dz == 0 || dz == w - 1) && dx % 4 == 2)
                if (door || slit) continue
                if (dy == 11 && (dx + dz) % 2 == 0) continue 
                p.set(x0 + dx, low + dy, z0 + dz, if (random.nextInt(100) < 22) Material.MOSSY_STONE_BRICKS else Material.STONE_BRICKS)
            } else if (dy == 5 && !(dx == 4 && dz == 1)) {
                p.set(x0 + dx, low + dy, z0 + dz, Material.SPRUCE_PLANKS) 
            }
        }
        for (y in 1..5) p.put(x0 + 4, low + y, z0 + 1, Material.LADDER, "[facing=south]")
        for (dx in 1..3) p.put(x0 + dx, low + 1, z0 + 6, Material.SPRUCE_STAIRS, "[facing=north]")
        if (Loot.rare(random, 45.0)) Loot.chestAt(p, random, x0 + 7, low + 1, z0 + 2, "tower")
    }

    
    private fun market(seed: Long, random: Random, p: Place, x0: Int, z0: Int, gen: AeroGenerator, desert: Boolean) {
        val w = 9
        val d = 5
        val low = Sites.flatBase(seed, gen, x0, z0, w, d, 1, 2, true) ?: return
        clear(p, x0, z0, w, d, low, 6)
        for (dx in 0 until w) for (dz in 0 until d) p.set(x0 + dx, low, z0 + dz, if (desert) Material.SMOOTH_SANDSTONE else Material.GRAVEL)
        for ((px, pz) in listOf(0 to 0, w - 1 to 0, 0 to d - 1, w - 1 to d - 1, 4 to 0, 4 to d - 1)) for (y in 1..3) p.set(x0 + px, low + y, z0 + pz, Material.OAK_FENCE)
        val awning = if (desert) Material.WHITE_WOOL else Material.GRAY_WOOL
        for (dx in 0 until w) for (dz in 0 until d) if (random.nextInt(100) < 90) p.set(x0 + dx, low + 4, z0 + dz, awning)
        for (dx in 1..w - 2) {
            p.set(x0 + dx, low + 1, z0 + 1, if (desert) Material.SANDSTONE_SLAB else Material.SPRUCE_SLAB)
            when (random.nextInt(4)) {
                0 -> p.put(x0 + dx, low + 2, z0 + 1, Material.BARREL, "[facing=up]")
                1 -> p.put(x0 + dx, low + 2, z0 + 1, Material.HAY_BLOCK, "[axis=y]")
                2 -> p.set(x0 + dx, low + 2, z0 + 1, Material.MELON)
                else -> Unit
            }
        }
        if (Loot.rare(random, 30.0)) Loot.chestAt(p, random, x0 + 4, low + 1, z0 + 3, "mall")
    }
}
