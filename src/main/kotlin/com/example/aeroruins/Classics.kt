package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.entity.EntityType
import java.util.Random
import kotlin.math.abs

private fun fdiv(a: Int, b: Int): Int = Math.floorDiv(a, b)






object Stronghold {
    private const val CELL = 2400
    private const val CHANCE = 1

    
    fun center(seed: Long, gx: Int, gz: Int): IntArray? {
        val h = mix(seed, gx, gz, 220)
        if (h % 100 >= CHANCE) return null
        val cx = gx * CELL + 200 + (h ushr 6) % (CELL - 400)
        val cz = gz * CELL + 200 + (h ushr 16) % (CELL - 400)
        if (zoneAt(seed, cx, cz) != Zone.JUNGLE) return null
        val s = Terrain.heightAt(seed, cx, cz)
        if (s < SEA_LEVEL + 3 || s > 110) return null
        return intArrayOf(cx, cz, 18 + (h shr 12) % 12)
    }

    fun centersAround(seed: Long, x: Int, z: Int, r: Int): List<IntArray> {
        val out = ArrayList<IntArray>()
        for (gx in fdiv(x, CELL) - r..fdiv(x, CELL) + r) for (gz in fdiv(z, CELL) - r..fdiv(z, CELL) + r) {
            center(seed, gx, gz)?.let { out.add(it) }
        }
        return out
    }

    
    fun entrance(c: IntArray): IntArray = intArrayOf(c[0] - 30 + 3, c[1] - 30 + 3)

    private fun present(seed: Long, c: IntArray, i: Int, j: Int, south: Boolean): Boolean =
        mix(seed, c[0] + i * 31 + (if (south) 7 else 0), c[1] + j * 37, 221) % 100 < 85

    private fun brick(random: Random): Material {
        val r = random.nextInt(100)
        return when {
            r < 55 -> Material.STONE_BRICKS
            r < 75 -> Material.MOSSY_STONE_BRICKS
            r < 92 -> Material.CRACKED_STONE_BRICKS
            else -> Material.INFESTED_STONE_BRICKS
        }
    }

    fun populate(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val c = center(seed, fdiv(bx, CELL), fdiv(bz, CELL)) ?: return
        val x0 = c[0] - SchematicStronghold.width() / 2
        val z0 = c[1] - SchematicStronghold.length() / 2
        if (bx + 15 < x0 || bx >= x0 + SchematicStronghold.width() ||
            bz + 15 < z0 || bz >= z0 + SchematicStronghold.length()
        ) return
        SchematicStronghold.place(p, random, x0, c[2], z0)
    }

    private fun corridor(p: Place, random: Random, ax: Int, az: Int, y0: Int, alongX: Boolean) {
        // Vanilla stronghold halls are five blocks wide, with a solid floor and
        // ceiling and a three-block-high passage between the side walls.
        for (a in 0 until 7) for (b in 0 until 5) {
            val x = if (alongX) ax + a else ax + b
            val z = if (alongX) az + b else az + a
            val side = b == 0 || b == 4
            p.set(x, y0, z, brick(random))
            p.set(x, y0 + 4, z, brick(random))
            for (y in y0 + 1..y0 + 3) p.set(x, y, z, if (side) brick(random) else Material.AIR)
            if (!side && random.nextInt(100) < 18) p.set(x, y0 + 3, z, Material.COBWEB)
        }
    }

    private fun room(seed: Long, random: Random, p: Place, gen: AeroGenerator, c: IntArray, i: Int, j: Int, ox: Int, oz: Int, y0: Int) {
        val doorW = i > 0 && present(seed, c, i - 1, j, false)
        val doorE = i < 2 && present(seed, c, i, j, false)
        val doorN = j > 0 && present(seed, c, i, j - 1, true)
        val doorS = j < 2 && present(seed, c, i, j, true)
        for (dx in 0..13) for (dz in 0..13) {
            val x = ox + dx
            val z = oz + dz
            val edge = dx == 0 || dx == 13 || dz == 0 || dz == 13
            p.set(x, y0, z, brick(random))
            p.set(x, y0 + 6, z, brick(random))
            for (y in y0 + 1..y0 + 5) {
                if (edge) {
                    val door = y <= y0 + 3 && ((dx == 0 && doorW && dz in 5..7) || (dx == 13 && doorE && dz in 5..7) ||
                        (dz == 0 && doorN && dx in 5..7) || (dz == 13 && doorS && dx in 5..7))
                    p.set(x, y, z, if (door) Material.AIR else brick(random))
                } else {
                    p.set(x, y, z, Material.AIR)
                }
            }
        }
        val cx = ox + 7
        val cz = oz + 7
        val kind = if (i == 1 && j == 1) -1 else mix(seed, c[0] + i, c[1] + j, 222) % 6
        when (kind) {
            -1 -> portalRoom(p, random, cx, cz, y0)
            0 -> {
                // Two-storey library: shelves surround a reading floor and the
                // central aisle remains open like the vanilla stronghold library.
                for (dx in 1..4) for (y in 1..3) {
                    p.set(ox + dx, y0 + y, oz + 1, Material.BOOKSHELF)
                    p.set(ox + dx, y0 + y, oz + 12, Material.BOOKSHELF)
                }
                for (dx in 9..12) for (y in 1..3) {
                    p.set(ox + dx, y0 + y, oz + 1, Material.BOOKSHELF)
                    p.set(ox + dx, y0 + y, oz + 12, Material.BOOKSHELF)
                }
                for (dz in 3..10 step 2) for (y in 1..2)
                    p.set(ox + 2, y0 + y, oz + dz, Material.BOOKSHELF)
                p.put(cx, y0 + 1, cz, Material.LECTERN, "[facing=south]")
                if (Loot.rare(random, 70.0)) Loot.chestAt(p, random, ox + 3, y0 + 1, oz + 6, "salon")
            }
            1 -> { 
                for (dz in 2..11) if (dz % 4 != 0) for (y in 1..3) {
                    p.put(ox + 5, y0 + y, oz + dz, Material.IRON_BARS, "[north=true,south=true]")
                    p.put(ox + 8, y0 + y, oz + dz, Material.IRON_BARS, "[north=true,south=true]")
                }
                if (Loot.rare(random, 30.0)) Loot.chestAt(p, random, ox + 3, y0 + 1, oz + 3, "deep")
            }
            2 -> { 
                for (dx in -2..2) for (dz in -2..2) {
                    val ring = abs(dx) == 2 || abs(dz) == 2
                    if (ring) p.set(cx + dx, y0 + 1, cz + dz, Material.STONE_BRICK_SLAB) else p.set(cx + dx, y0, cz + dz, Material.WATER)
                }
            }
            3 -> { 
                for ((px, pz) in listOf(1 to 1, 12 to 1, 1 to 12, 12 to 12)) {
                    p.put(ox + px, y0 + 1, oz + pz, Material.BARREL, "[facing=up]")
                    p.put(ox + px, y0 + 2, oz + pz, Material.BARREL, "[facing=up]")
                }
                if (Loot.rare(random, 60.0)) Loot.chestAt(p, random, ox + 6, y0 + 1, oz + 1, "tower")
                if (Loot.rare(random, 45.0)) Loot.chestAt(p, random, ox + 8, y0 + 1, oz + 12, "tower")
            }
            4 -> { 
                for (dx in -1..1) for (dz in -1..1) p.set(cx + dx, y0 + 1, cz + dz, Material.STONE_BRICKS)
                p.set(cx, y0 + 2, cz, Material.CHISELED_STONE_BRICKS)
                p.put(cx, y0 + 3, cz, Material.CANDLE, "[candles=3,lit=true]")
                if (Loot.rare(random, 40.0)) Loot.chestAt(p, random, cx, y0 + 1, cz + 3, "deep")
            }
            else -> { 
                for ((px, pz) in listOf(3 to 3, 3 to 10, 10 to 3, 10 to 10)) {
                    for (y in 1..5) p.set(ox + px, y0 + y, oz + pz, if (random.nextInt(100) < 25) Material.MOSSY_STONE_BRICKS else Material.STONE_BRICKS)
                }
            }
        }
        if (i == 0 && j == 0) entranceShaft(seed, random, p, gen, ox, oz, y0)
    }

    private fun portalRoom(p: Place, random: Random, cx: Int, cz: Int, y0: Int) {
        for (dx in -1..1) for (dz in -1..1) {
            p.set(cx + dx, y0, cz + dz, Material.LAVA)
            p.set(cx + dx, y0 - 1, cz + dz, Material.STONE_BRICKS)
        }
        for (k in -1..1) {
            frame(p, random, cx + k, y0 + 1, cz - 2, "south")
            frame(p, random, cx + k, y0 + 1, cz + 2, "north")
            frame(p, random, cx - 2, y0 + 1, cz + k, "east")
            frame(p, random, cx + 2, y0 + 1, cz + k, "west")
        }
        for (dx in -3..3) for (dz in intArrayOf(-4, 4))
            p.set(cx + dx, y0 + 1, cz + dz, Material.IRON_BARS)
        for (dx in -3..3) p.set(cx + dx, y0 + 1, cz - 5, Material.STONE_BRICKS) 
        p.spawner(cx, y0 + 2, cz - 5, EntityType.SILVERFISH)
    }

    private fun frame(p: Place, random: Random, x: Int, y: Int, z: Int, face: String) {
        p.put(x, y, z, Material.END_PORTAL_FRAME, "[facing=$face,eye=${random.nextInt(100) < 10}]")
    }

    
    private fun entranceShaft(seed: Long, random: Random, p: Place, gen: AeroGenerator, ox: Int, oz: Int, y0: Int) {
        val sh = gen.heightAt(seed, ox + 3, oz + 3)
        if (sh <= y0 + 2) return
        for (dx in 1..5) for (dz in 1..5) {
            val ring = dx == 1 || dx == 5 || dz == 1 || dz == 5
            for (y in y0 + 1..sh) {
                if (ring) p.set(ox + dx, y, oz + dz, if (random.nextInt(100) < 30) Material.MOSSY_STONE_BRICKS else Material.STONE_BRICKS)
                else p.set(ox + dx, y, oz + dz, Material.AIR)
            }
            if (ring && random.nextInt(100) < 60) p.set(ox + dx, sh + 1, oz + dz, Material.MOSSY_COBBLESTONE)
        }
        for (y in y0 + 1..sh) p.put(ox + 3, y, oz + 2, Material.LADDER, "[facing=south]")
    }
}


object Classics {
    private val DRY = setOf("yellow_plains", "scrub_savanna", "dust_flats", "dry_acacia_plain", "sun_bleached_grove", "rust_meadow", "dandelion_plain")

    fun populate(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        if (!Settings.classics) return
        if (AncientCity.populate(seed, random, p, bx, bz)) return
        if (TrialChamber.populate(seed, random, p, bx, bz)) return
        if (WaterMonument.populate(seed, random, p, bx, bz)) return
        Stronghold.populate(seed, random, p, bx, bz, gen)
        val mx = bx + 8
        val mz = bz + 8
        if (zoneAt(seed, mx, mz) != Zone.JUNGLE || !gen.usesTerrain(seed, mx, mz)) return
        if (Realms.at(seed, mx, mz) == Realm.NEIGHBORHOOD) return
        val climate = ClimateMap.at(seed, mx, mz)
        val r = random.nextInt(1000)
        when {
            r < 22 -> dungeon(seed, random, p, bx, bz, gen)
            r < 31 -> mineshaft(seed, random, p, bx, bz, gen)
            r < 36 -> ruinedPortal(seed, random, p, bx, bz, gen)
            r < 39 -> outpost(seed, random, p, bx, bz, gen)
            r < 43 && climate.id in DRY -> pyramid(seed, random, p, bx, bz, gen)
            r < 46 && climate.id in DRY -> desertWell(seed, random, p, bx, bz, gen)
            r < 49 && climate.id == "grey_fen" -> witchHut(seed, random, p, bx, bz, gen)
            r < 52 && climate.cold -> igloo(seed, random, p, bx, bz, gen)
            r < 55 && climate.warm -> jungleTemple(seed, random, p, bx, bz, gen)
        }
    }

    private fun dungeon(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 3 + random.nextInt(6)
        val z0 = bz + 3 + random.nextInt(6)
        val surface = gen.heightAt(seed, x0 + 3, z0 + 3)
        if (surface < 40) return
        // Keep the room in the normal underground band instead of intersecting
        // the surface on low hills.
        val y0 = maxOf(10, minOf(surface - 8, surface - 12 - random.nextInt(10)))
        for (dx in 0..6) for (dz in 0..6) for (y in y0..y0 + 5) {
            val edge = dx == 0 || dx == 6 || dz == 0 || dz == 6 || y == y0 || y == y0 + 5
            val m = if (edge) (if (random.nextInt(100) < 35) Material.MOSSY_COBBLESTONE else Material.COBBLESTONE) else Material.AIR
            p.set(x0 + dx, y, z0 + dz, m)
        }
        val mobs = arrayOf(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SPIDER)
        p.spawner(x0 + 3, y0 + 1, z0 + 3, mobs[random.nextInt(mobs.size)])
        Loot.chestAt(p, random, x0 + 1, y0 + 1, z0 + 1, "cave")
        Loot.chestAt(p, random, x0 + 5, y0 + 1, z0 + 5, "cave")
    }

    private fun mineshaft(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val alongX = random.nextBoolean()
        val x0 = bx + 1 + random.nextInt(2)
        val z0 = bz + 1 + random.nextInt(2)
        val surface = gen.heightAt(seed, x0 + 8, z0 + 8)
        if (surface < 46) return
        val y = 18 + random.nextInt(minOf(20, surface - 30))
        for (a in 0..14) for (b in 0..2) {
            val x = if (alongX) x0 + a else x0 + b
            val z = if (alongX) z0 + b else z0 + a
            for (yy in y + 1..y + 3) p.set(x, yy, z, Material.AIR)
            p.set(x, y, z, Material.OAK_PLANKS)
            if (a % 4 == 0) { 
                if (b != 1) for (yy in y + 1..y + 2) p.set(x, yy, z, Material.OAK_FENCE)
                p.set(x, y + 3, z, Material.OAK_PLANKS)
            } else if (b == 1 && random.nextInt(100) < 65) {
                p.put(x, y + 1, z, Material.RAIL, if (alongX) "[shape=east_west]" else "[shape=north_south]")
            } else if (random.nextInt(100) < 6) {
                p.set(x, y + 2, z, Material.COBWEB)
            }
        }
        val mx = if (alongX) x0 + 7 else x0 + 1
        val mz = if (alongX) z0 + 1 else z0 + 7
        if (random.nextInt(100) < 15) p.spawner(mx, y + 1, mz, EntityType.CAVE_SPIDER)
        val ex = if (alongX) x0 + 14 else x0 + 1
        val ez = if (alongX) z0 + 1 else z0 + 14
        if (Loot.rare(random, 40.0)) Loot.chestAt(p, random, ex, y + 1, ez, "cave")
    }

    private fun ruinedPortal(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 4 + random.nextInt(4)
        val z0 = bz + 4 + random.nextInt(4)
        val low = Sites.flatBase(seed, gen, x0, z0, 6, 5, 1, 3) ?: return
        for (dx in -1..6) for (dz in -1..5) {
            if (random.nextInt(100) >= 45) continue
            val r = random.nextInt(100)
            p.set(x0 + dx, low, z0 + dz, if (r < 15) Material.MAGMA_BLOCK else if (r < 30) Material.BLACKSTONE else Material.NETHERRACK)
        }
        for (dx in 0..3) for (dy in 0..4) {
            val edge = dx == 0 || dx == 3 || dy == 0 || dy == 4
            if (!edge || random.nextInt(100) < 18) continue
            p.set(x0 + 1 + dx, low + 1 + dy, z0 + 2, if (random.nextInt(100) < 20) Material.CRYING_OBSIDIAN else Material.OBSIDIAN)
        }
        if (random.nextInt(100) < 40) p.set(x0 + 2, low, z0 + 1, Material.GOLD_BLOCK)
        if (Loot.rare(random, 70.0)) Loot.chestAt(p, random, x0 + 5, low + 1, z0 + 3, "deep")
    }

    private fun outpost(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 3 + random.nextInt(3)
        val z0 = bz + 3 + random.nextInt(3)
        val low = Sites.flatBase(seed, gen, x0 - 1, z0 - 1, 12, 8, 1, 3) ?: return
        
        for (y in 1..10) for (dx in 0..4) for (dz in 0..4) {
            val corner = (dx == 0 || dx == 4) && (dz == 0 || dz == 4)
            if (corner) p.set(x0 + dx, low + y, z0 + dz, if (y <= 2) Material.COBBLESTONE else Material.DARK_OAK_LOG)
            else if (dz == 0 && y <= 9) p.set(x0 + dx, low + y, z0 + dz, Material.DARK_OAK_PLANKS)
        }
        for (floorY in intArrayOf(5, 9)) {
            for (dx in 0..4) for (dz in 0..4) p.set(x0 + dx, low + floorY, z0 + dz, Material.DARK_OAK_PLANKS)
            for (dx in 0..4) for (dz in 0..4) {
                if (dx == 0 || dx == 4 || dz == 0 || dz == 4) p.set(x0 + dx, low + floorY + 1, z0 + dz, Material.DARK_OAK_FENCE)
            }
        }
        for (y in 1..8) p.put(x0 + 2, low + y, z0 + 1, Material.LADDER, "[facing=south]")
        p.set(x0 + 2, low + 9, z0 + 1, Material.AIR) 
        Loot.chestAt(p, random, x0 + 3, low + 10, z0 + 3, "tower")
        Loot.mobAt(p, x0 + 1, low + 10, z0 + 2, EntityType.PILLAGER)
        Loot.mobAt(p, x0 + 2, low + 6, z0 + 3, EntityType.PILLAGER)
        
        val tx = x0 + 7
        for (dx in 0..2) for (dz in 0..2) {
            p.set(tx + dx, low + 1, z0 + 1 + dz, if (dx == 1 && dz == 1) Material.AIR else Material.GRAY_WOOL)
        }
        for (dx in 0..2) for (dz in 0..2) p.set(tx + dx, low + 2, z0 + 1 + dz, Material.GRAY_WOOL)
    }

    private fun pyramid(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 2
        val z0 = bz + 2
        val low = Sites.flatBase(seed, gen, x0, z0, 21, 21, 1, 3) ?: return
        // Desert temples taper to a four-block cap; leave a real hollow
        // interior rather than producing a solid pyramid.
        for (k in 0..9) {
            val size = 21 - 2 * k
            for (dx in 0 until size) for (dz in 0 until size) {
                val stripe = k == 4 || k == 7
                val m = if (stripe && (dx == 0 || dz == 0 || dx == size - 1 || dz == size - 1)) Material.ORANGE_TERRACOTTA else Material.SANDSTONE
                p.set(x0 + k + dx, low + 1 + k, z0 + k + dz, m)
            }
        }
        
        for (dx in 7..13) for (dz in 7..13) for (y in 1..8) p.set(x0 + dx, low + y, z0 + dz, Material.AIR)
        for (dx in 9..11) for (y in 1..3) for (dz in 13..20) p.set(x0 + dx, low + y, z0 + dz, Material.AIR)
        for (dx in 8..12) for (dz in 8..12) p.set(x0 + dx, low, z0 + dz, if ((dx + dz) % 2 == 0) Material.CHISELED_SANDSTONE else Material.SMOOTH_SANDSTONE)
        for ((cx, cz) in listOf(8 to 8, 12 to 8, 8 to 12, 12 to 12)) Loot.chestAt(p, random, x0 + cx, low + 1, z0 + cz, "tower")
    }

    private fun desertWell(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 6
        val z0 = bz + 6
        val low = Sites.flatBase(seed, gen, x0, z0, 5, 5, 2, 2) ?: return
        for (dx in 0..4) for (dz in 0..4) {
            p.set(x0 + dx, low, z0 + dz, Material.SANDSTONE)
            if (dx in 1..3 && dz in 1..3) p.set(x0 + dx, low + 1, z0 + dz, Material.WATER)
        }
        for ((dx, dz) in listOf(0 to 0, 4 to 0, 0 to 4, 4 to 4)) {
            p.set(x0 + dx, low + 1, z0 + dz, Material.SANDSTONE)
            p.set(x0 + dx, low + 2, z0 + dz, Material.SANDSTONE)
        }
        for (dx in 0..4) for (dz in 0..4) {
            p.set(x0 + dx, low + 3, z0 + dz, Material.SANDSTONE_SLAB)
            if (dx == 2 || dz == 2) p.set(x0 + dx, low + 4, z0 + dz, Material.SANDSTONE_SLAB)
        }
    }

    private fun witchHut(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 4
        val z0 = bz + 4
        val low = Sites.flatBase(seed, gen, x0, z0, 7, 7, 2, 3) ?: return
        for (dx in 0..6) for (dz in 0..6) {
            for (y in low + 1..low + 5) p.set(x0 + dx, y, z0 + dz, Material.AIR)
            if (dx in 1..5 && dz in 1..5) p.set(x0 + dx, low, z0 + dz, Material.OAK_PLANKS)
        }
        for (dx in 0..6) for (dz in 0..6) {
            val edge = dx == 0 || dx == 6 || dz == 0 || dz == 6
            if (edge) for (y in 1..3) p.set(x0 + dx, low + y, z0 + dz, Material.OAK_PLANKS)
        }
        for (dx in -1..7) for (dz in -1..7) {
            val roof = abs(dx - 3) + abs(dz - 3) <= 5
            if (roof) p.set(x0 + dx, low + 4, z0 + dz, Material.SPRUCE_PLANKS)
        }
        for (dx in 0..6) p.put(x0 + dx, low + 5, z0 + 3, Material.SPRUCE_STAIRS, "[facing=south]")
        p.set(x0 + 2, low + 1, z0 + 2, Material.CAULDRON)
        p.set(x0 + 4, low + 1, z0 + 2, Material.CRAFTING_TABLE)
        p.set(x0 + 3, low + 1, z0 + 4, Material.BARREL)
        Loot.mobAt(p, x0 + 3, low + 1, z0 + 3, EntityType.WITCH)
    }

    private fun igloo(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 4
        val z0 = bz + 4
        val low = Sites.flatBase(seed, gen, x0, z0, 7, 7, 2, 3) ?: return
        for (dx in 0..6) for (dz in 0..6) {
            for (y in low + 1..low + 5) p.set(x0 + dx, y, z0 + dz, Material.AIR)
            val d = (dx - 3) * (dx - 3) + (dz - 3) * (dz - 3)
            if (d <= 10) p.set(x0 + dx, low + 1, z0 + dz, Material.SNOW_BLOCK)
            if (d in 5..18) p.set(x0 + dx, low + 2, z0 + dz, Material.SNOW_BLOCK)
            if (d <= 8) p.set(x0 + dx, low + 3, z0 + dz, Material.SNOW_BLOCK)
        }
        p.set(x0 + 3, low + 1, z0 + 1, Material.RED_CARPET)
        p.set(x0 + 2, low + 1, z0 + 3, Material.FURNACE)
        p.set(x0 + 4, low + 1, z0 + 3, Material.CRAFTING_TABLE)
        p.put(x0 + 3, low + 1, z0 + 5, Material.SPRUCE_DOOR, "[half=lower,facing=south]")
        p.put(x0 + 3, low + 2, z0 + 5, Material.SPRUCE_DOOR, "[half=upper,facing=south]")
        if (Loot.rare(random, 55.0)) Loot.chestAt(p, random, x0 + 1, low + 1, z0 + 3, "cave")
    }

    private fun jungleTemple(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 2
        val z0 = bz + 2
        val low = Sites.flatBase(seed, gen, x0, z0, 11, 13, 2, 4) ?: return
        for (tier in 0..2) {
            val left = tier
            val right = 10 - tier
            val front = tier
            val back = 12 - tier
            for (x in left..right) for (z in front..back) {
                val edge = x == left || x == right || z == front || z == back
                if (edge) p.set(x0 + x, low + 1 + tier * 2, z0 + z, if (random.nextInt(100) < 18) Material.MOSSY_COBBLESTONE else Material.COBBLESTONE)
            }
        }
        for (y in low + 1..low + 6) {
            p.set(x0 + 5, y, z0 + 1, Material.AIR)
            p.set(x0 + 5, y, z0 + 2, Material.AIR)
        }
        for (x in 2..8) p.set(x0 + x, low + 7, z0 + 6, Material.COBBLESTONE)
        p.put(x0 + 5, low + 1, z0 + 12, Material.JUNGLE_DOOR, "[half=lower,facing=south]")
        p.put(x0 + 5, low + 2, z0 + 12, Material.JUNGLE_DOOR, "[half=upper,facing=south]")
        p.put(x0 + 3, low + 1, z0 + 5, Material.TRIPWIRE_HOOK, "[facing=east]")
        p.put(x0 + 7, low + 1, z0 + 5, Material.TRIPWIRE_HOOK, "[facing=west]")
        p.set(x0 + 5, low + 1, z0 + 5, Material.TRIPWIRE)
        p.set(x0 + 5, low + 1, z0 + 3, Material.DISPENSER)
        Loot.chestAt(p, random, x0 + 2, low + 1, z0 + 9, "cave")
        Loot.chestAt(p, random, x0 + 8, low + 1, z0 + 9, "cave")
    }
}
