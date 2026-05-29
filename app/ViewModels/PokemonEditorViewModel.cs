using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using CommunityToolkit.Mvvm.Messaging;
using PKHeX.Android.Models;
using PKHeX.Core;

namespace PKHeX.Android.ViewModels;

[QueryProperty(nameof(BoxIndex), "boxIndex")]
[QueryProperty(nameof(SlotIndex), "slotIndex")]
[QueryProperty(nameof(PartyIndex), "partyIndex")]
public sealed partial class PokemonEditorViewModel : BaseViewModel,
    IRecipient<MoveSelectedMessage>
{
    private readonly SaveContext _context;
    private PKM? _pkm;

    public PokemonEditorViewModel(SaveContext context)
    {
        _context = context;
        Title = "Pokémon";
        WeakReferenceMessenger.Default.Register(this);
    }

    // ── Route params ────────────────────────────────────────────────────────
    private bool _boxSet, _slotSet;
    [ObservableProperty] private int _boxIndex;
    [ObservableProperty] private int _slotIndex;
    [ObservableProperty] private int _partyIndex = -1;

    partial void OnBoxIndexChanged(int value)   { _boxSet  = true; TryLoad(); }
    partial void OnSlotIndexChanged(int value)  { _slotSet = true; TryLoad(); }
    partial void OnPartyIndexChanged(int value) { if (value >= 0) LoadPokemon(); }
    private void TryLoad() { if (_boxSet && _slotSet) LoadPokemon(); }

    // ── Core display ────────────────────────────────────────────────────────
    [ObservableProperty] private string _speciesName = string.Empty;
    [ObservableProperty] private string _nickname = string.Empty;
    [ObservableProperty] private int _species;
    [ObservableProperty] private bool _isShiny;
    [ObservableProperty] private string _originalTrainer = string.Empty;
    [ObservableProperty] private string _heldItem = string.Empty;
    [ObservableProperty] private string _ballName = string.Empty;
    [ObservableProperty] private string _type1 = string.Empty;
    [ObservableProperty] private string _type2 = string.Empty;
    [ObservableProperty] private bool _hasType2;
    [ObservableProperty] private string _spriteUrl = string.Empty;

    // ── Editable scalars ────────────────────────────────────────────────────
    [ObservableProperty] private int _level;
    [ObservableProperty] private int _friendship;

    // ── Species picker ──────────────────────────────────────────────────────
    [ObservableProperty] private ObservableCollection<SpeciesEntry> _speciesList = [];
    [ObservableProperty] private SpeciesEntry? _selectedSpecies;

    partial void OnSelectedSpeciesChanged(SpeciesEntry? value)
    {
        if (value is null || _pkm is null || value.Id == _pkm.Species) return;
        _pkm.Species = value.Id;
        _pkm.Form = 0;
        _pkm.ResetPartyStats();
        LoadPokemon();
    }

    // ── Nature picker ────────────────────────────────────────────────────────
    [ObservableProperty] private ObservableCollection<NatureEntry> _natures = [];
    [ObservableProperty] private NatureEntry? _selectedNature;

    partial void OnSelectedNatureChanged(NatureEntry? value)
    {
        if (value is null || _pkm is null) return;
        _pkm.Nature = value.Value;
        _pkm.StatNature = value.Value;
        _pkm.ResetPartyStats();
        RefreshStats();
    }

    // ── Ability picker ───────────────────────────────────────────────────────
    [ObservableProperty] private ObservableCollection<AbilityEntry> _abilities = [];
    [ObservableProperty] private AbilityEntry? _selectedAbility;

    partial void OnSelectedAbilityChanged(AbilityEntry? value)
    {
        if (value is null || _pkm is null) return;
        _pkm.Ability = value.AbilityId;
        _pkm.AbilityNumber = value.Slot;
    }

    // ── Stats ────────────────────────────────────────────────────────────────
    [ObservableProperty] private ObservableCollection<StatRowViewModel> _stats = [];

    // ── Moves ────────────────────────────────────────────────────────────────
    [ObservableProperty] private ObservableCollection<MoveViewModel> _moves = [];

    // ── Legality ─────────────────────────────────────────────────────────────
    [ObservableProperty] private bool _isLegal;
    [ObservableProperty] private string _legalityResult = string.Empty;

    // ── Is new (empty slot) ──────────────────────────────────────────────────
    [ObservableProperty] private bool _isNewPokemon;

    // ────────────────────────────────────────────────────────────────────────

    private void LoadPokemon()
    {
        var sav = _context.SaveFile;
        if (sav is null) return;

        _pkm = PartyIndex >= 0
            ? sav.GetPartySlotAtIndex(PartyIndex)
            : sav.GetBoxSlotAtIndex(BoxIndex, SlotIndex);

        var strings = GameInfo.GetStrings("en");

        // Empty slot — show species picker so user can add a new Pokémon
        if (_pkm.Species == 0)
        {
            IsNewPokemon = true;
            Title = "New Pokémon";
            SpeciesName = string.Empty;
            Nickname = string.Empty;
            Level = 1;
            IsShiny = false;
            OriginalTrainer = string.Empty;
            HeldItem = string.Empty;
            BallName = string.Empty;
            Type1 = string.Empty;
            Type2 = string.Empty;
            HasType2 = false;
            SpriteUrl = string.Empty;
            Friendship = 0;
            Stats.Clear();
            Moves.Clear();
            IsLegal = false;
            LegalityResult = string.Empty;
            BuildSpeciesList(strings, sav);
            BuildNatureList(strings);
            return;
        }

        IsNewPokemon = false;
        Species = _pkm.Species;
        SpeciesName = _pkm.Species < strings.Species.Count ? strings.Species[_pkm.Species] : $"#{_pkm.Species}";
        Nickname = _pkm.Nickname;
        Level = _pkm.CurrentLevel;
        IsShiny = _pkm.IsShiny;
        OriginalTrainer = _pkm.OriginalTrainerName;
        Friendship = _pkm.CurrentFriendship;
        HeldItem = _pkm.HeldItem < strings.Item.Count ? strings.Item[_pkm.HeldItem] : string.Empty;
        BallName = ((Ball)_pkm.Ball).ToString().Replace("_", " ");
        Title = SpeciesName;

        SpriteUrl = IsShiny
            ? $"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/shiny/{Species}.png"
            : $"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/{Species}.png";

        var pi = _pkm.PersonalInfo;
        Type1 = pi.Type1 < strings.Types.Count ? strings.Types[pi.Type1] : string.Empty;
        if (pi.Type2 != pi.Type1) { Type2 = pi.Type2 < strings.Types.Count ? strings.Types[pi.Type2] : string.Empty; HasType2 = true; }
        else { HasType2 = false; }

        BuildSpeciesList(strings, sav);
        BuildNatureList(strings);
        BuildAbilityList(strings, pi);
        LoadStats();
        LoadMoves(strings);
        CheckLegality();
    }

    private void BuildSpeciesList(GameStrings strings, SaveFile sav)
    {
        if (SpeciesList.Count > 0) return;
        SpeciesList.Clear();
        ushort max = sav.MaxSpeciesID;
        for (ushort i = 1; i <= max; i++)
        {
            var name = i < strings.Species.Count ? strings.Species[i] : $"#{i}";
            if (!string.IsNullOrWhiteSpace(name)) SpeciesList.Add(new SpeciesEntry(i, $"#{i:000} {name}"));
        }
        SelectedSpecies = SpeciesList.FirstOrDefault(s => s.Id == _pkm!.Species);
    }

    private void BuildNatureList(GameStrings strings)
    {
        if (Natures.Count > 0)
        {
            SelectedNature = Natures.FirstOrDefault(n => n.Value == _pkm!.StatNature);
            return;
        }
        Natures.Clear();
        for (int i = 0; i < 25; i++)
        {
            var name = i < strings.Natures.Count ? strings.Natures[i] : $"Nature {i}";
            Natures.Add(new NatureEntry((Nature)i, name));
        }
        SelectedNature = Natures.FirstOrDefault(n => n.Value == _pkm!.StatNature);
    }

    private void BuildAbilityList(GameStrings strings, PersonalInfo pi)
    {
        Abilities.Clear();
        int count = pi.AbilityCount;
        string[] slotLabels = ["Ability 1", "Ability 2", "Hidden"];
        var seen = new HashSet<int>();
        for (int i = 0; i < count && i < 3; i++)
        {
            int id = pi.GetAbilityAtIndex(i);
            if (id == 0 || !seen.Add(id)) continue;
            var name = id < strings.Ability.Count ? strings.Ability[id] : $"#{id}";
            Abilities.Add(new AbilityEntry(id, $"{name} ({slotLabels[i]})", 1 << i));
        }
        if (Abilities.Count == 0)
        {
            var name = _pkm!.Ability < strings.Ability.Count ? strings.Ability[_pkm.Ability] : $"#{_pkm.Ability}";
            Abilities.Add(new AbilityEntry(_pkm.Ability, name, 1));
        }
        SelectedAbility = Abilities.FirstOrDefault(a => a.AbilityId == _pkm!.Ability) ?? Abilities.FirstOrDefault();
    }

    private void LoadStats()
    {
        Stats.Clear();
        var pkm = _pkm!;
        pkm.ResetPartyStats(); // always recalculate — box Pokémon have zeroed party stats in raw data
        (string Name, int StatVal, int IV, int EV, int Base)[] rows =
        [
            ("HP",  pkm.Stat_HPCurrent, pkm.IV_HP,  pkm.EV_HP,  pkm.PersonalInfo.HP),
            ("Atk", pkm.Stat_ATK,       pkm.IV_ATK, pkm.EV_ATK, pkm.PersonalInfo.ATK),
            ("Def", pkm.Stat_DEF,       pkm.IV_DEF, pkm.EV_DEF, pkm.PersonalInfo.DEF),
            ("SpA", pkm.Stat_SPA,       pkm.IV_SPA, pkm.EV_SPA, pkm.PersonalInfo.SPA),
            ("SpD", pkm.Stat_SPD,       pkm.IV_SPD, pkm.EV_SPD, pkm.PersonalInfo.SPD),
            ("Spe", pkm.Stat_SPE,       pkm.IV_SPE, pkm.EV_SPE, pkm.PersonalInfo.SPE),
        ];
        foreach (var r in rows)
            Stats.Add(new StatRowViewModel(r.Name, r.StatVal, r.IV, r.EV, r.Base));
    }

    private void RefreshStats()
    {
        var pkm = _pkm!;
        pkm.ResetPartyStats();
        int[] vals = [pkm.Stat_HPCurrent, pkm.Stat_ATK, pkm.Stat_DEF, pkm.Stat_SPA, pkm.Stat_SPD, pkm.Stat_SPE];
        for (int i = 0; i < Stats.Count; i++)
            Stats[i].Value = vals[i];
    }

    private void LoadMoves(GameStrings strings)
    {
        Moves.Clear();
        (int Id, int PP)[] slots =
        [
            (_pkm!.Move1, _pkm.Move1_PP),
            (_pkm.Move2, _pkm.Move2_PP),
            (_pkm.Move3, _pkm.Move3_PP),
            (_pkm.Move4, _pkm.Move4_PP),
        ];
        for (int i = 0; i < 4; i++)
        {
            var name = slots[i].Id > 0 && slots[i].Id < strings.Move.Count
                ? strings.Move[slots[i].Id]
                : "---";
            Moves.Add(new MoveViewModel(i, name, slots[i].PP, (ushort)slots[i].Id));
        }
    }

    private void CheckLegality()
    {
        if (_pkm is null) return;
        var la = new LegalityAnalysis(_pkm);
        IsLegal = la.Valid;
        LegalityResult = la.Report();
    }

    [RelayCommand]
    private async Task OpenMovePickerAsync(MoveViewModel move)
    {
        await Shell.Current.GoToAsync(
            $"movepicker?moveIndex={move.SlotIndex}&currentMoveId={move.MoveId}");
    }

    public void Receive(MoveSelectedMessage msg)
    {
        if (_pkm is null) return;
        var strings = GameInfo.GetStrings("en");
        switch (msg.MoveIndex)
        {
            case 0: _pkm.Move1 = msg.MoveId; _pkm.Move1_PP = _pkm.GetMovePP(msg.MoveId, 0); break;
            case 1: _pkm.Move2 = msg.MoveId; _pkm.Move2_PP = _pkm.GetMovePP(msg.MoveId, 0); break;
            case 2: _pkm.Move3 = msg.MoveId; _pkm.Move3_PP = _pkm.GetMovePP(msg.MoveId, 0); break;
            case 3: _pkm.Move4 = msg.MoveId; _pkm.Move4_PP = _pkm.GetMovePP(msg.MoveId, 0); break;
        }
        LoadMoves(strings);
        CheckLegality();
    }

    [RelayCommand]
    private async Task SaveChangesAsync()
    {
        if (_pkm is null || _context.SaveFile is null) return;
        if (_pkm.Species == 0) return; // nothing to save for truly empty slot

        _pkm.Nickname = Nickname;
        _pkm.CurrentFriendship = (byte)Math.Clamp(Friendship, 0, 255);
        _pkm.CurrentLevel = (byte)Math.Clamp(Level, 1, 100);

        var s = Stats;
        if (s.Count == 6)
        {
            _pkm.IV_HP  = Math.Clamp(s[0].IV, 0, 31); _pkm.EV_HP  = Math.Clamp(s[0].EV, 0, 252);
            _pkm.IV_ATK = Math.Clamp(s[1].IV, 0, 31); _pkm.EV_ATK = Math.Clamp(s[1].EV, 0, 252);
            _pkm.IV_DEF = Math.Clamp(s[2].IV, 0, 31); _pkm.EV_DEF = Math.Clamp(s[2].EV, 0, 252);
            _pkm.IV_SPA = Math.Clamp(s[3].IV, 0, 31); _pkm.EV_SPA = Math.Clamp(s[3].EV, 0, 252);
            _pkm.IV_SPD = Math.Clamp(s[4].IV, 0, 31); _pkm.EV_SPD = Math.Clamp(s[4].EV, 0, 252);
            _pkm.IV_SPE = Math.Clamp(s[5].IV, 0, 31); _pkm.EV_SPE = Math.Clamp(s[5].EV, 0, 252);
        }

        _pkm.ResetPartyStats();
        if (PartyIndex >= 0)
            _context.SaveFile.SetPartySlotAtIndex(_pkm, PartyIndex);
        else
            _context.SaveFile.SetBoxSlotAtIndex(_pkm, BoxIndex, SlotIndex);

        CheckLegality();
        await Shell.Current.GoToAsync("..");
    }
}

// ── Supporting types ─────────────────────────────────────────────────────────

public sealed record SpeciesEntry(ushort Id, string DisplayName);
public sealed record NatureEntry(Nature Value, string Name);
public sealed record AbilityEntry(int AbilityId, string DisplayName, int Slot);

public sealed partial class StatRowViewModel(string name, int value, int iv, int ev, int @base)
    : ObservableObject
{
    public string Name { get; } = name;
    public int Base { get; } = @base;

    [ObservableProperty] private int _value = value;
    [ObservableProperty] private int _iV = iv;
    [ObservableProperty] private int _eV = ev;

    public double FillRatio => Math.Min(Value / 255.0, 1.0);

    partial void OnIVChanged(int v) => OnPropertyChanged(nameof(FillRatio));
    partial void OnValueChanged(int v) => OnPropertyChanged(nameof(FillRatio));
}

public sealed class MoveViewModel(int slotIndex, string name, int pp, ushort moveId)
{
    public int    SlotIndex { get; } = slotIndex;
    public string Name      { get; } = name;
    public int    PP        { get; } = pp;
    public ushort MoveId    { get; } = moveId;
    public bool   IsEmpty   => MoveId == 0;
}
