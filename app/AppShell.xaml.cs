using PKHeX.Android.Features.MovePicker;
using PKHeX.Android.Features.PokemonEditor;

namespace PKHeX.Android;

public partial class AppShell : Shell
{
    public AppShell()
    {
        InitializeComponent();
        Routing.RegisterRoute("//main/box/editor", typeof(PokemonEditorPage));
        Routing.RegisterRoute("movepicker", typeof(MovePickerPage));
    }
}
