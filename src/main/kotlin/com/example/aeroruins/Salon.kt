package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.entity.EntityType
import java.util.Random


object Salon {
    private const val W = 15
    private const val D = 11
    private const val H = 7

    fun build(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        val x0 = bx
        val z0 = bz + 2

        for (x in listOf(x0 - 1, x0 + W)) for (z in listOf(z0 - 1, z0 + D)) {
            if (zoneAt(seed, x, z) != Zone.JUNGLE) return
        }
        val hs = listOf(
            gen.heightAt(seed, x0, z0),
            gen.heightAt(seed, x0 + W - 1, z0),
            gen.heightAt(seed, x0, z0 + D - 1),
            gen.heightAt(seed, x0 + W - 1, z0 + D - 1),
            gen.heightAt(seed, x0 + W / 2, z0 + D / 2)
        )
        val y0 = hs.min()
        if (y0 < SEA_LEVEL + 2 || hs.max() - y0 > 2) return

        
        for (dx in 0 until W) for (dz in 0 until D) for (dy in 1..H + 2) p.set(x0 + dx, y0 + dy, z0 + dz, Material.AIR)

        
        for (dx in 0 until W) for (dz in 0 until D) {
            val roll = random.nextInt(100)
            val m = if (roll < 55) Material.MOSS_BLOCK else if (roll < 80) Material.DARK_OAK_PLANKS else Material.COARSE_DIRT
            p.set(x0 + dx, y0, z0 + dz, m)
        }

        
        for (dx in 0 until W) for (dz in 0 until D) for (rel in 1..H) {
            val inWedge = dz <= D - rel
            val m: Material? = when {
                dz == 0 -> Material.SMOOTH_SANDSTONE
                dx == W - 1 && inWedge -> Material.SMOOTH_SANDSTONE
                dx == 0 && inWedge -> if (dz % 4 == 0) Material.BLACK_CONCRETE else Material.LIGHT_BLUE_STAINED_GLASS
                dz == D - rel -> if (dx % 4 == 0) Material.BLACK_CONCRETE else Material.LIGHT_BLUE_STAINED_GLASS
                else -> null
            }
            if (m == null) continue
            val hole = when (m) {
                Material.SMOOTH_SANDSTONE -> 14
                Material.BLACK_CONCRETE -> 8
                else -> 12
            }
            if (random.nextInt(100) >= hole) p.set(x0 + dx, y0 + rel, z0 + dz, m)
        }

        
        for (dx in 0 until W) for (dz in 0..D - H) {
            if (random.nextInt(100) >= 22) p.set(x0 + dx, y0 + H + 1, z0 + dz, Material.BLACK_CONCRETE)
            if (random.nextInt(100) < 45) p.leaf(x0 + dx, y0 + H + 2, z0 + dz)
            if (random.nextInt(100) < 20) p.leaf(x0 + dx, y0 + H + 3, z0 + dz)
        }

        
        for (dx in 1 until W - 1) for (dz in 1 until D - 1) {
            when (random.nextInt(100)) {
                in 0..34 -> p.set(x0 + dx, y0 + 1, z0 + dz, Material.MOSS_CARPET)
                in 35..52 -> p.set(x0 + dx, y0 + 1, z0 + dz, Material.SHORT_GRASS)
                in 53..60 -> p.set(x0 + dx, y0 + 1, z0 + dz, Material.FERN)
                in 61..64 -> p.set(x0 + dx, y0 + 1, z0 + dz, Material.CORNFLOWER)
            }
        }

        
        for (dx in 0 until W) for (dz in 1..D - H) {
            if (random.nextInt(100) < 22) p.hang(x0 + dx, y0 + H, z0 + dz, 1 + random.nextInt(5))
        }
        StructureDetails.roofline(p, x0, z0, W, D, y0 + H + 1, modern = true)

        
        for (dx in 9..11) for (dz in 2..3) p.set(x0 + dx, y0 + 1, z0 + dz, Material.POLISHED_BLACKSTONE)
        for (dx in 9..11) p.set(x0 + dx, y0 + 2, z0 + 3, Material.WHITE_CARPET)
        for (dx in 9..11) p.set(x0 + dx, y0 + 2, z0 + 2, Material.POLISHED_BLACKSTONE_SLAB)
        p.set(x0 + 10, y0 + 1, z0 + 5, Material.DARK_OAK_SLAB)

        
        for (dx in 3..5) p.put(x0 + dx, y0 + 1, z0 + 2, Material.SPRUCE_STAIRS, "[facing=south]")
        for (dz in 4..6) p.put(x0 + 1, y0 + 1, z0 + dz, Material.SPRUCE_STAIRS, "[facing=east]")

        
        p.set(x0 + 1, y0 + 1, z0 + 8, Material.BIRCH_PLANKS)
        p.set(x0 + 1, y0 + 2, z0 + 8, Material.NOTE_BLOCK)

        Loot.fill(
            p, random, x0, z0, W, D, 2, listOf(y0 + 1), "salon",
            listOf(EntityType.ZOMBIE_VILLAGER, EntityType.SKELETON), if (Loot.rare(random, 50.0)) 1 else 0, 2
        )
    }
}
