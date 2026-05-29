using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using CommunityToolkit.Mvvm.Messaging;
using PKHeX.Core;

namespace PKHeX.Android.ViewModels;

public sealed record MoveSelectedMessage(int MoveIndex, ushort MoveId, string MoveName);

[QueryProperty(nameof(MoveIndex), "moveIndex")]
[QueryProperty(nameof(CurrentMoveId), "currentMoveId")]
public sealed partial class MovePickerViewModel : BaseViewModel
{
    private readonly List<MoveEntry> _allMoves;

    [ObservableProperty] private int _moveIndex;
    [ObservableProperty] private int _currentMoveId;
    [ObservableProperty] private string _searchText = string.Empty;
    [ObservableProperty] private ObservableCollection<MoveEntry> _filteredMoves = [];

    public MovePickerViewModel()
    {
        Title = "Choose Move";
        var strings = GameInfo.GetStrings("en");
        _allMoves = [];
        _allMoves.Add(new MoveEntry(0, "--- (None)"));
        for (ushort i = 1; i < strings.Move.Count; i++)
        {
            var name = strings.Move[i];
            if (!string.IsNullOrWhiteSpace(name))
                _allMoves.Add(new MoveEntry(i, name));
        }
        ApplyFilter();
    }

    partial void OnSearchTextChanged(string value) => ApplyFilter();

    private void ApplyFilter()
    {
        var q = SearchText.Trim();
        FilteredMoves.Clear();
        var matches = string.IsNullOrEmpty(q)
            ? _allMoves
            : _allMoves.Where(m => m.Name.Contains(q, StringComparison.OrdinalIgnoreCase));
        foreach (var m in matches)
            FilteredMoves.Add(m);
    }

    [RelayCommand]
    private async Task SelectMoveAsync(MoveEntry? entry)
    {
        if (entry is null) return;
        WeakReferenceMessenger.Default.Send(new MoveSelectedMessage(MoveIndex, entry.Id, entry.Name));
        await Shell.Current.GoToAsync("..");
    }
}

public sealed record MoveEntry(ushort Id, string Name);
