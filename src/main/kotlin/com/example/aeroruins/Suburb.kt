package com.example.aeroruins

import org.bukkit.Material
import java.util.Random





object Suburb {
    private const val REGION = 192
    private val WALLS = arrayOf(Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.WHITE_TERRACOTTA, Material.SMOOTH_SANDSTONE)
    private val DESERT_WALLS = arrayOf(Material.SANDSTONE, Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE, Material.TERRACOTTA)
    private val RUSTIC_WALLS = arrayOf(Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.STRIPPED_OAK_LOG, Material.DARK_OAK_PLANKS)
    private val RUSTIC_ROOFS = arrayOf(
        Material.OAK_STAIRS to Material.OAK_SLAB,
        Material.SPRUCE_STAIRS to Material.SPRUCE_SLAB,
        Material.DARK_OAK_STAIRS to Material.DARK_OAK_SLAB
    )
    private val ROOFS = arrayOf(
        Material.POLISHED_ANDESITE_STAIRS to Material.POLISHED_ANDESITE_SLAB,
        Material.DEEPSLATE_BRICK_STAIRS to Material.DEEPSLATE_BRICK_SLAB,
        Material.COBBLESTONE_STAIRS to Material.COBBLESTONE_SLAB,
        Material.BRICK_STAIRS to Material.BRICK_SLAB
    )

    
    fun fieldAt(seed: Long, x: Int, z: Int): Boolean {
        if (!Settings.suburbCeilings) return false 
        val gx = Math.floorDiv(x, REGION)
        val gz = Math.floorDiv(z, REGION)
        if (mix(seed, gx, gz, 95) % 1000 >= 15) return false
        if (Terrain.heightAt(seed, gx * REGION + REGION / 2, gz * REGION + REGION / 2) >= 92) return false
        return zoneAt(seed, x, z) == Zone.JUNGLE
    }

    private fun ceilingY(seed: Long, x: Int, z: Int): Int {
        val gx = Math.floorDiv(x, REGION)
        val gz = Math.floorDiv(z, REGION)
        return maxOf(Terrain.heightAt(seed, gx * REGION + REGION / 2, gz * REGION + REGION / 2) + 24, 96)
    }

    
    fun ceiling(seed: Long, p: Place, bx: Int, bz: Int) {
        for (x in bx..bx + 15) for (z in bz..bz + 15) {
            if (!fieldAt(seed, x, z)) continue
            val y = ceilingY(seed, x, z)
            val seam = Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0
            val panel = Math.floorMod(x, 4) in 1..2 && Math.floorMod(z, 4) in 1..2 &&
                mix(seed, Math.floorDiv(x, 4), Math.floorDiv(z, 4), 96) % 100 < 60
            p.set(x, y, z, if (panel) Material.SEA_LANTERN else if (seam) Material.LIGHT_GRAY_CONCRETE else Material.WHITE_CONCRETE)
        }
    }

    
    
    fun house(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator, rustic: Boolean = false, at: IntArray? = null, desert: Boolean = false, decay: Int = 0): IntArray? {
        val w = 9
        val d = 7
        val x0 = at?.get(0) ?: (bx + 3 + random.nextInt(2))
        val z0 = at?.get(1) ?: (bz + 2 + random.nextInt(2))
        val low = Sites.flatBase(seed, gen, x0, z0, w, d, 2, 3, rustic) ?: return null
        val wallMat = if (desert) DESERT_WALLS[random.nextInt(DESERT_WALLS.size)] else if (rustic) RUSTIC_WALLS[random.nextInt(RUSTIC_WALLS.size)] else WALLS[random.nextInt(WALLS.size)]
        val roof = if (desert) (Material.SANDSTONE_STAIRS to Material.SANDSTONE_SLAB) else if (rustic) RUSTIC_ROOFS[random.nextInt(RUSTIC_ROOFS.size)] else ROOFS[random.nextInt(ROOFS.size)]
        val doorMat = if (desert) Material.ACACIA_DOOR else if (rustic) Material.SPRUCE_DOOR else Material.BIRCH_DOOR
        for (dx in -1..w) for (dz in -1..d) for (y in low + 1..low + 9) p.set(x0 + dx, y, z0 + dz, Material.AIR)
        for (dx in 0 until w) for (dz in 0 until d) p.set(x0 + dx, low, z0 + dz, Material.OAK_PLANKS)
        
        for (dy in 1..3) for (dx in 0 until w) for (dz in 0 until d) {
            val edge = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1
            if (!edge) continue
            val window = dy == 2 && (dx == 2 || dx == 6) && (dz == 0 || dz == d - 1)
            val door = dx == w / 2 && dz == d - 1 && dy <= 2
            if (door) continue
            p.set(x0 + dx, low + dy, z0 + dz, if (window) Material.GLASS else wallMat)
        }
        p.put(x0 + w / 2, low + 1, z0 + d - 1, doorMat, "[half=lower,facing=south]")
        p.put(x0 + w / 2, low + 2, z0 + d - 1, doorMat, "[half=upper,facing=south]")
        
        val mid = (d + 1) / 2
        for (idx in 0..d + 1) {
            val dz = idx - 1
            val level = if (idx <= mid) idx else d + 1 - idx
            for (dx in -1..w) {
                val x = x0 + dx
                val z = z0 + dz
                val y = low + 4 + level
                when {
                    idx < mid -> p.put(x, y, z, roof.first, "[facing=south]") 
                    idx > mid -> p.put(x, y, z, roof.first, "[facing=north]")
                    else -> p.set(x, y, z, roof.second)
                }
            }
        }
        for (dz in 0 until d) { 
            val idx = dz + 1
            val level = if (idx <= mid) idx else d + 1 - idx
            for (dy in 0 until level) {
                p.set(x0, low + 4 + dy, z0 + dz, wallMat)
                p.set(x0 + w - 1, low + 4 + dy, z0 + dz, wallMat)
            }
        }
        StructureDetails.facade(p, random, x0, z0, w, d, low, 3, modern = !rustic)
        StructureDetails.roofline(p, x0, z0, w, d, low + 4 + (d + 1) / 2, modern = !rustic)
        Interiors.furnish(p, random, x0, z0, w, d, 1, listOf(low), 7)

        if (!rustic) {
            
            for (z in z0 + d until bz + 15) {
                val gy = gen.heightAt(seed, x0 + w / 2, z)
                for (a in 0..1) p.set(x0 + w / 2 + a, gy, z, Material.LIGHT_GRAY_CONCRETE)
            }
            val lx = x0 + w / 2 + 3
            val lz = bz + 14
            val ly = gen.heightAt(seed, lx, lz)
            for (i in 1..4) p.set(lx, ly + i, lz, Material.DARK_OAK_FENCE)
            p.set(lx, ly + 5, lz, Material.OCHRE_FROGLIGHT)
        }
        if (decay > 0) Decay.ruin(p, random, x0, z0, w, d, low + 1, low + 10, decay) 
        return intArrayOf(x0, z0, w, d, low)
    }
}
