using PKHeX.Android.ViewModels;

namespace PKHeX.Android.Features.MovePicker;

public partial class MovePickerPage : ContentPage
{
    public MovePickerPage(MovePickerViewModel vm)
    {
        InitializeComponent();
        BindingContext = vm;
    }
}
