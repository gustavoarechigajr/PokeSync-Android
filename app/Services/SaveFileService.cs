using PKHeX.Core;
using PKHeX.Android.Models;

namespace PKHeX.Android.Services;

public sealed class SaveFileService(SaveContext context)
{
    public async Task<(bool Success, string? Error)> OpenAsync(string path)
    {
        try
        {
            var data = await File.ReadAllBytesAsync(path);
            if (!SaveUtil.TryGetSaveFile(new Memory<byte>(data), out var sav))
                return (false, "File is not a recognised Pokémon save.");

            context.Load(sav, path);
            return (true, null);
        }
        catch (Exception ex)
        {
            return (false, ex.Message);
        }
    }

    public async Task<(bool Success, string? Error)> SaveAsync()
    {
        if (context.SaveFile is null || context.FilePath is null)
            return (false, "No save file loaded.");
        try
        {
            var data = context.SaveFile.Write();
            await File.WriteAllBytesAsync(context.FilePath, data);
            return (true, null);
        }
        catch (Exception ex)
        {
            return (false, ex.Message);
        }
    }

    public async Task<(bool Success, string? Error)> ExportPkmAsync(PKM pkm, string directory)
    {
        try
        {
            var name = pkm.FileName;
            var invalid = Path.GetInvalidFileNameChars();
            name = string.Concat(name.Select(c => invalid.Contains(c) ? '_' : c));
            var path = Path.Combine(directory, name);
            await File.WriteAllBytesAsync(path, pkm.Data.ToArray());
            return (true, null);
        }
        catch (Exception ex)
        {
            return (false, ex.Message);
        }
    }
}
