/**
 * Navigation intents — the single vocabulary that every input source (gamepad, keyboard, touch)
 * maps onto. Screens react to intents and never touch raw input. Add a screen's behavior by handling
 * these in a `useInputIntents` callback (see use-input-intents.ts and StorageScreen).
 */
export type NavIntent =
  | 'up' | 'down' | 'left' | 'right'
  | 'confirm'   // A / Enter
  | 'back'      // B / Escape
  | 'prevBox'   // L / [
  | 'nextBox'   // R / ]
  | 'switchPane'// L2/R2 / Tab
  | 'transfer'  // X / t  (move selected Pokémon between save and vault)
  | 'overview'; // Y / o  (reserved for the box-overview slice)
