package com.pokesync.android.data.local

import android.os.Environment
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class EmulatorConfig(
    val name: String,
    val saveDirs: List<String>,
    val extensions: List<String>,
    val exactNames: List<String> = emptyList(), // extensionless files to match by name
    val note: String? = null,
)

data class DetectedSaveFile(
    val file: File,
    val emulator: String,
    val gameName: String? = null,
)

@Singleton
class EmulatorScanner @Inject constructor() {

    companion object {
        private val root get() = Environment.getExternalStorageDirectory().absolutePath

        val KNOWN_EMULATORS = listOf(
            EmulatorConfig(
                name = "RetroArch",
                saveDirs = listOf("$root/RetroArch/saves"),
                extensions = listOf("srm", "sav", "rtc"),
            ),
            EmulatorConfig(
                name = "Azahar",
                saveDirs = listOf(
                    "$root/Azahar/sdmc/Nintendo 3DS",
                    "$root/Android/data/org.citra_emu.azahar/files/sdmc/Nintendo 3DS",
                ),
                extensions = listOf("sav"),
                exactNames = listOf("main"), // 3DS cartridge save — no extension
            ),
            EmulatorConfig(
                name = "Eden",
                saveDirs = listOf(
                    "$root/Android/data/dev.eden.eden_emulator/files/nand/user/save",
                    // Eden "Export Save Data" destination
                    "$root/Eden Data/Saves",
                    "$root/Eden Data/saves",
                ),
                extensions = listOf("bin", "sav"),
                // Switch saves stored by Eden are extensionless files named "main" or "main2"
                exactNames = listOf("main", "main2"),
                note = "Requires All Files Access permission",
            ),
            EmulatorConfig(
                name = "DraStic",
                saveDirs = listOf(
                    "$root/DraStic/backup",
                    "$root/DraStic/savefile",
                ),
                extensions = listOf("dsv", "sav"),
            ),
            EmulatorConfig(
                name = "My Boy!",
                saveDirs = listOf("$root/MyBoy"),
                extensions = listOf("sav"),
            ),
            EmulatorConfig(
                name = "ClassicBoy",
                saveDirs = listOf("$root/ClassicBoy"),
                extensions = listOf("sav", "srm"),
            ),
        )
    }

    /**
     * Scans all known emulator directories and returns every save-like file found.
     * The server (PKHeX) is the final authority on whether a file is a Pokemon save.
     */
    fun scanAll(): List<DetectedSaveFile> {
        val results = mutableListOf<DetectedSaveFile>()
        for (config in KNOWN_EMULATORS) {
            for (dirPath in config.saveDirs) {
                val dir = File(dirPath)
                if (!dir.exists() || !dir.isDirectory) continue
                scanDir(dir, config, results, depth = 0)
            }
        }
        return results
    }

    fun scanEmulator(emulatorName: String): List<DetectedSaveFile> {
        val config = KNOWN_EMULATORS.find { it.name == emulatorName } ?: return emptyList()
        val results = mutableListOf<DetectedSaveFile>()
        for (dirPath in config.saveDirs) {
            val dir = File(dirPath)
            if (dir.exists() && dir.isDirectory) scanDir(dir, config, results, depth = 0)
        }
        return results
    }

    /** Returns which emulator directories actually exist on this device. */
    fun installedEmulators(): List<EmulatorConfig> =
        KNOWN_EMULATORS.filter { config ->
            config.saveDirs.any { File(it).exists() }
        }

    private fun scanDir(
        dir: File,
        config: EmulatorConfig,
        results: MutableList<DetectedSaveFile>,
        depth: Int,
    ) {
        if (depth > 8) return
        dir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> scanDir(file, config, results, depth + 1)
                (file.extension.lowercase() in config.extensions ||
                    file.name in config.exactNames) && isPokemonSaveSize(file) -> {
                    val detected = tryResolvePokemon(file, config) ?: return@forEach
                    results.add(detected)
                }
            }
        }
    }

    private fun tryResolvePokemon(file: File, config: EmulatorConfig): DetectedSaveFile? {
        return when (config.name) {
            "Eden" -> {
                // Eden path: .../nand/user/save/{userId}/{accountId32}/{titleId16}/{saveType}/file
                // Require a matching Pokémon title ID so non-Pokémon games don't clutter the list.
                val match = PokemonTitleDatabase.resolveFromPath(file) ?: return null
                DetectedSaveFile(file, config.name, match.gameName)
            }
            "Azahar" -> {
                // Azahar path: .../title/00040000/0011c400/data/00000001/main
                // titleId is split across two consecutive 8-char hex path segments
                val match = PokemonTitleDatabase.resolveFromSplitPath(file)
                    ?: PokemonTitleDatabase.resolveFromPath(file)
                    ?: return null
                DetectedSaveFile(file, config.name, match.gameName)
            }
            else -> {
                DetectedSaveFile(file, config.name, gameName = null)
            }
        }
    }

    /**
     * Quick size-based pre-filter to avoid uploading obviously wrong files.
     * PKHeX is the real filter; this just skips 0-byte files and huge ISOs.
     */
    private fun isPokemonSaveSize(file: File): Boolean {
        val size = file.length()
        return size in 4_096..32_000_000
    }
}
