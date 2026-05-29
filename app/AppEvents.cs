namespace PKHeX.Android;

/// <summary>App-wide lifecycle signals. Raised from MauiProgram's platform lifecycle hooks.</summary>
public static class AppEvents
{
    /// <summary>Fired when the app returns to the foreground (e.g. after granting a permission).</summary>
    public static event Action? Resumed;

    public static void RaiseResumed() => Resumed?.Invoke();
}
