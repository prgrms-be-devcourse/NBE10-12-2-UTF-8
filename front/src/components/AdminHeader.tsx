'use client';

import Link from 'next/link';
import type { ReactNode } from 'react';

const LOGO_CHARS = [
  { c: 'T', color: '#3b7ff2' }, { c: 'a', color: '#ea4c4c' }, { c: 'n', color: '#f5b400' },
  { c: 'g', color: '#3b7ff2' }, { c: 'b', color: '#34a06b' }, { c: 'i', color: '#ea4c4c' },
  { c: 's', color: '#f5b400' }, { c: 'i', color: '#3b7ff2' }, { c: 'l', color: '#34a06b' },
];
function TangbisilLogo({ size = 20 }: { size?: number }) {
  return (
    <span style={{ fontFamily: "var(--font-baloo2), 'Baloo 2', sans-serif", fontSize: size, fontWeight: 700, lineHeight: 1, letterSpacing: '-.6px', userSelect: 'none' }}>
      {LOGO_CHARS.map(({ c, color }, i) => <span key={i} style={{ color }}>{c}</span>)}
    </span>
  );
}

const TABS = [
  { href: '/admin/stats', key: 'stats', label: '통계' },
  { href: '/admin/members', key: 'members', label: '회원 목록' },
] as const;

export default function AdminHeader({ active, right }: { active: 'stats' | 'members'; right?: ReactNode }) {
  return (
    <div style={{ height: 54, flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 26px', background: '#fff', borderBottom: '1px solid #ebebeb' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
        <Link href="/" style={{ textDecoration: 'none' }}><TangbisilLogo size={20} /></Link>
        <span style={{ fontSize: 13, color: '#5f6368', borderLeft: '1px solid #dadce0', paddingLeft: 12 }}>관리자</span>
        <div style={{ display: 'flex', gap: 4 }}>
          {TABS.map(t => {
            const isActive = t.key === active;
            return (
              <Link
                key={t.key}
                href={t.href}
                style={{
                  fontSize: 13, fontWeight: 600, textDecoration: 'none', padding: '6px 12px', borderRadius: 16,
                  color: isActive ? '#3b7ff2' : '#5f6368',
                  background: isActive ? '#e8f0fe' : 'transparent',
                }}
              >
                {t.label}
              </Link>
            );
          })}
        </div>
      </div>
      {right}
    </div>
  );
}
