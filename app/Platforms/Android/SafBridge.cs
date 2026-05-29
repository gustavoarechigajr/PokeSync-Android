using System.Text.Json;
using Android.Content;
using Android.Database;
using Android.Provider;
using Microsoft.Maui.ApplicationModel;
using Microsoft.Maui.Storage;
using AndroidUri = Android.Net.Uri;

namespace PKHeX.Android;

/// <summary>
/// Storage Access Framework bridge for importing emulator saves AND writing changes back to the
/// exact file the user picked (so the emulator/game sees them).
///
/// Flow: pick a save with persistable READ+WRITE permission → copy its bytes into the app sandbox
/// (android-saves/) so the embedded backend can read/parse it, and remember the picked URI keyed by
/// the sandbox path. After the user commits changes (backend writes the modified save into the
/// sandbox file), WriteBackAll() copies each sandbox save back out to its remembered URI.
/// </summary>
public static class SafBridge
{
    public const int PickRequestCode = 0x5A7E; // "SAvE"
    private const string MapKey = "saf_uri_map"; // Preferences: { sandboxRelPath: contentUri }

    private static TaskCompletionSource<AndroidUri?>? _pickTcs;

    private static ContentResolver Resolver => Platform.CurrentActivity!.ContentResolver!;

    // ---- URI map persistence ----
    private static Dictionary<string, string> LoadMap()
    {
        var json = Preferences.Get(MapKey, "");
        if (string.IsNullOrEmpty(json)) return new();
        try { return JsonSerializer.Deserialize<Dictionary<string, string>>(json) ?? new(); }
        catch { return new(); }
    }

    private static void SaveMap(Dictionary<string, string> map) =>
        Preferences.Set(MapKey, JsonSerializer.Serialize(map));

    // ---- Picking ----
    public static Task<AndroidUri?> PickSaveAsync()
    {
        _pickTcs = new();
        var intent = new Intent(Intent.ActionOpenDocument);
        intent.AddCategory(Intent.CategoryOpenable!);
        intent.SetType("*/*");
        intent.AddFlags(ActivityFlags.GrantReadUriPermission | ActivityFlags.GrantWriteUriPermission
            | ActivityFlags.GrantPersistableUriPermission);
        Platform.CurrentActivity!.StartActivityForResult(intent, PickRequestCode);
        return _pickTcs.Task;
    }

    /// <summary>Called from MainActivity.OnActivityResult.</summary>
    public static void OnPickResult(AndroidUri? uri)
    {
        if (uri != null)
        {
            try
            {
                Resolver.TakePersistableUriPermission(uri,
                    ActivityFlags.GrantReadUriPermission | ActivityFlags.GrantWriteUriPermission);
            }
            catch { /* some providers don't support persistable perms; write-back may be unavailable */ }
        }
        _pickTcs?.TrySetResult(uri);
    }

    // ---- Read / write ----
    public static byte[] ReadAll(AndroidUri uri)
    {
        using var stream = Resolver.OpenInputStream(uri)!;
        using var ms = new MemoryStream();
        stream.CopyTo(ms);
        return ms.ToArray();
    }

    public static void WriteAll(AndroidUri uri, byte[] data)
    {
        // "wt" = write + truncate, so the destination file is fully replaced.
        using var stream = Resolver.OpenOutputStream(uri, "wt")!;
        stream.Write(data, 0, data.Length);
        stream.Flush();
    }

    public static string GetDisplayName(AndroidUri uri)
    {
        try
        {
            using ICursor? c = Resolver.Query(uri, null, null, null, null);
            if (c != null && c.MoveToFirst())
            {
                int idx = c.GetColumnIndex("_display_name"); // OpenableColumns.DISPLAY_NAME
                if (idx >= 0) return c.GetString(idx) ?? "save";
            }
        }
        catch { }
        return "save";
    }

    /// <summary>Remember which picked URI a sandbox save came from (for write-back).</summary>
    public static void RememberSource(string sandboxRelPath, AndroidUri uri)
    {
        var map = LoadMap();
        map[sandboxRelPath] = uri.ToString();
        SaveMap(map);
    }

    /// <summary>
    /// Copy every imported save's (committed) sandbox bytes back to the file the user picked.
    /// Returns the number of saves written. Safe to call repeatedly; unchanged saves rewrite identical bytes.
    /// </summary>
    public static int WriteBackAll()
    {
        var map = LoadMap();
        int written = 0;
        foreach (var (relPath, uriStr) in map)
        {
            var full = Path.Combine(FileSystem.AppDataDirectory, relPath);
            if (!File.Exists(full)) continue;
            var uri = AndroidUri.Parse(uriStr);
            if (uri == null) continue;
            try
            {
                WriteAll(uri, File.ReadAllBytes(full));
                written++;
            }
            catch { /* permission lost / file moved — skip */ }
        }
        return written;
    }
}
