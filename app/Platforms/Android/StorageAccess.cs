using Android.Content;
using Android.OS;
using Android.Provider;
using Microsoft.Maui.ApplicationModel;
using AndroidEnv = Android.OS.Environment;
using AndroidUri = Android.Net.Uri;

namespace PKHeX.Android;

/// <summary>
/// All Files Access (MANAGE_EXTERNAL_STORAGE) — needed to read/write emulator saves in their live
/// (often restricted, e.g. /Android/data) directories, exactly as the previous PokeSync version did.
/// </summary>
public static class StorageAccess
{
    public static bool IsGranted =>
        Build.VERSION.SdkInt < BuildVersionCodes.R || AndroidEnv.IsExternalStorageManager;

    /// <summary>Open the system screen where the user toggles "Allow access to manage all files".</summary>
    public static void OpenSettings()
    {
        try
        {
            var ctx = Platform.CurrentActivity ?? global::Android.App.Application.Context;
            var intent = new Intent(Settings.ActionManageAppAllFilesAccessPermission,
                AndroidUri.FromParts("package", ctx.PackageName, null));
            intent.AddFlags(ActivityFlags.NewTask);
            ctx.StartActivity(intent);
        }
        catch
        {
            // Fallback: the global "all files access" list.
            var ctx = global::Android.App.Application.Context;
            var intent = new Intent(Settings.ActionManageAllFilesAccessPermission);
            intent.AddFlags(ActivityFlags.NewTask);
            ctx.StartActivity(intent);
        }
    }
}
