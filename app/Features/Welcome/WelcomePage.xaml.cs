using PKHeX.Android.ViewModels;

namespace PKHeX.Android.Features.Welcome;

public partial class WelcomePage : ContentPage
{
    public WelcomePage(WelcomeViewModel vm)
    {
        InitializeComponent();
        BindingContext = vm;
    }
}
