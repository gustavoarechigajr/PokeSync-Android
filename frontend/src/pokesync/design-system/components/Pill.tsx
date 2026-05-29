/** Pill-shaped title/label bar — used for box titles, the save selector, section headers. */
import { css, cx } from '@emotion/css';
import type React from 'react';
import { font, palette, radius, space } from '../tokens';

type PillProps = {
  tone?: 'teal' | 'dark' | 'light';
  size?: 'sm' | 'md';
  className?: string;
  onClick?: () => void;
} & React.PropsWithChildren;

const tones = {
  teal: { background: palette.teal, color: palette.white },
  dark: { background: palette.tealDeep, color: palette.white },
  light: { background: palette.mint, color: palette.tealDeep },
} as const;

export const Pill: React.FC<PillProps> = ({ children, tone = 'teal', size = 'md', className, onClick }) => (
  <div
    onClick={onClick}
    className={cx(
      css({
        ...tones[tone],
        fontFamily: font.display,
        fontWeight: 700,
        borderRadius: radius.pill,
        padding: size === 'sm' ? `${space.xs} ${space.md}` : `${space.sm} ${space.xl}`,
        fontSize: size === 'sm' ? 13 : 16,
        textAlign: 'center',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: space.sm,
        whiteSpace: 'nowrap',
        cursor: onClick ? 'pointer' : 'default',
        userSelect: 'none',
      }),
      className,
    )}
  >
    {children}
  </div>
);
