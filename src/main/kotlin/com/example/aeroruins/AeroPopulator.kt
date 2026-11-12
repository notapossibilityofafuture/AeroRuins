package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.generator.BlockPopulator
import org.bukkit.generator.LimitedRegion
import org.bukkit.generator.WorldInfo
import java.util.Random

class AeroPopulator(private val gen: AeroGenerator) : BlockPopulator() {

    override fun populate(info: WorldInfo, random: Random, cx: Int, cz: Int, region: LimitedRegion) {
        val p = Place(region)
        val seed = info.seed
        val bx = cx * 16
        val bz = cz * 16

        groundCover(seed, random, p, bx, bz)
        plantTrees(seed, random, p, bx, bz)

        when (zoneAt(seed, bx + 8, bz + 8)) {
            Zone.JUNGLE -> {
                val realm = Realms.at(seed, bx + 8, bz + 8)
                if (Suburb.fieldAt(seed, bx + 8, bz + 8)) {
                    
                    Suburb.ceiling(seed, p, bx, bz)
                    if (random.nextInt(100) < 55) Suburb.house(seed, random, p, bx, bz, gen, decay = 15)
                } else if (realm == Realm.NEIGHBORHOOD) {
                    
                    Roads.decorate(seed, random, p, bx, bz, gen)
                    if (random.nextInt(100) < 65) Neighborhood.lot(seed, random, p, bx, bz, gen)
                } else if (realm == Realm.GARDEN) {
                    Garden.maybe(seed, random, p, bx, bz, gen) 
                    if (random.nextInt(100) < 4) Ruins.maybe(seed, random, p, bx, bz, gen, false)
                } else if (realm == Realm.FLOWERS) {
                    if (random.nextInt(100) < 3) Garden.maybe(seed, random, p, bx, bz, gen) 
                } else if (Roads.townFactor(seed, bx + 8, bz + 8) > 0.25) {
                    
                    Roads.decorate(seed, random, p, bx, bz, gen)
                    if (random.nextInt(100) < 55) City.lot(seed, random, p, bx, bz, gen)
                } else {
                    Roads.decorate(seed, random, p, bx, bz, gen)
                    Village.populate(seed, random, p, bx, bz, gen) 
                    val gloom = ClimateMap.at(seed, bx + 8, bz + 8).gloom
                    val ruin = Ruins.maybe(seed, random, p, bx, bz, gen, gloom)
                    val roll = if (ruin) 100 else random.nextInt(1000)
                    
                    val m = if (gloom) 2 else 1
                    when {
                        roll < 4 * m -> Tokyo3.build(seed, random, p, bx, bz, gen)
                        roll < 8 * m -> Cathedral.build(seed, random, p, bx, bz, gen)
                        roll < 8 * m + 25 * m -> Towers.build(seed, random, p, bx, bz, gen)
                        roll < 8 * m + 70 * m -> Apartments.build(seed, random, p, bx, bz, gen)
                        roll < 8 * m + 90 * m -> Frames.build(seed, random, p, bx, bz, gen)
                        roll < 8 * m + 100 * m -> Salon.build(seed, random, p, bx, bz, gen)
                        else -> Unit
                    }
                }
            }
            Zone.AERO -> DomeFlora.populate(seed, random, p, bx, bz)
            else -> Unit 
        }

        Classics.populate(seed, random, p, bx, bz, gen) 
        Fauna.populate(seed, random, p, bx, bz, gen)   
        Scatter.populate(seed, random, p, bx, bz, gen) 
        Ores.populate(random, p, bx, bz, info.minHeight, ClimateMap.at(seed, bx + 8, bz + 8))
        Underground.populate(seed, random, p, bx, bz, gen)
    }

    
    private fun plantTrees(seed: Long, random: Random, p: Place, bx: Int, bz: Int) {
        if (!gen.usesTerrain(seed, bx + 8, bz + 8)) return
        val c0 = ClimateMap.at(seed, bx + 8, bz + 8)
        val n = (c0.density * (0.4 + random.nextDouble() * 1.2)).toInt()
        repeat(n) {
            val x = bx + 1 + random.nextInt(14)
            val z = bz + 1 + random.nextInt(14)
            if (!gen.usesTerrain(seed, x, z)) return@repeat
            val h = gen.heightAt(seed, x, z)
            if (h < SEA_LEVEL + 2 || p.type(x, h, z) != Material.GRASS_BLOCK || !p.isAir(x, h + 1, z)) return@repeat
            val c = ClimateMap.at(seed, x, z)
            if (c.bare) {
                Trees.bigTree(p, random, x, h + 1, z, c, 6 + random.nextInt(5), 3)
                return@repeat
            }
            val type = if (random.nextInt(100) < 70) c.tree else c.alt
            if (!p.tree(x, h + 1, z, random, type) && !Place.treesWork) {
                Trees.bigTree(p, random, x, h + 1, z, c, 7 + random.nextInt(5), 3) 
            }
        }
    }

    private val TALL = arrayOf(Material.LILAC, Material.ROSE_BUSH, Material.PEONY, Material.SUNFLOWER)

    private fun flower(p: Place, random: Random, x: Int, y: Int, z: Int, c: Climate) {
        if (c.flora >= 40 && random.nextInt(100) < 22 && p.isAir(x, y + 1, z)) { 
            val m = TALL[random.nextInt(TALL.size)]
            p.put(x, y, z, m, "[half=lower]")
            p.put(x, y + 1, z, m, "[half=upper]")
        } else {
            p.set(x, y, z, c.flowers[random.nextInt(c.flowers.size)])
        }
    }

    private fun groundCover(seed: Long, random: Random, p: Place, bx: Int, bz: Int) {
        val realm = Realms.at(seed, bx + 8, bz + 8)
        val attempts = when (realm) {
            Realm.FLOWERS -> 190 
            Realm.GARDEN -> 70
            else -> 28
        }
        repeat(attempts) {
            val x = bx + random.nextInt(16)
            val z = bz + random.nextInt(16)
            if (!gen.isOpenAir(seed, x, z)) return@repeat
            val h = gen.surfaceY(seed, x, z)

            if (gen.usesTerrain(seed, x, z) && h < SEA_LEVEL) {
                if (random.nextInt(100) < 4 && p.type(x, SEA_LEVEL, z) == Material.WATER && p.isAir(x, SEA_LEVEL + 1, z)) {
                    p.set(x, SEA_LEVEL + 1, z, Material.LILY_PAD)
                }
                return@repeat
            }
            if (h > SEA_LEVEL + 1 && p.type(x, h, z) == Material.GRASS_BLOCK && p.isAir(x, h + 1, z)) {
                val c = ClimateMap.at(seed, x, z)
                val r = random.nextInt(100)
                when {
                    r < c.flora -> flower(p, random, x, h + 1, z, c)
                    r < c.flora + 45 -> p.set(x, h + 1, z, Material.SHORT_GRASS)
                    r < c.flora + 55 -> p.set(x, h + 1, z, Material.FERN)
                    r < c.flora + 60 -> bush(p, random, x, h + 1, z)
                    else -> Unit
                }
            }
        }
    }

    private fun bush(p: Place, random: Random, x: Int, y: Int, z: Int) {
        p.leaf(x, y, z)
        if (random.nextInt(100) < 70) p.leaf(x + 1, y, z)
        if (random.nextInt(100) < 70) p.leaf(x - 1, y, z)
        if (random.nextInt(100) < 70) p.leaf(x, y, z + 1)
        if (random.nextInt(100) < 70) p.leaf(x, y, z - 1)
        p.leaf(x, y + 1, z)
    }
}
