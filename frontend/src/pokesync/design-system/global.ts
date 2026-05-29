/**
 * Global styles + self-hosted fonts for the PokeSync UI.
 * Imported once from PokeSyncApp. Fonts are bundled via @fontsource (offline-friendly — the app
 * runs with no network).
 */
import '@fontsource/baloo-2/400.css';
import '@fontsource/baloo-2/600.css';
import '@fontsource/baloo-2/700.css';
import '@fontsource/baloo-2/800.css';
import '@fontsource/nunito/400.css';
import '@fontsource/nunito/600.css';
import '@fontsource/nunito/700.css';
import '@fontsource/nunito/800.css';

import { injectGlobal } from '@emotion/css';
import { appGradient, font, palette } from './tokens';

injectGlobal`
  *, *::before, *::after { box-sizing: border-box; }

  html, body, #root {
    margin: 0;
    height: 100%;
    width: 100%;
    overflow: hidden;
  }

  body {
    font-family: ${font.body};
    color: ${palette.ink};
    background: ${appGradient};
    background-attachment: fixed;
    -webkit-font-smoothing: antialiased;
    text-rendering: optimizeLegibility;
    /* Disable text selection + tap highlight for an app-like feel on touch/controller. */
    -webkit-tap-highlight-color: transparent;
    user-select: none;
    -webkit-user-select: none;
    overscroll-behavior: none;
  }

  h1, h2, h3, h4 { font-family: ${font.display}; margin: 0; font-weight: 700; }

  button { font-family: inherit; cursor: pointer; }

  /* Thin rounded scrollbars that match the theme. */
  ::-webkit-scrollbar { width: 8px; height: 8px; }
  ::-webkit-scrollbar-thumb { background: ${palette.tealLight}; border-radius: 999px; }
  ::-webkit-scrollbar-track { background: transparent; }
`;
