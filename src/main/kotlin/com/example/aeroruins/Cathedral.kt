package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.entity.EntityType
import java.util.Random





object Cathedral {
    private const val W = 15
    private const val LEN = 29

    fun build(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx + 1 + random.nextInt(2)
        val z0 = bz + 1 + random.nextInt(2)
        val low = Sites.flatBase(seed, gen, x0, z0, W, LEN, 1, 4) ?: return

        val stones = arrayOf(Material.DEEPSLATE_BRICKS, Material.DEEPSLATE_BRICKS, Material.MOSSY_STONE_BRICKS, Material.CRACKED_STONE_BRICKS)
        
        for (dx in 0 until W) for (dz in 0 until LEN) {
            for (y in low + 1..low + 6) p.set(x0 + dx, y, z0 + dz, Material.AIR)
            p.set(x0 + dx, low, z0 + dz, stones[random.nextInt(stones.size)])
        }
        
        val wallH = 13 + random.nextInt(3)
        for (dz in 0 until LEN) for (dy in 1..wallH) {
            val window = dz % 6 in 2..3 && dy in 4..wallH - 3
            for (dx in intArrayOf(0, W - 1)) {
                if (window) continue
                if (dy > wallH - 4 && random.nextInt(100) < 45) continue 
                p.set(x0 + dx, low + dy, z0 + dz, Material.DEEPSLATE_BRICKS)
            }
        }
        for (dx in 0 until W) for (dy in 1..wallH + 2) {
            val window = dx % 4 in 1..2 && dy in 3..wallH
            val decay = if (dy > wallH - 3) 55 else 6
            if (window || random.nextInt(100) < decay) continue
            p.set(x0 + dx, low + dy, z0 + LEN - 1, Material.DEEPSLATE_BRICKS)
        }
        
        for (k in 0..3) {
            val pz = z0 + 3 + k * 6
            for (px in intArrayOf(x0 + 3, x0 + 10)) {
                val ph = 15 + random.nextInt(4) - if (random.nextInt(100) < 20) random.nextInt(8) else 0
                for (dy in 1..ph) for (a in 0..1) for (b in 0..1) {
                    p.set(px + a, low + dy, pz + b, if (dy % 5 == 0) Material.POLISHED_DEEPSLATE else Material.DEEPSLATE_BRICKS)
                }
                for (a in -1..2) for (b in -1..2) {
                    if (random.nextInt(100) < 70) p.set(px + a, low + ph + 1, pz + b, Material.DEEPSLATE_BRICKS)
                }
            }
            for (s in 0..3) { 
                if (random.nextInt(100) < 40) continue
                p.set(x0 + 5 + s, low + 16 + s, pz, Material.DEEPSLATE_BRICKS)
                p.set(x0 + 9 - s, low + 16 + s, pz, Material.DEEPSLATE_BRICKS)
            }
        }
        
        Trees.bigTree(p, random, x0 + 6 + random.nextInt(2), low + 1, z0 + 8 + random.nextInt(8), Climate.ASHEN_CATHEDRAL, 8 + random.nextInt(4), 3)
        
        for (dz in 4..22 step 3) for (dx in intArrayOf(4, 5, 6, 8, 9, 10)) {
            if (random.nextInt(100) < 30) continue
            if (p.isAir(x0 + dx, low + 1, z0 + dz)) p.put(x0 + dx, low + 1, z0 + dz, Material.DARK_OAK_STAIRS, "[facing=north]")
        }
        StructureDetails.roofline(p, x0, z0, W, LEN, low + wallH + 1, modern = false)
        
        val ax = x0 + W / 2
        val az = z0 + LEN - 4
        for (dx in -2..2) p.set(ax + dx, low + 1, az, Material.STONE_BRICK_SLAB)
        p.set(ax, low + 2, az, Material.QUARTZ_BLOCK)
        p.put(ax, low + 3, az, Material.CANDLE, "[candles=3,lit=true]")
        if (Loot.rare(random, 45.0)) p.chest(ax, low + 1, az + 1, "south", "salon")
        
        repeat(40) {
            val x = x0 + 1 + random.nextInt(W - 2)
            val z = z0 + 1 + random.nextInt(LEN - 2)
            if (p.isAir(x, low + 1, z)) p.set(x, low + 1, z, if (random.nextBoolean()) Material.COBWEB else Material.MOSS_CARPET)
        }
        Loot.mobAt(p, ax, low + 1, az - 3, EntityType.SKELETON)
    }
}
