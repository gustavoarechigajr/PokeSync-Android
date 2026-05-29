using PKHeX.Core;

namespace PKHeX.Android.Models;

public sealed class SaveContext
{
    public SaveFile? SaveFile { get; private set; }
    public string? FilePath { get; private set; }
    public bool IsLoaded => SaveFile is not null;

    public event Action? SaveLoaded;
    public event Action? SaveClosed;

    public void Load(SaveFile sav, string path)
    {
        SaveFile = sav;
        FilePath = path;
        SaveLoaded?.Invoke();
    }

    public void Close()
    {
        SaveFile = null;
        FilePath = null;
        SaveClosed?.Invoke();
    }
}
