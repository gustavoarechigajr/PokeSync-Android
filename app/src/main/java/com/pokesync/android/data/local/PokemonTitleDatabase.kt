package com.pokesync.android.data.local

import java.io.File

object PokemonTitleDatabase {

    // Nintendo Switch title IDs → game name
    private val SWITCH = mapOf(
        "0100ABF008968000" to "Pokémon Sword",
        "01008DB008C2C000" to "Pokémon Shield",
        "010003F003A34000" to "Pokémon: Let's Go, Pikachu!",
        "0100187003A36000" to "Pokémon: Let's Go, Eevee!",
        "010049900F546000" to "Pokémon Brilliant Diamond",
        "01009EF00DD2C000" to "Pokémon Shining Pearl",
        "01001F5010DFA000" to "Pokémon Legends: Arceus",
        "0100A3D008C5C000" to "Pokémon Scarlet",
        "01006F8002326000" to "Pokémon Violet",
        "010018F011D57000" to "Pokémon Legends: Z-A",
    )

    // 3DS title IDs → game name (USA + EUR + JPN variants)
    private val CIT = mapOf(
        // X / Y
        "0004000000055D00" to "Pokémon X",
        "0004000000055E00" to "Pokémon Y",
        "0004000000067600" to "Pokémon X",
        "000400000006B400" to "Pokémon Y",
        "0004000000030800" to "Pokémon X",
        "0004000000030900" to "Pokémon Y",
        // Omega Ruby / Alpha Sapphire
        "000400000011C400" to "Pokémon Omega Ruby",
        "000400000011C500" to "Pokémon Alpha Sapphire",
        "000400000011AE00" to "Pokémon Omega Ruby",
        "000400000011AF00" to "Pokémon Alpha Sapphire",
        "000400000011B600" to "Pokémon Omega Ruby",
        "000400000011B700" to "Pokémon Alpha Sapphire",
        // Sun / Moon
        "0004000000164800" to "Pokémon Sun",
        "0004000000175E00" to "Pokémon Moon",
        "0004000000175C00" to "Pokémon Sun",
        "0004000000175D00" to "Pokémon Moon",
        "000400000011CC00" to "Pokémon Sun",
        "000400000011CD00" to "Pokémon Moon",
        // Ultra Sun / Ultra Moon
        "00040000001B5000" to "Pokémon Ultra Sun",
        "00040000001B5100" to "Pokémon Ultra Moon",
        "00040000001B4F00" to "Pokémon Ultra Sun",
        "00040000001B5200" to "Pokémon Ultra Moon",
        "00040000001AE000" to "Pokémon Ultra Sun",
        "00040000001AE100" to "Pokémon Ultra Moon",
    )

    // ROM-name substrings for RetroArch / DraStic / My Boy! (case-insensitive)
    private val ROM_PATTERNS = listOf(
        "pokemon" to null,      // catch-all — server will identify the exact game
        "pocket monsters" to null,
    )

    fun findByTitleId(id: String): String? =
        SWITCH[id.uppercase()] ?: CIT[id.uppercase()]

    fun isPokemonTitleId(id: String): Boolean = findByTitleId(id) != null

    /**
     * Walks the file's path components looking for a 16-hex-char title ID
     * that matches a known Pokémon game.
     */
    fun resolveFromPath(file: File): TitleMatch? {
        val components = file.absolutePath.split("/")
        for (segment in components) {
            if (segment.length == 16 && segment.all { it.isHexDigit() }) {
                val name = findByTitleId(segment) ?: continue
                return TitleMatch(segment.uppercase(), name)
            }
        }
        return null
    }

    /**
     * Walks path components looking for two consecutive 8-hex-char segments that together
     * form a known 16-char Pokémon title ID (Azahar 3DS layout: .../00040000/0011c400/...).
     */
    fun resolveFromSplitPath(file: File): TitleMatch? {
        val components = file.absolutePath.split("/")
        for (i in 0 until components.size - 1) {
            val a = components[i]
            val b = components[i + 1]
            if (a.length == 8 && b.length == 8 &&
                a.all { it.isHexDigit() } && b.all { it.isHexDigit() }
            ) {
                val combined = (a + b).uppercase()
                val name = findByTitleId(combined) ?: continue
                return TitleMatch(combined, name)
            }
        }
        return null
    }

    /**
     * Tries to extract a title ID from filenames like "000400000011C400-514343050A01".
     */
    fun resolveFromFilename(filename: String): TitleMatch? {
        val part = filename.substringBefore("-")
        if (part.length == 16 && part.all { it.isHexDigit() }) {
            val name = findByTitleId(part) ?: return null
            return TitleMatch(part.uppercase(), name)
        }
        return null
    }

    /** Returns true if the filename looks like it comes from a Pokémon ROM save. */
    fun looksLikePokemonRomSave(filename: String): Boolean =
        ROM_PATTERNS.any { (pattern, _) ->
            filename.contains(pattern, ignoreCase = true)
        }

    data class TitleMatch(val titleId: String, val gameName: String)

    private fun Char.isHexDigit() = this.isDigit() || this.uppercaseChar() in 'A'..'F'
}
