/**
 * Unifies controller + keyboard input into NavIntents and delivers them to a callback.
 *
 * - Keyboard / D-pad-as-keys: Android handhelds (incl. the AYN Thor) often deliver D-pad and face
 *   buttons as key events. We map Arrow keys + Enter/Escape + bracket/Tab keys.
 * - Gamepad API: polled each animation frame for devices that expose a standard gamepad (D-pad,
 *   left stick, A/B, L/R, L2/R2), with edge-detection and hold-to-repeat for directions.
 *
 * Touch is handled directly by components (onClick), not here.
 */
import { useEffect, useRef } from 'react';
import type { NavIntent } from './intents';

const REPEAT_DELAY = 380; // ms before a held direction repeats
const REPEAT_RATE = 110;  // ms between repeats
const AXIS_THRESHOLD = 0.6;

const KEY_MAP: Record<string, NavIntent> = {
  ArrowUp: 'up', ArrowDown: 'down', ArrowLeft: 'left', ArrowRight: 'right',
  Enter: 'confirm', ' ': 'confirm',
  Escape: 'back', Backspace: 'back',
  '[': 'prevBox', ']': 'nextBox', q: 'prevBox', e: 'nextBox',
  PageUp: 'prevBox', PageDown: 'nextBox',
  Tab: 'switchPane', o: 'overview', t: 'transfer',
};

// Standard-gamepad button index → intent (non-directional).
const BUTTON_MAP: Record<number, NavIntent> = {
  0: 'confirm',   // A / south
  1: 'back',      // B / east
  2: 'transfer',  // X / west
  3: 'overview',  // Y / north
  4: 'prevBox',   // L1
  5: 'nextBox',   // R1
  6: 'switchPane',// L2
  7: 'switchPane',// R2
};
const DPAD = { 12: 'up', 13: 'down', 14: 'left', 15: 'right' } as const;

export const useInputIntents = (onIntent: (intent: NavIntent) => void) => {
  const cb = useRef(onIntent);
  cb.current = onIntent;

  // Keyboard
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      const intent = KEY_MAP[e.key];
      if (!intent) return;
      // Don't hijack typing in inputs/selects.
      const tag = (e.target as HTMLElement)?.tagName;
      if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') return;
      e.preventDefault();
      cb.current(intent);
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, []);

  // Gamepad
  useEffect(() => {
    let raf = 0;
    const pressed = new Set<NavIntent | number>();
    const dirSince: Record<string, number> = {};

    const fireDir = (intent: NavIntent, now: number) => {
      const last = dirSince[intent];
      if (last == null) { dirSince[intent] = now; cb.current(intent); return; }
      const held = now - last;
      const due = held >= REPEAT_DELAY && (held - REPEAT_DELAY) % REPEAT_RATE < 18;
      if (due) cb.current(intent);
    };

    const poll = () => {
      const pads = navigator.getGamepads?.() ?? [];
      const now = performance.now();
      const activeDirs = new Set<NavIntent>();

      for (const pad of pads) {
        if (!pad) continue;

        // Directions: dpad buttons + left stick.
        for (const [idx, dir] of Object.entries(DPAD)) {
          if (pad.buttons[+idx]?.pressed) activeDirs.add(dir as NavIntent);
        }
        const [ax, ay] = [pad.axes[0] ?? 0, pad.axes[1] ?? 0];
        if (ax < -AXIS_THRESHOLD) activeDirs.add('left');
        if (ax > AXIS_THRESHOLD) activeDirs.add('right');
        if (ay < -AXIS_THRESHOLD) activeDirs.add('up');
        if (ay > AXIS_THRESHOLD) activeDirs.add('down');

        // Non-directional buttons: fire once on press (edge).
        for (const [idx, intent] of Object.entries(BUTTON_MAP)) {
          const key = +idx;
          if (pad.buttons[key]?.pressed) {
            if (!pressed.has(key)) { pressed.add(key); cb.current(intent); }
          } else {
            pressed.delete(key);
          }
        }
      }

      (['up', 'down', 'left', 'right'] as NavIntent[]).forEach(dir => {
        if (activeDirs.has(dir)) fireDir(dir, now);
        else delete dirSince[dir];
      });

      raf = requestAnimationFrame(poll);
    };
    raf = requestAnimationFrame(poll);
    return () => cancelAnimationFrame(raf);
  }, []);
};
