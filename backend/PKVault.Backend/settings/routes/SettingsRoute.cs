using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.ModelBinding;

namespace PKVault.Backend.settings.routes;

[ApiController]
[Route("api/[controller]")]
public class SettingsController(DataService dataService, ISettingsService settingsService, IFileIOService fileIOService, ISessionService sessionService) : ControllerBase
{
    [HttpGet]
    public async Task<ActionResult<SettingsDTO>> Get()
    {
        return await settingsService.GetSettingsWithUserId();
    }

    [HttpGet("test-save-globs")]
    public ActionResult<List<string>> GetSaveGlobsResults([FromQuery] string[] globs, int limit)
    {
        var results = fileIOService.Matcher.SearchPaths(globs);

        if (results.Count > limit)
        {
            throw new ArgumentException($"Too much results ({results.Count}) for given globs");
        }

        return results;
    }

    // Re-scan save files (e.g. after the Android app imports a save into the sandbox saves dir).
    // Reuses the settings update path: persists pending actions if any, then restarts the session
    // which reloads saves from SAVE_GLOBS.
    [HttpPost("rescan-saves")]
    public async Task<ActionResult<DataDTO>> RescanSaves()
    {
        var current = settingsService.GetSettings().SettingsMutable;

        DataUpdateFlags flags = new();
        flags.Saves.All = true;

        await settingsService.UpdateSettings(current, restartSession: true, persistSession: true, flags);

        return await dataService.CreateDataFromUpdateFlags(flags);
    }

    [HttpPost]
    public async Task<ActionResult<DataDTO>> Edit([BindRequired] SettingsMutableDTO settingsMutable)
    {
        settingsMutable = settingsMutable with
        {
            SAVE_GLOBS = [.. settingsMutable.SAVE_GLOBS.Select(glob => glob.Trim())],
            PKM_EXTERNAL_GLOBS = [.. (settingsMutable.PKM_EXTERNAL_GLOBS ?? []).Select(glob => glob.Trim())],
        };

        DataUpdateFlags flags = new();

        var (RestartSession, PersistSession) = settingsService.GetUpdateDiff(settingsMutable, flags);

        if (RestartSession && !sessionService.HasEmptyActionList())
        {
            throw new InvalidOperationException($"Empty action list is required");
        }

        if (settingsMutable.LANGUAGE == null || !SettingsService.AllowedLanguages.Contains(settingsMutable.LANGUAGE))
        {
            throw new ArgumentException($"Language value not allowed: {settingsMutable.LANGUAGE}");
        }

        await settingsService.UpdateSettings(settingsMutable, RestartSession, PersistSession, flags);

        return await dataService.CreateDataFromUpdateFlags(flags);
    }
}
