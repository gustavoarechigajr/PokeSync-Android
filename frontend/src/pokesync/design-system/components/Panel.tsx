/** Rounded white surface — the base container for panes and cards. */
import { css, cx } from '@emotion/css';
import type React from 'react';
import { palette, radius, shadow, space } from '../tokens';

const base = css({
  background: palette.panel,
  borderRadius: radius.xl,
  boxShadow: shadow.panel,
  padding: space.lg,
  display: 'flex',
  flexDirection: 'column',
  minHeight: 0,
});

export const Panel: React.FC<React.PropsWithChildren<{ className?: string }>> = ({ children, className }) => (
  <div className={cx(base, className)}>{children}</div>
);
