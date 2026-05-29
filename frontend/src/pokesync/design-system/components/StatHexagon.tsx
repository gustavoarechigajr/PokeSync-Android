/**
 * Hexagonal stat radar (HOME-style). Pass 6 values + labels (clockwise from top).
 * Renders the web grid + the stat polygon; values are scaled against `max`.
 */
import { css } from '@emotion/css';
import type React from 'react';
import { font, palette } from '../tokens';

type StatHexagonProps = {
  values: number[];   // length 6
  labels: string[];   // length 6
  max?: number;
  size?: number;
};

const point = (cx: number, cy: number, r: number, i: number) => {
  const angle = (Math.PI / 3) * i - Math.PI / 2; // start at top, clockwise
  return [cx + r * Math.cos(angle), cy + r * Math.sin(angle)] as const;
};

export const StatHexagon: React.FC<StatHexagonProps> = ({ values, labels, max = 200, size = 168 }) => {
  const cx = size / 2;
  const cy = size / 2;
  const r = size / 2 - 26;

  const rings = [0.25, 0.5, 0.75, 1];
  const gridPolys = rings.map(scale =>
    Array.from({ length: 6 }, (_, i) => point(cx, cy, r * scale, i).join(',')).join(' '),
  );

  const statPoly = values
    .map((v, i) => point(cx, cy, r * Math.min(1, v / max), i).join(','))
    .join(' ');

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
      {gridPolys.map((p, i) => (
        <polygon key={i} points={p} fill="none" stroke={palette.line} strokeWidth={1} />
      ))}
      {Array.from({ length: 6 }, (_, i) => {
        const [x, y] = point(cx, cy, r, i);
        return <line key={i} x1={cx} y1={cy} x2={x} y2={y} stroke={palette.line} strokeWidth={1} />;
      })}
      <polygon
        points={statPoly}
        fill={palette.teal + '55'}
        stroke={palette.tealDark}
        strokeWidth={2}
        strokeLinejoin="round"
      />
      {values.map((v, i) => {
        const [x, y] = point(cx, cy, r + 13, i);
        return (
          <text
            key={i}
            x={x}
            y={y}
            textAnchor="middle"
            dominantBaseline="middle"
            className={css({ fontFamily: font.display, fontSize: 9, fontWeight: 700, fill: palette.inkSoft })}
          >
            <tspan x={x} dy={-4}>{labels[i]}</tspan>
            <tspan x={x} dy={11} className={css({ fill: palette.tealDark })}>{v}</tspan>
          </text>
        );
      })}
    </svg>
  );
};
