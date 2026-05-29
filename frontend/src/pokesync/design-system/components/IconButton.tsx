/** Circular icon button (nav arrows, menu, grid toggle). Touch-sized, with a focused state. */
import { css, cx } from '@emotion/css';
import type React from 'react';
import { palette, shadow, TOUCH } from '../tokens';

type IconButtonProps = {
  onClick?: () => void;
  disabled?: boolean;
  focused?: boolean;
  tone?: 'teal' | 'white';
  label: string;
  size?: number;
} & React.PropsWithChildren;

export const IconButton: React.FC<IconButtonProps> = ({
  children, onClick, disabled, focused, tone = 'white', label, size = TOUCH,
}) => (
  <button
    aria-label={label}
    disabled={disabled}
    onClick={onClick}
    className={cx(
      css({
        width: size,
        height: size,
        borderRadius: '50%',
        border: 'none',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: tone === 'teal' ? palette.teal : palette.white,
        color: tone === 'teal' ? palette.white : palette.tealDark,
        boxShadow: focused ? shadow.cursor : shadow.raised,
        fontSize: size * 0.42,
        lineHeight: 1,
        transition: 'transform 90ms ease, box-shadow 120ms ease, opacity 120ms',
        opacity: disabled ? 0.4 : 1,
        ':active': { transform: disabled ? undefined : 'scale(0.92)' },
      }),
    )}
  >
    {children}
  </button>
);
