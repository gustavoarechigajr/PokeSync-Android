using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Hosting;
using Microsoft.Maui.Storage;
using PKVault.Backend;

namespace PKHeX.Android;

// Hosts the real PKVault backend (Kestrel) in-process inside the MAUI Android app.
// Mirrors PKVault.Desktop/LocalWebServer.cs, but binds to a loopback port and stores all data
// under the app's writable data directory.
public static class WebServer
{
    private static readonly TaskCompletionSource<string> _readyTcs =
        new(TaskCreationOptions.RunContinuationsAsynchronously);
    private static int _started;

    // Sandbox directory where imported emulator saves are copied; covered by the default SAVE_GLOBS.
    public const string SavesFolderName = "android-saves";

    public static string SavesDirectory =>
        Path.Combine(FileSystem.AppDataDirectory, SavesFolderName);

    // Completes with the bound base URL once the backend is listening AND its data is set up.
    public static Task<string> Ready => _readyTcs.Task;

    // The bound base URL once listening (for native code, e.g. re-scan after a permission grant).
    public static string? BaseUrl { get; private set; }

    // Whether the initial save scan ran with All Files Access. If false and access is later granted,
    // the app re-scans on resume to pick up emulator saves.
    public static bool ScannedWithAccess { get; set; }

    public static async Task StartAsync()
    {
        if (Interlocked.CompareExchange(ref _started, 1, 0) != 0)
            return;

        try
        {
            // All backend storage (config, SQLite DBs, backups) lives under the app sandbox.
            SettingsService.AppDirectoryOverride = FileSystem.AppDataDirectory;

            // Imported saves (SAF fallback) land here; seed the default save glob.
            Directory.CreateDirectory(SavesDirectory);
            SettingsService.DefaultSaveGlobsOverride = [$"{SavesFolderName}/**/*"];

            // Always scan emulators' live save dirs (needs All Files Access) so changes write back to
            // the real game saves — the model the previous PokeSync version used.
#if ANDROID
            SettingsService.AutoSaveGlobs = [$"{SavesFolderName}/**/*", .. EmulatorScanner.BuildSaveGlobs()];
            ScannedWithAccess = StorageAccess.IsGranted;
#endif

            var port = Program.GetAvailablePort();
            var url = $"http://127.0.0.1:{port}";
            BaseUrl = url;

            var host = Host.CreateDefaultBuilder()
                .ConfigureWebHostDefaults(webBuilder => webBuilder
                    .UseUrls(url)
                    .UseStartup<Startup>())
                .Build();

            await host.StartAsync();

            // One-time data init (session/DB migrations for the single "default" user).
            var postRun = await Program.SetupData(host, []);
            if (postRun != null)
                await postRun();

            _readyTcs.TrySetResult(url);
        }
        catch (Exception ex)
        {
            _readyTcs.TrySetException(ex);
        }
    }
}
