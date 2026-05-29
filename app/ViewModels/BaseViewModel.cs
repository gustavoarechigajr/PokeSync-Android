using CommunityToolkit.Mvvm.ComponentModel;

namespace PKHeX.Android.ViewModels;

public abstract partial class BaseViewModel : ObservableObject
{
    [ObservableProperty]
    private bool _isBusy;

    [ObservableProperty]
    private string _title = string.Empty;
}
