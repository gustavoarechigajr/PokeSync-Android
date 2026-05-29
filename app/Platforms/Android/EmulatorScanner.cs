using AndroidEnv = Android.OS.Environment;

namespace PKHeX.Android;

/// <summary>
/// Builds glob patterns pointing at the LIVE save directories of known emulators, so the embedded
/// backend reads/writes those saves in place (changes round-trip to the actual game). Ported from the
/// previous PokeSync-Android EmulatorScanner. Requires All Files Access (see StorageAccess).
/// </summary>
public static class EmulatorScanner
{
    private record Emu(string[] Dirs, string[] Extensions, string[] ExactNames);

    private static string Root => AndroidEnv.ExternalStorageDirectory?.AbsolutePath ?? "/storage/emulated/0";

    private static Emu[] Known() =>
    [
        new(   // RetroArch (GBA/GBC/GB/DS via cores)
            [$"{Root}/RetroArch/saves"],
            ["srm", "sav", "rtc"], []),
        new(   // Azahar (3DS)
            [$"{Root}/Azahar/sdmc/Nintendo 3DS", $"{Root}/Android/data/org.citra_emu.azahar/files/sdmc/Nintendo 3DS"],
            ["sav"], ["main"]),
        new(   // Eden (Switch) — live nand saves + its "Export Save Data" destination
            [$"{Root}/Android/data/dev.eden.eden_emulator/files/nand/user/save", $"{Root}/Eden data/Saves", $"{Root}/Eden Data/Saves"],
            ["bin", "sav"], ["main", "main2"]),
        new(   // DraStic (DS)
            [$"{Root}/DraStic/backup", $"{Root}/DraStic/savefile"],
            ["dsv", "sav"], []),
        new(   // melonDS / MelonDualDS (DS) — app-private saves + common custom save folders.
               // The .sav (512KB) is the game save; .ml0/.ml1 are savestates (ignored by extension).
            [$"{Root}/Android/data/me.magnum.melonds/files/saves",
             $"{Root}/Android/data/me.magnum.melondualds/files/saves",
             $"{Root}/DS Saves", $"{Root}/melonDS", $"{Root}/MelonDS"],
            ["sav", "dsv"], []),
        new([$"{Root}/MyBoy"], ["sav"], []),                  // My Boy! (GBA)
        new([$"{Root}/ClassicBoy"], ["sav", "srm"], []),      // ClassicBoy
    ];

    /// <summary>Absolute globs for every known emulator save dir (the matcher skips ones that don't exist).</summary>
    public static string[] BuildSaveGlobs()
    {
        var globs = new List<string>();
        foreach (var emu in Known())
        {
            foreach (var dir in emu.Dirs)
            {
                foreach (var ext in emu.Extensions)
                    globs.Add($"{dir}/**/*.{ext}");
                foreach (var name in emu.ExactNames)
                    globs.Add($"{dir}/**/{name}");
            }
        }
        return [.. globs];
    }

    /// <summary>Emulator directories that actually exist on this device (for showing what was detected).</summary>
    public static string[] DetectedDirs() =>
        [.. Known().SelectMany(e => e.Dirs).Where(Directory.Exists)];
}
