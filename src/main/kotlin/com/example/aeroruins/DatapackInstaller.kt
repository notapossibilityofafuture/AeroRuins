package com.example.aeroruins

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.ZipFile

class DatapackInstaller(private val plugin: AeroPlugin) {
    private val cacheFile = File(plugin.dataFolder, "aeroruins-datapack.zip")

    fun enabled(): Boolean = Settings.datapackAutoDownload && Settings.datapackUrl.isNotBlank()

    fun prepare(worldName: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            try {
                val zip = download()
                installBeforeWorldCreation(worldName, zip)
                plugin.server.scheduler.runTask(plugin, Runnable { onSuccess() })
            } catch (error: Exception) {
                plugin.server.scheduler.runTask(plugin, Runnable {
                    onFailure(error.message ?: "No se pudo instalar el datapack.")
                })
            }
        })
    }

    private fun download(): File {
        plugin.dataFolder.mkdirs()
        if (!cacheFile.isFile || cacheFile.length() == 0L) {
            val uri = URI(Settings.datapackUrl)
            require(uri.scheme.equals("https", true)) { "La URL del datapack debe usar HTTPS." }
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = Settings.datapackTimeoutSeconds * 1000
            connection.readTimeout = Settings.datapackTimeoutSeconds * 1000
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            connection.connect()
            require(connection.responseCode in 200..299) {
                "El servidor devolvio HTTP ${connection.responseCode}."
            }
            val maxBytes = Settings.datapackMaxSizeMb.toLong() * 1024L * 1024L
            require(connection.contentLengthLong <= 0L || connection.contentLengthLong <= maxBytes) {
                "El datapack supera el limite configurado de ${Settings.datapackMaxSizeMb} MB."
            }
            val temporary = File(plugin.dataFolder, "aeroruins-datapack.download")
            connection.inputStream.use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= maxBytes) {
                            "El datapack supera el limite configurado de ${Settings.datapackMaxSizeMb} MB."
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
            validateZip(temporary)
            if (!temporary.renameTo(cacheFile)) {
                temporary.copyTo(cacheFile, overwrite = true)
                temporary.delete()
            }
            connection.disconnect()
        }
        validateZip(cacheFile)
        return cacheFile
    }

    private fun validateZip(file: File) {
        ZipFile(file).use { zip ->
            require(zip.getEntry("pack.mcmeta") != null) {
                "El archivo descargado no contiene pack.mcmeta en la raiz."
            }
        }
    }

    private fun installBeforeWorldCreation(worldName: String, zip: File) {
        require(worldName.matches(Regex("[A-Za-z0-9._-]+"))) {
            "El nombre del mundo contiene caracteres no permitidos."
        }
        val datapacks = File(plugin.server.worldContainer, "$worldName/datapacks")
        require(datapacks.mkdirs() || datapacks.isDirectory) {
            "No se pudo crear la carpeta de datapacks del mundo."
        }
        val destination = File(datapacks, Settings.datapackFileName)
        zip.copyTo(destination, overwrite = true)
    }
}
