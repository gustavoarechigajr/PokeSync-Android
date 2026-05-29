/**
 * PokeSync data layer — thin, well-named hooks over the existing Orval SDK + react-query.
 *
 * We deliberately reuse the upstream data plumbing (SDK fetchers, query keys, and the box/slot
 * indexing in src/data/hooks/use-pkm-*-index) rather than reinventing it. New UI = new presentation,
 * same data layer. To add a screen, prefer composing these hooks.
 */
import { usePkmSaveIndex } from '../../data/hooks/use-pkm-save-index';
import { usePkmVariantIndex } from '../../data/hooks/use-pkm-variant-index';
import type { BankDTO, BoxDTO, PkmSaveDTO, PkmVariantDTO, SaveInfosDTO } from '../../data/sdk/model';
import { useSaveInfosGetAll } from '../../data/sdk/save-infos/save-infos.gen';
import { useStorageGetBoxes, useStorageGetMainBanks } from '../../data/sdk/storage/storage.gen';

/** Banks (each carries `view.mainBoxIds` for the vault and `view.saves` for save box mappings). */
export const useBanks = () => {
  const q = useStorageGetMainBanks();
  return { banks: (q.data?.data ?? []) as BankDTO[], isLoading: q.isLoading };
};

/** All boxes (vault + save), looked up by their integer id. */
export const useBoxesById = () => {
  const q = useStorageGetBoxes();
  const list = (q.data?.data ?? []) as BoxDTO[];
  const byId: Record<number, BoxDTO> = {};
  list.forEach(b => { byId[b.idInt] = b; });
  return { boxesById: byId, isLoading: q.isLoading };
};

/** Imported saves, as a sorted list. */
export const useSaves = () => {
  const q = useSaveInfosGetAll();
  const raw = (q.data?.data ?? {}) as Record<string, SaveInfosDTO>;
  const list = Object.values(raw).sort((a, b) => a.id - b.id);
  return { saves: list, isLoading: q.isLoading };
};

/** Vault Pokémon indexed by box id then slot (array per slot; first is the canonical variant). */
export const useVaultPkms = () => {
  const q = usePkmVariantIndex();
  return { byBox: q.data?.data.byBox ?? {}, isLoading: q.isLoading };
};

/** Save Pokémon for one save, indexed by box id then slot. */
export const useSavePkms = (saveId: number | undefined) => {
  const q = usePkmSaveIndex(saveId ?? 0);
  return { byBox: q.data?.data.byBox ?? {}, isLoading: q.isLoading };
};

export type AnyPkm = PkmVariantDTO | PkmSaveDTO;
