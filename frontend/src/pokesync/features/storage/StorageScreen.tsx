/**
 * The main dual-pane storage screen: local Saves (left) ⇄ PKVault Vault (right).
 * Selecting a Pokémon shows its enriched Summary on the OPPOSITE pane (per the PokeSync mockups),
 * with a Transfer action. Works by touch AND controller/keyboard (see shared/navigation).
 */
import { css } from '@emotion/css';
import React from 'react';
import type { PkmBaseDTO, PkmSaveDTO, PkmVariantDTO } from '../../../data/sdk/model';
import { useStorageMovePkm } from '../../../data/sdk/storage/storage.gen';
import { useStaticData } from '../../../hooks/use-static-data';
import { getGameInfos } from '../../../pokedex/details/util/get-game-infos';
import { Pill } from '../../design-system/components/Pill';
import { font, palette, radius, space } from '../../design-system/tokens';
import { useBanks, useBoxesById, useSavePkms, useSaves, useVaultPkms } from '../../data/use-storage-data';
import { useInputIntents } from '../../shared/navigation/use-input-intents';
import type { NavIntent } from '../../shared/navigation/intents';
import { SummaryPanel } from '../summary/SummaryPanel';
import { BoxGrid } from './BoxGrid';
import { BoxPane } from './BoxPane';

const COLS = 6;

type Selection = { pane: 'saves' | 'vault'; pkm: PkmBaseDTO } | null;

const toSlots = (count: number, pick: (slot: number) => PkmBaseDTO | undefined) =>
  Array.from({ length: count }, (_, i) => pick(i));

const firstEmpty = (slots: (PkmBaseDTO | undefined)[]) => slots.findIndex(s => !s);

const EmptyHint: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <div className={css({
    height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center',
    textAlign: 'center', color: palette.inkFaint, fontWeight: 700, padding: space.xl,
  })}>{children}</div>
);

const ControlsBar: React.FC = () => (
  <div className={css({
    display: 'flex', justifyContent: 'center', gap: space.lg, flexWrap: 'wrap',
    padding: `6px ${space.lg}`, color: palette.white, fontFamily: font.display, fontWeight: 700,
    fontSize: 12, opacity: 0.92, textShadow: '0 1px 2px rgba(0,0,0,0.25)',
  })}>
    <span>✚ Move</span><span>Ⓐ Select</span><span>Ⓑ Back</span>
    <span>L/R Box</span><span>L2/R2 Switch side</span><span>Ⓧ Transfer</span>
  </div>
);

export const StorageScreen: React.FC = () => {
  const sd = useStaticData();
  const { banks } = useBanks();
  const { boxesById } = useBoxesById();
  const { saves } = useSaves();
  const { byBox: vaultByBox } = useVaultPkms();
  const moveMutation = useStorageMovePkm();

  const bank = banks.find(b => b.isDefault) ?? banks[0];
  // Vault boxes come from the boxes list (useStorageGetBoxes returns only main/vault boxes),
  // ordered — NOT from bank.view.mainBoxIds (which is empty).
  const vaultBoxes = Object.values(boxesById).sort((a, b) => a.order - b.order);

  const [vaultIdx, setVaultIdx] = React.useState(0);
  const [saveId, setSaveId] = React.useState<number | undefined>(undefined);
  const [saveIdx, setSaveIdx] = React.useState(0);
  const [selected, setSelected] = React.useState<Selection>(null);
  const [transferBusy, setTransferBusy] = React.useState(false);

  React.useEffect(() => {
    if (saveId == null && saves.length > 0) setSaveId(saves[0].id);
  }, [saves, saveId]);

  const { byBox: saveByBox } = useSavePkms(saveId);

  const vaultBox = vaultBoxes[vaultIdx];
  const vaultBoxId = vaultBox?.idInt;
  const vaultSlots = toSlots(vaultBox?.slotCount ?? 30, slot => (vaultByBox[vaultBoxId!]?.[slot]?.[0]) as PkmVariantDTO | undefined);

  // A save's boxes derive from its metadata + the Pokémon's boxId, not bank.view.saves (empty here).
  const currentSave = saves.find(s => s.id === saveId);
  const saveBoxCount = currentSave?.boxCount ?? 0;
  const saveSlotCount = currentSave?.boxSlotCount ?? 30;
  const saveSlots = toSlots(saveSlotCount, slot => (saveByBox[saveIdx]?.[slot]) as PkmSaveDTO | undefined);

  const gameName = (v: number | undefined) => (v != null ? sd.versions[v]?.name : undefined) ?? 'Save';

  const select = (pane: 'saves' | 'vault') => (pkm: PkmBaseDTO) =>
    setSelected(prev => (prev?.pkm.id === pkm.id ? null : { pane, pkm }));

  const summaryOn = (pane: 'saves' | 'vault') => selected && selected.pane !== pane;

  // ----- Transfer (save ⇄ vault) -----
  // Legality guard: a vault Pokémon can only go into a save whose game it's compatible with
  // (e.g. a Legends: Arceus Bidoof can't be placed into an Omega Ruby save).
  const targetVersion = currentSave?.displayedVersion;
  const incompatible =
    selected?.pane === 'vault' &&
    targetVersion != null &&
    Array.isArray((selected.pkm as PkmVariantDTO).compatibleWithVersions) &&
    !(selected.pkm as PkmVariantDTO).compatibleWithVersions.includes(targetVersion);
  const transferNote = incompatible ? `Can’t be moved into ${gameName(targetVersion)}` : undefined;

  const canTransfer = !!selected && !incompatible
    && (selected.pane === 'saves' ? vaultBoxId != null : saveId != null);

  const doTransfer = async () => {
    if (!selected || transferBusy) return;
    const fromSaves = selected.pane === 'saves';
    const targetSlots = fromSaves ? vaultSlots : saveSlots;
    const slot = firstEmpty(targetSlots);
    if (slot < 0) return; // target box full
    const targetBoxIdInt = fromSaves ? vaultBoxId : saveIdx; // save box id == box index
    if (targetBoxIdInt == null) return;

    setTransferBusy(true);
    try {
      await moveMutation.mutateAsync({
        params: {
          pkmIds: [selected.pkm.id],
          sourceSaveId: fromSaves ? saveId : undefined,
          targetSaveId: fromSaves ? undefined : saveId,
          targetBoxId: String(targetBoxIdInt),
          targetBoxSlots: [slot],
        },
      });
      setSelected(null); // cache update (via DataProvider onSettled) refreshes the grids
    } catch {
      /* errors surfaced by the global backend-error handler */
    } finally {
      setTransferBusy(false);
    }
  };

  // ----- Controller / keyboard navigation -----
  const [focus, setFocus] = React.useState<{ pane: 'saves' | 'vault'; index: number }>({ pane: 'vault', index: 0 });

  useInputIntents((intent: NavIntent) => {
    const slots = focus.pane === 'saves' ? saveSlots : vaultSlots;
    const count = slots.length || 1;
    switch (intent) {
      case 'right': setFocus(f => ({ ...f, index: Math.min(count - 1, f.index + 1) })); break;
      case 'left': setFocus(f => ({ ...f, index: Math.max(0, f.index - 1) })); break;
      case 'down': setFocus(f => ({ ...f, index: Math.min(count - 1, f.index + COLS) })); break;
      case 'up': setFocus(f => ({ ...f, index: Math.max(0, f.index - COLS) })); break;
      case 'switchPane': setFocus(f => ({ pane: f.pane === 'saves' ? 'vault' : 'saves', index: 0 })); break;
      case 'confirm': { const pkm = slots[focus.index]; if (pkm) select(focus.pane)(pkm); break; }
      case 'back': setSelected(null); break;
      case 'transfer': if (canTransfer) doTransfer(); break;
      case 'prevBox':
        if (focus.pane === 'saves') setSaveIdx(i => Math.max(0, i - 1));
        else setVaultIdx(i => Math.max(0, i - 1));
        break;
      case 'nextBox':
        if (focus.pane === 'saves') setSaveIdx(i => Math.min(saveBoxCount - 1, i + 1));
        else setVaultIdx(i => Math.min(vaultBoxes.length - 1, i + 1));
        break;
    }
  });

  const savesTitle = saves.length > 0 ? (
    <div className={css({ display: 'flex', alignItems: 'center', gap: 6, maxWidth: '100%' })}>
      {currentSave && <img src={getGameInfos(currentSave.displayedVersion).img} alt="" className={css({ width: 26, height: 26, objectFit: 'contain', flexShrink: 0 })} />}
      <select
        value={saveId ?? ''}
        onChange={e => { setSaveId(Number(e.target.value)); setSaveIdx(0); setSelected(null); }}
        className={css({
          appearance: 'none', border: 'none', cursor: 'pointer',
          background: palette.tealDeep, color: palette.white,
          fontFamily: font.display, fontWeight: 700, fontSize: 14,
          borderRadius: radius.pill, padding: `${space.sm} ${space.lg}`, maxWidth: 240,
        })}
      >
        {saves.map(s => (
          <option key={s.id} value={s.id}>{gameName(s.displayedVersion)} · {s.trainerName || s.id}</option>
        ))}
      </select>
    </div>
  ) : <Pill tone="dark">Local saves</Pill>;

  const transferLabel = selected?.pane === 'saves' ? 'Transfer to Vault ▸' : '◂ Transfer to Save';

  return (
    <div className={css({ height: '100%', display: 'flex', flexDirection: 'column' })}>
      <div className={css({
        flex: 1, minHeight: 0, display: 'flex', gap: space.lg, padding: `0 ${space.lg} ${space.sm}`,
        boxSizing: 'border-box',
      })}>
        {/* Saves pane */}
        <BoxPane
          title={savesTitle}
          boxLabel={saves.length > 0 ? `Box ${saveIdx + 1} / ${saveBoxCount}` : undefined}
          showNav={saves.length > 0 && !summaryOn('saves')}
          onPrev={() => setSaveIdx(i => Math.max(0, i - 1))}
          onNext={() => setSaveIdx(i => Math.min(saveBoxCount - 1, i + 1))}
          canPrev={saveIdx > 0}
          canNext={saveIdx < saveBoxCount - 1}
        >
          {summaryOn('saves') && selected
            ? <SummaryPanel pkm={selected.pkm} onTransfer={canTransfer ? doTransfer : undefined} transferLabel={transferLabel} transferBusy={transferBusy} transferNote={transferNote} />
            : saves.length === 0
              ? <EmptyHint>No saves yet.<br />Tap “Import save” to add an emulator save.</EmptyHint>
              : <BoxGrid
                  slots={saveSlots}
                  selectedId={selected?.pkm.id}
                  focusedSlot={focus.pane === 'saves' ? focus.index : undefined}
                  onSelect={select('saves')}
                  onFocusSlot={slot => setFocus({ pane: 'saves', index: slot })}
                />}
        </BoxPane>

        {/* Vault pane */}
        <BoxPane
          title={<Pill>{bank?.name ?? 'Vault'}{vaultBox ? ` · ${vaultBox.name}` : ''}</Pill>}
          boxLabel={vaultBoxes.length > 0 ? `Box ${vaultIdx + 1} / ${vaultBoxes.length}` : undefined}
          showNav={!summaryOn('vault') && vaultBoxes.length > 0}
          onPrev={() => setVaultIdx(i => Math.max(0, i - 1))}
          onNext={() => setVaultIdx(i => Math.min(vaultBoxes.length - 1, i + 1))}
          canPrev={vaultIdx > 0}
          canNext={vaultIdx < vaultBoxes.length - 1}
        >
          {summaryOn('vault') && selected
            ? <SummaryPanel pkm={selected.pkm} onTransfer={canTransfer ? doTransfer : undefined} transferLabel={transferLabel} transferBusy={transferBusy} transferNote={transferNote} />
            : <BoxGrid
                slots={vaultSlots}
                selectedId={selected?.pkm.id}
                focusedSlot={focus.pane === 'vault' ? focus.index : undefined}
                onSelect={select('vault')}
                onFocusSlot={slot => setFocus({ pane: 'vault', index: slot })}
              />}
        </BoxPane>
      </div>
      <ControlsBar />
    </div>
  );
};
