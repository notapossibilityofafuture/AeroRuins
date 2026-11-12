package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.entity.Villager
import java.util.Random






object Rustic {
    private val JOBS = arrayOf(
        Material.LECTERN to Villager.Profession.LIBRARIAN,
        Material.BLAST_FURNACE to Villager.Profession.ARMORER,
        Material.SMOKER to Villager.Profession.BUTCHER,
        Material.COMPOSTER to Villager.Profession.FARMER,
        Material.CARTOGRAPHY_TABLE to Villager.Profession.CARTOGRAPHER,
        Material.BREWING_STAND to Villager.Profession.CLERIC,
        Material.GRINDSTONE to Villager.Profession.WEAPONSMITH,
        Material.LOOM to Villager.Profession.SHEPHERD,
        Material.FLETCHING_TABLE to Villager.Profession.FLETCHER,
        Material.SMITHING_TABLE to Villager.Profession.TOOLSMITH,
        Material.STONECUTTER to Villager.Profession.MASON,
        Material.BARREL to Villager.Profession.FISHERMAN
    )

    
    fun cottage(seed: Long, random: Random, p: Place, x0: Int, z0: Int, gen: AeroGenerator, desert: Boolean, ruined: Boolean): Boolean {
        val size = random.nextInt(3)
        val w = intArrayOf(5, 7, 9)[size]
        val d = intArrayOf(5, 6, 7)[size]
        val low = Sites.flatBase(seed, gen, x0, z0, w, d, 1, 3, true) ?: return false
        val oak = random.nextBoolean()
        val log = if (desert) Material.CUT_SANDSTONE else if (oak) Material.OAK_LOG else Material.SPRUCE_LOG
        val plank = if (desert) Material.SANDSTONE else if (oak) Material.OAK_PLANKS else Material.SPRUCE_PLANKS
        val stairs = if (desert) Material.SANDSTONE_STAIRS else if (oak) Material.OAK_STAIRS else Material.SPRUCE_STAIRS
        val slab = if (desert) Material.SANDSTONE_SLAB else if (oak) Material.OAK_SLAB else Material.SPRUCE_SLAB
        val doorMat = if (desert) Material.ACACIA_DOOR else if (oak) Material.OAK_DOOR else Material.SPRUCE_DOOR

        for (dx in -1..w) for (dz in -1..d) for (y in low + 1..low + 9) p.set(x0 + dx, y, z0 + dz, Material.AIR)
        for (dx in 0 until w) for (dz in 0 until d) {
            val edge = dx == 0 || dx == w - 1 || dz == 0 || dz == d - 1
            p.set(x0 + dx, low, z0 + dz, if (edge) Material.COBBLESTONE else if (desert) Material.SMOOTH_SANDSTONE else Material.OAK_PLANKS)
        }
        for (dy in 1..3) for (dx in 0 until w) for (dz in 0 until d) {
            val onX = dx == 0 || dx == w - 1
            val onZ = dz == 0 || dz == d - 1
            if (!onX && !onZ) continue
            val corner = onX && onZ
            if (dz == d - 1 && dx == w / 2 && dy <= 2) continue 
            val window = dy == 2 && !corner && ((onZ && dx % 3 == 1 && dx != w / 2) || (onX && dz % 3 == 1))
            if (window) {
                p.put(x0 + dx, low + dy, z0 + dz, Material.GLASS_PANE, if (onZ) "[east=true,west=true]" else "[north=true,south=true]")
            } else {
                p.set(x0 + dx, low + dy, z0 + dz, if (corner) log else plank)
            }
        }
        p.put(x0 + w / 2, low + 1, z0 + d - 1, doorMat, "[half=lower,facing=south]")
        p.put(x0 + w / 2, low + 2, z0 + d - 1, doorMat, "[half=upper,facing=south]")
        Village.gable(p, x0, z0, w, d, low + 4, w >= d, stairs, slab, plank)

        
        val (jobBlock, job) = JOBS[random.nextInt(JOBS.size)]
        p.set(x0 + 1, low + 1, z0 + 1, jobBlock)
        if (p.isAir(x0 + w - 2, low + 1, z0 + 2) && p.isAir(x0 + w - 2, low + 1, z0 + 1)) {
            p.put(x0 + w - 2, low + 1, z0 + 2, Material.RED_BED, "[facing=north,part=foot]")
            p.put(x0 + w - 2, low + 1, z0 + 1, Material.RED_BED, "[facing=north,part=head]")
        }
        p.set(x0 + w / 2, low + 1, z0 + d / 2, Material.OAK_FENCE)
        p.set(x0 + w / 2, low + 2, z0 + d / 2, Material.OAK_PRESSURE_PLATE)
        p.set(x0 + 1, low + 1, z0 + d - 2, Material.TORCH)
        if (Loot.rare(random, 25.0)) Loot.chestAt(p, random, x0 + w - 2, low + 1, z0 + d - 2, "frame")

        if (ruined) {
            Decay.ruin(p, random, x0, z0, w, d, low + 1, low + 9, 24)
            if (random.nextInt(100) < 55) p.villager(x0 + w / 2, low + 1, z0 + d / 2 - 1, job, true)
        } else if (random.nextInt(100) < 75) {
            p.villager(x0 + w / 2, low + 1, z0 + d / 2 - 1, job, false)
        }
        return true
    }
}
