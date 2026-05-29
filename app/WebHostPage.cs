using Microsoft.Maui;
using Microsoft.Maui.Controls;
using Microsoft.Maui.Graphics;
using Microsoft.Maui.Storage;

namespace PKHeX.Android;

// Full-screen WebView hosting the embedded PKVault frontend, plus native toolbar buttons:
//  • Import save  — pick an emulator save via SAF (with persistable write permission) into the sandbox.
//  • Save to game — commit staged changes, then write the modified save BACK to the picked file.
// (The sandboxed web UI can't touch Android storage, so both must go through native + SAF.)
public class WebHostPage : ContentPage
{
    private readonly WebView _webView = new()
    {
        IsVisible = false,
        VerticalOptions = LayoutOptions.Fill,
        HorizontalOptions = LayoutOptions.Fill,
    };

    private readonly ActivityIndicator _spinner = new()
    {
        IsRunning = true,
        VerticalOptions = LayoutOptions.Center,
        HorizontalOptions = LayoutOptions.Center,
        Color = Colors.MediumPurple,
    };

    private readonly Button _importButton = MakeButton("Import save", "#2A8F82");
    private readonly Button _saveButton = MakeButton("Save to game", "#1E6F66");

    private static Button MakeButton(string text, string color) => new()
    {
        Text = text,
        IsVisible = false,
        Padding = new Thickness(14, 4),
        BackgroundColor = Color.FromArgb(color),
        TextColor = Colors.White,
        FontSize = 13,
        CornerRadius = 20,
    };

    private string? _serverUrl;

    public WebHostPage()
    {
        _importButton.Clicked += OnImportClicked;
        _saveButton.Clicked += OnSaveToGameClicked;

        // Toolbar of native buttons, top-right (over the empty side of the app bar).
        var toolbar = new HorizontalStackLayout
        {
            Spacing = 8,
            Margin = new Thickness(0, 6, 12, 0),
            HorizontalOptions = LayoutOptions.End,
            VerticalOptions = LayoutOptions.Start,
            Children = { _saveButton, _importButton },
        };

        Content = new Grid { Children = { _webView, _spinner, toolbar } };
        AppEvents.Resumed += OnAppResumed;
        _ = LoadAsync();
    }

    // When the user returns after granting All Files Access, re-scan so emulator saves appear.
    private async void OnAppResumed()
    {
#if ANDROID
        if (_serverUrl is null || WebServer.ScannedWithAccess || !StorageAccess.IsGranted) return;
        WebServer.ScannedWithAccess = true;
        try
        {
            Busy(true);
            await Post("/api/settings/rescan-saves");
            _webView.Reload();
        }
        catch { }
        finally { Busy(false); }
#else
        await Task.CompletedTask;
#endif
    }

    private async Task LoadAsync()
    {
        try
        {
            await WebServer.StartAsync();
            _serverUrl = await WebServer.Ready;

            _webView.Source = new UrlWebViewSource { Url = _serverUrl };
            _webView.IsVisible = true;
            _importButton.IsVisible = true;
            _saveButton.IsVisible = true;
            _spinner.IsRunning = false;
            _spinner.IsVisible = false;

#if ANDROID
            // Ask for All Files Access so we can read emulator saves from their live folders and write
            // changes back to the actual games (Eden, RetroArch, …). One-time grant.
            if (!StorageAccess.IsGranted)
            {
                var grant = await DisplayAlert("Allow access to your saves",
                    "PokeSync needs “All files access” to find your emulator saves (Eden, RetroArch, Azahar…) "
                    + "and write changes back to the games. Without it you can still import saves manually.",
                    "Grant access", "Not now");
                if (grant) StorageAccess.OpenSettings();
            }
#endif
        }
        catch (Exception ex)
        {
            _spinner.IsRunning = false;
            await DisplayAlert("Local server error", ex.Message, "OK");
        }
    }

    private async void OnImportClicked(object? sender, EventArgs e)
    {
#if ANDROID
        try
        {
            var uri = await SafBridge.PickSaveAsync();
            if (uri is null) return;

            Busy(true);

            var name = SafBridge.GetDisplayName(uri);
            var rel = $"{WebServer.SavesFolderName}/{name}";
            var dest = Path.Combine(FileSystem.AppDataDirectory, rel);
            Directory.CreateDirectory(Path.GetDirectoryName(dest)!);
            File.WriteAllBytes(dest, SafBridge.ReadAll(uri));
            SafBridge.RememberSource(rel, uri); // so "Save to game" can write changes back here

            await Post("/api/settings/rescan-saves");
            _webView.Reload();
            await DisplayAlert("Save imported", $"{name} imported and scanned.", "OK");
        }
        catch (Exception ex) { await DisplayAlert("Import failed", ex.Message, "OK"); }
        finally { Busy(false); }
#else
        await DisplayAlert("Unsupported", "Save import is only available on Android.", "OK");
#endif
    }

    private async void OnSaveToGameClicked(object? sender, EventArgs e)
    {
#if ANDROID
        var confirm = await DisplayAlert("Save to game",
            "Write your changes back to the original save file(s)? This overwrites the game's save.",
            "Save", "Cancel");
        if (!confirm) return;

        try
        {
            Busy(true);
            await Post("/api/storage/action/save"); // commit staged changes → backend writes sandbox saves
            var n = SafBridge.WriteBackAll();        // copy sandbox saves back to the picked files
            await DisplayAlert("Saved",
                n > 0 ? $"Wrote changes back to {n} save file(s). Reload the save in your emulator."
                      : "No linked save files to write back. Re-import the save via “Import save” first.",
                "OK");
        }
        catch (Exception ex) { await DisplayAlert("Save failed", ex.Message, "OK"); }
        finally { Busy(false); }
#else
        await DisplayAlert("Unsupported", "Saving is only available on Android.", "OK");
#endif
    }

    private async Task Post(string path)
    {
        if (_serverUrl is null) return;
        using var http = new HttpClient { Timeout = TimeSpan.FromMinutes(2) };
        using var resp = await http.PostAsync($"{_serverUrl}{path}", null);
        resp.EnsureSuccessStatusCode();
    }

    private void Busy(bool busy)
    {
        _spinner.IsVisible = busy;
        _spinner.IsRunning = busy;
    }
}
