package com.pokesync.android.data.local

import android.os.Environment
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class EmulatorConfig(
    val name: String,
    val saveDirs: List<String>,
    val extensions: List<String>,
    val note: String? = null,
)

data class DetectedSaveFile(
    val file: File,
    val emulator: String,
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
                    "$root/Azahar",
                    "$root/Android/data/org.citra_emu.azahar/files",
                ),
                extensions = listOf("sav", "bin"),
            ),
            EmulatorConfig(
                name = "Eden",
                saveDirs = listOf(
                    "$root/Android/data/dev.eden.eden_emulator/files/nand/user/save",
                ),
                extensions = listOf("bin", "sav"),
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
        if (depth > 6) return
        dir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> scanDir(file, config, results, depth + 1)
                file.extension.lowercase() in config.extensions && isPokemonSaveSize(file) ->
                    results.add(DetectedSaveFile(file, config.name))
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
