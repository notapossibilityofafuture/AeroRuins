package com.example.aeroruins

import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.World
import org.bukkit.block.Biome
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.ChunkGenerator
import org.bukkit.generator.WorldInfo
import org.bukkit.util.noise.SimplexOctaveGenerator
import java.util.Random
import java.util.concurrent.ConcurrentHashMap


object BiomeIds {
    val SURFACE: List<String> = Climate.entries.map { it.id }                                         
    val WATER = listOf("still_lake", "deep_lake", "marsh_water", "cold_water")                          
    val SPECIAL = listOf("fluorescent_suburb", "torn_veil")                                             
    val HYPO = listOf("sterile_wards", "quarantine_wing", "bloodied_wards")                             
    val MUSCLE = listOf("fleshy_downtown", "sinew_district", "rotted_arcade")                           
    val DOME = listOf("sunlit_plaza", "crystal_atrium", "aqua_conservatory", "pearl_arboretum")         
    val BACKROOMS = listOf("levelzero_lobby", "damp_annex", "greenroom_garden", "flicker_wing", "pedestrian_underpass", "flooded_subway") 
    val MALL = listOf("atrium_concourse", "fountain_court", "deadmall_east", "skylight_arcade")         
    val QUARRY = listOf("chalk_quarry", "flooded_pit", "daisy_cavern")                                  
    val CAVES: List<String> = CaveStyles.BIOMES                                                         
    val ALL: List<String> = SURFACE + WATER + SPECIAL + HYPO + MUSCLE + DOME + BACKROOMS + MALL + QUARRY + CAVES 
}

class AeroBiomeProvider : BiomeProvider() {

    private val cache = ConcurrentHashMap<String, Biome>()

    
    private fun pick(name: String, fallback: Biome): Biome =
        cache.computeIfAbsent(name) { Registry.BIOME.get(NamespacedKey("aeroruins", name)) ?: fallback }

    private fun outdoor(seed: Long, x: Int, y: Int, z: Int): Biome {
        val c = ClimateMap.at(seed, x, z)
        if (ClimateMap.useVanilla(seed, x, z)) return c.fallback
        if (y in 46..SEA_LEVEL && !c.gloom) {
            val id = when {
                y <= 52 -> "deep_lake"
                c.warm -> "marsh_water"
                c.cold -> "cold_water"
                else -> "still_lake"
            }
            return pick(id, Biome.RIVER)
        }
        if (Layers.tearAt(seed, x, z) != null) return pick("torn_veil", c.fallback)
        if (Suburb.fieldAt(seed, x, z)) return pick("fluorescent_suburb", c.fallback)
        return pick(c.id, c.fallback)
    }

    private fun cave(seed: Long, x: Int, y: Int, z: Int): Biome {
        val st = CaveStyles.at(seed, x, y, z)
        return pick(st.biome, if (y < DEEP_Y) Biome.DEEP_DARK else Biome.LUSH_CAVES)
    }

    
    private fun hall(seed: Long, x: Int, y: Int, z: Int, floorY: Int, ceilY: Int, id: String): Biome = when {
        y < floorY - 3 -> cave(seed, x, y, z)
        y <= ceilY + 1 -> pick(id, Biome.JUNGLE)
        else -> outdoor(seed, x, y, z)
    }

    override fun getBiome(info: WorldInfo, x: Int, y: Int, z: Int): Biome {
        val seed = info.seed
        return when (zoneAt(seed, x, z)) {
            Zone.BACKROOMS -> hall(seed, x, y, z, BF, BC, BiomeIds.BACKROOMS[backroomsVariant(seed, x, z)])
            Zone.MALL -> hall(seed, x, y, z, MF, MC, BiomeIds.MALL[mallVariant(seed, x, z)])
            Zone.QUARRY -> when {
                y < QF - 3 -> cave(seed, x, y, z)
                y <= QC + 1 -> pick(BiomeIds.QUARRY[quarryVariant(seed, x, z)], Biome.JUNGLE)
                else -> outdoor(seed, x, y, z)
            }
            Zone.AERO -> {
                val dome = domeAt(seed, x, z)
                if (dome != null) {
                    if (y < FLOOR - 18) cave(seed, x, y, z)
                    else if (dome.d < 12.0) pick("sunlit_plaza", Biome.JUNGLE)
                    else pick(BiomeIds.DOME[1 + domeVariant(seed, x, z)], Biome.JUNGLE)
                } else if (y < 46) cave(seed, x, y, z) else outdoor(seed, x, y, z)
            }
            Zone.JUNGLE -> if (y < 46) cave(seed, x, y, z) else outdoor(seed, x, y, z)
        }
    }

    override fun getBiomes(info: WorldInfo): List<Biome> =
        BiomeIds.ALL.map { pick(it, Biome.JUNGLE) }.distinct()
}

class AeroGenerator(private val wideCaves: Boolean = false) : ChunkGenerator() {

    private val ponds = ConcurrentHashMap<Long, SimplexOctaveGenerator>()
    private val caveNoises = ConcurrentHashMap<Long, Array<SimplexOctaveGenerator>>()

    internal fun pondNoise(seed: Long, x: Int, z: Int): Double =
        ponds.computeIfAbsent(seed) {
            SimplexOctaveGenerator(Random(seed xor 0x5DEECE66DL), 3).also { it.setScale(0.03) }
        }.noise(x.toDouble(), z.toDouble(), 0.5, 0.5, true)

    internal fun caveGens(seed: Long): Array<SimplexOctaveGenerator> =
        caveNoises.computeIfAbsent(seed) {
            arrayOf(
                SimplexOctaveGenerator(Random(seed xor 0x11L), 2).also { it.setScale(0.022) },
                SimplexOctaveGenerator(Random(seed xor 0x22L), 2).also { it.setScale(0.022) },
                SimplexOctaveGenerator(Random(seed xor 0x33L), 2).also { it.setScale(0.011) },
                SimplexOctaveGenerator(Random(seed xor 0x44L), 2).also { it.setScale(0.009) }, 
                SimplexOctaveGenerator(Random(seed xor 0x55L), 2).also { it.setScale(0.012) }, 
                SimplexOctaveGenerator(Random(seed xor 0x66L), 1).also { it.setScale(0.004) }  
            )
        }

    private val samplers = ConcurrentHashMap<Long, CaveSampler>()
    private val grids = ThreadLocal.withInitial { CaveGrid() }

    
    internal fun beginCaves(info: WorldInfo, cx: Int, cz: Int) {
        val sampler = samplers.computeIfAbsent(info.seed) { CaveSampler(it, caveGens(it), wideCaves) }
        caveGrid().begin(sampler, cx * 16, cz * 16, info.minHeight + 5)
    }

    
    internal fun caveGrid(): CaveGrid = grids.get()

    
    fun heightAt(seed: Long, x: Int, z: Int): Int = Terrain.heightAt(seed, x, z)

    
    fun usesTerrain(seed: Long, x: Int, z: Int): Boolean = when (zoneAt(seed, x, z)) {
        Zone.JUNGLE, Zone.BACKROOMS, Zone.MALL, Zone.QUARRY -> true
        Zone.AERO -> domeAt(seed, x, z) == null
        else -> false
    }

    
    fun isOpenAir(seed: Long, x: Int, z: Int): Boolean = when (zoneAt(seed, x, z)) {
        Zone.JUNGLE, Zone.QUARRY, Zone.BACKROOMS, Zone.MALL -> true
        Zone.AERO -> domeAt(seed, x, z) == null
        else -> false
    }

    fun surfaceY(seed: Long, x: Int, z: Int): Int =
        if (usesTerrain(seed, x, z)) heightAt(seed, x, z) else FLOOR

    override fun generateNoise(info: WorldInfo, random: Random, cx: Int, cz: Int, data: ChunkData) {
        val seed = info.seed
        Portals.prepare(seed, cx, cz)
        if (Settings.caves) beginCaves(info, cx, cz)
        for (x in 0..15) for (z in 0..15) {
            val wx = cx * 16 + x
            val wz = cz * 16 + z
            data.setBlock(x, info.minHeight, z, Material.BEDROCK)
            when (zoneAt(seed, wx, wz)) {
                Zone.JUNGLE -> terrainColumn(info, data, x, z, wx, wz)
                Zone.AERO -> {
                    val dome = domeAt(seed, wx, wz)
                    if (dome == null) terrainColumn(info, data, x, z, wx, wz)
                    else domeColumn(info, data, x, z, wx, wz, dome)
                }
                Zone.BACKROOMS -> backroomsColumn(info, data, x, z, wx, wz)
                Zone.MALL -> mallColumn(info, data, x, z, wx, wz)
                Zone.QUARRY -> quarryColumn(info, data, x, z, wx, wz)
            }
        }
        if (Settings.caves) sweepOrphans(data, info.minHeight + 5, 70) 
    }

    override fun getDefaultPopulators(world: World): List<BlockPopulator> = listOf(AeroPopulator(this))

    
    override fun shouldGenerateStructures(worldInfo: WorldInfo, random: Random, chunkX: Int, chunkZ: Int): Boolean =
        Settings.vanillaStructures &&
            Math.floorMod(
                mix(worldInfo.seed, Math.floorDiv(chunkX, 16), Math.floorDiv(chunkZ, 16), 901),
                100
            ) < Settings.vanillaMixChance

    override fun getFixedSpawnLocation(world: World, random: Random): Location {
        val h = maxOf(heightAt(world.seed, 0, 0), SEA_LEVEL)
        return Location(world, 0.5, h + 1.0, 0.5)
    }
}


class VeilBiomeProvider(private val layer: Int) : BiomeProvider() {
    private val cache = ConcurrentHashMap<String, Biome>()

    private fun pick(name: String, fallback: Biome): Biome =
        cache.computeIfAbsent(name) { Registry.BIOME.get(NamespacedKey("aeroruins", name)) ?: fallback }

    override fun getBiome(info: WorldInfo, x: Int, y: Int, z: Int): Biome = when (layer) {
        1 -> pick(CaveStyles.at(info.seed, x, y, z).biome, Biome.LUSH_CAVES)
        2 -> pick(BiomeIds.HYPO[Layers.hypoVariant(info.seed, x, z)], Biome.PLAINS)
        else -> pick(BiomeIds.MUSCLE[Layers.muscleVariant(info.seed, x, z)], Biome.NETHER_WASTES)
    }

    override fun getBiomes(info: WorldInfo): List<Biome> =
        when (layer) {
            1 -> CaveStyles.BIOMES
            2 -> BiomeIds.HYPO
            else -> BiomeIds.MUSCLE
        }.map { pick(it, Biome.PLAINS) }.distinct()
}
