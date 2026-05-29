/**
 * A single box slot. Presentational: pass the sprite (e.g. <SpeciesImg/>) as children.
 * States: empty (flat teal), filled, selected (teal ring), focused (HOME-style yellow cursor ring).
 */
import { css, cx } from '@emotion/css';
import type React from 'react';
import { palette, radius, shadow } from '../tokens';

type SlotProps = {
  filled?: boolean;
  selected?: boolean;
  focused?: boolean;
  shiny?: boolean;
  onClick?: () => void;
  onPointerEnter?: () => void;
} & React.PropsWithChildren;

export const Slot: React.FC<SlotProps> = ({ filled, selected, focused, shiny, onClick, onPointerEnter, children }) => (
  <div
    onClick={onClick}
    onPointerEnter={onPointerEnter}
    className={cx(
      css({
        position: 'relative',
        aspectRatio: '1 / 1',
        borderRadius: radius.md,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: filled
          ? `linear-gradient(160deg, ${palette.white}, ${palette.mint})`
          : palette.slotEmpty,
        boxShadow: filled ? shadow.raised : shadow.inset,
        border: `2px solid ${selected ? palette.tealDark : 'transparent'}`,
        transition: 'transform 90ms ease, box-shadow 120ms ease',
        cursor: 'pointer',
        ':active': { transform: 'scale(0.94)' },
      }),
      focused && css({
        boxShadow: shadow.cursor,
        transform: 'scale(1.04)',
        zIndex: 1,
      }),
    )}
  >
    {children}
    {shiny && filled && (
      <span
        className={css({
          position: 'absolute',
          top: 2,
          right: 4,
          fontSize: 11,
          color: palette.shiny,
          textShadow: '0 1px 1px rgba(0,0,0,0.4)',
        })}
      >
        ★
      </span>
    )}
  </div>
);
