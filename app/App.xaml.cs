using Microsoft.Extensions.DependencyInjection;

namespace PKHeX.Android;

public partial class App : Application
{
	public App()
	{
		InitializeComponent();
	}

	protected override Window CreateWindow(IActivationState? activationState)
	{
		// Vertical slice: launch straight into the embedded-server WebView.
		// (Existing AppShell native pages are kept in the project but bypassed for now.)
		return new Window(new WebHostPage());
	}
}