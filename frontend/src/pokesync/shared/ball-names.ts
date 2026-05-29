/**
 * Maps PKHeX Ball ids to display names. (The backend exposes `ball` as a Ball enum id, not an item id,
 * and the static data has no per-ball name list — so this mirrors PKHeX's Ball enum.)
 */
const BALL_NAMES: Record<number, string> = {
  0: 'None',
  1: 'Master Ball', 2: 'Ultra Ball', 3: 'Great Ball', 4: 'Poké Ball',
  5: 'Safari Ball', 6: 'Net Ball', 7: 'Dive Ball', 8: 'Nest Ball',
  9: 'Repeat Ball', 10: 'Timer Ball', 11: 'Luxury Ball', 12: 'Premier Ball',
  13: 'Dusk Ball', 14: 'Heal Ball', 15: 'Quick Ball', 16: 'Cherish Ball',
  17: 'Fast Ball', 18: 'Level Ball', 19: 'Lure Ball', 20: 'Heavy Ball',
  21: 'Love Ball', 22: 'Friend Ball', 23: 'Moon Ball', 24: 'Sport Ball',
  25: 'Dream Ball', 26: 'Beast Ball',
  // Legends: Arceus
  27: 'Strange Ball', 28: 'Poké Ball (Hisui)', 29: 'Great Ball (Hisui)',
  30: 'Ultra Ball (Hisui)', 31: 'Feather Ball', 32: 'Wing Ball', 33: 'Jet Ball',
  34: 'Heavy Ball (Hisui)', 35: 'Leaden Ball', 36: 'Gigaton Ball', 37: 'Origin Ball',
};

export const ballName = (id: number): string => BALL_NAMES[id] ?? `Ball #${id}`;
