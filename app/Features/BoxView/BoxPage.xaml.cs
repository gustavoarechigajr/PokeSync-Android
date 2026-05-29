using PKHeX.Android.ViewModels;

namespace PKHeX.Android.Features.BoxView;

public partial class BoxPage : ContentPage
{
    private readonly BoxViewModel _vm;

    public BoxPage(BoxViewModel vm)
    {
        InitializeComponent();
        BindingContext = vm;
        _vm = vm;
        _vm.PropertyChanged += OnVmPropertyChanged;
    }

    protected override void OnAppearing()
    {
        base.OnAppearing();
        // Refresh slots after returning from editor so new/edited Pokémon show up
        _vm.RefreshCurrentView();
    }

    private void OnVmPropertyChanged(object? sender, System.ComponentModel.PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(BoxViewModel.SelectedSlot) && _vm.SelectedSlot is { } slot)
        {
            var url = slot.BoxIndex < 0
                ? $"//main/box/editor?partyIndex={slot.SlotIndex}"
                : $"//main/box/editor?boxIndex={slot.BoxIndex}&slotIndex={slot.SlotIndex}";
            Shell.Current.GoToAsync(url);
        }
    }

    private void OnBoxTabTapped(object sender, TappedEventArgs e)
    {
        if (e.Parameter is string name)
        {
            var idx = _vm.BoxNames.IndexOf(name);
            if (idx >= 0)
                _vm.SelectedBoxIndex = idx;
        }
    }
}
