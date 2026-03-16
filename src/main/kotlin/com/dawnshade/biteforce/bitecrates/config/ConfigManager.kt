package com.dawnshade.biteforce.bitecrates.config

import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.feature.key.Key
import com.dawnshade.biteforce.bitecrates.feature.opening.inventory.InventoryOpeningAnimation
import com.dawnshade.biteforce.bitecrates.feature.opening.world.WorldOpeningAnimation
import com.dawnshade.biteforce.bitecrates.feature.particle.ParticleAnimationOptions
import com.dawnshade.biteforce.bitecrates.data.previews.Preview
import com.dawnshade.biteforce.bitecrates.util.Utils
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.stream.Collectors

object ConfigManager {
    private var assetPackage = "assets/${BiteCrates.MOD_ID}"

    lateinit var CONFIG: BiteCratesConfig
    lateinit var CRATES: MutableMap<String, Crate>
    lateinit var KEYS: MutableMap<String, Key>
    lateinit var OPENINGS_INVENTORY: MutableMap<String, InventoryOpeningAnimation>
    lateinit var OPENINGS_WORLD: MutableMap<String, WorldOpeningAnimation>
    lateinit var PARTICLES: MutableMap<String, ParticleAnimationOptions>
    lateinit var PREVIEW: MutableMap<String, Preview>
    lateinit var KEYS_MENU: KeysMenu

    fun load() {
        
        copyDefaults()

        
        CONFIG = loadFile("config.json", BiteCratesConfig())
        loadCrates()
        loadKeys()
        loadInventoryOpenings()
        loadWorldOpenings()
        loadParticles()
        loadPreviews()
        KEYS_MENU = loadFile("keys.json", KeysMenu(), "menus")
    }

    private fun copyDefaults() {
        val classLoader = BiteCrates::class.java.classLoader

        BiteCrates.INSTANCE.configDir.mkdirs()

        attemptDefaultFileCopy(classLoader, "config.json")
        attemptDefaultDirectoryCopy(classLoader, "crates")
        attemptDefaultDirectoryCopy(classLoader, "keys")
        attemptDefaultDirectoryCopy(classLoader, "openings/inventory")
        attemptDefaultDirectoryCopy(classLoader, "openings/world")
        attemptDefaultDirectoryCopy(classLoader, "particles")
        attemptDefaultDirectoryCopy(classLoader, "previews")
        attemptDefaultFileCopy(classLoader, "menus/keys.json")
    }

    private fun loadCrates() {
        CRATES = mutableMapOf()

        val dir = BiteCrates.INSTANCE.configDir.resolve("crates")
        if (dir.exists() && dir.isDirectory) {
            val files = Files.walk(dir.toPath())
                .filter { path: Path -> Files.isRegularFile(path) }
                .map { it.toFile() }
                .collect(Collectors.toList())
            if (files != null) {
                BiteCrates.LOGGER.info("Found ${files.size} crate files: ${files.map { it.name }}")
                val enabledFiles = mutableListOf<String>()
                for (file in files) {
                    val fileName = file.name
                    if (file.isFile && fileName.contains(".json")) {
                        val id = fileName.substring(0, fileName.lastIndexOf(".json"))
                        val jsonReader = JsonReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8))
                        try {
                            val config = BiteCrates.INSTANCE.gsonPretty.fromJson(JsonParser.parseReader(jsonReader), Crate::class.java)
                            if (config.enabled) {
                                config.id = id
                                CRATES[id] = config
                                enabledFiles.add(fileName)
                            } else {
                                Utils.printError("Crate $fileName is disabled, skipping...")
                            }
                        } catch (ex: Exception) {
                            Utils.printError("Error while trying to parse the crate $fileName!")
                            BiteCrates.LOGGER.error("Failed to parse crate file {}", fileName, ex)
                        }
                    } else {
                        Utils.printError("File $fileName is either not a file or is not a .json file!")
                    }
                }
                Utils.printInfo("Successfully read and loaded the following enabled crate files: $enabledFiles")
            }
        } else {
            Utils.printError("The 'crate' directory either does not exist or is not a directory!")
        }
    }

    private fun loadKeys() {
        KEYS = mutableMapOf()

        val dir = BiteCrates.INSTANCE.configDir.resolve("keys")
        if (dir.exists() && dir.isDirectory) {
            val files = Files.walk(dir.toPath())
                .filter { path: Path -> Files.isRegularFile(path) }
                .map { it.toFile() }
                .collect(Collectors.toList())
            if (files != null) {
                BiteCrates.LOGGER.info("Found ${files.size} key files: ${files.map { it.name }}")
                val enabledFiles = mutableListOf<String>()
                for (file in files) {
                    val fileName = file.name
                    if (file.isFile && fileName.contains(".json")) {
                        val id = fileName.substring(0, fileName.lastIndexOf(".json"))
                        val jsonReader = JsonReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8))
                        try {
                            val config = BiteCrates.INSTANCE.gsonPretty.fromJson(JsonParser.parseReader(jsonReader), Key::class.java)
                            if (config.enabled) {
                                config.id = id
                                KEYS[id] = config
                                enabledFiles.add(fileName)
                            } else {
                                Utils.printError("Key $fileName is disabled, skipping...")
                            }
                        } catch (ex: Exception) {
                            Utils.printError("Error while trying to parse the key $fileName!")
                            BiteCrates.LOGGER.error("Failed to parse key file {}", fileName, ex)
                        }
                    } else {
                        Utils.printError("File $fileName is either not a file or is not a .json file!")
                    }
                }
                Utils.printInfo("Successfully read and loaded the following enabled key files: $enabledFiles")
            }
        } else {
            Utils.printError("The 'keys' directory either does not exist or is not a directory!")
        }
    }

    private fun loadInventoryOpenings() {
        OPENINGS_INVENTORY = mutableMapOf()

        val dir = BiteCrates.INSTANCE.configDir.resolve("openings/inventory")
        if (dir.exists() && dir.isDirectory) {
            val files = Files.walk(dir.toPath())
                .filter { path: Path -> Files.isRegularFile(path) }
                .map { it.toFile() }
                .collect(Collectors.toList())
            if (files != null) {
                BiteCrates.LOGGER.info("Found ${files.size} inventory opening files: ${files.map { it.name }}")
                val enabledFiles = mutableListOf<String>()
                for (file in files) {
                    val fileName = file.name
                    if (file.isFile && fileName.contains(".json")) {
                        val id = fileName.substring(0, fileName.lastIndexOf(".json"))
                        val jsonReader = JsonReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8))
                        try {
                            val config = BiteCrates.INSTANCE.gsonPretty.fromJson(
                                JsonParser.parseReader(jsonReader),
                                InventoryOpeningAnimation::class.java
                            )
                            OPENINGS_INVENTORY[id] = config
                            enabledFiles.add(fileName)
                        } catch (ex: Exception) {
                            Utils.printError("Error while trying to parse the inventory opening $fileName!")
                            BiteCrates.LOGGER.error("Failed to parse inventory opening file {}", fileName, ex)
                        }
                    } else {
                        Utils.printError("File $fileName is either not a file or is not a .json file!")
                    }
                }
                Utils.printInfo("Successfully read and loaded the following enabled inventory opening files: $enabledFiles")
            }
        } else {
            Utils.printError("The 'openings/inventory' directory either does not exist or is not a directory!")
        }
    }

    private fun loadWorldOpenings() {
        OPENINGS_WORLD = mutableMapOf()

        val dir = BiteCrates.INSTANCE.configDir.resolve("openings/world")
        if (dir.exists() && dir.isDirectory) {
            val files = Files.walk(dir.toPath())
                .filter { path: Path -> Files.isRegularFile(path) }
                .map { it.toFile() }
                .collect(Collectors.toList())
            if (files != null) {
                BiteCrates.LOGGER.info("Found ${files.size} world opening files: ${files.map { it.name }}")
                val enabledFiles = mutableListOf<String>()
                for (file in files) {
                    val fileName = file.name
                    if (file.isFile && fileName.contains(".json")) {
                        val id = fileName.substring(0, fileName.lastIndexOf(".json"))
                        val jsonReader = JsonReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8))
                        try {
                            val config = BiteCrates.INSTANCE.gsonPretty.fromJson(JsonParser.parseReader(jsonReader), WorldOpeningAnimation::class.java)
                            OPENINGS_WORLD[id] = config
                            enabledFiles.add(fileName)
                        } catch (ex: Exception) {
                            Utils.printError("Error while trying to parse the world opening $fileName!")
                            BiteCrates.LOGGER.error("Failed to parse world opening file {}", fileName, ex)
                        }
                    } else {
                        Utils.printError("File $fileName is either not a file or is not a .json file!")
                    }
                }
                Utils.printInfo("Successfully read and loaded the following enabled world opening files: $enabledFiles")
            }
        } else {
            Utils.printError("The 'openings/world' directory either does not exist or is not a directory!")
        }
    }

    private fun loadParticles() {
        PARTICLES = mutableMapOf()

        val dir = BiteCrates.INSTANCE.configDir.resolve("particles")
        if (dir.exists() && dir.isDirectory) {
            val files = Files.walk(dir.toPath())
                .filter { path: Path -> Files.isRegularFile(path) }
                .map { it.toFile() }
                .collect(Collectors.toList())
            if (files != null) {
                BiteCrates.LOGGER.info("Found ${files.size} particle files: ${files.map { it.name }}")
                val enabledFiles = mutableListOf<String>()
                for (file in files) {
                    val fileName = file.name
                    if (file.isFile && fileName.contains(".json")) {
                        val id = fileName.substring(0, fileName.lastIndexOf(".json"))
                        val jsonReader = JsonReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8))
                        try {
                            val config = BiteCrates.INSTANCE.gsonPretty.fromJson(JsonParser.parseReader(jsonReader), ParticleAnimationOptions::class.java)
                            PARTICLES[id] = config
                            enabledFiles.add(fileName)
                        } catch (ex: Exception) {
                            Utils.printError("Error while trying to parse the particle $fileName!")
                            BiteCrates.LOGGER.error("Failed to parse particle file {}", fileName, ex)
                        }
                    } else {
                        Utils.printError("File $fileName is either not a file or is not a .json file!")
                    }
                }
                Utils.printInfo("Successfully read and loaded the following enabled particle files: $enabledFiles")
            }
        } else {
            Utils.printError("The 'particles' directory either does not exist or is not a directory!")
        }
    }

    private fun loadPreviews() {
        PREVIEW = mutableMapOf()

        val dir = BiteCrates.INSTANCE.configDir.resolve("previews")
        if (dir.exists() && dir.isDirectory) {
            val files = Files.walk(dir.toPath())
                .filter { path: Path -> Files.isRegularFile(path) }
                .map { it.toFile() }
                .collect(Collectors.toList())
            if (files != null) {
                BiteCrates.LOGGER.info("Found ${files.size} preview files: ${files.map { it.name }}")
                val enabledFiles = mutableListOf<String>()
                for (file in files) {
                    val fileName = file.name
                    if (file.isFile && fileName.contains(".json")) {
                        val id = fileName.substring(0, fileName.lastIndexOf(".json"))
                        val jsonReader = JsonReader(InputStreamReader(FileInputStream(file), Charsets.UTF_8))
                        try {
                            val config = BiteCrates.INSTANCE.gsonPretty.fromJson(JsonParser.parseReader(jsonReader), Preview::class.java)
                            PREVIEW[id] = config
                            enabledFiles.add(fileName)
                        } catch (ex: Exception) {
                            Utils.printError("Error while trying to parse the preview $fileName!")
                            BiteCrates.LOGGER.error("Failed to parse preview file {}", fileName, ex)
                        }
                    } else {
                        Utils.printError("File $fileName is either not a file or is not a .json file!")
                    }
                }
                Utils.printInfo("Successfully read and loaded the following enabled preview files: $enabledFiles")
            }
        } else {
            Utils.printError("The 'previews' directory either does not exist or is not a directory!")
        }
    }

    fun <T : Any> loadFile(filename: String, default: T, path: String = "", create: Boolean = false): T {
        var dir = BiteCrates.INSTANCE.configDir
        if (path.isNotEmpty()) {
            dir = dir.resolve(path)
        }
        val file = File(dir, filename)
        var value: T = default
        try {
            Files.createDirectories(BiteCrates.INSTANCE.configDir.toPath())
            if (file.exists()) {
                FileReader(file).use { reader ->
                    val jsonReader = JsonReader(reader)
                    value = BiteCrates.INSTANCE.gsonPretty.fromJson(jsonReader, default::class.java)
                }
            } else if (create) {
                Files.createFile(file.toPath())
                FileWriter(file).use { fileWriter ->
                    fileWriter.write(BiteCrates.INSTANCE.gsonPretty.toJson(default))
                    fileWriter.flush()
                }
            }
        } catch (e: IOException) {
            BiteCrates.LOGGER.error("Failed to load config file {}", filename, e)
        }
        return value
    }

    fun <T> saveFile(filename: String, `object`: T): Boolean {
        val dir = BiteCrates.INSTANCE.configDir
        val file = File(dir, filename)
        try {
            FileWriter(file).use { fileWriter ->
                fileWriter.write(BiteCrates.INSTANCE.gsonPretty.toJson(`object`))
                fileWriter.flush()
            }
        } catch (e: Exception) {
            BiteCrates.LOGGER.error("Failed to save config file {}", filename, e)
            return false
        }
        return true
    }

    private fun attemptDefaultFileCopy(classLoader: ClassLoader, fileName: String) {
        val file = BiteCrates.INSTANCE.configDir.resolve(fileName)
        if (!file.exists()) {
            file.mkdirs()
            try {
                val stream = classLoader.getResourceAsStream("${assetPackage}/$fileName")
                    ?: throw NullPointerException("File not found $fileName")

                Files.copy(stream, file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                Utils.printError("Failed to copy the default file '$fileName': $e")
            }
        }
    }

    private fun attemptDefaultDirectoryCopy(classLoader: ClassLoader, directoryName: String) {
        val directory = BiteCrates.INSTANCE.configDir.resolve(directoryName)
        if (!directory.exists()) {
            directory.mkdirs()
            try {
                val sourceUrl = classLoader.getResource("${assetPackage}/$directoryName")
                    ?: throw NullPointerException("Directory not found $directoryName")
                val sourcePath = Paths.get(sourceUrl.toURI())

                Files.walk(sourcePath).use { stream ->
                    stream.forEach { sourceFile ->
                        val destinationFile = directory.resolve(sourcePath.relativize(sourceFile).toString())
                        if (Files.isDirectory(sourceFile)) {
                            
                            destinationFile.mkdirs()
                        } else {
                            
                            Files.copy(sourceFile, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                }
            } catch (e: Exception) {
                Utils.printError("Failed to copy the default directory '$directoryName': " + e.message)
            }
        }
    }
}
