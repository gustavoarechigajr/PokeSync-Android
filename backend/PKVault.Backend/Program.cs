using System.IO.Abstractions;
using System.IO.Compression;
using System.Net;
using System.Net.Sockets;
using System.Security.Cryptography.X509Certificates;
using Microsoft.AspNetCore.Mvc.ApplicationModels;
using Microsoft.AspNetCore.ResponseCompression;
using Microsoft.EntityFrameworkCore;
using Serilog;

namespace PKVault.Backend;

public class Program
{
    // For migration file generation
    // EF Core uses this method at design time to access the DbContext
    public static IHostBuilder CreateHostBuilder(string[] args)
        => Host.CreateDefaultBuilder(args)
            .ConfigureWebHostDefaults(
                webBuilder => webBuilder.UseStartup<Startup>());

    public static async Task Main(string[] args)
    {
        LogUtil.Initialize();

        try {
            Copyright();

            var time = Log.Logger.Time($"Setup backend load");

            var app = await PrepareWebApp(5000);
            var setupPostRun = await SetupData(app, args);
            time.Dispose();

            if (setupPostRun != null)
            {
                var appTask = app.RunAsync();

                var setupPostRunTime = Log.Logger.Time($"Setup post-run");

                await setupPostRun();

                setupPostRunTime.Dispose();

                await appTask;
            }
        }
        catch (Exception ex)
        {
            Log.Fatal(ex, "An unhandled exception occurred during startup");
        }
        finally
        {
            LogUtil.Dispose();
        }
    }

    public static void Copyright()
    {
        var (BuildID, Version) = SettingsService.GetBuildInfo();
        Log.Information("PKVault Copyright (C) 2026  Richard Haddad"
        + "\nThis program comes with ABSOLUTELY NO WARRANTY."
        + "\nThis is free software, and you are welcome to redistribute it under certain conditions."
        + "\nFull license can be accessed here: https://github.com/Chnapy/PKVault/blob/main/LICENSE"
        + $"\nPKVault v{Version} BuildID = {BuildID}"
        + $"\nCurrent time UTC = {DateTime.UtcNow}\n");
    }

    public static async Task<Func<Task>?> SetupData(IHost host, string[] args)
    {
        var initialMemoryUsedMB = System.Diagnostics.Process.GetCurrentProcess().WorkingSet64 / 1_000_000;

#if MODE_GEN_POKEAPI
        await host.Services.GetRequiredService<GenStaticDataService>().GenerateFiles();
        return null;
#elif MODE_DEFAULT

        var setupedMemoryUsedMB = System.Diagnostics.Process.GetCurrentProcess().WorkingSet64 / 1_000_000;

        Log.Information($"Memory checks: initial={initialMemoryUsedMB} MB setuped={setupedMemoryUsedMB} MB diff={setupedMemoryUsedMB - initialMemoryUsedMB} MB");

        // if (args.Length > 0 && args[0] == "clean")
        // {
        //     await host.Services.GetRequiredService<MaintenanceService>().CleanMainStorageFiles();
        //     return null;
        // }

        // if (args.Length > 0 && args[0] == "test-bkp")
        // {
        //     await host.Services.GetRequiredService<BackupService>().CreateBackup();
        //     return null;
        // }

        // Single local user: no auth DB. The session DB is migrated lazily in SessionService.
        // Ensure the default user's session exists at startup.
        await host.Services.GetRequiredService<ISessionServiceMinimal>().EnsureSessionCreated();

        return () => Task.CompletedTask;
#else
        throw new Exception("Mode not defined");
#endif
    }

    public static async Task<WebApplication> PrepareWebApp(int port)
    {
        var builder = WebApplication.CreateBuilder([]);

        ConfigureServices(builder.Services);

        var sp = builder.Services.BuildServiceProvider();
        var fileIOService = sp.GetRequiredService<IFileIOService>();
        var settings = sp.GetRequiredService<ISettingsService>()
            .GetSettings();

        X509Certificate2? GetCertificate()
        {
            var certPemPath = settings.GetHttpsCertPemPathPath();
            var keyPemPath = settings.GetHttpsKeyPemPathPath();

            return certPemPath != null && keyPemPath != null
                ? X509Certificate2.CreateFromPem(certPemPath, keyPemPath)
                : null;
        }

        var certificate = GetCertificate();

        builder.WebHost.ConfigureKestrel(serverOptions =>
        {
            serverOptions.ListenAnyIP(port, listenOptions =>
            {
                if (certificate != default)
                {
                    listenOptions.UseHttps(certificate);
                }
                else if (settings.SettingsMutable.HTTPS_NOCERT == true)
                {
                    listenOptions.UseHttps();
                }

            });
        });

        var app = builder.Build();

        ConfigureAppBuilder(app, certificate != default || settings.SettingsMutable.HTTPS_NOCERT == true);

        return app;
    }

    public static void ConfigureServices(IServiceCollection services)
    {
        services.AddResponseCompression(opts =>
        {
            opts.Providers.Add<BrotliCompressionProvider>();
            opts.Providers.Add<GzipCompressionProvider>();
            opts.EnableForHttps = true;
        });

        services.Configure<BrotliCompressionProviderOptions>(options =>
        {
            options.Level = CompressionLevel.Optimal;
        });

        services.Configure<GzipCompressionProviderOptions>(options =>
        {
            options.Level = CompressionLevel.Optimal;
        });

        services
            .AddControllers(options =>
            {
                options.Conventions.Add(new RouteTokenTransformerConvention(new SlugifyParameterTransformer()));
            })
            // required by PublishedTrimmed
            .AddJsonOptions(options =>
            {
                options.JsonSerializerOptions.TypeInfoResolver = RouteJsonContext.Default;
                options.JsonSerializerOptions.Converters.Add(new ByteArrayJsonConverter());
            });

        services.AddMemoryCache();

        // Auth/multi-user removed for the local single-user app — see SessionService ("default" user).

#if MODE_GEN_POKEAPI
        services.AddSingleton<PokeApiService>();
        services.AddSingleton<GenStaticDataService>();
#endif

        services.AddSingleton(TimeProvider.System);

        Log.Information($"Setup services - DB");
        services.AddDbContext<SessionDbContext>();

        services.AddSingleton<ISessionService, SessionService>();
        services.AddSingleton<ISessionServiceMinimal, ISessionService>(sp => sp.GetRequiredService<ISessionService>());   // use same instance as ISessionService
        services.AddSingleton<IDbSeedingService, DbSeedingService>();

        Log.Information($"Setup services - Main");
        services.AddSingleton<IFileSystem>(new FileSystem());
        services.AddSingleton<IFileIOService, FileIOService>();
        services.AddSingleton<StaticDataService>();
        services.AddSingleton<StorageQueryService>();
        services.AddSingleton<ActionService>();
        // services.AddSingleton<MaintenanceService>();
        services.AddSingleton<DexService>();
        services.AddSingleton<WarningsService>();
        services.AddSingleton<BackupService>();
        services.AddSingleton<ISettingsService, SettingsService>();
        services.AddSingleton<ILegalityAnalysisService, LegalityAnalysisService>();
        services.AddSingleton<DataService>();
        services.AddSingleton<IPkmConvertService, PkmConvertService>();
        services.AddSingleton<IPkmSharePropertiesService, PkmSharePropertiesService>();
        services.AddSingleton<PkmUpdateService>();
        services.AddSingleton<PkmLegalityService>();

        Log.Information($"Setup services - Actions");
        services.AddScoped<DataNormalizeAction>();
        services.AddScoped<UpdateExternalPkmAction>();
        services.AddScoped<SynchronizePkmAction>();
        services.AddScoped<MainCreateBoxAction>();
        services.AddScoped<MainUpdateBoxAction>();
        services.AddScoped<MainDeleteBoxAction>();
        services.AddScoped<MainCreateBankAction>();
        services.AddScoped<MainUpdateBankAction>();
        services.AddScoped<MainDeleteBankAction>();
        services.AddScoped<MovePkmAction>();
        services.AddScoped<MovePkmBankAction>();
        services.AddScoped<MainCreatePkmVariantAction>();
        services.AddScoped<EditPkmVariantAction>();
        services.AddScoped<EditPkmSaveAction>();
        services.AddScoped<DetachPkmSaveAction>();
        services.AddScoped<DeletePkmVariantAction>();
        services.AddScoped<SaveDeletePkmAction>();
        services.AddScoped<EvolvePkmAction>();
        services.AddScoped<SortPkmAction>();
        services.AddScoped<DexSyncAction>();

        Log.Information($"Setup services - Loaders");
        services.AddScoped<IMetaLoader, MetaLoader>();
        services.AddScoped<IBankLoader, BankLoader>();
        services.AddScoped<IBoxLoader, BoxLoader>();
        services.AddScoped<IPkmVariantLoader, PkmVariantLoader>();
        services.AddScoped<IPkmFileLoader, PkmFileLoader>();
        services.AddScoped<IDexLoader, DexLoader>();
        services.AddSingleton<ISavesLoadersService, SavesLoadersService>();   // singleton for perf reasons

#if ENABLE_SWAGGER
        // Desktop-only (build with -p:EnableSwagger=true) to (re)generate the frontend SDK via Orval.
        // Excluded from the Android build so NSwag's transitive ASP.NET FrameworkReference never leaks in.
        services.AddEndpointsApiExplorer();
        services.AddSwaggerDocument(document =>
        {
            document.PostProcess = doc =>
            {
                doc.Info.Title = "PKVault API";

                // Required for PKHeX.Core.Gender which has duplicates
                foreach (var enumSchema in doc.Definitions.Values.Where(s => s.IsEnumeration))
                {
                    var distinctValues = enumSchema.Enumeration.Distinct().ToList();
                    enumSchema.Enumeration.Clear();
                    foreach (var value in distinctValues)
                    {
                        enumSchema.Enumeration.Add(value);
                    }
                }
            };
        });
#endif

        Log.Information($"Setup services - Finished");
    }

    public static void ConfigureAppBuilder(IApplicationBuilder app, bool useHttps)
    {
        app.UseResponseCompression();

        if (useHttps)
        {
            app.UseHttpsRedirection();
        }

        // Serve the embedded React frontend (wwwroot embedded into this assembly).
        var frontendFiles = new Microsoft.Extensions.FileProviders.ManifestEmbeddedFileProvider(
            typeof(Program).Assembly, "wwwroot");
        app.UseDefaultFiles(new DefaultFilesOptions { FileProvider = frontendFiles });
        app.UseStaticFiles(new StaticFileOptions { FileProvider = frontendFiles });

        app.UseRouting();

        // Single local user: ensure the "default" session is initialized on every request
        // (cheap no-op once created).
        app.Use(async (context, next) =>
        {
            var session = context.RequestServices.GetRequiredService<ISessionServiceMinimal>();
            await session.EnsureSessionCreated();
            await next();
        });

        app.UseMiddleware<ExceptionHandlingMiddleware>();
        app.UseEndpoints(endpoints =>
        {
            endpoints.MapControllers();
            // SPA fallback: any non-API route serves the React index.html (client-side routing).
            endpoints.MapFallbackToFile("index.html", new StaticFileOptions { FileProvider = frontendFiles });
        });

#if ENABLE_SWAGGER
        app.UseOpenApi();
        app.UseSwaggerUi();
#endif
    }

    public static int GetAvailablePort()
    {
        var listener = new TcpListener(IPAddress.Loopback, 0);
        listener.Start();

        int port = ((IPEndPoint)listener.LocalEndpoint).Port;

        listener.Stop();

        return port;
    }

    public static bool HasEmptyActionList(IHost host)
    {
        return host.Services.GetRequiredService<ISessionService>().HasEmptyActionList();
    }
}
