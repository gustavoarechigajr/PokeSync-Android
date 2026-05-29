/**
 * PokeSync design tokens — the single source of truth for the visual language.
 *
 * Aesthetic: Pokémon HOME-inspired teal/mint. Clean, friendly, rounded, touch-first.
 * Carried over from the original PokeSync-Android Kotlin palette (Color.kt) and the user's mockups.
 *
 * Use these tokens everywhere (via the `t` export) instead of hardcoding values, so the whole app
 * can be re-themed from one place. Pokémon *type* colors live in `typeColor()` below.
 */

export const palette = {
  // Brand teal
  teal: '#3EBDAD',
  tealDark: '#2A8F82',
  tealDeep: '#1E6F66',
  tealLight: '#80D8CF',
  mint: '#E8F8F7',
  mintDeep: '#D2F0EC',

  // Surfaces
  white: '#FFFFFF',
  panel: '#FFFFFF',
  slotEmpty: '#C7ECE7',
  slotEmptyEdge: '#AEDFD9',

  // Accents
  yellow: '#FFD740',
  coral: '#FF7A6B',
  blue: '#4FA4E0',

  // Ink
  ink: '#13343B',
  inkSoft: '#3D5A60',
  inkFaint: '#7FA0A4',
  line: '#DCEEEC',

  // Status
  shiny: '#FFCB05',
  danger: '#E5575B',
} as const;

/** App background: soft vertical teal gradient (HOME-like atmosphere). */
export const appGradient =
  `radial-gradient(120% 90% at 50% -10%, ${palette.tealLight} 0%, ${palette.teal} 38%, ${palette.tealDark} 100%)`;

export const radius = {
  sm: '8px',
  md: '14px',
  lg: '20px',
  xl: '28px',
  pill: '999px',
} as const;

export const space = {
  xs: '4px',
  sm: '8px',
  md: '12px',
  lg: '16px',
  xl: '24px',
  xxl: '32px',
} as const;

export const shadow = {
  panel: '0 10px 30px rgba(20, 80, 75, 0.18)',
  raised: '0 4px 10px rgba(20, 80, 75, 0.20)',
  inset: 'inset 0 2px 4px rgba(20, 80, 75, 0.12)',
  cursor: '0 0 0 3px #fff, 0 0 0 6px ' + palette.yellow,
} as const;

export const font = {
  display: '"Baloo 2", system-ui, sans-serif',
  body: '"Nunito", system-ui, sans-serif',
} as const;

export const z = {
  base: 0,
  cursor: 5,
  header: 10,
  summary: 20,
  overlay: 100,
} as const;

/** Min touch target per mobile guidelines. */
export const TOUCH = 44;

/**
 * Canonical Pokémon type colors. The backend's static data provides type *names* by id; this maps
 * a lowercased type name to its color. Mirrors the upstream `src/ui/theme.ts` type palette.
 */
const TYPE_COLORS: Record<string, string> = {
  normal: '#9FA19F', fighting: '#FF8000', flying: '#81B9EF', fly: '#81B9EF',
  poison: '#9141CB', ground: '#915121', rock: '#AFA981', bug: '#91A119',
  ghost: '#704170', steel: '#60A1B8', fire: '#E62829', water: '#2980EF',
  grass: '#3FA129', electric: '#FAC000', psychic: '#EF4179', ice: '#3FD8FF',
  dragon: '#5060E1', dark: '#50413F', fairy: '#EF70EF', stellar: '#F6A516',
  unknown: '#64A894',
};

export const typeColor = (typeName: string | undefined): string =>
  TYPE_COLORS[(typeName ?? '').toLowerCase()] ?? palette.inkFaint;

/** Convenience namespace. */
export const t = { palette, radius, space, shadow, font, z, appGradient } as const;
