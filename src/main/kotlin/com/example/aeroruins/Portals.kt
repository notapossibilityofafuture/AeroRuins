package com.example.aeroruins

import org.bukkit.Material
import org.bukkit.generator.ChunkGenerator.ChunkData
import kotlin.math.abs







object Portals {
    class Portal(val side: Int, val edge: Int, val lat: Int, val floorY: Int)

    private const val FLAT = 14
    const val MAX_LEN = 200

    private val current = ThreadLocal.withInitial { ArrayList<Portal>() }

    private fun floorOf(zone: Zone): Int = when (zone) {
        Zone.BACKROOMS -> BF
        Zone.MALL -> MF
        Zone.QUARRY -> QF
        else -> -1
    }

    
    fun prepare(seed: Long, chunkX: Int, chunkZ: Int) {
        val list = current.get()
        list.clear()
        val wx0 = chunkX * 16
        val wz0 = chunkZ * 16
        for (cx in cellOf(wx0 - MAX_LEN)..cellOf(wx0 + 15 + MAX_LEN)) {
            for (cz in cellOf(wz0 - MAX_LEN)..cellOf(wz0 + 15 + MAX_LEN)) {
                val fy = floorOf(zoneOfCell(seed, cx, cz))
                if (fy < 0) continue
                val minX = cx * CELL - CELL / 2
                val maxX = minX + CELL - 1
                val minZ = cz * CELL - CELL / 2
                val maxZ = minZ + CELL - 1
                for (side in 0..3) for (k in 0..1) {
                    val h = mix(seed, cx * 4 + side, cz * 2 + k, 90)
                    if (h % 100 >= (if (k == 0) 85 else 45)) continue
                    val lat = 30 + (h shr 8) % 164
                    when (side) {
                        0 -> list.add(Portal(0, minX, minZ + lat, fy))
                        1 -> list.add(Portal(1, maxX, minZ + lat, fy))
                        2 -> list.add(Portal(2, minZ, minX + lat, fy))
                        else -> list.add(Portal(3, maxZ, minX + lat, fy))
                    }
                }
            }
        }
    }

    



    fun carve(data: ChunkData, x: Int, z: Int, wx: Int, wz: Int, surface: Int, hallEdge: Boolean) {
        val list = current.get()
        if (list.isEmpty()) return
        for (p in list) {
            val d: Int
            val dl: Int
            when (p.side) {
                0 -> { d = p.edge - wx; dl = wz - p.lat }
                1 -> { d = wx - p.edge; dl = wz - p.lat }
                2 -> { d = p.edge - wz; dl = wx - p.lat }
                else -> { d = wz - p.edge; dl = wx - p.lat }
            }
            if (abs(dl) > 2 || d < 0 || d > MAX_LEN) continue
            if (hallEdge != (d == 0)) continue

            val k = if (d > FLAT) d - FLAT else 0
            
            val fy = p.floorY + if (k <= 120) k / 2 else 60 + (k - 120)
            var top = fy + 4
            if (!hallEdge) {
                if (fy + 1 > surface + 1) continue 
                if (top >= surface - 1) top = surface 
            }
            data.setRegion(x, fy + 1, z, x + 1, top + 1, z + 1, Material.AIR)
            data.setBlock(x, fy, z, if ((wx + wz) % 3 == 0) Material.MOSSY_COBBLESTONE else Material.COBBLESTONE)
            
            if (!hallEdge && d % 12 == 6 && dl == 0 && top == fy + 4) {
                data.setBlock(x, fy + 4, z, Material.LANTERN.createBlockData("[hanging=true]"))
            }
        }
    }
}
