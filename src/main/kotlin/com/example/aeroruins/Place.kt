package com.example.aeroruins

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.data.BlockData
import org.bukkit.entity.EntityType
import org.bukkit.block.CreatureSpawner
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Villager
import org.bukkit.entity.ZombieVillager
import org.bukkit.generator.LimitedRegion
import org.bukkit.loot.Lootable

fun leafData(m: Material = Material.JUNGLE_LEAVES): BlockData = m.createBlockData("[persistent=true]")


class Place(private val r: LimitedRegion) {
    fun inside(x: Int, y: Int, z: Int): Boolean = r.isInRegion(x, y, z)

    
    fun tree(x: Int, y: Int, z: Int, random: java.util.Random, type: org.bukkit.TreeType): Boolean {
        if (!inside(x, y, z) || !treesWork) return false
        return try {
            r.generateTree(Location(null, x.toDouble(), y.toDouble(), z.toDouble()), random, type)
        } catch (e: Exception) {
            treesWork = false
            false
        }
    }

    
    fun villager(x: Int, y: Int, z: Int, job: Villager.Profession, zombie: Boolean) {
        if (!Settings.mobs || !inside(x, y, z)) return
        try {
            val e = r.spawnEntity(Location(null, x + 0.5, y.toDouble(), z + 0.5), if (zombie) EntityType.ZOMBIE_VILLAGER else EntityType.VILLAGER)
            if (e is Villager) e.profession = job
            if (e is ZombieVillager) e.villagerProfession = job
            if (e is LivingEntity) e.removeWhenFarAway = false
        } catch (ex: Exception) {
            
        }
    }

    
    fun animal(x: Int, y: Int, z: Int, type: EntityType) {
        if (!Settings.mobs || !inside(x, y, z)) return
        try {
            r.spawnEntity(Location(null, x + 0.5, y.toDouble(), z + 0.5), type)
        } catch (ex: Exception) {
            
        }
    }

    
    fun spawner(x: Int, y: Int, z: Int, type: EntityType) {
        if (!inside(x, y, z)) return
        set(x, y, z, Material.SPAWNER)
        try {
            val st = r.getBlockState(x, y, z)
            if (st is CreatureSpawner) {
                st.spawnedType = type
                st.update(true, false)
            }
        } catch (ex: Exception) {
            
        }
    }

    companion object {
        @Volatile
        var treesWork = true
    }

    fun set(x: Int, y: Int, z: Int, m: Material) {
        if (inside(x, y, z)) r.setType(x, y, z, m)
    }

    fun setData(x: Int, y: Int, z: Int, data: BlockData) {
        if (inside(x, y, z)) r.setBlockData(x, y, z, data)
    }

    fun put(x: Int, y: Int, z: Int, m: Material, props: String) {
        if (inside(x, y, z)) r.setBlockData(x, y, z, m.createBlockData(props))
    }

    fun type(x: Int, y: Int, z: Int): Material =
        if (inside(x, y, z)) r.getType(x, y, z) else Material.BEDROCK

    fun isAir(x: Int, y: Int, z: Int): Boolean = inside(x, y, z) && r.getType(x, y, z) == Material.AIR

    fun leaf(x: Int, y: Int, z: Int, m: Material = Material.JUNGLE_LEAVES) {
        if (isAir(x, y, z)) r.setBlockData(x, y, z, leafData(m))
    }

    fun vine(x: Int, y: Int, z: Int, face: String) = put(x, y, z, Material.VINE, "[$face=true]")

    fun hang(x: Int, topY: Int, z: Int, len: Int) {
        for (i in 0 until len) {
            val y = topY - i
            if (!isAir(x, y, z)) break
            set(x, y, z, if (i == len - 1) Material.CAVE_VINES else Material.CAVE_VINES_PLANT)
        }
    }

    
    fun chest(x: Int, y: Int, z: Int, face: String, table: String) {
        if (!Settings.chests || !inside(x, y, z)) return
        try {
            if (r.getType(x, y, z) != Material.CHEST) {
                r.setBlockData(x, y, z, Material.CHEST.createBlockData("[facing=$face]"))
            }
            val st = r.getBlockState(x, y, z)
            if (st is Lootable) {
                val key = if (table.contains(':')) NamespacedKey.fromString(table)
                else NamespacedKey("aeroruins", "chests/$table")
                val lt = key?.let(Bukkit::getLootTable)
                    ?: Bukkit.getLootTable(NamespacedKey("aeroruins", "chests/$table"))
                    ?: Bukkit.getLootTable(NamespacedKey.minecraft("chests/simple_dungeon"))
                if (lt != null) {
                    st.lootTable = lt
                    st.update(true)
                }
            }
        } catch (ex: Exception) {
            
        }
    }

    fun spawn(x: Int, y: Int, z: Int, type: EntityType) {
        if (!Settings.mobs || !inside(x, y, z)) return
        try {
            val e = r.spawnEntity(Location(null, x + 0.5, y.toDouble(), z + 0.5), type)
            e.isPersistent = true
            if (e is LivingEntity) e.removeWhenFarAway = false
        } catch (ex: Exception) {
            
        }
    }
}
