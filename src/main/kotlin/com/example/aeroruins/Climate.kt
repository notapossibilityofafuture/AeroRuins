package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.TreeType
import org.bukkit.block.Biome
import org.bukkit.util.noise.SimplexOctaveGenerator
import java.util.Random
import java.util.concurrent.ConcurrentHashMap

private val WILD = listOf(Material.DANDELION, Material.POPPY, Material.OXEYE_DAISY, Material.AZURE_BLUET)
private val WHITE = listOf(Material.OXEYE_DAISY, Material.AZURE_BLUET, Material.LILY_OF_THE_VALLEY)
private val DRY = listOf(Material.DANDELION, Material.DANDELION, Material.POPPY)
private val COLD = listOf(Material.AZURE_BLUET, Material.OXEYE_DAISY)
private val REDS = listOf(Material.POPPY, Material.POPPY, Material.RED_TULIP)
private val BLUES = listOf(Material.CORNFLOWER, Material.AZURE_BLUET, Material.ALLIUM)
private val YELLOWS = listOf(Material.DANDELION, Material.DANDELION, Material.OXEYE_DAISY)

enum class Climate(
    val id: String,
    val log: Material,
    val flowers: List<Material>,
    val fallback: Biome,
    val tree: TreeType,
    val alt: TreeType,
    val density: Int,
    val cold: Boolean = false,
    val warm: Boolean = false,
    val gloom: Boolean = false,
    val bare: Boolean = false,
    val high: Boolean = false,
    val flora: Int = 6
) {
    FROSTBITTEN_STEPPE("frostbitten_steppe", Material.SPRUCE_LOG, COLD, Biome.SNOWY_PLAINS, TreeType.REDWOOD, TreeType.TALL_REDWOOD, 1, cold = true),
    PAPER_BIRCH_BARRENS("paper_birch_barrens", Material.BIRCH_LOG, WHITE, Biome.BIRCH_FOREST, TreeType.BIRCH, TreeType.TALL_BIRCH, 5, cold = true),
    HOARFROST_TAIGA("hoarfrost_taiga", Material.SPRUCE_LOG, COLD, Biome.SNOWY_TAIGA, TreeType.REDWOOD, TreeType.TALL_REDWOOD, 12, cold = true),
    SILVER_BIRCH_VALE("silver_birch_vale", Material.BIRCH_LOG, WHITE, Biome.BIRCH_FOREST, TreeType.BIRCH, TreeType.TALL_BIRCH, 10, cold = true),
    FOGBOUND_SPRUCE("fogbound_spruce", Material.SPRUCE_LOG, COLD, Biome.TAIGA, TreeType.REDWOOD, TreeType.TALL_REDWOOD, 16, cold = true),
    GREY_FEN("grey_fen", Material.MANGROVE_LOG, COLD, Biome.SWAMP, TreeType.SWAMP, TreeType.TREE, 6, cold = true),
    DRY_MEADOW("dry_meadow", Material.OAK_LOG, WILD, Biome.MEADOW, TreeType.TREE, TreeType.BIG_TREE, 1),
    WINDSWEPT_HEATH("windswept_heath", Material.OAK_LOG, WILD, Biome.WINDSWEPT_HILLS, TreeType.TREE, TreeType.REDWOOD, 1),
    OLD_OAK_FOREST("old_oak_forest", Material.OAK_LOG, WILD, Biome.FOREST, TreeType.TREE, TreeType.BIG_TREE, 12),
    WHITE_BIRCH_FOREST("white_birch_forest", Material.BIRCH_LOG, WHITE, Biome.BIRCH_FOREST, TreeType.BIRCH, TreeType.TALL_BIRCH, 12),
    WET_WOODLAND("wet_woodland", Material.OAK_LOG, WILD, Biome.FOREST, TreeType.TREE, TreeType.BIG_TREE, 14),
    MOSSY_HOLLOWS("mossy_hollows", Material.DARK_OAK_LOG, WILD, Biome.DARK_FOREST, TreeType.TREE, TreeType.DARK_OAK, 15),
    YELLOW_PLAINS("yellow_plains", Material.OAK_LOG, DRY, Biome.PLAINS, TreeType.TREE, TreeType.BIG_TREE, 1, warm = true),
    SCRUB_SAVANNA("scrub_savanna", Material.ACACIA_LOG, DRY, Biome.SAVANNA, TreeType.ACACIA, TreeType.ACACIA, 3, warm = true),
    MIXED_FOREST("mixed_forest", Material.OAK_LOG, WILD, Biome.FOREST, TreeType.TREE, TreeType.BIRCH, 12),
    DARK_OAK_FOREST("dark_oak_forest", Material.DARK_OAK_LOG, WILD, Biome.DARK_FOREST, TreeType.DARK_OAK, TreeType.TREE, 16),
    OVERGROWN_THICKET("overgrown_thicket", Material.OAK_LOG, WILD, Biome.FOREST, TreeType.TREE, TreeType.BIG_TREE, 18),
    DAMP_JUNGLE_EDGE("damp_jungle_edge", Material.JUNGLE_LOG, WILD, Biome.JUNGLE, TreeType.SMALL_JUNGLE, TreeType.JUNGLE, 8, warm = true),
    DUST_FLATS("dust_flats", Material.ACACIA_LOG, DRY, Biome.SAVANNA, TreeType.ACACIA, TreeType.ACACIA, 1, warm = true),
    DRY_ACACIA_PLAIN("dry_acacia_plain", Material.ACACIA_LOG, DRY, Biome.SAVANNA, TreeType.ACACIA, TreeType.TREE, 4, warm = true),
    SUN_BLEACHED_GROVE("sun_bleached_grove", Material.OAK_LOG, DRY, Biome.FOREST, TreeType.TREE, TreeType.BIG_TREE, 6, warm = true),
    OLIVE_WOODLAND("olive_woodland", Material.OAK_LOG, WILD, Biome.FOREST, TreeType.TREE, TreeType.BIG_TREE, 9, warm = true),
    HUMID_RAINFOREST("humid_rainforest", Material.JUNGLE_LOG, WILD, Biome.JUNGLE, TreeType.JUNGLE, TreeType.SMALL_JUNGLE, 14, warm = true),
    BLACK_BOG("black_bog", Material.MANGROVE_LOG, WILD, Biome.SWAMP, TreeType.SWAMP, TreeType.TREE, 8, warm = true),
    STONE_HIGHLANDS("stone_highlands", Material.SPRUCE_LOG, COLD, Biome.WINDSWEPT_FOREST, TreeType.REDWOOD, TreeType.TREE, 2, high = true),
    CLOUD_CREST("cloud_crest", Material.SPRUCE_LOG, COLD, Biome.WINDSWEPT_HILLS, TreeType.REDWOOD, TreeType.REDWOOD, 1, cold = true, high = true),
    FROZEN_ESTATE("frozen_estate", Material.SPRUCE_LOG, COLD, Biome.SNOWY_PLAINS, TreeType.REDWOOD, TreeType.REDWOOD, 2, cold = true, gloom = true, bare = true),
    ASHEN_CATHEDRAL("ashen_cathedral", Material.DARK_OAK_LOG, COLD, Biome.DARK_FOREST, TreeType.TREE, TreeType.TREE, 3, gloom = true, bare = true),
    GLOOMWOOD("gloomwood", Material.DARK_OAK_LOG, COLD, Biome.DARK_FOREST, TreeType.DARK_OAK, TreeType.TREE, 14, gloom = true),
    GREY_MARSH("grey_marsh", Material.MANGROVE_LOG, COLD, Biome.SWAMP, TreeType.SWAMP, TreeType.TREE, 5, gloom = true),
    DEAD_ORCHARD("dead_orchard", Material.OAK_LOG, DRY, Biome.PLAINS, TreeType.TREE, TreeType.TREE, 4, gloom = true, bare = true),
    SOOT_FIELDS("soot_fields", Material.ACACIA_LOG, DRY, Biome.PLAINS, TreeType.TREE, TreeType.TREE, 3, gloom = true, bare = true),
    SILENT_BOULEVARD("silent_boulevard", Material.BIRCH_LOG, WHITE, Biome.PLAINS, TreeType.BIRCH, TreeType.BIRCH, 3, gloom = true, bare = true),
    RUST_MEADOW("rust_meadow", Material.ACACIA_LOG, DRY, Biome.SAVANNA, TreeType.ACACIA, TreeType.TREE, 2, gloom = true),
    HOLLOW_MIST("hollow_mist", Material.SPRUCE_LOG, COLD, Biome.TAIGA, TreeType.REDWOOD, TreeType.TALL_REDWOOD, 12, gloom = true),
    REQUIEM_HEATH("requiem_heath", Material.DARK_OAK_LOG, COLD, Biome.PLAINS, TreeType.TREE, TreeType.TREE, 3, gloom = true, bare = true),
    FORGOTTEN_GARDEN("forgotten_garden", Material.OAK_LOG, WILD, Biome.FLOWER_FOREST, TreeType.TREE, TreeType.BIG_TREE, 7, flora = 18),
    IVY_COURTYARD("ivy_courtyard", Material.DARK_OAK_LOG, WILD, Biome.FOREST, TreeType.TREE, TreeType.DARK_OAK, 9, flora = 14),
    WILD_ORCHARD("wild_orchard", Material.OAK_LOG, WILD, Biome.FOREST, TreeType.TREE, TreeType.BIG_TREE, 10, flora = 16),
    PALE_FLOWER_FIELDS("pale_flower_fields", Material.OAK_LOG, WHITE, Biome.FLOWER_FOREST, TreeType.TREE, TreeType.BIRCH, 1, flora = 48),
    POPPY_MEADOW("poppy_meadow", Material.OAK_LOG, REDS, Biome.MEADOW, TreeType.TREE, TreeType.TREE, 1, flora = 46),
    DANDELION_PLAIN("dandelion_plain", Material.OAK_LOG, YELLOWS, Biome.PLAINS, TreeType.TREE, TreeType.BIG_TREE, 1, warm = true, flora = 46),
    CORNFLOWER_HAZE("cornflower_haze", Material.BIRCH_LOG, BLUES, Biome.FLOWER_FOREST, TreeType.BIRCH, TreeType.TREE, 1, flora = 44),
    QUIET_SUBURBIA("quiet_suburbia", Material.OAK_LOG, WILD, Biome.PLAINS, TreeType.TREE, TreeType.BIRCH, 3, flora = 8),
    CUL_DE_SAC("cul_de_sac", Material.BIRCH_LOG, WHITE, Biome.PLAINS, TreeType.BIRCH, TreeType.TREE, 2, flora = 8);

    companion object {
        val GRID: Array<Array<Array<Climate>>> = arrayOf(
            arrayOf(arrayOf(FROSTBITTEN_STEPPE, PAPER_BIRCH_BARRENS), arrayOf(HOARFROST_TAIGA, SILVER_BIRCH_VALE), arrayOf(FOGBOUND_SPRUCE, GREY_FEN)),
            arrayOf(arrayOf(DRY_MEADOW, WINDSWEPT_HEATH), arrayOf(OLD_OAK_FOREST, WHITE_BIRCH_FOREST), arrayOf(WET_WOODLAND, MOSSY_HOLLOWS)),
            arrayOf(arrayOf(YELLOW_PLAINS, SCRUB_SAVANNA), arrayOf(MIXED_FOREST, DARK_OAK_FOREST), arrayOf(OVERGROWN_THICKET, DAMP_JUNGLE_EDGE)),
            arrayOf(arrayOf(DUST_FLATS, DRY_ACACIA_PLAIN), arrayOf(SUN_BLEACHED_GROVE, OLIVE_WOODLAND), arrayOf(HUMID_RAINFOREST, BLACK_BOG))
        )
        val GLOOM: Array<Climate> = arrayOf(FROZEN_ESTATE, ASHEN_CATHEDRAL, GLOOMWOOD, GREY_MARSH, DEAD_ORCHARD, SOOT_FIELDS, SILENT_BOULEVARD, RUST_MEADOW, HOLLOW_MIST, REQUIEM_HEATH)
        val GARDEN: Array<Climate> = arrayOf(FORGOTTEN_GARDEN, IVY_COURTYARD, WILD_ORCHARD)
        val FLOWERS: Array<Climate> = arrayOf(PALE_FLOWER_FIELDS, POPPY_MEADOW, DANDELION_PLAIN, CORNFLOWER_HAZE)
        val HOOD: Array<Climate> = arrayOf(QUIET_SUBURBIA, CUL_DE_SAC)
        val HIGH_WARM = STONE_HIGHLANDS
        val HIGH_COLD = CLOUD_CREST
    }
}

object ClimateMap {
    private class Noises(seed: Long) {
        val t = SimplexOctaveGenerator(Random(seed xor 0x1F2E3D4C5B6AL), 2).also { it.setScale(0.0012) }
        val h = SimplexOctaveGenerator(Random(seed xor 0x2A3B4C5D6E7FL), 2).also { it.setScale(0.0012) }
        val v = SimplexOctaveGenerator(Random(seed xor 0x3C4D5E6F7081L), 2).also { it.setScale(0.0022) }
        val g = SimplexOctaveGenerator(Random(seed xor 0x4D5E6F708192L), 2).also { it.setScale(0.0009) }
    }

    private val noises = ConcurrentHashMap<Long, Noises>()

    private class Memo { var seed = 0L; var x = Int.MIN_VALUE; var z = Int.MIN_VALUE; var value: Climate = Climate.OLD_OAK_FOREST }
    private val memo = ThreadLocal.withInitial { Memo() }

    fun at(seed: Long, x: Int, z: Int): Climate {
        val m = memo.get()
        if (m.seed == seed && m.x == x && m.z == z) return m.value
        val value = compute(seed, x, z)
        m.seed = seed; m.x = x; m.z = z; m.value = value
        return value
    }

    private fun compute(seed: Long, x: Int, z: Int): Climate {
        val n = noises.computeIfAbsent(seed) { Noises(it) }
        val fx = x.toDouble()
        val fz = z.toDouble()
        val tv = n.t.noise(fx, fz, 0.5, 0.5, true)
        val hv = n.h.noise(fx, fz, 0.5, 0.5, true)
        val vv = n.v.noise(fx, fz, 0.5, 0.5, true)
        val gv = n.g.noise(fx, fz, 0.5, 0.5, true)

        if (Terrain.heightAt(seed, x, z) >= HIGH_Y) return if (tv > 0.0) Climate.HIGH_WARM else Climate.HIGH_COLD

        val tb = when {
            tv < -0.18 -> 0
            tv < 0.0 -> 1
            tv < 0.18 -> 2
            else -> 3
        }
        val hb = when {
            hv < -0.12 -> 0
            hv < 0.12 -> 1
            else -> 2
        }
        when (Realms.at(seed, x, z)) {
            Realm.GARDEN -> return Climate.GARDEN[(hb + (if (vv < 0.0) 0 else 1)) % Climate.GARDEN.size]
            Realm.FLOWERS -> return Climate.FLOWERS[tb % Climate.FLOWERS.size]
            Realm.NEIGHBORHOOD -> return Climate.HOOD[if (vv < 0.0) 0 else 1]
            else -> Unit
        }
        if (gv > 0.28) return Climate.GLOOM[(tb * 3 + hb) % Climate.GLOOM.size]
        return Climate.GRID[tb][hb][if (vv < 0.0) 0 else 1]
    }

    fun useVanilla(seed: Long, x: Int, z: Int): Boolean {
        if (Settings.vanillaMixChance <= 0) return false
        val regionX = Math.floorDiv(x, 256)
        val regionZ = Math.floorDiv(z, 256)
        return Math.floorMod(mix(seed, regionX, regionZ, 777), 100) < Settings.vanillaMixChance
    }
}
