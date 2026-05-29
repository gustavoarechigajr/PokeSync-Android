using Microsoft.Extensions.Logging;
using Microsoft.Maui.LifecycleEvents;
using PKHeX.Android.Features.BoxView;
using PKHeX.Android.Features.MovePicker;
using PKHeX.Android.Features.PokemonEditor;
using PKHeX.Android.Features.Welcome;
using PKHeX.Android.Models;
using PKHeX.Android.Services;
using PKHeX.Android.ViewModels;

namespace PKHeX.Android;

public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();
        builder
            .UseMauiApp<App>()
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
            })
            .ConfigureLifecycleEvents(events =>
            {
#if ANDROID
                events.AddAndroid(a => a.OnResume(_ => AppEvents.RaiseResumed()));
#endif
            });

        // Core services (singletons shared across the app)
        builder.Services.AddSingleton<SaveContext>();
        builder.Services.AddSingleton<SaveFileService>();

        // View models
        builder.Services.AddSingleton<WelcomeViewModel>();
        builder.Services.AddSingleton<BoxViewModel>();
        builder.Services.AddTransient<PokemonEditorViewModel>();
        builder.Services.AddTransient<MovePickerViewModel>();

        // Pages
        builder.Services.AddSingleton<WelcomePage>();
        builder.Services.AddSingleton<BoxPage>();
        builder.Services.AddTransient<PokemonEditorPage>();
        builder.Services.AddTransient<MovePickerPage>();

#if DEBUG
        builder.Logging.AddDebug();
#endif

        return builder.Build();
    }
}
