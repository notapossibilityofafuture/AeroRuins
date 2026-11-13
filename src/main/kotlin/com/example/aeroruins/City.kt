package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.TreeType
import java.util.Random


object City {
    private val CAR_COLORS = arrayOf(
        Material.GRAY_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.WHITE_CONCRETE, Material.RED_TERRACOTTA,
        Material.BLUE_TERRACOTTA, Material.BROWN_TERRACOTTA, Material.GREEN_TERRACOTTA, Material.BLACK_CONCRETE
    )

    
    fun car(p: Place, random: Random, x: Int, y: Int, z: Int, alongX: Boolean) {
        val body = CAR_COLORS[random.nextInt(CAR_COLORS.size)]
        for (i in 0..3) for (j in 0..1) {
            val px = if (alongX) x + i else x + j
            val pz = if (alongX) z + j else z + i
            if (!p.isAir(px, y, pz)) return
        }
        for (i in 0..3) for (j in 0..1) {
            val px = if (alongX) x + i else x + j
            val pz = if (alongX) z + j else z + i
            p.set(px, y, pz, body)
            if (i in 1..2) {
                p.set(px, y + 1, pz, if (random.nextInt(100) < 25) Material.AIR else Material.LIGHT_GRAY_STAINED_GLASS)
                p.set(px, y + 2, pz, body)
            }
        }
    }

    
    fun lamp(p: Place, random: Random, x: Int, groundY: Int, z: Int) {
        for (i in 1..4) p.set(x, groundY + i, z, Material.DARK_OAK_FENCE)
        p.set(x, groundY + 5, z, if (random.nextInt(100) < 55) Material.OCHRE_FROGLIGHT else Material.GRAY_CONCRETE)
    }

    
    fun trafficLight(p: Place, random: Random, x: Int, groundY: Int, z: Int) {
        for (i in 1..4) p.put(x, groundY + i, z, Material.COBBLESTONE_WALL, "[up=true]")
        val lit = random.nextInt(3)
        val on = arrayOf(Material.RED_CONCRETE, Material.YELLOW_CONCRETE, Material.GREEN_CONCRETE)
        val off = arrayOf(Material.RED_TERRACOTTA, Material.YELLOW_TERRACOTTA, Material.GREEN_TERRACOTTA)
        for (i in 0..2) p.set(x, groundY + 5 + i, z, if (i == lit) on[i] else off[i])
        p.set(x, groundY + 8, z, Material.GRAY_CONCRETE)
    }

    private fun clearPad(p: Place, x0: Int, z0: Int, w: Int, d: Int, low: Int, floor: Material) {
        for (dx in 0 until w) for (dz in 0 until d) {
            for (y in low + 1..low + 9) p.set(x0 + dx, y, z0 + dz, Material.AIR)
            p.set(x0 + dx, low, z0 + dz, floor)
        }
    }

    
    fun lot(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        when (random.nextInt(100)) {
            in 0..13 -> Suburb.house(seed, random, p, bx, bz, gen, decay = 12)
            in 14..28 -> Apartments.build(seed, random, p, bx, bz, gen)
            in 29..36 -> Towers.build(seed, random, p, bx, bz, gen)
            in 37..51 -> shop(seed, random, p, bx, bz, gen)
            in 52..59 -> gasStation(seed, random, p, bx, bz, gen)
            in 60..71 -> parkingLot(seed, random, p, bx, bz, gen)
            in 72..79 -> busStop(seed, random, p, bx, bz, gen)
            in 80..90 -> park(seed, random, p, bx, bz, gen)
            in 91..94 -> Frames.build(seed, random, p, bx, bz, gen)
            in 95..97 -> Salon.build(seed, random, p, bx, bz, gen)
            else -> Tokyo3.build(seed, random, p, bx, bz, gen)
        }
    }

    
    fun shop(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val w = 12
        val d = 8
        val x0 = bx + 2 + random.nextInt(3)
        val z0 = bz + 2 + random.nextInt(3)
        val low = Sites.flatBase(seed, gen, x0, z0, w, d, 1, 3) ?: return
        clearPad(p, x0, z0, w, d, low, if (random.nextBoolean()) Material.GRAVEL else Material.POLISHED_ANDESITE)
        val wall = if (random.nextBoolean()) Material.BRICKS else Material.WHITE_TERRACOTTA
        val trim = if (wall == Material.BRICKS) Material.SPRUCE_LOG else Material.DARK_OAK_LOG
        for (dy in 1..4) for (dx in 0 until w) for (dz in 0 until d) {
            val edge = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1
            if (!edge) continue
            val front = dz == d - 1
            val door = front && dx in 5..6 && dy <= 2
            if (door) continue
            val corner = (dx == 0 || dx == w - 1) && (dz == 0 || dz == d - 1)
            val m = when {
                corner -> trim
                front && dy in 2..3 -> Material.GLASS_PANE
                else -> wall
            }
            p.set(x0 + dx, low + dy, z0 + dz, m)
        }
        for (dx in 0 until w) for (dz in 0 until d) p.set(x0 + dx, low + 5, z0 + dz, trim)
        val roofY = low + 6
        val roofRows = d / 2
        for (row in 0..roofRows) {
            val y = roofY + row
            val frontZ = z0 - 1 + row
            val backZ = z0 + d - row
            for (x in x0 - 1..x0 + w) {
                p.put(x, y, frontZ, Material.SPRUCE_STAIRS, "[facing=south]")
                p.put(x, y, backZ, Material.SPRUCE_STAIRS, "[facing=north]")
            }
        }
        for (x in x0 - 1..x0 + w) {
            p.set(x, roofY + roofRows + 1, z0 + d / 2, Material.SPRUCE_SLAB)
            p.set(x, roofY + roofRows + 1, z0 + d / 2 - 1, Material.SPRUCE_SLAB)
        }
        for (dx in 1 until w - 1) {
            if (random.nextInt(100) < 85) p.set(x0 + dx, low + 1, z0 + 1, Material.BOOKSHELF)
            if (random.nextInt(100) < 60) p.set(x0 + dx, low + 2, z0 + 1, Material.BOOKSHELF)
        }
        for (dx in 2..6) p.set(x0 + dx, low + 1, z0 + d - 3, Material.SPRUCE_PLANKS) 
        StructureDetails.facade(p, random, x0, z0, w, d, low, 4, modern = true)
        StructureDetails.roofline(p, x0, z0, w, d, low + 5, modern = false)
        StructureDetails.plaza(p, random, x0 - 1, z0 - 1, w + 2, d + 2, low)
        Interiors.furnish(p, random, x0, z0, w, d, 2, listOf(low), 16)
        if (Loot.rare(random, 35.0)) Loot.chestAt(p, random, x0 + w - 2, low + 1, z0 + 2, "mall")
    }

    
    fun gasStation(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val w = 20
        val d = 16
        val x0 = bx + 1
        val z0 = bz + 1
        val low = Sites.flatBase(seed, gen, x0, z0, w, d, 1, 3) ?: return
        clearPad(p, x0, z0, w, d, low, Material.GRAY_CONCRETE)
        for (dx in 0 until w) for (dz in 0 until d) if (random.nextInt(100) < 8) p.set(x0 + dx, low, z0 + dz, Material.GRAVEL)
        
        for (dx in 3..16) for (dz in 2..9) {
            p.set(x0 + dx, low + 6, z0 + dz, Material.WHITE_CONCRETE)
            val trim = dx == 3 || dx == 16 || dz == 2 || dz == 9
            p.set(x0 + dx, low + 7, z0 + dz, if (trim) Material.LIGHT_GRAY_CONCRETE else Material.WHITE_CONCRETE)
        }
        for ((px, pz) in listOf(4 to 3, 4 to 8, 15 to 3, 15 to 8)) for (y in 1..5) p.set(x0 + px, low + y, z0 + pz, Material.LIGHT_GRAY_CONCRETE)
        
        for (px in intArrayOf(8, 12)) for (pz in 5..6) {
            p.set(x0 + px, low + 1, z0 + pz, Material.RED_TERRACOTTA)
            p.set(x0 + px, low + 2, z0 + pz, Material.LIGHT_GRAY_STAINED_GLASS)
        }
        
        val sx = x0 + 5
        val sz = z0 + 11
        for (dy in 1..4) for (dx in 0..9) for (dz in 0..4) {
            val edge = dx == 0 || dx == 9 || dz == 0 || dz == 4
            if (!edge) continue
            val front = dz == 0
            val door = front && dx in 4..5 && dy <= 2
            if (door) continue
            p.set(sx + dx, low + dy, sz + dz, if (front && dy in 2..3) Material.LIGHT_GRAY_STAINED_GLASS else Material.WHITE_CONCRETE)
        }
        for (dx in 0..9) for (dz in 0..4) p.set(sx + dx, low + 5, sz + dz, Material.GRAY_CONCRETE)
        StructureDetails.roofline(p, sx, sz, 10, 5, low + 6, modern = true)
        StructureDetails.plaza(p, random, x0, z0, w, d, low)
        Interiors.furnish(p, random, sx, sz, 10, 5, 1, listOf(low), 10)
        if (Loot.rare(random, 40.0)) Loot.chestAt(p, random, sx + 8, low + 1, sz + 3, "mall")
        repeat(1 + random.nextInt(2)) { car(p, random, x0 + 1 + random.nextInt(5), low + 1, z0 + 4 + random.nextInt(4), true) }
    }

    
    fun parkingLot(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val w = 24
        val d = 16
        val x0 = bx + 1
        val z0 = bz + 1
        val low = Sites.flatBase(seed, gen, x0, z0, w, d, 1, 3) ?: return
        clearPad(p, x0, z0, w, d, low, Material.GRAY_CONCRETE)
        for (dx in 0 until w) for (dz in 0 until d) {
            val stripe = dx % 4 == 0 && dz in 2..6 || dx % 4 == 0 && dz in 9..13
            val r = random.nextInt(100)
            val m = when {
                r < 6 -> Material.MOSS_BLOCK
                r < 10 -> Material.ANDESITE
                stripe -> Material.WHITE_CONCRETE
                else -> Material.GRAY_CONCRETE
            }
            p.set(x0 + dx, low, z0 + dz, m)
            if (m == Material.MOSS_BLOCK && random.nextBoolean()) p.set(x0 + dx, low + 1, z0 + dz, Material.SHORT_GRASS)
        }
        for ((lx, lz) in listOf(0 to 0, w - 1 to 0, 0 to d - 1, w - 1 to d - 1)) lamp(p, random, x0 + lx, low, z0 + lz)
        repeat(3 + random.nextInt(4)) {
            val slot = random.nextInt(5)
            val row = if (random.nextBoolean()) 3 else 10
            car(p, random, x0 + 2 + slot * 4 - (if (slot == 0) 0 else 0), low + 1, z0 + row, false)
        }
    }

    
    fun busStop(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 4 + random.nextInt(4)
        val z0 = bz + 4 + random.nextInt(4)
        val low = Sites.flatBase(seed, gen, x0, z0, 6, 3, 1, 2) ?: return
        clearPad(p, x0, z0, 6, 3, low, Material.LIGHT_GRAY_CONCRETE)
        for ((px, pz) in listOf(0 to 0, 5 to 0, 0 to 2, 5 to 2)) for (y in 1..3) p.put(x0 + px, low + y, z0 + pz, Material.COBBLESTONE_WALL, "[up=true]")
        for (dx in 0..5) for (dz in 0..2) p.set(x0 + dx, low + 4, z0 + dz, Material.SMOOTH_STONE_SLAB)
        for (dx in 1..4) for (y in 1..2) p.set(x0 + dx, low + y, z0, if (random.nextInt(100) < 20) Material.AIR else Material.LIGHT_GRAY_STAINED_GLASS)
        for (dx in 1..4 step 3) p.put(x0 + dx, low + 1, z0 + 1, Material.SPRUCE_STAIRS, "[facing=south]")
        p.set(x0 + 5, low + 5, z0 + 1, Material.BLUE_CONCRETE) 
    }

    
    fun park(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val s = 20
        val x0 = bx + 1
        val z0 = bz + 1
        val low = Sites.flatBase(seed, gen, x0, z0, s, s, 1, 3) ?: return
        for (dx in 0 until s) for (dz in 0 until s) {
            for (y in low + 1..low + 9) p.set(x0 + dx, y, z0 + dz, Material.AIR)
            val path = dx in 9..10 || dz in 9..10
            p.set(x0 + dx, low, z0 + dz, if (path) Material.GRAVEL else Material.GRASS_BLOCK)
            if (!path && random.nextInt(100) < 30) p.set(x0 + dx, low + 1, z0 + dz, Material.SHORT_GRASS)
        }
        for ((bxx, bzz) in listOf(8 to 5, 11 to 14, 5 to 8, 14 to 11)) {
            val face = if (bzz == 5 || bzz == 14) "south" else "east"
            p.put(x0 + bxx, low + 1, z0 + bzz, Material.SPRUCE_STAIRS, "[facing=$face]")
        }
        for ((lx, lz) in listOf(8 to 8, 11 to 11)) lamp(p, random, x0 + lx, low, z0 + lz)
        repeat(5) {
            val tx = x0 + 1 + random.nextInt(s - 2)
            val tz = z0 + 1 + random.nextInt(s - 2)
            if (!(tx - x0 in 8..11 || tz - z0 in 8..11)) p.tree(tx, low + 1, tz, random, if (random.nextBoolean()) TreeType.TREE else TreeType.BIRCH)
        }
    }
}
