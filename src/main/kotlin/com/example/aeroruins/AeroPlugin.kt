package com.example.aeroruins

import org.bukkit.Bukkit
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.generator.BiomeProvider
import org.bukkit.generator.ChunkGenerator
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class AeroPlugin : JavaPlugin() {
    private lateinit var datapackInstaller: DatapackInstaller

    override fun onEnable() {
        saveDefaultConfig()
        Settings.load(config)
        datapackInstaller = DatapackInstaller(this)
        server.pluginManager.registerEvents(SpawnListener(), this)
        server.pluginManager.registerEvents(VeilListener(this), this)
        getCommand("aeroworld")?.setExecutor(this)
        getCommand("aerotp")?.setExecutor(this)
        getCommand("aeroveil")?.setExecutor(this)
        server.scheduler.runTask(this, Runnable { loadSavedBases() }) 
        server.scheduler.runTaskTimer(this, Runnable { veilParticles() }, 40L, 10L)
        logger.info("AeroRuins listo.")
    }

    
    override fun getDefaultWorldGenerator(worldName: String, id: String?): ChunkGenerator = when (id?.lowercase()) {
        "dermis" -> VeilGenerator(1)
        "hypodermis" -> VeilGenerator(2)
        "muscle" -> VeilGenerator(3)
        else -> AeroGenerator()
    }

    override fun getDefaultBiomeProvider(worldName: String, id: String?): BiomeProvider = when (id?.lowercase()) {
        "dermis" -> VeilBiomeProvider(1)
        "hypodermis" -> VeilBiomeProvider(2)
        "muscle" -> VeilBiomeProvider(3)
        else -> AeroBiomeProvider()
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean =
        when (command.name.lowercase()) {
            "aeroworld" -> createWorld(sender, args)
            "aerotp" -> teleportToZone(sender, args)
            "aeroveil" -> veilTp(sender, args)
            else -> false
        }

    

    
    fun worldFor(base: String, layer: Int, seed: Long): World? {
        val name = base + Veil.SUFFIX[layer]
        Bukkit.getWorld(name)?.let {
            ensureWorldDataFolder(name)
            return it
        }
        if (!ensureWorldDataFolder(name)) return null
        val creator = WorldCreator(name)
        if (layer == 0) {
            creator.generator(AeroGenerator()).biomeProvider(AeroBiomeProvider()).generateStructures(true)
        } else {
            creator.seed(seed).generator(VeilGenerator(layer)).biomeProvider(VeilBiomeProvider(layer)).generateStructures(false)
        }
        return creator.createWorld()?.also { ensureWorldDataFolder(name) }
    }

    private fun ensureWorldDataFolder(name: String): Boolean {
        val worldDir = File(server.worldContainer, name)
        val dataDir = File(worldDir, "data")
        if (dataDir.isDirectory) return true
        if (dataDir.mkdirs() || dataDir.isDirectory) {
            logger.info("Created missing world data directory: ${dataDir.path}")
            return true
        }
        logger.severe("Cannot create world data directory: ${dataDir.path}")
        return false
    }

    private fun applyBorder(world: World) {
        if (Settings.worldSize > 0) {
            world.worldBorder.setCenter(0.0, 0.0)
            world.worldBorder.size = Settings.worldSize.toDouble()
        }
    }

    private fun ensureVeil(base: String, surface: World) {
        if (!Settings.veil) return
        for (layer in 1..3) worldFor(base, layer, surface.seed)?.let { applyBorder(it) }
    }

    private val basesFile: File get() = File(dataFolder, "worlds.yml")

    private fun savedBases(): List<String> = YamlConfiguration.loadConfiguration(basesFile).getStringList("bases")

    private fun registerBase(base: String) {
        val list = savedBases().toMutableList()
        if (base in list) return
        list.add(base)
        dataFolder.mkdirs()
        val y = YamlConfiguration()
        y.set("bases", list)
        y.save(basesFile)
    }

    private fun loadSavedBases() {
        for (base in savedBases()) {
            val w = worldFor(base, 0, 0L) ?: continue
            applyBorder(w)
            ensureVeil(base, w)
        }
    }

    private fun createWorld(sender: CommandSender, args: Array<out String>): Boolean {
        val name = args.getOrElse(0) { "aeroworld" }
        if (datapackInstaller.enabled() && Bukkit.getWorld(name) == null) {
            sender.sendMessage("Descargando e instalando el datapack antes de crear '$name'...")
            datapackInstaller.prepare(name, {
                createWorldNow(sender, name)
            }, { error ->
                sender.sendMessage("No se pudo instalar el datapack: $error")
            })
            return true
        }
        createWorldNow(sender, name)
        return true
    }

    private fun createWorldNow(sender: CommandSender, name: String) {
        sender.sendMessage("Cargando '$name' y sus dimensiones de The Veil... (la primera vez tarda)")
        val world = worldFor(name, 0, 0L)
        if (world == null) {
            sender.sendMessage("No se pudo crear el mundo.")
            return
        }
        applyBorder(world)
        ensureVeil(name, world)
        registerBase(name)
        if (sender is Player) sender.teleport(world.spawnLocation)
    }

    private fun veilTp(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("Solo jugadores.")
            return true
        }
        val layer = Veil.layerOf(player.world)
        if (layer < 0) {
            sender.sendMessage("Este mundo no es de AeroRuins. Usa /aeroworld primero.")
            return true
        }
        val target = when (args.getOrNull(0)?.lowercase()) {
            "surface", "epidermis", "0" -> 0
            "dermis", "1" -> 1
            "hypodermis", "hipodermis", "2" -> 2
            "muscle", "musculo", "3" -> 3
            else -> -1
        }
        if (target < 0) {
            sender.sendMessage("Uso: /aeroveil <surface|dermis|hypodermis|muscle>")
            return true
        }
        val dimName = arrayOf("surface", "dermis", "hypodermis", "muscle")[target]
        if (!sender.hasPermission("aeroruins.veil.tp.$dimName")) {
            sender.sendMessage("No tienes permiso para esa dimension (aeroruins.veil.tp.$dimName).")
            return true
        }
        val dest = worldFor(Veil.baseName(player.world), target, player.world.seed)
        if (dest == null) {
            sender.sendMessage("No se pudo cargar esa dimension.")
            return true
        }
        player.teleport(dest.spawnLocation)
        player.sendActionBar(Component.text("The Veil: ${Veil.NAMES[target]}"))
        return true
    }

    
    private fun veilParticles() {
        if (!Settings.veil) return
        for (player in server.onlinePlayers) {
            val w = player.world
            val layer = Veil.layerOf(w)
            if (layer < 0) continue
            val seed = w.seed
            val px = player.location.blockX
            val pz = player.location.blockZ
            val sites = ArrayList<Site>()
            if (layer <= 2) Layers.siteNear(seed, layer, px, pz, 24)?.let { sites.add(it) }
            if (layer >= 1) Layers.siteNear(seed, layer - 1, px, pz, 24)?.let { sites.add(it) }
            for (s in sites) {
                val y = Veil.floorOf(layer, seed, s) + 1.5
                player.spawnParticle(Particle.PORTAL, Location(w, s.cx + 0.5, y, s.cz + 0.5), 25, 0.8, 1.0, 0.8, 0.2)
            }
        }
    }

    private val FEATURES = listOf("town", "city", "village", "tear", "dome", "garden", "flowers", "neighborhood", "stronghold")

    private fun nearestOf(list: List<IntArray>, px: Int, pz: Int): IntArray? =
        list.minByOrNull { val dx = (it[0] - px).toLong(); val dz = (it[1] - pz).toLong(); dx * dx + dz * dz }

    
    private fun teleportToFeature(player: Player, name: String): Boolean {
        val world = player.world
        if (world.generator !is AeroGenerator) {
            player.sendMessage("Este comando solo funciona en la superficie de AeroRuins (usa /aeroveil surface).")
            return true
        }
        val seed = world.seed
        val px = player.location.blockX
        val pz = player.location.blockZ
        var target: Location? = null
        val label: String
        when (name) {
            "town", "city" -> {
                label = "ciudad abandonada"
                nearestOf(Roads.townCenters(seed, px, pz, 8), px, pz)?.let { target = Location(world, it[0] + 0.5, world.getHighestBlockYAt(it[0], it[1]) + 1.0, it[1] + 0.5) }
            }
            "village" -> {
                label = "aldea"
                nearestOf(Village.centersAround(seed, px, pz, 10), px, pz)?.let { target = Location(world, it[0] + 0.5, world.getHighestBlockYAt(it[0], it[1]) + 2.0, it[1] + 0.5) }
            }
            "tear" -> {
                label = "desgarro"
                val sites = ArrayList<IntArray>()
                for (gx in Math.floorDiv(px, 1200) - 4..Math.floorDiv(px, 1200) + 4) for (gz in Math.floorDiv(pz, 1200) - 4..Math.floorDiv(pz, 1200) + 4) {
                    Layers.siteIn(seed, 0, gx, gz)?.let { sites.add(intArrayOf(it.cx, it.cz, it.dmax)) }
                }
                nearestOf(sites, px, pz)?.let {
                    val y = Veil.floorOf(0, seed, Site(it[0], it[1], it[2])) + 1.0
                    target = Location(world, it[0] + 4.5, y, it[1] + 0.5) 
                }
            }
            "stronghold" -> {
                label = "fortaleza (entrada)"
                val ents = Stronghold.centersAround(seed, px, pz, 4).map { val e = Stronghold.entrance(it); intArrayOf(e[0], e[1] - 4) }
                nearestOf(ents, px, pz)?.let { target = Location(world, it[0] + 0.5, world.getHighestBlockYAt(it[0], it[1]) + 1.0, it[1] + 0.5) }
            }
            "dome" -> {
                label = "esfera aero"
                nearestOf(domeCenters(seed, px, pz), px, pz)?.let { target = Location(world, it[0] + 0.5, FLOOR + 1.0, it[1] + 0.5) }
            }
            else -> {
                val realm = when (name) {
                    "garden" -> Realm.GARDEN
                    "flowers" -> Realm.FLOWERS
                    else -> Realm.NEIGHBORHOOD
                }
                label = when (realm) { Realm.GARDEN -> "jardin olvidado"; Realm.FLOWERS -> "campo de flores"; else -> "vecindario" }
                Realms.find(seed, px, pz, realm)?.let { target = Location(world, it[0] + 0.5, world.getHighestBlockYAt(it[0], it[1]) + 1.0, it[1] + 0.5) }
            }
        }
        val t = target
        if (t == null) {
            player.sendMessage("No encontre ninguna $label cerca. Muevete y vuelve a intentar.")
            return true
        }
        player.teleport(t)
        player.sendMessage("Teletransportado a: $label (${t.blockX}, ${t.blockY}, ${t.blockZ}).")
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (args.size != 1) return null
        val first = args[0].lowercase()
        return when (command.name.lowercase()) {
            "aerotp" -> (Zone.entries.map { it.name.lowercase() } + FEATURES).filter { it.startsWith(first) }
            "aeroveil" -> listOf("surface", "dermis", "hypodermis", "muscle").filter { it.startsWith(first) }
            else -> null
        }
    }

    private fun teleportToZone(sender: CommandSender, args: Array<out String>): Boolean {
        val player = sender as? Player ?: run {
            sender.sendMessage("Solo jugadores.")
            return true
        }
        val arg = (args.getOrNull(0) ?: "").lowercase()
        val zone = Zone.entries.firstOrNull { it.name.equals(arg, ignoreCase = true) }
        if ((zone != null || arg in FEATURES) && !player.hasPermission("aeroruins.tp.$arg")) {
            player.sendMessage("No tienes permiso para ese destino (aeroruins.tp.$arg).")
            return true
        }
        if (zone == null) {
            if (arg in FEATURES) return teleportToFeature(player, arg)
            sender.sendMessage("Uso: /aerotp <" + (Zone.entries.map { it.name.lowercase() } + FEATURES).joinToString("|") + ">")
            return true
        }
        val world = player.world
        if (world.generator !is AeroGenerator) {
            sender.sendMessage("Este mundo no usa AeroRuins. Usa /aeroworld primero.")
            return true
        }

        val pcx = cellOf(player.location.blockX)
        val pcz = cellOf(player.location.blockZ)
        var best: Pair<Int, Int>? = null
        var bestDist = Int.MAX_VALUE
        for (dx in -14..14) for (dz in -14..14) {
            if (zoneOfCell(world.seed, pcx + dx, pcz + dz) != zone) continue
            val dist = dx * dx + dz * dz
            if (dist < bestDist) {
                bestDist = dist
                best = Pair(pcx + dx, pcz + dz)
            }
        }
        if (best == null) {
            sender.sendMessage("No hay esa zona cerca. Muevete un poco y vuelve a intentar.")
            return true
        }

        val ox = best.first * CELL
        val oz = best.second * CELL
        var target: Location? = null
        if (zone == Zone.JUNGLE) {
            val x = ox + 8
            val z = oz + 8
            target = Location(world, x + 0.5, world.getHighestBlockYAt(x, z) + 1.0, z + 0.5)
        } else {
            
            val floorY = when (zone) {
                Zone.BACKROOMS -> BF
                Zone.MALL -> MF
                Zone.QUARRY -> QF
                else -> FLOOR
            }
            search@ for (dx in 8..40) for (dz in 8..40) {
                val x = ox + dx
                val z = oz + dz
                if (world.getBlockAt(x, floorY, z).type.isSolid &&
                    world.getBlockAt(x, floorY + 1, z).isPassable &&
                    world.getBlockAt(x, floorY + 2, z).isPassable
                ) {
                    target = Location(world, x + 0.5, floorY + 1.0, z + 0.5)
                    break@search
                }
            }
        }
        if (target == null) {
            sender.sendMessage("No encontre un lugar libre, intenta otra vez.")
            return true
        }
        player.teleport(target)
        sender.sendMessage("Teletransportado a ${zone.name.lowercase()}.")
        return true
    }
}
