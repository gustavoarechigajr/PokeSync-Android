/**
 * Enriched Pokémon summary (shown on the pane opposite the selection).
 * Richer than the original mockup: identity, types/tera, held item, ball, OT/ID, origin, friendship,
 * stat hexagon, IVs/EVs, moves (+relearn), ribbons/marks, status flags, HOME tracker.
 */
import { css } from '@emotion/css';
import type React from 'react';
import { Gender, GameVersion } from '../../../data/sdk/model';
import type { PkmBaseDTO } from '../../../data/sdk/model';
import { ItemImg } from '../../../ui/img/item-img';
import { BallImg } from '../../../ui/img/ball-img';
import { useStaticData } from '../../../hooks/use-static-data';
import { ballName } from '../../shared/ball-names';
import { Panel } from '../../design-system/components/Panel';
import { StatHexagon } from '../../design-system/components/StatHexagon';
import { TypeBadge } from '../../design-system/components/TypeBadge';
import { font, palette, radius, space } from '../../design-system/tokens';
import { PkmSprite } from '../storage/pkm-sprite';

const STAT_LABELS = ['HP', 'Atk', 'Def', 'Spe', 'SpA', 'SpD'];

const sectionTitle = css({
  fontFamily: font.display, fontWeight: 700, fontSize: 12, letterSpacing: 0.5,
  textTransform: 'uppercase', color: palette.tealDark, margin: `${space.md} 0 ${space.xs}`,
});

const Row: React.FC<{ label: string; children: React.ReactNode }> = ({ label, children }) => (
  <div className={css({ display: 'flex', alignItems: 'center', gap: space.sm, padding: '3px 0', fontSize: 13 })}>
    <span className={css({ color: palette.inkFaint, minWidth: 78, fontWeight: 700 })}>{label}</span>
    <span className={css({ color: palette.ink, fontWeight: 600, display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' })}>{children}</span>
  </div>
);

export const SummaryPanel: React.FC<{
  pkm: PkmBaseDTO;
  onTransfer?: () => void;
  transferLabel?: string;
  transferBusy?: boolean;
  transferNote?: string;
}> = ({ pkm, onTransfer, transferLabel, transferBusy, transferNote }) => {
  const sd = useStaticData();

  const speciesName =
    sd.species[pkm.species]?.forms?.[pkm.context]?.[pkm.form]?.name
    ?? sd.species[pkm.species]?.forms?.[pkm.context]?.[0]?.name
    ?? `#${pkm.species}`;
  const natureName = sd.natures[pkm.nature]?.name ?? String(pkm.nature);
  const abilityName = sd.abilities[pkm.ability]?.name ?? `#${pkm.ability}`;
  const typeNames = (pkm.types ?? []).map(id => sd.types[id]?.name).filter(Boolean) as string[];
  const teraName = pkm.teraType != null ? sd.types[pkm.teraType]?.name : undefined;
  const genderSym = pkm.gender === Gender.Male ? '♂' : pkm.gender === Gender.Female ? '♀' : '';

  const moveBadges = (pkm.moves ?? []).filter(m => m > 0).map((m, i) => {
    const mv = sd.moves[m];
    return <TypeBadge key={i} name={mv?.name ?? `#${m}`} small />;
  });
  const relearn = (pkm.relearnMoves ?? []).filter(m => m > 0).map(m => sd.moves[m]?.name ?? `#${m}`);

  // Render the specific ball sprite: balls are items in static data, keyed by name. Fall back to the
  // generic Poké Ball sprite if the name doesn't resolve (e.g. region-specific variants).
  const ballNm = ballName(pkm.ball);
  const ballItem = sd.getItem(GameVersion.Any, ballNm);

  const statMax = Math.max(...(pkm.stats ?? [1]), 1);
  const ribbonsCount = pkm.ribbons ? Object.values(pkm.ribbons).filter(Boolean).length : 0;
  const marksCount = pkm.markings?.filter(m => m && m !== 'None').length ?? 0;

  return (
    <Panel className={css({ flex: 1, minWidth: 0, overflow: 'auto', gap: 0 })}>
      {/* Header */}
      <div className={css({ display: 'flex', gap: space.md, alignItems: 'center' })}>
        <div className={css({
          width: 84, height: 84, borderRadius: radius.lg, flexShrink: 0,
          background: `linear-gradient(160deg, ${palette.white}, ${palette.mint})`,
          display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: palette ? undefined : undefined,
        })}>
          <PkmSprite pkm={pkm} />
        </div>
        <div className={css({ minWidth: 0 })}>
          <div className={css({ fontFamily: font.display, fontWeight: 800, fontSize: 22, color: palette.ink, lineHeight: 1.1 })}>
            {pkm.isNicknamed ? pkm.nickname : speciesName} <span className={css({ color: palette.inkFaint })}>{genderSym}</span>
          </div>
          <div className={css({ color: palette.inkSoft, fontWeight: 700, fontSize: 13 })}>
            {speciesName} · Lv. {pkm.level}
          </div>
          <div className={css({ display: 'flex', gap: 6, marginTop: 6, flexWrap: 'wrap' })}>
            {typeNames.map(tn => <TypeBadge key={tn} name={tn} small />)}
            {pkm.isShiny && <span className={css({ color: palette.shiny, fontWeight: 800, fontSize: 12 })}>★ Shiny</span>}
            {pkm.isAlpha && <span className={css({ color: palette.coral, fontWeight: 800, fontSize: 12 })}>α Alpha</span>}
            {pkm.isEgg && <span className={css({ color: palette.blue, fontWeight: 800, fontSize: 12 })}>Egg</span>}
          </div>
        </div>
      </div>

      {onTransfer && (
        <button
          onClick={onTransfer}
          disabled={transferBusy}
          className={css({
            marginTop: space.md, border: 'none', cursor: 'pointer',
            background: `linear-gradient(180deg, ${palette.teal}, ${palette.tealDark})`,
            color: palette.white, fontFamily: font.display, fontWeight: 800, fontSize: 15,
            borderRadius: radius.pill, padding: `${space.sm} ${space.lg}`,
            boxShadow: '0 4px 10px rgba(20,80,75,0.25)', opacity: transferBusy ? 0.6 : 1,
            ':active': { transform: 'scale(0.97)' },
          })}
        >
          {transferBusy ? 'Transferring…' : (transferLabel ?? 'Transfer')}
        </button>
      )}
      {!onTransfer && transferNote && (
        <div className={css({
          marginTop: space.md, padding: `${space.sm} ${space.md}`, borderRadius: radius.md,
          background: '#FCEBEC', color: palette.danger, fontWeight: 700, fontSize: 13, textAlign: 'center',
        })}>
          {transferNote}
        </div>
      )}

      <div className={sectionTitle}>Details</div>
      <Row label="Nature">{natureName}</Row>
      <Row label="Ability">{abilityName}</Row>
      {teraName && <Row label="Tera">{<TypeBadge name={teraName} small />}</Row>}
      <Row label="Held item">
        {pkm.heldItem > 0
          ? <><ItemImg item={pkm.heldItem} version={pkm.contextVersion} /> {sd.getItem(pkm.contextVersion, pkm.heldItem)?.name ?? ''}</>
          : <span className={css({ color: palette.inkFaint })}>—</span>}
      </Row>
      <Row label="Ball">{ballItem ? <ItemImg item={ballNm} version={GameVersion.Any} /> : <BallImg />} {ballNm}</Row>
      <Row label="Friendship">{pkm.friendship}</Row>

      <div className={sectionTitle}>Trainer & origin</div>
      <Row label="OT">{pkm.originTrainerName} <span className={css({ color: palette.inkFaint })}>({pkm.tid}{pkm.sid != null ? `/${pkm.sid}` : ''})</span></Row>
      <Row label="Met">{pkm.originMetLocation || '—'}{pkm.originMetLevel != null ? ` · Lv.${pkm.originMetLevel}` : ''}</Row>
      {pkm.originMetDate && <Row label="Date">{pkm.originMetDate}</Row>}
      {pkm.homeTracker ? <Row label="HOME">{String(pkm.homeTracker)}</Row> : null}

      <div className={sectionTitle}>Stats</div>
      <div className={css({ display: 'flex', justifyContent: 'center' })}>
        <StatHexagon values={pkm.stats ?? []} labels={STAT_LABELS} max={statMax} />
      </div>
      <Row label="IVs">{(pkm.iVs ?? []).join(' / ')}</Row>
      <Row label="EVs">{(pkm.eVs ?? []).join(' / ')}</Row>

      <div className={sectionTitle}>Moves</div>
      <div className={css({ display: 'flex', flexWrap: 'wrap', gap: 6 })}>{moveBadges}</div>
      {relearn.length > 0 && (
        <Row label="Relearn">{relearn.join(', ')}</Row>
      )}

      {(ribbonsCount > 0 || marksCount > 0 || pkm.isPokerusInfected || pkm.isPokerusCured) && (
        <>
          <div className={sectionTitle}>Extras</div>
          {ribbonsCount > 0 && <Row label="Ribbons">{ribbonsCount}</Row>}
          {marksCount > 0 && <Row label="Marks">{marksCount}</Row>}
          {pkm.isPokerusInfected && <Row label="Pokérus">Infected</Row>}
          {pkm.isPokerusCured && <Row label="Pokérus">Cured</Row>}
        </>
      )}
    </Panel>
  );
};
