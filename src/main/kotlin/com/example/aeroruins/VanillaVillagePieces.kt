package com.example.aeroruins

import org.bukkit.Material
import java.util.Random

object VanillaVillagePieces {
    private val houses = mapOf(
        "desert" to "/vanilla/minecraft/structure/village/desert/houses/desert_small_house_1.nbt",
        "savanna" to "/vanilla/minecraft/structure/village/savanna/houses/savanna_small_house_1.nbt",
        "taiga" to "/vanilla/minecraft/structure/village/taiga/houses/taiga_small_house_1.nbt",
        "snowy" to "/vanilla/minecraft/structure/village/snowy/houses/snowy_small_house_1.nbt",
        "cherry" to "/vanilla/brutos/structure/village/cherry/houses/cherry_small_house_1.nbt",
        "swamp" to "/vanilla/brutos/structure/village/swamp/houses/swamp_small_house_1.nbt",
        "plains" to "/vanilla/minecraft/structure/village/plains/houses/plains_small_house_1.nbt"
    )
    private val cache = HashMap<String, List<VillageBlock>>()

    fun placeHouse(seed: Long, random: Random, p: Place, x: Int, z: Int, gen: AeroGenerator, climate: Climate) {
        val key = when {
            climate.id in setOf("yellow_plains", "scrub_savanna", "dust_flats", "dry_acacia_plain") -> "desert"
            climate.cold -> "snowy"
            climate.id.contains("cherry") -> "cherry"
            climate.id.contains("swamp") || climate.id == "grey_fen" -> "swamp"
            climate.tree.name.contains("SPRUCE") -> "taiga"
            climate.tree.name.contains("ACACIA") -> "savanna"
            else -> "plains"
        }
        val blocks = cache.getOrPut(key) { load(houses.getValue(key)) }
        val base = gen.heightAt(seed, x, z)
        for (block in blocks) {
            val bx = x + block.x
            val by = base + block.y
            val bz = z + block.z
            if (block.data.material == Material.AIR) continue
            p.setData(bx, by, bz, block.data)
        }
    }

    private fun load(resource: String): List<VillageBlock> {
        val root = SchematicNbt.read(VanillaVillagePieces::class.java.getResourceAsStream(resource)
            ?: error("Missing village structure $resource"))
        val palette = root["palette"] as List<*>
        val states = palette.map { state ->
            val map = state as Map<*, *>
            val name = map["Name"].toString().removePrefix("minecraft:")
            val props = map["Properties"] as? Map<*, *>
            val material = Material.matchMaterial(name.uppercase()) ?: Material.AIR
            val suffix = props?.entries?.joinToString(",", "[", "]") { "${it.key}=${it.value}" } ?: ""
            if (suffix.isEmpty()) material.createBlockData() else material.createBlockData(suffix)
        }
        return (root["blocks"] as List<*>).mapNotNull { raw ->
            val map = raw as Map<*, *>
            val pos = map["pos"] as List<*>
            VillageBlock(
                (pos[0] as Number).toInt(),
                (pos[1] as Number).toInt(),
                (pos[2] as Number).toInt(),
                states[(map["state"] as Number).toInt()]
            )
        }
    }
}

private data class VillageBlock(
    val x: Int,
    val y: Int,
    val z: Int,
    val data: org.bukkit.block.data.BlockData
)
