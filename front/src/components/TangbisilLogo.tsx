const LOGO_CHARS = [
  { c: 'T', color: '#3b7ff2' }, { c: 'a', color: '#ea4c4c' },
  { c: 'n', color: '#f5b400' }, { c: 'g', color: '#3b7ff2' },
  { c: 'b', color: '#34a06b' }, { c: 'i', color: '#ea4c4c' },
  { c: 's', color: '#f5b400' }, { c: 'i', color: '#3b7ff2' },
  { c: 'l', color: '#34a06b' },
];

export function TangbisilLogo({ size = 26 }: { size?: number }) {
  const ls = size >= 50 ? '-1.5px' : size >= 35 ? '-1.2px' : size >= 22 ? '-0.8px' : '-0.6px';
  return (
    <span style={{ fontFamily: "var(--font-baloo2), 'Baloo 2', sans-serif", fontSize: size, fontWeight: 700, lineHeight: 1, letterSpacing: ls, userSelect: 'none' }}>
      {LOGO_CHARS.map(({ c, color }, i) => <span key={i} style={{ color }}>{c}</span>)}
    </span>
  );
}
