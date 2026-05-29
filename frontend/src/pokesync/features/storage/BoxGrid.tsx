/** A box's slot grid. Pure presentation: receives the per-slot Pokémon array. */
import { css } from '@emotion/css';
import type React from 'react';
import type { PkmBaseDTO } from '../../../data/sdk/model';
import { Slot } from '../../design-system/components/Slot';
import { space } from '../../design-system/tokens';
import { PkmSprite } from './pkm-sprite';

type BoxGridProps = {
  slots: (PkmBaseDTO | undefined)[];
  columns?: number;
  selectedId?: string;
  focusedSlot?: number;
  onSelect: (pkm: PkmBaseDTO, slot: number) => void;
  onFocusSlot?: (slot: number) => void;
};

export const BoxGrid: React.FC<BoxGridProps> = ({
  slots, columns = 6, selectedId, focusedSlot, onSelect, onFocusSlot,
}) => (
  <div
    className={css({
      display: 'grid',
      gridTemplateColumns: `repeat(${columns}, 1fr)`,
      gap: space.sm,
      width: '100%',
    })}
  >
    {slots.map((pkm, slot) => (
      <Slot
        key={slot}
        filled={!!pkm}
        selected={!!pkm && pkm.id === selectedId}
        focused={focusedSlot === slot}
        shiny={pkm?.isShiny}
        onClick={() => pkm && onSelect(pkm, slot)}
        onPointerEnter={() => onFocusSlot?.(slot)}
      >
        {pkm && <PkmSprite pkm={pkm} small />}
      </Slot>
    ))}
  </div>
);
