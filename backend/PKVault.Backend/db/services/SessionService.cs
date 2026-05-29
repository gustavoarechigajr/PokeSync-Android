using System.Collections.Concurrent;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;

public interface ISessionService : ISessionServiceMinimal
{
    public DateTime? StartTime { get; }
    public List<SessionService.ActionRecord> Actions { get; }

    public bool HasMainDb();
    public bool HasEmptyActionList();
    public List<DataActionPayload> GetActionPayloadList();

    public Task StartNewSession(bool checkInitialActions, DataUpdateFlags? flags);
    public Task PersistSession(IServiceScope scope);
}

public interface ISessionServiceMinimal
{
    public string MainDbPath { get; }
    public string MainDbRelativePath { get; }
    public string SessionDbPath { get; }

    public Task EnsureSessionCreated(Guid? byPassContextId = null);
}

public class SessionService(
    ILogger<SessionService> log,
    IServiceProvider sp, TimeProvider timeProvider,
    IFileIOService fileIOService, ISettingsService settingsService,
    ISavesLoadersService savesLoadersService
) : ISessionService
{
    public record ActionRecord(
        Func<IServiceScope, DataUpdateFlags, Task<DataActionPayload>> ActionFn,
        DataActionPayload Payload
    );

    private sealed class UserSessionState
    {
        public Task<DataUpdateFlags>? StartTask;
        public Guid? ByPassContextId;
        public DateTime? StartTime;
        public readonly List<ActionRecord> Actions = [];
    }

    private const string DefaultUserId = "default";

    private readonly ConcurrentDictionary<string, UserSessionState> _userSessions = new();

    private string DbFolderPath => settingsService.GetSettings().GetDbPath();

    // Single local user: always the "default" user (auth/multi-user removed for the on-device app).
    private string GetEffectiveUserId() => DefaultUserId;

    private UserSessionState GetCurrentSession() =>
        _userSessions.GetOrAdd(GetEffectiveUserId(), _ => new UserSessionState());

    // Per-user DB paths
    public string MainDbPath => GetMainDbPath(GetEffectiveUserId());
    public string MainDbRelativePath => Path.Combine(
        settingsService.GetSettings().SettingsMutable.DB_PATH,
        $"pkvault-{GetEffectiveUserId()}.db");
    public string SessionDbPath => GetSessionDbPath(GetEffectiveUserId());

    private string GetMainDbPath(string userId) => Path.Combine(DbFolderPath, $"pkvault-{userId}.db");
    private string GetSessionDbPath(string userId) => Path.Combine(DbFolderPath, $"pkvault-session-{userId}.db");

    // ISessionService interface — delegate to per-user state
    public DateTime? StartTime => GetCurrentSession().StartTime;
    public List<ActionRecord> Actions => GetCurrentSession().Actions;

    public bool HasMainDb() => fileIOService.Exists(MainDbPath);

    public List<DataActionPayload> GetActionPayloadList() =>
        [.. GetCurrentSession().Actions.Select(a => a.Payload)];

    public bool HasEmptyActionList() => GetCurrentSession().Actions.Count == 0;

    public async Task StartNewSession(bool checkInitialActions, DataUpdateFlags? flags)
    {
        var userId = GetEffectiveUserId();
        var session = GetCurrentSession();

        session.StartTime = timeProvider.GetUtcNow().DateTime;

        using var _ = log.Time("Starting new session");

        session.Actions.Clear();

        session.StartTask = Task.Run(async () =>
        {
            flags ??= new();

            await Task.WhenAll(
                ResetDbSession(userId, flags),
                savesLoadersService.Setup(flags)
            );

            if (checkInitialActions)
            {
                using var scope = sp.CreateScope();

                session.ByPassContextId = scope.ServiceProvider.GetRequiredService<SessionDbContext>()
                    .ContextId.InstanceId;

                var hadDataToNormalize = await CheckDataToNormalize(scope, flags);

                await CheckSaveToSynchronize(scope, flags);

                if (hadDataToNormalize)
                {
                    await CheckFirstRunAutoSave(userId, scope, flags);
                }

                session.ByPassContextId = null;
            }

            return flags;
        });

        await session.StartTask;
    }

    private async Task<bool> CheckDataToNormalize(IServiceScope scope, DataUpdateFlags flags)
    {
        var actionService = scope.ServiceProvider.GetRequiredService<ActionService>();
        var dataNormalizeAction = scope.ServiceProvider.GetRequiredService<DataNormalizeAction>();
        var updateExternalPkmAction = scope.ServiceProvider.GetRequiredService<UpdateExternalPkmAction>();

        var dataToNormalizeInput = await dataNormalizeAction.HasDataToNormalize();

        if (dataToNormalizeInput.ShouldRun)
        {
            await actionService.DataNormalize(dataToNormalizeInput, scope, flags);
        }

        try
        {
            var externalPkmsToUpdateInput = await updateExternalPkmAction.HasExternalPkmsToUpdate();

            if (externalPkmsToUpdateInput.ShouldRun)
            {
                await actionService.UpdateExternalPkm(externalPkmsToUpdateInput, scope, flags);
            }

            return externalPkmsToUpdateInput.ShouldRun;
        }
        catch (Exception ex)
        {
            log.LogError(ex.ToString());
        }
        return false;
    }

    private async Task CheckSaveToSynchronize(IServiceScope scope, DataUpdateFlags flags)
    {
        var actionService = scope.ServiceProvider.GetRequiredService<ActionService>();
        var synchronizePkmAction = scope.ServiceProvider.GetRequiredService<SynchronizePkmAction>();

        var synchronizationData = await synchronizePkmAction.GetSavesPkmsToSynchronize();

        foreach (var data in synchronizationData)
        {
            if (data.pkmVariantAndPkmSaveIds.Length > 0)
            {
                await actionService.SynchronizePkm(data, scope, flags);
            }
        }
    }

    private async Task CheckFirstRunAutoSave(string userId, IServiceScope scope, DataUpdateFlags flags)
    {
        var savesLoaders = scope.ServiceProvider.GetRequiredService<ISavesLoadersService>();
        var pkmVariantLoader = scope.ServiceProvider.GetRequiredService<IPkmVariantLoader>();

        var hasAnyData = savesLoaders.GetAllLoaders().Length > 0
            || await pkmVariantLoader.Any();

        if (!hasAnyData)
        {
            log.LogInformation("Fresh start detected - Session persisting & restarting");
            await PersistSession(scope);
            await StartNewSession(checkInitialActions: false, flags);
        }
    }

    public async Task EnsureSessionCreated(Guid? byPassContextId = null)
    {
        var userId = GetEffectiveUserId();
        var session = GetCurrentSession();

        if (session.StartTask == null)
        {
            log.LogInformation("Session not created for user {UserId} - Starting new one", userId);
            await StartNewSession(checkInitialActions: true, null);
        }
        else if (byPassContextId != null && byPassContextId == session.ByPassContextId)
        {
            return;
        }
        else
        {
            await session.StartTask;
        }
    }

    public async Task PersistSession(IServiceScope scope)
    {
        var userId = GetEffectiveUserId();
        var session = GetCurrentSession();

        using var _ = log.Time("Persist session with copy session to main");

        var pkmFileLoader = scope.ServiceProvider.GetRequiredService<IPkmFileLoader>();
        await pkmFileLoader.WriteToFiles();

        await savesLoadersService.WriteToFiles();
        savesLoadersService.Clear();

        session.Actions.Clear();

        await CloseConnection(userId);
        session.StartTask = null;

        log.LogDebug("Move session DB to main for user {UserId}", userId);
        fileIOService.Move(GetSessionDbPath(userId), GetMainDbPath(userId), overwrite: true);

        session.StartTime = null;
    }

    private async Task ResetDbSession(string userId, DataUpdateFlags flags)
    {
        var sessionDbPath = GetSessionDbPath(userId);
        var mainDbPath = GetMainDbPath(userId);

        if (fileIOService.Exists(sessionDbPath))
        {
            using var scope = sp.CreateScope();
            using var db = scope.ServiceProvider.GetRequiredService<SessionDbContext>();

            var deleted1 = await db.Database.EnsureDeletedAsync();
            var deleted2 = fileIOService.Delete(sessionDbPath);
            fileIOService.Delete(sessionDbPath + "-shm");
            fileIOService.Delete(sessionDbPath + "-wal");

            log.LogDebug("DB session deleted={D1}/{D2} for user {UserId}", deleted1, deleted2, userId);
        }

        if (fileIOService.Exists(mainDbPath))
        {
            fileIOService.Copy(mainDbPath, sessionDbPath, overwrite: true);
            log.LogDebug("DB main copied to session for user {UserId}", userId);
        }

        await RunDbMigrations();

        flags.MainBanks.All = true;
        flags.MainBoxes.All = true;
        flags.MainPkmVariants.All = true;
        flags.Dex.All = true;
        flags.Warnings = true;
    }

    private async Task RunDbMigrations()
    {
        using var _ = log.Time("Data Migration + Clean + Seeding");

        using var scope = sp.CreateScope();
        using var db = scope.ServiceProvider.GetRequiredService<SessionDbContext>();

        var migrations = db.Database.GetMigrations();
        if (!migrations.Any())
        {
            throw new InvalidOperationException("No migration files");
        }

        var pendingMigrations = await db.Database.GetPendingMigrationsAsync();

        log.LogDebug("{Count} pending migrations", pendingMigrations.Count());

        fileIOService.CreateDirectoryIfAny(MainDbPath);
        fileIOService.CreateDirectoryIfAny(SessionDbPath);

        await db.Database.MigrateAsync();

        var appliedMigrations = await db.Database.GetAppliedMigrationsAsync();

        log.LogInformation("{Count} applied migrations", appliedMigrations.Count());
    }

    private async Task CloseConnection(string userId)
    {
        using var _ = log.Time("SessionService.CloseConnection");

        using var scope = sp.CreateScope();
        using var db = scope.ServiceProvider.GetRequiredService<SessionDbContext>();

        if (db.Database.GetDbConnection() is SqliteConnection sqliteConnection)
        {
            SqliteConnection.ClearPool(sqliteConnection);
        }

        await db.Database.CloseConnectionAsync();

        log.LogInformation("DB session connection closed for user {UserId}", userId);
    }
}
