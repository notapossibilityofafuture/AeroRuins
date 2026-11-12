package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.entity.EntityType
import java.util.Random

object Loot {
    private val faces = arrayOf("north", "south", "east", "west")

    fun face(random: Random): String = faces[random.nextInt(4)]

    
    fun rare(random: Random, percent: Double): Boolean = random.nextDouble() * 100.0 < percent * Settings.chestScale

    fun chestAt(p: Place, random: Random, x: Int, y: Int, z: Int, table: String): Boolean {
        if (!p.type(x, y - 1, z).isOccluding || !p.isAir(x, y, z)) return false
        p.chest(x, y, z, face(random), table)
        return true
    }

    fun mobAt(p: Place, x: Int, y: Int, z: Int, type: EntityType): Boolean {
        if (java.util.concurrent.ThreadLocalRandom.current().nextDouble() >= Settings.structureMobRate) return false
        if (!p.type(x, y - 1, z).isOccluding || !p.isAir(x, y, z) || !p.isAir(x, y + 1, z)) return false
        p.spawn(x, y, z, type)
        return true
    }

    
    fun fill(
        p: Place, random: Random, x0: Int, z0: Int, w: Int, d: Int, margin: Int, floors: List<Int>,
        table: String, mobs: List<EntityType>, chests: Int, mobCount: Int
    ) {
        var c = 0
        var m = 0
        var tries = 0
        while ((c < chests || m < mobCount) && tries < 70) {
            tries++
            val x = x0 + margin + random.nextInt(maxOf(w - 2 * margin, 1))
            val z = z0 + margin + random.nextInt(maxOf(d - 2 * margin, 1))
            val y = floors[random.nextInt(floors.size)]
            val wantChest = c < chests && (m >= mobCount || random.nextBoolean())
            if (wantChest) {
                if (chestAt(p, random, x, y, z, table)) c++
            } else if (m < mobCount && mobs.isNotEmpty()) {
                if (mobAt(p, x, y, z, mobs[random.nextInt(mobs.size)])) m++
            }
        }
    }
}


object Underground {
    fun populate(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        when (zoneAt(seed, bx + 8, bz + 8)) {
            Zone.BACKROOMS -> hall(
                seed, random, p, bx, bz, Zone.BACKROOMS, BF, "backrooms", 2.0,
                listOf(EntityType.ENDERMAN, EntityType.SILVERFISH, EntityType.ZOMBIE)
            ) { it == Material.GRASS_BLOCK || it == Material.COARSE_DIRT || it == Material.MOSS_BLOCK }
            Zone.MALL -> {
                hall(
                    seed, random, p, bx, bz, Zone.MALL, MF, "mall", 0.0,
                    listOf(EntityType.ZOMBIE_VILLAGER, EntityType.SKELETON, EntityType.WITCH)
                ) { it == Material.STONE_BRICKS || it == Material.POLISHED_ANDESITE }
                if (Loot.rare(random, 7.0)) mallShop(seed, random, p, bx, bz)
            }
            Zone.QUARRY -> hall(
                seed, random, p, bx, bz, Zone.QUARRY, QF, "quarry", 2.5,
                listOf(EntityType.CAVE_SPIDER, EntityType.ZOMBIE, EntityType.SKELETON)
            ) { it == Material.GRASS_BLOCK }
            else -> Unit
        }
        caves(seed, random, p, bx, bz, gen)
        geodes(seed, random, p, bx, bz, gen)
    }

    private fun hall(
        seed: Long, random: Random, p: Place, bx: Int, bz: Int, zone: Zone, floorY: Int, table: String, chestPct: Double,
        mobs: List<EntityType>, okFloor: (Material) -> Boolean
    ) {
        if (chestPct > 0.0 && Loot.rare(random, chestPct)) {
            for (attempt in 0 until 10) {
                val x = bx + random.nextInt(16)
                val z = bz + random.nextInt(16)
                if (zoneAt(seed, x, z) == zone && okFloor(p.type(x, floorY, z)) &&
                    Loot.chestAt(p, random, x, floorY + 1, z, table)
                ) break
            }
        }
        if (random.nextInt(100) < 30) {
            repeat(10) {
                val x = bx + random.nextInt(16)
                val z = bz + random.nextInt(16)
                if (zoneAt(seed, x, z) == zone && okFloor(p.type(x, floorY, z)) &&
                    Loot.mobAt(p, x, floorY + 1, z, mobs[random.nextInt(mobs.size)])
                ) return@repeat
            }
        }
    }

    
    private fun mallShop(seed: Long, random: Random, p: Place, bx: Int, bz: Int) {
        val w = 9
        val d = 6
        val x0 = bx + 2 + random.nextInt(4)
        val z0 = bz + 2 + random.nextInt(4)
        val ok = setOf(Material.STONE_BRICKS, Material.POLISHED_ANDESITE)
        for (dx in 0 until w) for (dz in 0 until d) {
            if (zoneAt(seed, x0 + dx, z0 + dz) != Zone.MALL) return
            if (p.type(x0 + dx, MF, z0 + dz) !in ok) return
            for (y in MF + 1..MF + 4) if (!p.isAir(x0 + dx, y, z0 + dz)) return
        }
        for (dx in -1..w) for (dz in -1..d) {
            val edge = dx == -1 || dx == w || dz == -1 || dz == d
            p.set(x0 + dx, MF, z0 + dz, if (edge) Material.STONE_BRICKS else Material.DARK_OAK_PLANKS)
            for (y in MF + 1..MF + 4) {
                if (!edge) continue
                val front = dz == -1
                val door = front && dx in 3..5 && y <= MF + 2
                val m = when {
                    door -> Material.AIR
                    front && y <= MF + 3 -> Material.LIGHT_BLUE_STAINED_GLASS
                    else -> Material.GRAY_CONCRETE
                }
                p.set(x0 + dx, y, z0 + dz, m)
            }
            p.set(x0 + dx, MF + 5, z0 + dz, Material.GRAY_CONCRETE)
        }
        p.set(x0 + w / 2, MF + 4, z0 + d / 2, Material.SEA_LANTERN)
        for (dx in 1 until w - 1) {
            p.set(x0 + dx, MF + 1, z0 + d - 1, Material.BOOKSHELF)
            p.set(x0 + dx, MF + 2, z0 + d - 1, Material.BOOKSHELF)
        }
        p.set(x0 + 1, MF + 1, z0 + 2, Material.BARREL)
        p.set(x0 + w - 2, MF + 1, z0 + 2, Material.BARREL)
        p.chest(x0 + w / 2, MF + 1, z0 + d - 2, "north", "mall")
        Loot.mobAt(p, x0 + 2, MF + 1, z0 + 3, EntityType.ZOMBIE_VILLAGER)
    }

    private fun caves(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        if (!Settings.caves || !Loot.rare(random, 4.0)) return
        val deep = random.nextInt(100) < 35
        val range = if (deep) -8 downTo -46 else 44 downTo 18
        var chestDone = false
        var mobDone = false
        repeat(10) {
            val x = bx + random.nextInt(16)
            val z = bz + random.nextInt(16)
            val zn = zoneAt(seed, x, z)
            if (zn != Zone.JUNGLE && zn != Zone.AERO) return@repeat
            for (y in range) {
                if (!p.isAir(x, y, z) || !p.type(x, y - 1, z).isOccluding) continue
                val below = p.type(x, y - 1, z)
                if (!chestDone) {
                    val table = when {
                        deep -> "deep"
                        below == Material.AMETHYST_BLOCK || below == Material.CALCITE || below == Material.BUDDING_AMETHYST -> "amethyst"
                        else -> "cave"
                    }
                    if (Loot.chestAt(p, random, x, y, z, table)) chestDone = true
                } else if (!mobDone && random.nextInt(100) < 40) {
                    val type = if (random.nextBoolean()) EntityType.SKELETON else EntityType.CAVE_SPIDER
                    if (Loot.mobAt(p, x, y, z, type)) mobDone = true
                }
                break
            }
        }
    }

    private fun geodes(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        if (!Settings.caves) return
        val gx0 = Math.floorDiv(bx, Geodes.G)
        val gz0 = Math.floorDiv(bz, Geodes.G)
        for (gx in gx0 - 1..gx0 + 1) for (gz in gz0 - 1..gz0 + 1) {
            val g = Geodes.inCell(seed, gen, gx, gz) ?: continue
            if (g.cx < bx || g.cx >= bx + 16 || g.cz < bz || g.cz >= bz + 16) continue
            if (!Loot.rare(random, 45.0)) continue
            for (y in g.cy + 3 downTo g.cy - g.r + 3) {
                if (p.isAir(g.cx, y, g.cz) && p.type(g.cx, y - 1, g.cz).isOccluding) {
                    Loot.chestAt(p, random, g.cx, y, g.cz, "amethyst")
                    break
                }
            }
        }
    }
}
