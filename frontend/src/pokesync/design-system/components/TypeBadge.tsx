/** Colored pill for a Pokémon type or a move's type. */
import { css } from '@emotion/css';
import type React from 'react';
import { font, palette, radius } from '../tokens';
import { typeColor } from '../tokens';

export const TypeBadge: React.FC<{ name: string; small?: boolean }> = ({ name, small }) => (
  <span
    className={css({
      background: typeColor(name),
      color: palette.white,
      fontFamily: font.display,
      fontWeight: 700,
      textTransform: 'uppercase',
      letterSpacing: 0.4,
      fontSize: small ? 10 : 12,
      padding: small ? '2px 8px' : '3px 10px',
      borderRadius: radius.pill,
      textShadow: '0 1px 1px rgba(0,0,0,0.25)',
      display: 'inline-block',
    })}
  >
    {name}
  </span>
);
