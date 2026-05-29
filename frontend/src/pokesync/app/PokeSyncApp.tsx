/**
 * Root of the new PokeSync UI. Rendered inside the existing providers (DataProvider + SplashMain),
 * so react-query, settings and static data are already available.
 *
 * To add a screen: build it under features/, then route to it here (a real router/nav comes with the
 * navigation slice — for now the storage screen is the single surface). See src/pokesync/README.md.
 */
import '../design-system/global';

import { css } from '@emotion/css';
import type React from 'react';
import { font, palette, space } from '../design-system/tokens';
import { StorageScreen } from '../features/storage/StorageScreen';

const AppBar: React.FC = () => (
  <header className={css({
    display: 'flex', alignItems: 'center', gap: space.sm,
    padding: `${space.sm} ${space.lg}`, flexShrink: 0,
  })}>
    <span className={css({
      width: 26, height: 26, borderRadius: '50%', flexShrink: 0,
      background: `conic-gradient(${palette.coral} 0 50%, ${palette.white} 50% 100%)`,
      border: `3px solid ${palette.ink}`, boxShadow: '0 2px 6px rgba(0,0,0,0.25)',
    })} />
    <span className={css({
      fontFamily: font.display, fontWeight: 800, fontSize: 24, color: palette.white,
      textShadow: '0 2px 4px rgba(0,0,0,0.25)', letterSpacing: 0.5,
    })}>
      Poke<span className={css({ color: palette.yellow })}>Sync</span>
    </span>
  </header>
);

export const PokeSyncApp: React.FC = () => (
  <div className={css({ height: '100%', display: 'flex', flexDirection: 'column' })}>
    <AppBar />
    <main className={css({ flex: 1, minHeight: 0 })}>
      <StorageScreen />
    </main>
  </div>
);
