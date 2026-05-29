/** A pane (saves or vault): header with box navigation (+ L/R bumper labels + box number), then content. */
import { css } from '@emotion/css';
import type React from 'react';
import { IconButton } from '../../design-system/components/IconButton';
import { Panel } from '../../design-system/components/Panel';
import { font, palette, radius, space } from '../../design-system/tokens';

type BoxPaneProps = {
  title: React.ReactNode;       // a Pill, or the save selector (icon + <select>)
  boxLabel?: string;            // e.g. "3 / 32"
  onPrev?: () => void;
  onNext?: () => void;
  canPrev?: boolean;
  canNext?: boolean;
  showNav?: boolean;
} & React.PropsWithChildren;

/** Small bumper badge ("L"/"R") shown next to the box arrows, à la Pokémon HOME. */
const Bumper: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <span className={css({
    fontFamily: font.display, fontWeight: 800, fontSize: 11, color: palette.tealDark,
    background: palette.mint, border: `1.5px solid ${palette.tealLight}`,
    borderRadius: radius.sm, padding: '1px 6px', lineHeight: 1.3,
  })}>{children}</span>
);

export const BoxPane: React.FC<BoxPaneProps> = ({
  title, boxLabel, onPrev, onNext, canPrev = true, canNext = true, showNav = true, children,
}) => (
  <Panel className={css({ flex: 1, minWidth: 0, gap: space.sm })}>
    <div className={css({ display: 'flex', alignItems: 'center', gap: space.sm, justifyContent: 'space-between' })}>
      {showNav
        ? <div className={css({ display: 'flex', alignItems: 'center', gap: 4 })}>
            <Bumper>L</Bumper>
            <IconButton label="Previous box" onClick={onPrev} disabled={!canPrev} size={38}>‹</IconButton>
          </div>
        : <span />}

      <div className={css({ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2, minWidth: 0 })}>
        {title}
        {boxLabel && (
          <span className={css({ fontFamily: font.display, fontWeight: 700, fontSize: 11, color: palette.inkFaint })}>
            {boxLabel}
          </span>
        )}
      </div>

      {showNav
        ? <div className={css({ display: 'flex', alignItems: 'center', gap: 4 })}>
            <IconButton label="Next box" onClick={onNext} disabled={!canNext} size={38}>›</IconButton>
            <Bumper>R</Bumper>
          </div>
        : <span />}
    </div>
    <div className={css({ flex: 1, minHeight: 0, overflow: 'auto' })}>{children}</div>
  </Panel>
);
