using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using PKHeX.Android.Models;
using PKHeX.Android.Services;
using PKHeX.Core;

namespace PKHeX.Android.ViewModels;

public enum ViewMode { Boxes, Party, Items }

public sealed partial class BoxViewModel : BaseViewModel
{
    private readonly SaveContext _context;
    private readonly SaveFileService _saveService;
    private PlayerBag? _bag;

    public BoxViewModel(SaveContext context, SaveFileService saveService)
    {
        _context = context;
        _saveService = saveService;
        _context.SaveLoaded += OnSaveLoaded;
        Title = "Boxes";

        if (_context.IsLoaded)
            OnSaveLoaded();
    }

    [ObservableProperty] private ObservableCollection<BoxSlotViewModel> _slots = [];
    [ObservableProperty] private ObservableCollection<string> _boxNames = [];
    [ObservableProperty] private int _selectedBoxIndex;
    [ObservableProperty] private string _trainerName = string.Empty;
    [ObservableProperty] private string _gameName = string.Empty;
    [ObservableProperty] private int _trainerID;
    [ObservableProperty] private bool _isDirty;
    [ObservableProperty] private string? _statusMessage;
    [ObservableProperty] private BoxSlotViewModel? _selectedSlot;

    // ── View mode ──────────────────────────────────────────────────────────────
    [ObservableProperty] private ViewMode _currentView = ViewMode.Boxes;

    public bool IsBoxView   => CurrentView == ViewMode.Boxes;
    public bool IsPartyView => CurrentView == ViewMode.Party;
    public bool IsItemsView => CurrentView == ViewMode.Items;

    partial void OnCurrentViewChanged(ViewMode value)
    {
        OnPropertyChanged(nameof(IsBoxView));
        OnPropertyChanged(nameof(IsPartyView));
        OnPropertyChanged(nameof(IsItemsView));
        if (value == ViewMode.Party) LoadParty();
        if (value == ViewMode.Items) LoadItems();
    }

    [RelayCommand] private void ShowBoxView()   => CurrentView = ViewMode.Boxes;
    [RelayCommand] private void ShowPartyView() => CurrentView = ViewMode.Party;
    [RelayCommand] private void ShowItemsView() => CurrentView = ViewMode.Items;

    // ── Party ──────────────────────────────────────────────────────────────────
    [ObservableProperty] private ObservableCollection<BoxSlotViewModel> _partySlots = [];

    private void LoadParty()
    {
        var sav = _context.SaveFile;
        if (sav is null) return;
        PartySlots.Clear();
        int count = sav.PartyCount;
        for (int i = 0; i < 6; i++)
        {
            var pkm = i < count ? sav.GetPartySlotAtIndex(i) : null;
            PartySlots.Add(new BoxSlotViewModel(pkm, -1, i));
        }
    }

    // ── Items ──────────────────────────────────────────────────────────────────
    [ObservableProperty] private ObservableCollection<ItemPouchViewModel> _itemPouches = [];

    private void LoadItems()
    {
        var sav = _context.SaveFile;
        if (sav is null) return;
        _bag = sav.Inventory;
        var strings = GameInfo.GetStrings("en");
        ItemPouches.Clear();
        foreach (var pouch in _bag.Pouches)
        {
            var entries = pouch.Items
                .Where(it => it.Index > 0 && it.Index < strings.Item.Count)
                .Select(it => new ItemEntryViewModel(it, strings.Item[it.Index]))
                .ToList();
            if (entries.Count > 0)
                ItemPouches.Add(new ItemPouchViewModel(pouch.Type.ToString().Replace("_", " "), entries));
        }
    }

    private void FlushItems()
    {
        if (_bag is null || _context.SaveFile is null) return;
        foreach (var pouch in ItemPouches)
            foreach (var entry in pouch.Items)
                entry.ApplyToSource();
        _bag.CopyTo(_context.SaveFile);
    }

    // ── Box ────────────────────────────────────────────────────────────────────
    partial void OnSelectedBoxIndexChanged(int value) => LoadBox(value);

    private void OnSaveLoaded()
    {
        var sav = _context.SaveFile!;

        TrainerName = sav.OT;
        TrainerID = (int)sav.DisplayTID;
        GameName = GameInfo.GetVersionName(sav.Version) ?? sav.Version.ToString();
        IsDirty = false;
        _bag = null;
        CurrentView = ViewMode.Boxes;

        BoxNames.Clear();
        for (int i = 0; i < sav.BoxCount; i++)
        {
            var name = sav is IBoxDetailNameRead named
                ? named.GetBoxName(i)
                : BoxDetailNameExtensions.GetDefaultBoxName(i);
            BoxNames.Add(name);
        }

        SelectedBoxIndex = 0;
        LoadBox(0);
    }

    private void LoadBox(int boxIndex)
    {
        var sav = _context.SaveFile;
        if (sav is null) return;

        Slots.Clear();
        int perBox = sav.BoxSlotCount;
        for (int slot = 0; slot < perBox; slot++)
        {
            var pkm = sav.GetBoxSlotAtIndex(boxIndex, slot);
            Slots.Add(new BoxSlotViewModel(pkm, boxIndex, slot));
        }
    }

    [RelayCommand]
    private void SelectSlot(BoxSlotViewModel? slot)
    {
        if (slot is null) return;
        SelectedSlot = slot;
    }

    [RelayCommand]
    private async Task SaveToFileAsync()
    {
        IsBusy = true;
        StatusMessage = null;
        try
        {
            FlushItems();
            var (success, error) = await _saveService.SaveAsync();
            StatusMessage = success ? "Saved ✓" : $"Error: {error}";
            if (success) IsDirty = false;
        }
        finally
        {
            IsBusy = false;
            if (StatusMessage == "Saved ✓")
            {
                await Task.Delay(2000);
                StatusMessage = null;
            }
        }
    }

    public void MarkDirty() => IsDirty = true;

    public void RefreshCurrentView()
    {
        switch (CurrentView)
        {
            case ViewMode.Boxes: LoadBox(SelectedBoxIndex); break;
            case ViewMode.Party: LoadParty(); break;
        }
    }
}

// ── Supporting types ──────────────────────────────────────────────────────────

public sealed partial class BoxSlotViewModel : ObservableObject
{
    public PKM? Pokemon  { get; }
    public int BoxIndex  { get; }
    public int SlotIndex { get; }

    private static readonly GameStrings _strings = GameInfo.GetStrings("en");

    public bool IsEmpty      => Pokemon is null || Pokemon.Species == 0;
    public bool IsOccupied   => !IsEmpty;
    public string SpeciesName   => IsEmpty ? string.Empty : (_strings.Species[Pokemon!.Species] ?? $"#{Pokemon.Species}");
    public string SpeciesNumber => IsEmpty ? string.Empty : $"#{Pokemon!.Species:000}";
    public int    Species    => Pokemon?.Species ?? 0;
    public bool   IsShiny    => Pokemon?.IsShiny ?? false;
    public int    Level      => Pokemon?.CurrentLevel ?? 0;
    public string DisplayLevel => IsEmpty ? string.Empty : $"Lv.{Level}";

    public string SpriteUrl => IsEmpty ? string.Empty
        : IsShiny
            ? $"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/shiny/{Species}.png"
            : $"https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/{Species}.png";

    public BoxSlotViewModel(PKM? pkm, int box, int slot)
    {
        Pokemon  = pkm?.Species == 0 ? null : pkm;
        BoxIndex  = box;
        SlotIndex = slot;
    }
}

public sealed class ItemPouchViewModel(string name, List<ItemEntryViewModel> items)
{
    public string Name { get; } = name;
    public List<ItemEntryViewModel> Items { get; } = items;
}

public sealed partial class ItemEntryViewModel : ObservableObject
{
    private readonly InventoryItem _source;

    public string ItemName { get; }
    [ObservableProperty] private int _count;

    public ItemEntryViewModel(InventoryItem source, string name)
    {
        _source  = source;
        ItemName = name;
        _count   = source.Count;
    }

    public void ApplyToSource() => _source.Count = Count;
}
