'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import {
  apiCreateMatch, apiGetMatch, apiGetActiveRoom,
  apiGetMe, getToken, INDUSTRY_NAMES,
} from '@/lib/api';
import { AppShell } from '@/components/AppShell';
import { TangbisilLogo } from '@/components/TangbisilLogo';

const MATCH_KEY = 'tangbisil_match';

const TOPICS = [
  { label: '야근 중',        count: '3.2천' },
  { label: '회의 폭탄',      count: '2.1천' },
  { label: '사내 연애 폭로',  count: '1.8천' },
  { label: '상사 억까',      count: '2.6천' },
  { label: '사내 정치 피로',  count: '1.4천' },
  { label: '이직 마려움',    count: '2.9천' },
  { label: '연봉 협상 앞둠',  count: '980'  },
  { label: '몰래 루팡중',    count: '1.7천' },
  { label: '기타',            count: '540'  },
];

function SearchIcon({ onClick }: { onClick?: () => void }) {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none"
      onClick={onClick}
      style={{ flexShrink: 0, cursor: onClick ? 'pointer' : 'default' }}
    >
      <circle cx="11" cy="11" r="7" stroke="#9aa0a6" strokeWidth="2" />
      <line x1="16.5" y1="16.5" x2="21" y2="21" stroke="#9aa0a6" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

function MicIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}>
      <rect x="9" y="3" width="6" height="11" rx="3" fill="#3b7ff2" />
      <path d="M5 11a7 7 0 0 0 14 0" stroke="#f5b400" strokeWidth="2" fill="none" strokeLinecap="round" />
      <line x1="12" y1="18" x2="12" y2="21" stroke="#34a06b" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

function XIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" style={{ flexShrink: 0 }}>
      <line x1="6" y1="6" x2="18" y2="18" stroke="#70757a" strokeWidth="2" strokeLinecap="round" />
      <line x1="18" y1="6" x2="6" y2="18" stroke="#70757a" strokeWidth="2" strokeLinecap="round" />
    </svg>
  );
}

function LockIcon() {
  return (
    <svg width="11" height="11" viewBox="0 0 24 24" fill="none">
      <rect x="5" y="11" width="14" height="9" rx="2" stroke="#3b7ff2" strokeWidth="2" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" stroke="#3b7ff2" strokeWidth="2" />
    </svg>
  );
}

export default function HomePage() {
  const router = useRouter();
  const [preClick, setPreClick]             = useState(true);
  const [situation, setSituation]           = useState('');
  const [selectedTopic, setSelectedTopic]   = useState<string | null>(null);
  const [userIndustry, setUserIndustry]     = useState('');
  const [isLoggedIn, setIsLoggedIn]         = useState(false);
  const [matchError, setMatchError]         = useState('');

  useEffect(() => {
    const token = getToken();
    setIsLoggedIn(!!token);
    if (!token) return;

    apiGetMe()
      .then(me => setUserIndustry(INDUSTRY_NAMES[me.industry] ?? me.industry))
      .catch(() => {});

    apiGetActiveRoom()
      .then(room => {
        if (room) { router.push(`/chat/${room.roomId}`); return; }

        const raw = localStorage.getItem(MATCH_KEY);
        if (!raw) return;
        let saved: { id: string; situation: string };
        try { saved = JSON.parse(raw); } catch { localStorage.removeItem(MATCH_KEY); return; }

        apiGetMatch(saved.id)
          .then(data => {
            if (data.status === 'MATCHED' && data.chatRoomId) {
              router.push(`/chat/${data.chatRoomId}`);
            } else if (data.status === 'PENDING') {
              router.push('/match');
            } else {
              localStorage.removeItem(MATCH_KEY);
            }
          })
          .catch(() => localStorage.removeItem(MATCH_KEY));
      })
      .catch(() => {});
  }, []);

  const startMatch = useCallback(async () => {
    if (!getToken()) { router.push('/login'); return; }
    if (!selectedTopic) { setMatchError('상황 칩을 선택해주세요'); return; }
    setMatchError('');
    try {
      const data = await apiCreateMatch(selectedTopic);
      localStorage.setItem(MATCH_KEY, JSON.stringify({ id: data.matchRequestId, situation: selectedTopic }));
      router.push('/match');
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 401) { router.push('/login'); return; }
      setMatchError((err as Error)?.message || '매칭을 시작하지 못했어요. 다시 시도해주세요');
    }
  }, [selectedTopic, router]);

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key !== 'Enter' || e.isComposing) return;
      if (preClick) { setPreClick(false); return; }
      startMatch();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [preClick, startMatch]);

  const s = {
    card: { width: 560, background: '#fff', borderRadius: 24, boxShadow: '0 1px 10px rgba(32,33,36,.18)', marginTop: 20 } as const,
    searchRow: { height: 46, display: 'flex', alignItems: 'center', gap: 13, padding: '0 16px' } as const,
    sep: { height: 1, background: '#e8eaed', margin: '0 14px' } as const,
    industryRow: { display: 'flex', alignItems: 'center', gap: 12, padding: '6px 18px', borderBottom: '1px solid #e8eaed' } as const,
    hintRow: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '9px 18px' } as const,
    hintText: { fontSize: 11, color: '#bdc1c6' } as const,
  };

  // Single unified layout — logo and search bar never move between A0 and A states.
  // Only content below the search row appears/disappears, growing the card downward.
  return (
    <AppShell isLoggedIn={isLoggedIn} topAlign>
      {/* Logo — same size and position in both preClick and !preClick */}
      <TangbisilLogo size={58} />

      {/* Card — always rendered at same marginTop so search bar never shifts */}
      <div style={s.card}>
        {/* Search row — identical DOM position in A0 and A */}
        <div style={s.searchRow}>
          <SearchIcon onClick={preClick ? () => setPreClick(false) : startMatch} />
          {preClick ? (
            <span
              onClick={() => setPreClick(false)}
              style={{ flex: 1, fontSize: 16, color: '#9aa0a6', cursor: 'text' }}
            >
              지금 겪고 있는 상황으로 익명 매칭하기
            </span>
          ) : (
            <input
              autoFocus
              type="text"
              value={situation}
              onChange={e => { setSituation(e.target.value); setSelectedTopic(null); }}
              placeholder="관심사를 입력하거나 상황을 골라 매칭하세요"
              style={{ flex: 1, border: 'none', outline: 'none', fontSize: 16, color: '#3c4043', background: 'transparent' }}
            />
          )}
          {!preClick && situation && (
            <button onClick={() => setSituation('')} style={{ border: 'none', background: 'none', cursor: 'pointer', padding: 2 }}>
              <XIcon />
            </button>
          )}
          <div style={{ width: 1, height: 24, background: '#dfe1e5', flexShrink: 0 }} />
          <MicIcon />
        </div>

        <div style={s.sep} />

        {/* Expandable section — only rendered in A state, grows card downward */}
        {!preClick && (
          <>
            <div style={s.industryRow}>
              <span style={{ fontSize: 11.5, color: '#9aa0a6', flexShrink: 0 }}>내 산업군</span>
              {userIndustry ? (
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, padding: '4px 11px', background: '#f3f8ff', color: '#3b7ff2', borderRadius: 14, fontSize: 12.5, fontWeight: 600, flexShrink: 0 }}>
                  <LockIcon />{userIndustry}
                </span>
              ) : (
                <span style={{ fontSize: 11.5, color: '#bdc1c6' }}>로그인 후 확인</span>
              )}
              {userIndustry && (
                <span style={{ fontSize: 11, color: '#bdc1c6' }}>가입 시 선택한 업계로 고정</span>
              )}
              <span style={{ marginLeft: 'auto', fontSize: 11.5, color: '#9aa0a6', flexShrink: 0 }}>지금 13,590명 활동 중</span>
            </div>

            <div style={{ padding: '12px 18px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginBottom: 10 }}>
                <span style={{ width: 7, height: 7, borderRadius: '50%', background: '#ea4c4c', display: 'inline-block' }} />
                <span style={{ fontSize: 11.5, color: '#5f6368' }}>지금 가장 많이 얘기되는 상황 — 골라서 매칭하세요</span>
              </div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                {TOPICS.map(t => {
                  const active = selectedTopic === t.label;
                  return (
                    <span
                      key={t.label}
                      onClick={() => { setMatchError(''); setSelectedTopic(active ? null : t.label); setSituation(''); }}
                      style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '6px 12px', background: active ? '#e8f0fe' : '#f8f9fa', border: `1px solid ${active ? '#3b7ff2' : '#e8eaed'}`, borderRadius: 16, fontSize: 13, color: active ? '#3b7ff2' : '#3c4043', fontWeight: active ? 600 : 400, cursor: 'pointer' }}
                    >
                      {t.label}
                      <span style={{ color: '#80868b', fontSize: 11.5 }}>{t.count}</span>
                    </span>
                  );
                })}
              </div>
              {matchError && (
                <div style={{ marginTop: 10, fontSize: 12, color: '#ea4c4c' }}>{matchError}</div>
              )}
            </div>

            <div style={s.sep} />
          </>
        )}

        {/* Hint row — always shown, text changes per state */}
        <div style={s.hintRow}>
          {preClick ? (
            <span style={{ ...s.hintText, display: 'flex', alignItems: 'center', gap: 5 }}>
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none">
                <path d="M12 5v6l4 2" stroke="#bdc1c6" strokeWidth="2" strokeLinecap="round" />
                <circle cx="12" cy="12" r="9" stroke="#bdc1c6" strokeWidth="2" />
              </svg>
              검색창을 누르면 상황 선택이 펼쳐집니다 →
            </span>
          ) : (
            <>
              <span style={s.hintText}>상황을 고르면 같은 업계의 익명의 동료와 매칭돼요</span>
              <span style={s.hintText}>돋보기 클릭 또는 Enter로 매칭</span>
            </>
          )}
        </div>
      </div>
    </AppShell>
  );
}
