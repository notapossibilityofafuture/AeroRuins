package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.block.Biome
import org.bukkit.entity.EntityType
import java.util.Random


object Fauna {
    private val DRY = setOf("yellow_plains", "scrub_savanna", "dust_flats", "dry_acacia_plain", "sun_bleached_grove", "rust_meadow", "dandelion_plain")

    private fun table(c: Climate, realm: Realm): Array<EntityType> = when {
        realm == Realm.FLOWERS -> arrayOf(EntityType.BEE, EntityType.BEE, EntityType.RABBIT, EntityType.SHEEP, EntityType.SHEEP, EntityType.HORSE)
        realm == Realm.GARDEN -> arrayOf(EntityType.RABBIT, EntityType.BEE, EntityType.FROG, EntityType.SHEEP)
        realm == Realm.NEIGHBORHOOD -> arrayOf(EntityType.CAT, EntityType.RABBIT, EntityType.CHICKEN)
        c.cold -> arrayOf(EntityType.SHEEP, EntityType.COW, EntityType.FOX, EntityType.RABBIT, EntityType.WOLF)
        c.id in DRY -> arrayOf(EntityType.HORSE, EntityType.DONKEY, EntityType.SHEEP, EntityType.RABBIT, EntityType.LLAMA)
        c.fallback == Biome.JUNGLE || c.fallback == Biome.BAMBOO_JUNGLE -> arrayOf(EntityType.PARROT, EntityType.OCELOT, EntityType.CHICKEN, EntityType.PIG)
        c.fallback == Biome.SWAMP -> arrayOf(EntityType.FROG, EntityType.RABBIT, EntityType.CHICKEN)
        else -> arrayOf(EntityType.SHEEP, EntityType.SHEEP, EntityType.COW, EntityType.PIG, EntityType.CHICKEN, EntityType.RABBIT, EntityType.HORSE)
    }

    private fun herdSize(random: Random, t: EntityType): Int = when (t) {
        EntityType.SHEEP, EntityType.COW, EntityType.PIG, EntityType.CHICKEN -> 3 + random.nextInt(4)
        EntityType.BEE -> 2 + random.nextInt(3)
        EntityType.HORSE, EntityType.DONKEY, EntityType.LLAMA, EntityType.WOLF -> 1 + random.nextInt(3)
        else -> 1 + random.nextInt(2)
    }

    fun populate(seed: Long, random: Random, p: Place, bx: Int, bz: Int, gen: AeroGenerator) {
        if (!Settings.mobs || Settings.faunaDensity <= 0.0) return
        val mx = bx + 8
        val mz = bz + 8
        if (!gen.usesTerrain(seed, mx, mz)) return
        val c = ClimateMap.at(seed, mx, mz)
        val realm = Realms.at(seed, mx, mz)
        val dens = Settings.faunaDensity * (if (c.gloom) 0.3 else 1.0) * (if (realm == Realm.FLOWERS) 1.8 else 1.0)
        val herds = Math.round(random.nextDouble() * 2.4 * dens).toInt()
        val choices = table(c, realm)
        repeat(herds) {
            val type = choices[random.nextInt(choices.size)]
            val cx = bx + 2 + random.nextInt(12)
            val cz = bz + 2 + random.nextInt(12)
            for (k in 0 until herdSize(random, type)) {
                val x = cx + random.nextInt(5) - 2
                val z = cz + random.nextInt(5) - 2
                if (!gen.usesTerrain(seed, x, z)) continue
                val h = gen.heightAt(seed, x, z)
                if (h <= SEA_LEVEL + 1 || p.type(x, h, z) != Material.GRASS_BLOCK || !p.isAir(x, h + 1, z)) continue
                p.animal(x, h + 1, z, type)
            }
        }
    }
}
