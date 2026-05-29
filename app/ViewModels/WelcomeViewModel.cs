using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PKHeX.Android.Models;
using PKHeX.Android.Services;

namespace PKHeX.Android.ViewModels;

public sealed partial class WelcomeViewModel(SaveFileService saveService, SaveContext context) : BaseViewModel
{
    [ObservableProperty]
    private string? _errorMessage;

    [RelayCommand]
    private async Task PickSaveFileAsync()
    {
        ErrorMessage = null;
        IsBusy = true;
        try
        {
            var result = await FilePicker.Default.PickAsync(new PickOptions
            {
                PickerTitle = "Open Save File",
            });

            if (result is null)
                return;

            var (success, error) = await saveService.OpenAsync(result.FullPath);
            if (!success)
            {
                ErrorMessage = error ?? "Failed to open save file.";
                return;
            }

            await Shell.Current.GoToAsync("//main/box");
        }
        finally
        {
            IsBusy = false;
        }
    }
}
