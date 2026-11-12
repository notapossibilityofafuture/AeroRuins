package com.example.aeroruins

import org.bukkit.block.data.BlockData
import org.bukkit.Material
import org.bukkit.Material as M
import org.bukkit.generator.ChunkGenerator.ChunkData
import org.bukkit.generator.WorldInfo
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

private fun fmod(a: Int, b: Int): Int = Math.floorMod(a, b)
private fun fdiv(a: Int, b: Int): Int = Math.floorDiv(a, b)




class Deco(val m: Material, val pct: Int, private val props: String? = null) {
    fun data(): BlockData = if (props == null) m.createBlockData() else m.createBlockData(props)
}





class CaveStyle(
    val biome: String,
    val floor: Array<Material>,
    val ceil: Array<Material>,
    val ceilPct: Int,
    val floorDeco: Array<Deco> = emptyArray(),
    val ceilDeco: Array<Deco> = emptyArray(),
    val hang: Int = 0,
    val hangPct: Int = 0
) {
    
    var ores: Array<Material> = emptyArray()
    var orePerMille = 0

    fun withOres(perMille: Int, vararg m: Material): CaveStyle {
        ores = arrayOf(*m)
        orePerMille = perMille
        return this
    }
}

private val DEEP_ORES: Map<Material, Material> = mapOf(
    Material.COAL_ORE to Material.DEEPSLATE_COAL_ORE, Material.IRON_ORE to Material.DEEPSLATE_IRON_ORE,
    Material.COPPER_ORE to Material.DEEPSLATE_COPPER_ORE, Material.GOLD_ORE to Material.DEEPSLATE_GOLD_ORE,
    Material.REDSTONE_ORE to Material.DEEPSLATE_REDSTONE_ORE, Material.LAPIS_ORE to Material.DEEPSLATE_LAPIS_ORE,
    Material.DIAMOND_ORE to Material.DEEPSLATE_DIAMOND_ORE, Material.EMERALD_ORE to Material.DEEPSLATE_EMERALD_ORE
)


private fun oreFor(base: Material, y: Int): Material = if (y < 0) DEEP_ORES[base] ?: base else base

private fun maybeOre(seed: Long, st: CaveStyle, wx: Int, k: Int, y: Int, salt: Int): Material? {
    if (st.ores.isEmpty()) return null
    if (mix(seed, wx, k, salt) % 1000 >= st.orePerMille) return null
    return oreFor(st.ores[mix(seed, wx, k, salt + 1) % st.ores.size], y)
}

object CaveStyles {
    private val UP = "[facing=up]"
    private val DOWN = "[facing=down]"
    private val DRIP_UP = "[vertical_direction=up,thickness=tip]"
    private val DRIP_DOWN = "[vertical_direction=down,thickness=tip]"

    
    val ALL: Array<CaveStyle> = arrayOf(
        
        CaveStyle(
            "mossy_hollow", arrayOf(M.MOSS_BLOCK, M.MOSS_BLOCK, M.MOSS_BLOCK, M.MOSS_BLOCK, M.CLAY),
            arrayOf(M.MOSS_BLOCK), 35,
            arrayOf(Deco(M.MOSS_CARPET, 30), Deco(M.SHORT_GRASS, 12), Deco(M.FERN, 6), Deco(M.AZALEA, 4), Deco(M.FLOWERING_AZALEA, 2)),
            arrayOf(Deco(M.SPORE_BLOSSOM, 3)), 1, 8
        ),
        
        CaveStyle(
            "amethyst_vault", arrayOf(M.CALCITE, M.CALCITE, M.CALCITE, M.AMETHYST_BLOCK, M.AMETHYST_BLOCK, M.BUDDING_AMETHYST, M.SMOOTH_BASALT, M.SMOOTH_BASALT),
            arrayOf(M.AMETHYST_BLOCK), 22,
            arrayOf(Deco(M.AMETHYST_CLUSTER, 14, UP), Deco(M.MEDIUM_AMETHYST_BUD, 6, UP), Deco(M.SMALL_AMETHYST_BUD, 6, UP)),
            arrayOf(Deco(M.AMETHYST_CLUSTER, 8, DOWN))
        ),
        
        CaveStyle(
            "spore_cavern", arrayOf(M.MYCELIUM, M.MYCELIUM, M.MYCELIUM, M.MYCELIUM, M.MYCELIUM, M.MYCELIUM, M.SHROOMLIGHT, M.PODZOL, M.PODZOL),
            arrayOf(M.SHROOMLIGHT), 7,
            arrayOf(Deco(M.RED_MUSHROOM, 9), Deco(M.BROWN_MUSHROOM, 9))
        ),
        
        CaveStyle(
            "glowworm_grotto", arrayOf(M.CLAY, M.CLAY, M.CLAY, M.GRAVEL, M.MOSS_BLOCK),
            arrayOf(M.DRIPSTONE_BLOCK, M.DRIPSTONE_BLOCK, M.VERDANT_FROGLIGHT), 28,
            arrayOf(Deco(M.AZURE_BLUET, 5), Deco(M.SHORT_GRASS, 8)),
            emptyArray(), 1, 16
        ),
        
        CaveStyle(
            "dripstone_gallery", arrayOf(M.DRIPSTONE_BLOCK, M.DRIPSTONE_BLOCK, M.DRIPSTONE_BLOCK, M.DRIPSTONE_BLOCK, M.DRIPSTONE_BLOCK, M.GRANITE, M.GRANITE),
            arrayOf(M.DRIPSTONE_BLOCK), 45,
            arrayOf(Deco(M.POINTED_DRIPSTONE, 12, DRIP_UP)),
            arrayOf(Deco(M.POINTED_DRIPSTONE, 18, DRIP_DOWN))
        ),
        
        CaveStyle(
            "frozen_cavern", arrayOf(M.PACKED_ICE, M.PACKED_ICE, M.PACKED_ICE, M.SNOW_BLOCK, M.SNOW_BLOCK, M.SNOW_BLOCK, M.BLUE_ICE),
            arrayOf(M.PACKED_ICE, M.PACKED_ICE, M.BLUE_ICE), 40
        ),
        
        CaveStyle(
            "sandstone_gallery", arrayOf(M.SAND, M.SAND, M.SAND, M.SANDSTONE, M.SMOOTH_SANDSTONE),
            arrayOf(M.SANDSTONE, M.CUT_SANDSTONE), 40,
            arrayOf(Deco(M.DEAD_BUSH, 6))
        ),
        
        CaveStyle(
            "bone_catacomb", arrayOf(M.STONE_BRICKS, M.STONE_BRICKS, M.STONE_BRICKS, M.CRACKED_STONE_BRICKS, M.CRACKED_STONE_BRICKS, M.MOSSY_STONE_BRICKS, M.BONE_BLOCK),
            arrayOf(M.STONE_BRICKS, M.COBBLESTONE), 30,
            arrayOf(Deco(M.COBWEB, 6), Deco(M.CANDLE, 3, "[candles=2,lit=true]"), Deco(M.BONE_BLOCK, 4)),
            arrayOf(Deco(M.COBWEB, 8))
        ),
        
        CaveStyle(
            "root_tangle", arrayOf(M.ROOTED_DIRT, M.ROOTED_DIRT, M.ROOTED_DIRT, M.ROOTED_DIRT, M.MOSS_BLOCK, M.COARSE_DIRT),
            arrayOf(M.ROOTED_DIRT, M.DIRT), 35,
            arrayOf(Deco(M.FERN, 6), Deco(M.BROWN_MUSHROOM, 3)), emptyArray(), 2, 18
        ),
        
        CaveStyle(
            "verdigris_vault", arrayOf(M.OXIDIZED_COPPER, M.OXIDIZED_COPPER, M.WEATHERED_COPPER, M.EXPOSED_COPPER, M.OXIDIZED_CUT_COPPER),
            arrayOf(M.OXIDIZED_COPPER, M.OXIDIZED_COPPER, M.OXIDIZED_CUT_COPPER, M.SEA_LANTERN), 30,
            arrayOf(Deco(M.LIGHTNING_ROD, 1, UP))
        ),
        
        CaveStyle(
            "tuff_labyrinth", arrayOf(M.TUFF, M.TUFF, M.TUFF, M.TUFF, M.POLISHED_TUFF, M.POLISHED_TUFF, M.TUFF_BRICKS),
            arrayOf(M.TUFF, M.TUFF, M.TUFF_BRICKS, M.TUFF, M.TUFF, M.TUFF, M.TUFF, M.GLOWSTONE), 40,
            arrayOf(Deco(M.COBWEB, 2))
        ),
        
        CaveStyle(
            "clay_warren", arrayOf(M.CLAY, M.CLAY, M.CLAY, M.TERRACOTTA, M.BROWN_TERRACOTTA, M.MUD),
            arrayOf(M.CLAY, M.TERRACOTTA), 40,
            arrayOf(Deco(M.DEAD_BUSH, 3), Deco(M.BROWN_MUSHROOM, 5)), emptyArray(), 2, 6
        ),
        
        CaveStyle(
            "cenote_pool", arrayOf(M.PRISMARINE, M.PRISMARINE, M.PRISMARINE_BRICKS, M.GRAVEL, M.SAND),
            arrayOf(M.PRISMARINE, M.PRISMARINE, M.PRISMARINE, M.SEA_LANTERN), 25,
            emptyArray(), emptyArray(), 1, 6
        ),
        
        CaveStyle(
            "spiderweb_hollow", arrayOf(M.BLACKSTONE, M.BLACKSTONE, M.GRAVEL, M.COARSE_DIRT),
            arrayOf(M.BLACKSTONE), 20,
            arrayOf(Deco(M.COBWEB, 20)), arrayOf(Deco(M.COBWEB, 25))
        ),
        
        CaveStyle(
            "terracotta_gallery", arrayOf(M.RED_SAND, M.RED_SAND, M.TERRACOTTA, M.ORANGE_TERRACOTTA, M.RED_SANDSTONE),
            arrayOf(M.RED_SANDSTONE, M.ORANGE_TERRACOTTA, M.SHROOMLIGHT), 30,
            arrayOf(Deco(M.DEAD_BUSH, 4))
        ),
        
        CaveStyle(
            "deepdark_abyss", arrayOf(M.SCULK, M.SCULK, M.SCULK, M.SCULK, M.DEEPSLATE, M.DEEPSLATE),
            arrayOf(M.SCULK, M.DEEPSLATE), 25,
            arrayOf(Deco(M.SCULK_SENSOR, 2))
        ),
        
        CaveStyle(
            "mantle_core", arrayOf(M.MAGMA_BLOCK, M.MAGMA_BLOCK, M.BASALT, M.BASALT, M.BLACKSTONE, M.BLACKSTONE),
            arrayOf(M.BASALT, M.BLACKSTONE, M.GLOWSTONE), 40
        ),
        
        CaveStyle(
            "obsidian_rift", arrayOf(M.OBSIDIAN, M.OBSIDIAN, M.OBSIDIAN, M.CRYING_OBSIDIAN, M.DEEPSLATE, M.BLACKSTONE),
            arrayOf(M.OBSIDIAN, M.CRYING_OBSIDIAN), 30,
            arrayOf(Deco(M.END_ROD, 2, UP))
        ),
        
        CaveStyle(
            "deepslate_halls", arrayOf(M.DEEPSLATE_TILES, M.DEEPSLATE_TILES, M.DEEPSLATE_TILES, M.DEEPSLATE_BRICKS, M.DEEPSLATE_BRICKS, M.CRACKED_DEEPSLATE_TILES, M.POLISHED_DEEPSLATE),
            arrayOf(M.DEEPSLATE_BRICKS, M.DEEPSLATE_TILES, M.GLOWSTONE), 30,
            arrayOf(Deco(M.SOUL_TORCH, 2), Deco(M.CANDLE, 2, "[candles=1,lit=true]"))
        ),
        
        CaveStyle(
            "bioluminescent_gallery", arrayOf(M.MOSS_BLOCK, M.MOSS_BLOCK, M.CLAY, M.DEEPSLATE),
            arrayOf(M.MOSS_BLOCK, M.MOSS_BLOCK, M.PEARLESCENT_FROGLIGHT, M.VERDANT_FROGLIGHT, M.OCHRE_FROGLIGHT), 35,
            arrayOf(Deco(M.BLUE_ORCHID, 6), Deco(M.SHORT_GRASS, 10), Deco(M.AZURE_BLUET, 5)), emptyArray(), 1, 22
        ),
        
        CaveStyle(
            "quartz_cavern", arrayOf(M.CALCITE, M.CALCITE, M.QUARTZ_BLOCK, M.DIORITE, M.SMOOTH_QUARTZ),
            arrayOf(M.CALCITE, M.QUARTZ_BLOCK, M.DIORITE), 40,
            arrayOf(Deco(M.QUARTZ_SLAB, 8))
        ),
        
        CaveStyle(
            "basalt_tubes", arrayOf(M.BASALT, M.SMOOTH_BASALT, M.BLACKSTONE, M.TUFF),
            arrayOf(M.BASALT, M.BLACKSTONE, M.MAGMA_BLOCK), 35
        ),
        
        CaveStyle(
            "mushroom_grove", arrayOf(M.MYCELIUM, M.MYCELIUM, M.PODZOL, M.RED_MUSHROOM_BLOCK, M.BROWN_MUSHROOM_BLOCK),
            arrayOf(M.SHROOMLIGHT, M.DIRT), 12,
            arrayOf(Deco(M.RED_MUSHROOM, 10), Deco(M.BROWN_MUSHROOM, 10))
        ),
        
        CaveStyle(
            "glacial_fissure", arrayOf(M.BLUE_ICE, M.PACKED_ICE, M.PACKED_ICE, M.STONE),
            arrayOf(M.PACKED_ICE, M.ICE, M.BLUE_ICE), 50,
            arrayOf(Deco(M.POWDER_SNOW, 5))
        ),
        
        CaveStyle(
            "timber_galleries", arrayOf(M.COBBLESTONE, M.COBBLESTONE, M.OAK_PLANKS, M.GRAVEL, M.COAL_ORE),
            arrayOf(M.OAK_PLANKS, M.COBBLESTONE, M.OAK_LOG), 25,
            arrayOf(Deco(M.COBWEB, 4), Deco(M.RAIL, 5), Deco(M.TORCH, 2)),
            arrayOf(Deco(M.COBWEB, 8))
        ),
        
        CaveStyle(
            "honeycomb_hollow", arrayOf(M.HONEYCOMB_BLOCK, M.MOSS_BLOCK, M.DIRT, M.ROOTED_DIRT),
            arrayOf(M.HONEYCOMB_BLOCK, M.MOSS_BLOCK), 40,
            arrayOf(Deco(M.FERN, 8), Deco(M.DANDELION, 5), Deco(M.SHORT_GRASS, 10)), emptyArray(), 2, 10
        ),
        
        CaveStyle(
            "drowned_crypt", arrayOf(M.MOSSY_STONE_BRICKS, M.STONE_BRICKS, M.CRACKED_STONE_BRICKS, M.CLAY, M.PRISMARINE),
            arrayOf(M.MOSSY_COBBLESTONE, M.STONE_BRICKS), 35,
            arrayOf(Deco(M.COBWEB, 5), Deco(M.BONE_BLOCK, 3)), emptyArray(), 1, 12
        ),
        
        CaveStyle(
            "ruby_hollow", arrayOf(M.DEEPSLATE, M.DEEPSLATE, M.RED_TERRACOTTA, M.REDSTONE_BLOCK),
            arrayOf(M.DEEPSLATE, M.REDSTONE_BLOCK), 30
        ),
        
        CaveStyle(
            "fossil_cavern", arrayOf(M.SMOOTH_SANDSTONE, M.SANDSTONE, M.BONE_BLOCK, M.CALCITE),
            arrayOf(M.SANDSTONE, M.BONE_BLOCK), 30,
            arrayOf(Deco(M.BONE_BLOCK, 6), Deco(M.COBWEB, 3), Deco(M.DEAD_BUSH, 3))
        ),
        
        CaveStyle(
            "salt_caverns", arrayOf(M.CALCITE, M.DIORITE, M.POLISHED_DIORITE),
            arrayOf(M.CALCITE, M.DIORITE, M.POLISHED_DIORITE), 45
        ),
        
        CaveStyle(
            "ember_cavity", arrayOf(M.NETHERRACK, M.NETHERRACK, M.MAGMA_BLOCK, M.BLACKSTONE),
            arrayOf(M.NETHERRACK, M.GLOWSTONE, M.BLACKSTONE), 45
        ),
        
        CaveStyle(
            "bamboo_grotto", arrayOf(M.MOSS_BLOCK, M.PODZOL, M.ROOTED_DIRT, M.DIRT),
            arrayOf(M.MOSS_BLOCK, M.DIRT), 30,
            arrayOf(Deco(M.BAMBOO, 14), Deco(M.FERN, 8), Deco(M.SHORT_GRASS, 8)), emptyArray(), 1, 6
        )
    )

    init {
        val O = M.COAL_ORE; val I = M.IRON_ORE; val C = M.COPPER_ORE; val G = M.GOLD_ORE
        val R = M.REDSTONE_ORE; val L = M.LAPIS_ORE; val D = M.DIAMOND_ORE; val E = M.EMERALD_ORE
        ALL[0].withOres(15, O, O, I)
        ALL[1].withOres(15, L, L, G, G)
        ALL[2].withOres(15, I, I, R)
        ALL[3].withOres(15, L, C, C)
        ALL[4].withOres(18, C, C, I)
        ALL[5].withOres(15, L, L, L, L, L, L, L, D)
        ALL[6].withOres(15, G, G, R, R)
        ALL[7].withOres(15, O, O, G, G, G, G, G, E)
        ALL[8].withOres(15, O, O, I)
        ALL[9].withOres(80, C)
        ALL[10].withOres(15, I, I, G)
        ALL[11].withOres(15, I, O)
        ALL[12].withOres(15, L, L, L, L, L, L, L, D)
        ALL[13].withOres(15, O, O, R)
        ALL[14].withOres(15, G, G, R, R)
        ALL[15].withOres(18, R, R, R, R, R, R, R, D)
        ALL[16].withOres(18, G, G, G, M.NETHERRACK) 
        ALL[17].withOres(18, G, G, G, G, G, G, G, D)
        ALL[18].withOres(18, I, I, R, R, R, R, R, D)
        ALL[19].withOres(15, L, C, C)
        ALL[20].withOres(15, G, L, L)
        ALL[21].withOres(15, I, C, G)
        ALL[22].withOres(15, I, R)
        ALL[23].withOres(15, L, L, L, L, L, L, L, D)
        ALL[24].withOres(60, O, O, O, I)
        ALL[25].withOres(15, O, I)
        ALL[26].withOres(15, G, G, G, G, G, G, G, E)
        ALL[27].withOres(70, R)
        ALL[28].withOres(18, G, O, I)
        ALL[29].withOres(15, I, G)
        ALL[30].withOres(20, G, G, G, G, G, G, G, D)
        ALL[31].withOres(15, C, L)
        require(ALL.size == 32) { "CaveStyles.ALL debe tener 32 estilos" }
    }

    val BIOMES: List<String> = ALL.map { it.biome }
}




private class Room(val cy: Int, val rx: Int, val ry: Int, val d2: Double, val pillar: Boolean)

private fun roomsAt(seed: Long, wx: Int, wz: Int, salt: Int, yLow: Int, yHigh: Int, out: ArrayList<Room>) {
    val gx0 = fdiv(wx, 128)
    val gz0 = fdiv(wz, 128)
    for (gx in gx0 - 1..gx0 + 1) for (gz in gz0 - 1..gz0 + 1) {
        val h = mix(seed, gx, gz, salt)
        if (h % 100 >= 38) continue
        val cx = gx * 128 + 30 + (h shr 8) % 68
        val cz = gz * 128 + 30 + (h shr 16) % 68
        val rx = 20 + (h shr 12) % 14
        val dx = wx - cx
        val dz = wz - cz
        val d2 = (dx * dx + dz * dz).toDouble()
        if (d2 >= rx.toDouble() * rx) continue
        val cy = yLow + (h shr 4) % (yHigh - yLow + 1)
        val ry = 8 + (h shr 20) % 7
        val pillar = fmod(dx, 11) in 0..1 && fmod(dz, 11) in 0..1
        out.add(Room(cy, rx, ry, d2, pillar))
    }
}





internal fun AeroGenerator.applyCaves(
    info: WorldInfo, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int, surface: Int, allowEntrance: Boolean,
    hardTop: Int = Int.MAX_VALUE
) {
    val seed = info.seed
    val open = allowEntrance && hardTop == Int.MAX_VALUE && surface > SEA_LEVEL + 1

    
    var sink = false
    if (open) {
        val sgx = fdiv(wx, 48)
        val sgz = fdiv(wz, 48)
        val sh = mix(seed, sgx, sgz, 28)
        if (sh % 100 < 12 && surface > SEA_LEVEL + 3) {
            val scx = sgx * 48 + 10 + (sh shr 8) % 28
            val scz = sgz * 48 + 10 + (sh shr 16) % 28
            val sr = 3 + (sh shr 4) % 4
            val ddx = wx - scx
            val ddz = wz - scz
            sink = ddx * ddx + ddz * ddz <= sr * sr
        }
    }
    val entrance = open && (sink || pondNoise(seed, wx, wz) > 0.52)

    val minY = info.minHeight + 5
    var maxY = if (entrance) surface - 1 else surface - 6
    if (hardTop < maxY) maxY = hardTop
    if (maxY <= minY) return

    
    var rooms: ArrayList<Room>? = null
    run {
        val tmp = ArrayList<Room>(2)
        roomsAt(seed, wx, wz, 26, 16, 35, tmp)
        roomsAt(seed, wx, wz, 27, -46, -22, tmp)
        if (tmp.isNotEmpty()) rooms = tmp
    }

    val air = BooleanArray(maxY - minY + 1)
    var any = false
    val grid = caveGrid()
    for (y in minY..maxY) {
        
        val pen = if (entrance) 0.0 else maxOf(0, y - (maxY - 6)) * 0.03
        
        var carve = grid.carved(x, y, z, pen)
        
        if (!carve && sink && y >= 14) carve = true
        
        val rs = rooms
        if (!carve && rs != null) {
            for (r in rs) {
                val dy = (y - r.cy).toDouble()
                val v = r.d2 / (r.rx.toDouble() * r.rx) + dy * dy / (r.ry.toDouble() * r.ry)
                if (v < 1.0 && !(r.pillar && v > 0.08)) {
                    carve = true
                    break
                }
            }
        }
        air[y - minY] = carve
        if (carve) any = true
    }
    if (!any) return

    
    for (y in minY..maxY) {
        if (!air[y - minY]) continue
        val m = when {
            y in LAKE_BOTTOM..LAKE_TOP -> Material.WATER
            y <= LAVA_Y -> Material.LAVA
            else -> Material.AIR
        }
        data.setBlock(x, y, z, m)
    }
    
    val seal = LAKE_BOTTOM - 1
    if (seal >= minY && LAKE_BOTTOM <= maxY && air[seal - minY] && air[LAKE_BOTTOM - minY]) {
        data.setBlock(x, seal, z, Material.CLAY)
        air[seal - minY] = false
    }
    if (entrance && air[maxY - minY]) data.setBlock(x, surface, z, Material.AIR) 

    for (y in minY..maxY) {
        if (!air[y - minY]) continue
        if (y in LAKE_BOTTOM..LAKE_TOP || y <= LAVA_Y) continue
        val floorBelow = y - 1 >= minY && !air[y - 1 - minY]
        val ceilAbove = y + 1 <= maxY && !air[y + 1 - minY]
        if (!floorBelow && !ceilAbove) continue
        val st = CaveStyles.ALL[caveTheme(seed, wx, y, wz)]
        if (floorBelow) lineFloor(seed, data, st, x, y, z, wx, wz)
        if (ceilAbove) lineCeiling(seed, data, st, x, y, z, wx, wz, air, minY)
    }
}

private fun lineFloor(seed: Long, data: ChunkData, st: CaveStyle, x: Int, y: Int, z: Int, wx: Int, wz: Int) {
    val k = wz * 31 + y
    data.setBlock(x, y - 1, z, maybeOre(seed, st, wx, k, y, 60) ?: st.floor[mix(seed, wx, k, 41) % st.floor.size])
    if (st.floorDeco.isEmpty()) return
    val h2 = mix(seed, wx, k, 42) % 100
    var acc = 0
    for (d in st.floorDeco) {
        acc += d.pct
        if (h2 < acc) {
            data.setBlock(x, y, z, d.data())
            return
        }
    }
}

private fun lineCeiling(
    seed: Long, data: ChunkData, st: CaveStyle, x: Int, y: Int, z: Int, wx: Int, wz: Int, air: BooleanArray, minY: Int
) {
    val k = wz * 31 + y
    val ceilOre = maybeOre(seed, st, wx, k, y, 64)
    if (ceilOre != null) {
        data.setBlock(x, y + 1, z, ceilOre)
    } else if (mix(seed, wx, k, 43) % 100 < st.ceilPct) {
        data.setBlock(x, y + 1, z, st.ceil[mix(seed, wx, k, 47) % st.ceil.size])
    }
    val h2 = mix(seed, wx, k, 44) % 100
    var acc = 0
    for (d in st.ceilDeco) {
        acc += d.pct
        if (h2 < acc) {
            data.setBlock(x, y, z, d.data())
            return
        }
    }
    if (st.hang == 0 || mix(seed, wx, k, 48) % 100 >= st.hangPct) return
    if (st.hang == 1) { 
        val len = 2 + mix(seed, wx, k, 45) % 4
        for (i in 0 until len) {
            val yy = y - i
            if (yy < minY || !air[yy - minY]) break
            val m = if (i == len - 1) Material.CAVE_VINES else Material.CAVE_VINES_PLANT
            data.setBlock(x, yy, z, m.createBlockData("[berries=true]"))
        }
    } else {
        data.setBlock(x, y, z, Material.HANGING_ROOTS)
    }
}



class Geode(val cx: Int, val cy: Int, val cz: Int, val r: Int)

object Geodes {
    const val G = 72

    fun inCell(seed: Long, gen: AeroGenerator, gx: Int, gz: Int): Geode? {
        if (mix(seed, gx, gz, 31) % 100 >= 35) return null
        val cx = gx * G + 12 + mix(seed, gx, gz, 32) % (G - 24)
        val cz = gz * G + 12 + mix(seed, gx, gz, 33) % (G - 24)
        val zone = zoneAt(seed, cx, cz)
        if (zone != Zone.JUNGLE && zone != Zone.AERO) return null
        val cy = 18 + mix(seed, gx, gz, 34) % 14
        val r = 8 + mix(seed, gx, gz, 35) % 5
        val surface = if (zone == Zone.AERO && domeAt(seed, cx, cz) != null) FLOOR else gen.heightAt(seed, cx, cz)
        if (cy + r + 6 > surface) return null
        return Geode(cx, cy, cz, r)
    }
}

internal fun AeroGenerator.applyGeodes(info: WorldInfo, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int) {
    val seed = info.seed
    val gx0 = Math.floorDiv(wx, Geodes.G)
    val gz0 = Math.floorDiv(wz, Geodes.G)
    for (gx in gx0 - 1..gx0 + 1) for (gz in gz0 - 1..gz0 + 1) {
        val gd = Geodes.inCell(seed, this, gx, gz) ?: continue
        val ddx = wx - gd.cx
        val ddz = wz - gd.cz
        val h2 = ddx * ddx + ddz * ddz
        if (h2 > gd.r * gd.r) continue

        var prev: Material? = null
        for (y in gd.cy - gd.r..gd.cy + gd.r) {
            val dy = (y - gd.cy) * 1.1
            val d = sqrt(h2 + dy * dy)
            val m: Material? = when {
                d > gd.r -> null
                d > gd.r - 1.5 -> Material.SMOOTH_BASALT
                d > gd.r - 2.7 -> Material.CALCITE
                d > gd.r - 4.2 ->
                    if (mix(seed, wx + y * 7, wz, 36) % 100 < 10) Material.BUDDING_AMETHYST else Material.AMETHYST_BLOCK
                else -> Material.AIR
            }
            if (m != null) data.setBlock(x, y, z, m)
            val wasAmethyst = prev == Material.AMETHYST_BLOCK || prev == Material.BUDDING_AMETHYST
            
            if (m == Material.AIR && wasAmethyst && mix(seed, wx + y * 5, wz, 37) % 100 < 22) {
                data.setBlock(x, y, z, Material.AMETHYST_CLUSTER.createBlockData("[facing=up]"))
            }
            if ((m == Material.AMETHYST_BLOCK || m == Material.BUDDING_AMETHYST) && prev == Material.AIR &&
                mix(seed, wx + y * 3, wz, 38) % 100 < 12
            ) {
                data.setBlock(x, y - 1, z, Material.AMETHYST_CLUSTER.createBlockData("[facing=down]"))
            }
            prev = m
        }
    }
}




fun backroomsVariant(seed: Long, x: Int, z: Int): Int = hallVariant(seed, x, z, 60, 6)


internal fun AeroGenerator.backroomsColumn(info: WorldInfo, data: ChunkData, x: Int, z: Int, wx: Int, wz: Int) {
    val seed = info.seed
    val h = baseGround(info, data, x, z, wx, wz)
    if (Settings.caves) applyCaves(info, data, x, z, wx, wz, h, false, BF - 5) 

    val edge = zoneAt(seed, wx - 1, wz) != Zone.BACKROOMS || zoneAt(seed, wx + 1, wz) != Zone.BACKROOMS ||
        zoneAt(seed, wx, wz - 1) != Zone.BACKROOMS || zoneAt(seed, wx, wz + 1) != Zone.BACKROOMS
    if (edge) {
        Portals.carve(data, x, z, wx, wz, h, true) 
        return 
    }

    val variant = backroomsVariant(seed, wx, wz)
    val wall = when (variant) {
        1 -> Material.LIGHT_GRAY_CONCRETE
        3 -> Material.SMOOTH_SANDSTONE
        else -> Material.WHITE_CONCRETE
    }

    data.setRegion(x, BF + 1, z, x + 1, BC, z + 1, Material.AIR)
    if (variant >= 4) {
        corridorColumn(data, x, z, wx, wz, seed, variant)
        return
    }

    val px = fmod(wx, 12)
    val pz = fmod(wz, 12)
    val pillar = px in 0..2 && pz in 0..2
    if (pillar) {
        data.setRegion(x, BF, z, x + 1, BC, z + 1, wall)
    } else {
        val bare = mix(seed, fdiv(wx, 4), fdiv(wz, 4), 5) % 100 < (if (variant == 3) 55 else 14)
        val puddle = (variant == 1 && pondNoise(seed, wx, wz) > 0.22) || (variant == 2 && pondNoise(seed, wx, wz) > 0.55)
        if (puddle) {
            data.setBlock(x, BF, z, Material.WATER)
        } else {
            val floor = when {
                variant == 1 && !bare -> Material.MOSS_BLOCK
                bare -> Material.COARSE_DIRT
                else -> Material.GRASS_BLOCK
            }
            data.setBlock(x, BF, z, floor)
            val grass = if (variant == 2) 70 else 45
            val roll = mix(seed, wx, wz, 6) % 100
            if (floor == Material.GRASS_BLOCK || floor == Material.MOSS_BLOCK) {
                if (roll < grass) data.setBlock(x, BF + 1, z, Material.SHORT_GRASS)
                else if (variant == 2 && roll < grass + 8) data.setBlock(x, BF + 1, z, Material.AZURE_BLUET)
            }
        }
    }
    val deadChance = if (variant == 3) 35 else 5
    val light = fmod(wx, 6) in 2..3 && fmod(wz, 6) in 2..3 && mix(seed, fdiv(wx, 6), fdiv(wz, 6), 8) % 100 >= deadChance
    data.setBlock(x, BC, z, if (light) Material.SEA_LANTERN else wall)
    data.setBlock(x, BC + 1, z, wall)
}





private fun AeroGenerator.corridorColumn(data: ChunkData, x: Int, z: Int, wx: Int, wz: Int, seed: Long, variant: Int) {
    val period = 24
    val subway = variant == 5
    val ax = fmod(wx, period)
    val az = fmod(wz, period)
    val openX = fmod(wz, period) in 8..15 || mix(seed, fdiv(wx, period), fdiv(wz, period), 71) % 100 < 25
    val openZ = fmod(wx, period) in 8..15 || mix(seed, fdiv(wx, period), fdiv(wz, period), 72) % 100 < 25
    val wallX = ax == 0 && !openX
    val wallZ = az == 0 && !openZ

    
    if (subway) {
        data.setBlock(x, BF, z, if (((wx + wz) and 1) == 0) Material.LIGHT_GRAY_CONCRETE else Material.WHITE_CONCRETE)
    } else {
        val checker = ((fdiv(wx, 2) + fdiv(wz, 2)) and 1) == 0
        data.setBlock(x, BF, z, if (checker) Material.WHITE_CONCRETE else Material.CYAN_TERRACOTTA)
    }

    val px = fmod(wx, 12)
    val pz = fmod(wz, 12)
    val pillar = !subway && px in 4..6 && pz in 4..6 && (px == 5 || pz == 5) 
    if (wallX || wallZ) {
        for (y in BF + 1 until BC) {
            val m = if (subway) Material.WHITE_TERRACOTTA else if (y <= BF + 2) Material.LIGHT_GRAY_CONCRETE else Material.WHITE_CONCRETE
            data.setBlock(x, y, z, m)
        }
    } else if (pillar) {
        data.setRegion(x, BF + 1, z, x + 1, BC, z + 1, Material.WHITE_CONCRETE)
    } else if (subway && pondNoise(seed, wx, wz) > -0.45) {
        data.setBlock(x, BF + 1, z, Material.WATER) 
    }

    
    if (subway) {
        if (fmod(wx, 6) in 1..4 && fmod(wz, 6) in 2..3) {
            data.setBlock(x, BC, z, Material.PINK_STAINED_GLASS)
            data.setBlock(x, BC + 1, z, Material.SEA_LANTERN) 
        } else {
            data.setBlock(x, BC, z, Material.WHITE_TERRACOTTA)
            data.setBlock(x, BC + 1, z, Material.WHITE_TERRACOTTA)
        }
    } else {
        val tube = fmod(wz, 6) == 3 && fmod(wx, 6) in 0..3 
        data.setBlock(x, BC, z, if (tube) Material.SEA_LANTERN else Material.WHITE_CONCRETE)
        data.setBlock(x, BC + 1, z, Material.WHITE_CONCRETE)
    }
}
