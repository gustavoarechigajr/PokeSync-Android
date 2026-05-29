using PKHeX.Android.ViewModels;

namespace PKHeX.Android.Features.PokemonEditor;

public partial class PokemonEditorPage : ContentPage
{
    public PokemonEditorPage(PokemonEditorViewModel vm)
    {
        InitializeComponent();
        BindingContext = vm;
    }
}
