package com.example.aeroruins

import org.bukkit.Material
import java.io.DataInputStream
import java.io.InputStream
import java.util.Random
import java.util.zip.GZIPInputStream

private data class SchematicBlock(val x: Int, val y: Int, val z: Int, val data: org.bukkit.block.data.BlockData)
private data class SchematicLoot(val x: Int, val y: Int, val z: Int, val table: String?)

object SchematicStronghold {
    private val structure = SchematicStructure("/stronghold.schem", 118, 26, 139)

    fun place(p: Place, random: Random, baseX: Int, baseY: Int, baseZ: Int) {
        structure.place(p, random, baseX, baseY, baseZ)
    }

    fun width(): Int = structure.width
    fun length(): Int = structure.length
}

internal class SchematicStructure(
    private val resource: String,
    val width: Int,
    private val height: Int,
    val length: Int
) {
    private val blocks: List<SchematicBlock>
    private val loot: List<SchematicLoot>

    init {
        val loaded = load()
        blocks = loaded.first
        loot = loaded.second
    }

    fun place(p: Place, random: Random, baseX: Int, baseY: Int, baseZ: Int) {
        for (block in blocks) {
            val x = baseX + block.x
            val y = baseY + block.y
            val z = baseZ + block.z
            if (p.inside(x, y, z)) p.setData(x, y, z, block.data)
        }
        for (entry in loot) {
            val x = baseX + entry.x
            val y = baseY + entry.y
            val z = baseZ + entry.z
            if (!p.inside(x, y, z)) continue
            if (entry.table != null) p.chest(x, y, z, "north", entry.table)
            else p.spawner(x, y, z, org.bukkit.entity.EntityType.SILVERFISH)
        }
    }

    private fun load(): Pair<List<SchematicBlock>, List<SchematicLoot>> {
        val root = SchematicNbt.read(
            SchematicStructure::class.java.getResourceAsStream(resource)
                ?: error("Missing $resource resource")
        )
        require((root["Width"] as Number).toInt() == width)
        require((root["Height"] as Number).toInt() == height)
        require((root["Length"] as Number).toInt() == length)
        val palette = root["Palette"] as Map<*, *>
        val paletteData = palette.entries.associate { (name, value) -> (value as Number).toInt() to blockData(name.toString()) }
        val encoded = root["BlockData"] as ByteArray
        val blockIds = decodeVarInts(encoded)
        require(blockIds.size == width * height * length) { "Invalid schematic block count for $resource" }
        val result = ArrayList<SchematicBlock>()
        for (index in blockIds.indices) {
            val state = paletteData[blockIds[index]] ?: continue
            if (state.material == Material.AIR) continue
            val x = index % width
            val z = (index / width) % length
            val y = index / (width * length)
            result += SchematicBlock(x, y, z, state)
        }
        val entities = root["BlockEntities"] as? List<*> ?: emptyList<Any>()
        val entries = entities.mapNotNull { entity ->
            val map = entity as? Map<*, *> ?: return@mapNotNull null
            val pos = map["Pos"] as? IntArray ?: return@mapNotNull null
            val id = map["Id"]?.toString()
            SchematicLoot(pos[0], pos[1], pos[2], if (id == "minecraft:chest") map["LootTable"]?.toString() else null)
        }
        return result to entries
    }

    private fun blockData(raw: String): org.bukkit.block.data.BlockData {
        val value = raw.removePrefix("minecraft:")
        val stateStart = value.indexOf('[')
        val name = if (stateStart < 0) value else value.substring(0, stateStart)
        val props = if (stateStart < 0) "" else value.substring(stateStart)
        val material = Material.matchMaterial(name.uppercase()) ?: Material.AIR
        return if (props.isEmpty()) material.createBlockData() else material.createBlockData(props)
    }

    private fun decodeVarInts(bytes: ByteArray): IntArray {
        val result = ArrayList<Int>()
        var value = 0
        var shift = 0
        for (raw in bytes) {
            value = value or ((raw.toInt() and 0x7f) shl shift)
            if (raw.toInt() and 0x80 == 0) {
                result += value
                value = 0
                shift = 0
            } else {
                shift += 7
            }
        }
        require(shift == 0) { "Truncated schematic block data" }
        return result.toIntArray()
    }
}

internal object SchematicNbt {
    fun read(input: InputStream): Map<String, Any> {
        DataInputStream(GZIPInputStream(input)).use { stream ->
            require(stream.readUnsignedByte() == 10) { "Schematic root is not a compound" }
            stream.readUTF()
            @Suppress("UNCHECKED_CAST")
            return readPayload(stream, 10) as Map<String, Any>
        }
    }

    private fun readPayload(input: DataInputStream, type: Int): Any = when (type) {
        1 -> input.readByte()
        2 -> input.readShort()
        3 -> input.readInt()
        4 -> input.readLong()
        5 -> input.readFloat()
        6 -> input.readDouble()
        7 -> ByteArray(input.readInt()).also(input::readFully)
        8 -> input.readUTF()
        9 -> {
            val childType = input.readUnsignedByte()
            List(input.readInt()) { readPayload(input, childType) }
        }
        10 -> buildMap {
            while (true) {
                val childType = input.readUnsignedByte()
                if (childType == 0) break
                put(input.readUTF(), readPayload(input, childType))
            }
        }
        11 -> IntArray(input.readInt()) { input.readInt() }
        12 -> LongArray(input.readInt()) { input.readLong() }
        else -> error("Unsupported schematic NBT tag $type")
    }
}
