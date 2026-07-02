'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import Link from 'next/link';
import {
  apiCreateMatch, apiGetMatch, apiCancelMatch,
  apiGetRoom, apiGetActiveRoom, apiCloseRoom, apiSendMessage, apiGetMessages,
  apiGetMe, getToken, INDUSTRY_NAMES, type ChatMsg,
} from '@/lib/api';

type Phase = 'search' | 'matching' | 'chatting';

// 백엔드 Situation enum 라벨과 정확히 일치해야 함
const TOPICS = [
  { label: '야근 중',       count: '3.2천' },
  { label: '회의 폭탄',     count: '2.1천' },
  { label: '사내 연애 폭로', count: '1.8천' },
  { label: '상사 억까',     count: '2.6천' },
  { label: '사내 정치 피로', count: '1.4천' },
  { label: '이직 마려움',    count: '2.9천' },
  { label: '연봉 협상 앞둠', count: '980'  },
  { label: '몰래 루팡중',    count: '1.7천' },
  { label: '기타',           count: '540'  },
];

const ROOM_KEY = 'tangbisil_room_id';
const MATCH_KEY = 'tangbisil_match';

/* ─── Logo ─────────────────────────────────────────── */
const LOGO_CHARS = [
  { c: 'T', color: '#3b7ff2' }, { c: 'a', color: '#ea4c4c' },
  { c: 'n', color: '#f5b400' }, { c: 'g', color: '#3b7ff2' },
  { c: 'b', color: '#34a06b' }, { c: 'i', color: '#ea4c4c' },
  { c: 's', color: '#f5b400' }, { c: 'i', color: '#3b7ff2' },
  { c: 'l', color: '#34a06b' },
];

function TangbisilLogo({ size = 26 }: { size?: number }) {
  const ls = size >= 50 ? '-1.5px' : size >= 35 ? '-1.2px' : size >= 22 ? '-0.8px' : '-0.6px';
  return (
    <span style={{ fontFamily: "var(--font-baloo2), 'Baloo 2', sans-serif", fontSize: size, fontWeight: 700, lineHeight: 1, letterSpacing: ls, userSelect: 'none' }}>
      {LOGO_CHARS.map(({ c, color }, i) => <span key={i} style={{ color }}>{c}</span>)}
    </span>
  );
}

/* ─── Icons ─────────────────────────────────────────── */
function SearchIcon({ onClick }: { onClick?: () => void }) {
  return (
    <svg
      width="20" height="20" viewBox="0 0 24 24" fill="none"
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

function ClockIcon({ stroke }: { stroke: string }) {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}>
      <circle cx="12" cy="12" r="9" stroke={stroke} strokeWidth="2" />
      <path d="M12 7v5l3.5 2" stroke={stroke} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

/* ─── Page ─────────────────────────────────────────── */
export default function HomePage() {
  const [phase, setPhase]               = useState<Phase>('search');
  const [situation, setSituation]       = useState('');
  const [selectedTopic, setSelectedTopic] = useState<string | null>(null);
  const [messages, setMessages]         = useState<ChatMsg[]>([]);
  const [input, setInput]               = useState('');
  const [elapsed, setElapsed]           = useState(0);
  const [chatTimeLeft, setChatTimeLeft] = useState(600);
  const [showResume, setShowResume]     = useState(false);
  const [userIndustry, setUserIndustry] = useState('');
  const [matchSituation, setMatchSituation] = useState('');
  const [isLoggedIn, setIsLoggedIn]     = useState(false);
  const [matchError, setMatchError]     = useState('');
  const [partnerLeft, setPartnerLeft]   = useState(false);

  const phaseRef          = useRef<Phase>('search');
  const matchRequestIdRef = useRef<string | null>(null);
  const chatRoomIdRef     = useRef<string | null>(null);
  const seenMsgIds        = useRef<Set<string>>(new Set());
  const matchPollRef      = useRef<ReturnType<typeof setInterval> | null>(null);
  const elapsedTimerRef   = useRef<ReturnType<typeof setInterval> | null>(null);
  const chatTimerRef      = useRef<ReturnType<typeof setInterval> | null>(null);
  const msgPollRef        = useRef<ReturnType<typeof setInterval> | null>(null);
  const lastMsgTimeRef    = useRef<string | null>(null);
  const chatEndRef        = useRef<HTMLDivElement>(null);
  const inputRef          = useRef<HTMLInputElement>(null);

  const setPhaseSync = useCallback((p: Phase) => {
    phaseRef.current = p; setPhase(p);
  }, []);

  const stopAll = useCallback(() => {
    [matchPollRef, elapsedTimerRef, chatTimerRef, msgPollRef].forEach(r => {
      if (r.current) { clearInterval(r.current); r.current = null; }
    });
  }, []);

  const endChat = useCallback(() => {
    stopAll();
    const roomId = chatRoomIdRef.current;
    if (roomId) apiCloseRoom(roomId).catch(() => { /* ignore */ });
    chatRoomIdRef.current = null;
    matchRequestIdRef.current = null;
    seenMsgIds.current.clear();
    lastMsgTimeRef.current = null;
    localStorage.removeItem(ROOM_KEY);
    localStorage.removeItem(MATCH_KEY);
    setMessages([]);
    setInput('');
    setMatchSituation('');
    setChatTimeLeft(600);
    setPartnerLeft(false);
    setPhaseSync('search');
  }, [stopAll, setPhaseSync]);

  const notifyPartnerLeft = useCallback(() => {
    stopAll();
    setPartnerLeft(true);
  }, [stopAll]);

  const enterChat = useCallback(async (roomId: string) => {
    chatRoomIdRef.current = roomId;
    matchRequestIdRef.current = null;
    localStorage.setItem(ROOM_KEY, roomId);
    localStorage.removeItem(MATCH_KEY);
    setPhaseSync('chatting');

    // 기존 메시지 초기 로드
    try {
      const { msgs: initial, closed } = await apiGetMessages(roomId);
      if (closed) { notifyPartnerLeft(); return; }
      if (initial && initial.length > 0) {
        const fresh = initial.filter(m => !seenMsgIds.current.has(m.messageId));
        fresh.forEach(m => seenMsgIds.current.add(m.messageId));
        setMessages(prev => [...prev, ...fresh]);
        lastMsgTimeRef.current = initial[initial.length - 1].createdAt;
      }
    } catch { /* ignore */ }

    // 2초마다 새 메시지 폴링
    if (msgPollRef.current) clearInterval(msgPollRef.current);
    msgPollRef.current = setInterval(async () => {
      if (phaseRef.current !== 'chatting') {
        clearInterval(msgPollRef.current!); msgPollRef.current = null; return;
      }
      const rid = chatRoomIdRef.current;
      if (!rid) return;
      try {
        const { msgs: newMsgs, closed } = await apiGetMessages(rid, lastMsgTimeRef.current ?? undefined);
        if (closed) { notifyPartnerLeft(); return; }
        if (newMsgs && newMsgs.length > 0) {
          const fresh = newMsgs.filter(m => !seenMsgIds.current.has(m.messageId));
          fresh.forEach(m => seenMsgIds.current.add(m.messageId));
          if (fresh.length > 0) {
            setMessages(prev => [...prev, ...fresh]);
            lastMsgTimeRef.current = newMsgs[newMsgs.length - 1].createdAt;
          }
        }
      } catch { /* ignore */ }
    }, 2000);

    try {
      const room = await apiGetRoom(roomId);
      if (room.status === 'CLOSED') { endChat(); return; }
      const endTime = new Date(room.createdAt).getTime() + 10 * 60 * 1000;
      const remaining = Math.max(0, Math.floor((endTime - Date.now()) / 1000));
      if (remaining <= 0) { endChat(); return; }
      setChatTimeLeft(remaining);
      if (chatTimerRef.current) clearInterval(chatTimerRef.current);
      chatTimerRef.current = setInterval(() => {
        const left = Math.max(0, Math.floor((endTime - Date.now()) / 1000));
        setChatTimeLeft(left);
        if (left <= 0) endChat();
      }, 1000);
    } catch {
      setChatTimeLeft(600);
      if (chatTimerRef.current) clearInterval(chatTimerRef.current);
      chatTimerRef.current = setInterval(() => {
        setChatTimeLeft(prev => { const n = prev - 1; if (n <= 0) { endChat(); return 0; } return n; });
      }, 1000);
    }
  }, [setPhaseSync, endChat, notifyPartnerLeft]);

  const startMatchPoll = useCallback((id: string) => {
    if (matchPollRef.current) clearInterval(matchPollRef.current);
    matchPollRef.current = setInterval(async () => {
      if (phaseRef.current !== 'matching') {
        clearInterval(matchPollRef.current!); matchPollRef.current = null; return;
      }
      try {
        const data = await apiGetMatch(id);
        if (data.status === 'MATCHED' && data.chatRoomId) {
          clearInterval(matchPollRef.current!); matchPollRef.current = null;
          clearInterval(elapsedTimerRef.current!); elapsedTimerRef.current = null;
          await enterChat(data.chatRoomId);
        }
      } catch { /* ignore */ }
    }, 2000);
  }, [enterChat]);

  const doStartMatch = useCallback(async () => {
    // 백엔드 Situation enum 이외의 값은 400 에러 → 칩 선택 필수
    if (!selectedTopic) {
      setMatchError('상황 칩을 선택해주세요');
      return;
    }
    setMatchError('');
    setMatchSituation(selectedTopic);
    try {
      const data = await apiCreateMatch(selectedTopic);
      matchRequestIdRef.current = data.matchRequestId;
      localStorage.setItem(MATCH_KEY, JSON.stringify({ id: data.matchRequestId, situation: selectedTopic }));
      setElapsed(0);
      setPhaseSync('matching');
      elapsedTimerRef.current = setInterval(() => setElapsed(p => p + 1), 1000);
      startMatchPoll(data.matchRequestId);
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 401) { window.location.href = '/login'; return; }
      setMatchSituation('');
      setMatchError((err as Error)?.message || '매칭을 시작하지 못했어요. 다시 시도해주세요');
    }
  }, [selectedTopic, setPhaseSync, startMatchPoll]);

  const startMatch = useCallback(() => {
    if (chatRoomIdRef.current) { setShowResume(true); return; }
    if (!getToken()) { window.location.href = '/login'; return; }
    doStartMatch();
  }, [doStartMatch]);

  // 검색 단계에서는 입력창 포커스 여부와 상관없이 Enter로 매칭을 시작할 수 있도록 전역 리스너 사용
  useEffect(() => {
    if (phase !== 'search') return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Enter' && !e.isComposing) startMatch();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [phase, startMatch]);

  const cancelMatch = useCallback(async () => {
    stopAll();
    const id = matchRequestIdRef.current;
    matchRequestIdRef.current = null;
    localStorage.removeItem(MATCH_KEY);
    setMatchSituation('');
    setPhaseSync('search');
    if (id) { try { await apiCancelMatch(id); } catch { /* ignore */ } }
  }, [stopAll, setPhaseSync]);

  // 매칭 대기 중에는 ESC로 취소 가능
  useEffect(() => {
    if (phase !== 'matching') return;
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') cancelMatch();
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [phase, cancelMatch]);

  const send = useCallback(async () => {
    const content = input.trim();
    if (!content || phaseRef.current !== 'chatting') return;
    const roomId = chatRoomIdRef.current;
    if (!roomId) return;
    setInput('');
    try {
      const sent = await apiSendMessage(roomId, content);
      if (!seenMsgIds.current.has(sent.messageId)) {
        seenMsgIds.current.add(sent.messageId);
        setMessages(prev => [...prev, {
          messageId: sent.messageId, senderNickname: sent.senderNickname,
          content: sent.content, createdAt: sent.createdAt, isMine: true,
        }]);
      }
    } catch (err) {
      const status = (err as { status?: number })?.status;
      if (status === 409) {
        notifyPartnerLeft();
      } else {
        setInput(content);
      }
    }
  }, [input, endChat, notifyPartnerLeft]);

  // On mount: check auth + industry + in-progress chat/match (새로고침으로 잃어버린 매칭 요청도 복구)
  useEffect(() => {
    const token = getToken();
    setIsLoggedIn(!!token);
    if (!token) return;
    apiGetMe()
      .then(me => setUserIndustry(INDUSTRY_NAMES[me.industry] ?? me.industry))
      .catch(() => {});
    apiGetActiveRoom()
      .then(room => {
        if (room) { chatRoomIdRef.current = room.roomId; setShowResume(true); return; }
        localStorage.removeItem(ROOM_KEY);

        const raw = localStorage.getItem(MATCH_KEY);
        if (!raw) return;
        let saved: { id: string; situation: string };
        try { saved = JSON.parse(raw); } catch { localStorage.removeItem(MATCH_KEY); return; }

        apiGetMatch(saved.id)
          .then(data => {
            if (data.status === 'MATCHED' && data.chatRoomId) {
              enterChat(data.chatRoomId);
            } else if (data.status === 'PENDING') {
              matchRequestIdRef.current = saved.id;
              setMatchSituation(saved.situation);
              setElapsed(0);
              setPhaseSync('matching');
              elapsedTimerRef.current = setInterval(() => setElapsed(p => p + 1), 1000);
              startMatchPoll(saved.id);
            } else {
              localStorage.removeItem(MATCH_KEY);
            }
          })
          .catch(() => localStorage.removeItem(MATCH_KEY));
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    if (phase === 'chatting') inputRef.current?.focus();
  }, [phase]);

  useEffect(() => () => stopAll(), [stopAll]);

  const fmtTimer = (s: number) =>
    `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

  const partnerNickname = messages.find(m => !m.isMine)?.senderNickname ?? '익명의 상대';

  /* ── Shared styles ─────────────────────────────────── */
  const s = {
    root: { minHeight: '100vh', display: 'flex', flexDirection: 'column' as const, background: '#fff', fontFamily: "Arial, 'Helvetica Neue', sans-serif" },
    nav:  { height: 50, flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 18, padding: '0 22px' } as const,
    navText: { fontSize: 13, color: '#202124', cursor: 'pointer' } as const,
    center: { flex: 1, display: 'flex', flexDirection: 'column' as const, alignItems: 'center', justifyContent: 'center', paddingBottom: 40 },
    card: { width: 560, background: '#fff', borderRadius: 24, boxShadow: '0 1px 10px rgba(32,33,36,.18)', marginTop: 20 } as const,
    searchRow: { height: 46, display: 'flex', alignItems: 'center', gap: 13, padding: '0 16px' } as const,
    sep: { height: 1, background: '#e8eaed', margin: '0 14px' } as const,
    industryRow: { display: 'flex', alignItems: 'center', gap: 12, padding: '6px 18px', borderBottom: '1px solid #e8eaed' } as const,
    hintRow: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '9px 18px' } as const,
    hintText: { fontSize: 11, color: '#bdc1c6' } as const,
    footer: { flexShrink: 0, background: '#f2f2f2', borderTop: '1px solid #e4e4e4', display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 24px' } as const,
  };

  return (
    <div style={s.root}>
      {/* ── Top nav ── */}
      <div style={s.nav}>
        <span style={s.navText}>메일</span>
        <span style={s.navText}>이미지</span>
        {isLoggedIn ? (
          <Link href="/me" style={{ textDecoration: 'none' }}>
            <div style={{ width: 30, height: 30, borderRadius: '50%', background: '#3b7ff2', color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700, cursor: 'pointer' }}>나</div>
          </Link>
        ) : (
          <Link href="/login" style={{ textDecoration: 'none', padding: '7px 15px', background: '#3b7ff2', color: '#fff', borderRadius: 6, fontSize: 13, fontWeight: 600 }}>로그인</Link>
        )}
      </div>

      {/* ── Center (dimmed when resume modal open) ── */}
      <div style={{ ...s.center, opacity: showResume ? 0.5 : 1, pointerEvents: showResume ? 'none' : 'auto' }}>
        <TangbisilLogo size={58} />

        {/* ── Card ── */}
        <div style={s.card}>

          {/* Search bar row */}
          <div style={s.searchRow}>
            <SearchIcon onClick={phase === 'search' ? startMatch : undefined} />

            {/* Input / display */}
            {phase === 'search' && (
              <input
                type="text"
                value={situation}
                onChange={e => { setSituation(e.target.value); setSelectedTopic(null); }}
                placeholder="관심사를 입력하거나 상황을 골라 매칭하세요"
                style={{ flex: 1, border: 'none', outline: 'none', fontSize: 16, color: '#3c4043', background: 'transparent' }}
              />
            )}
            {phase === 'matching' && (
              <span style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 9, fontSize: 16, color: '#3b7ff2' }}>
                <span style={{ display: 'flex', gap: 3, alignItems: 'center' }}>
                  {[0, 0.2, 0.4].map((d, i) => (
                    <span key={i} style={{ width: 6, height: 6, borderRadius: '50%', background: '#3b7ff2', display: 'inline-block', animation: `blink 1.1s infinite ${d}s` }} />
                  ))}
                </span>
                매칭 중... {elapsed}s
              </span>
            )}
            {phase === 'chatting' && (
              <input
                ref={inputRef}
                type="text"
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter' && !e.nativeEvent.isComposing) { e.preventDefault(); send(); } }}
                placeholder="메시지를 입력하고 Enter"
                style={{ flex: 1, border: 'none', outline: 'none', fontSize: 16, color: '#3c4043', background: 'transparent' }}
              />
            )}

            {/* Right: clear / cancel */}
            {phase === 'search' && situation && (
              <button onClick={() => setSituation('')} style={{ border: 'none', background: 'none', cursor: 'pointer', padding: 2 }}><XIcon /></button>
            )}
            {phase === 'matching' && (
              <button onClick={cancelMatch} style={{ border: 'none', background: 'none', cursor: 'pointer', padding: 2 }}><XIcon /></button>
            )}

            <div style={{ width: 1, height: 24, background: '#dfe1e5', flexShrink: 0 }} />
            <MicIcon />
          </div>

          <div style={s.sep} />

          {/* 내 산업군 row */}
          <div style={s.industryRow}>
            <span style={{ fontSize: 11.5, color: '#9aa0a6', flexShrink: 0 }}>내 산업군</span>
            {userIndustry ? (
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, padding: '4px 11px', background: '#f3f8ff', color: '#3b7ff2', borderRadius: 14, fontSize: 12.5, fontWeight: 600, flexShrink: 0 }}>
                <LockIcon />{userIndustry}
              </span>
            ) : (
              <span style={{ fontSize: 11.5, color: '#bdc1c6' }}>로그인 후 확인</span>
            )}
            {(phase === 'matching' || phase === 'chatting') && matchSituation && (
              <span style={{ display: 'inline-flex', alignItems: 'center', padding: '4px 11px', background: '#e8f0fe', border: '1px solid #3b7ff2', color: '#1a56c4', borderRadius: 14, fontSize: 12.5, fontWeight: 600, flexShrink: 0 }}>
                {matchSituation}
              </span>
            )}
            {phase === 'search' && userIndustry && (
              <span style={{ fontSize: 11, color: '#bdc1c6' }}>가입 시 선택한 업계로 고정</span>
            )}
            <span style={{ marginLeft: 'auto', fontSize: 11.5, color: '#9aa0a6', flexShrink: 0 }}>지금 13,590명 활동 중</span>
          </div>

          {/* ── SEARCH body: topic chips ── */}
          {phase === 'search' && (
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
          )}

          {/* ── MATCHING body: spinner ── */}
          {phase === 'matching' && (
            <div style={{ padding: '34px 18px 30px', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14 }}>
              <span style={{ width: 40, height: 40, border: '3px solid #e8eaed', borderTopColor: '#3b7ff2', borderRadius: '50%', animation: 'spin .9s linear infinite', display: 'inline-block' }} />
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 15, color: '#202124', fontWeight: 600 }}>같은 업계의 익명의 동료를 찾고 있어요</div>
                <div style={{ fontSize: 12.5, color: '#9aa0a6', marginTop: 5 }}>산업군 + 상황이 모두 맞는 상대를 우선 연결합니다</div>
              </div>
              <span onClick={cancelMatch} style={{ marginTop: 6, padding: '9px 22px', border: '1px solid #dadce0', color: '#5f6368', borderRadius: 20, fontSize: 13.5, fontWeight: 500, cursor: 'pointer' }}>
                매칭 취소
              </span>
            </div>
          )}

          {/* ── CHATTING body: messages ── */}
          {phase === 'chatting' && (
            <div style={{ padding: '12px 18px 14px' }}>
              {/* Partner left notification */}
              {partnerLeft && (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, background: '#fff8e1', border: '1px solid #f5b400', borderRadius: 10, padding: '10px 14px', marginBottom: 12 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 16 }}>👋</span>
                    <span style={{ fontSize: 13, color: '#5f3e00', fontWeight: 500 }}>상대방이 채팅을 종료했습니다</span>
                  </div>
                  <button
                    onClick={endChat}
                    style={{ flexShrink: 0, padding: '5px 14px', background: '#f5b400', color: '#fff', border: 'none', borderRadius: 8, fontSize: 12, fontWeight: 700, cursor: 'pointer' }}
                  >
                    확인
                  </button>
                </div>
              )}
              {/* Partner info + timer + end button */}
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, marginBottom: 11 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ width: 8, height: 8, borderRadius: '50%', background: '#34a06b', display: 'inline-block', animation: 'livePulse 1.8s infinite' }} />
                  <span style={{ fontSize: 12, color: '#5f6368' }}>
                    <b style={{ color: '#3c4043', fontWeight: 600 }}>{partnerNickname}</b>
                  </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 12, color: chatTimeLeft <= 60 ? '#ea4c4c' : '#5f6368', fontWeight: 600 }}>
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="12" r="9" stroke={chatTimeLeft <= 60 ? '#ea4c4c' : '#5f6368'} strokeWidth="2" />
                      <path d="M12 7v5l3 2" stroke={chatTimeLeft <= 60 ? '#ea4c4c' : '#5f6368'} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                    {fmtTimer(chatTimeLeft)} 남음
                  </span>
                  <span onClick={endChat} style={{ border: '1px solid #f3c0bb', background: '#fef6f5', color: '#c5221f', borderRadius: 14, padding: '4px 13px', fontSize: 12, fontWeight: 600, cursor: 'pointer' }}>
                    채팅 종료
                  </span>
                </div>
              </div>

              {/* Message list — Google search history style */}
              <div style={{ maxHeight: 220, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 1 }}>
                {messages.length === 0 && (
                  <div style={{ padding: '20px 0', textAlign: 'center', fontSize: 13, color: '#bdc1c6' }}>대화를 시작해보세요</div>
                )}
                {messages.map(msg => (
                  <div
                    key={msg.messageId}
                    style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '8px 16px', borderRadius: 8, background: msg.isMine ? 'transparent' : '#f8f9fa' }}
                  >
                    <ClockIcon stroke={msg.isMine ? '#3b7ff2' : '#9aa0a6'} />
                    <span style={{ flex: 1, fontSize: 15, color: '#202124' }}>{msg.content}</span>
                  </div>
                ))}
                <div ref={chatEndRef} />
              </div>
            </div>
          )}

          <div style={s.sep} />

          {/* Footer hint */}
          <div style={s.hintRow}>
            <span style={s.hintText}>
              {phase === 'search'    && '상황을 고르면 같은 업계의 익명의 동료와 매칭돼요'}
              {phase === 'matching'  && '상대가 없으면 대기 상태가 유지돼요'}
              {phase === 'chatting'  && '10분이 지나면 대화는 자동으로 종료돼요'}
            </span>
            <span style={s.hintText}>
              {phase === 'search'    && '돋보기 클릭 또는 Enter로 매칭'}
              {phase === 'matching'  && 'ESC 취소'}
              {phase === 'chatting'  && 'Enter 전송 · ⚑ 신고'}
            </span>
          </div>
        </div>
      </div>

      {/* ── Google-style footer ── */}
      <div style={s.footer}>
        <span style={{ fontSize: 13, color: '#70757a' }}>대한민국</span>
        <div style={{ display: 'flex', gap: 22 }}>
          {['정보', '약관', '설정'].map(t => (
            <span key={t} style={{ fontSize: 13, color: '#70757a', cursor: 'pointer' }}>{t}</span>
          ))}
        </div>
      </div>

      {/* ── Resume modal (A4) ── */}
      {showResume && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(32,33,36,.38)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50 }}>
          <div style={{ width: 380, background: '#fff', borderRadius: 16, boxShadow: '0 16px 48px rgba(0,0,0,.32)', padding: '28px 28px 24px' }}>
            <div style={{ fontSize: 18, color: '#202124', fontWeight: 600 }}>진행 중인 대화가 있어요</div>
            <div style={{ fontSize: 13.5, color: '#5f6368', lineHeight: 1.6, marginTop: 10 }}>
              이전 대화가 아직 열려 있어요. 이어서 대화할까요, 아니면 종료할까요?
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, margin: '16px 0 22px', fontSize: 12.5, color: '#ea4c4c', fontWeight: 600 }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="9" stroke="#ea4c4c" strokeWidth="2" />
                <path d="M12 7v5l3 2" stroke="#ea4c4c" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
              시간이 지나면 자동 종료
            </div>
            <div style={{ display: 'flex', gap: 10 }}>
              <div
                onClick={() => { setShowResume(false); endChat(); setTimeout(() => doStartMatch(), 50); }}
                style={{ flex: 1, textAlign: 'center', border: '1px solid #f3c0bb', background: '#fef6f5', color: '#c5221f', borderRadius: 9, padding: '12px 0', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}
              >
                대화 종료
              </div>
              <div
                onClick={() => { setShowResume(false); const r = chatRoomIdRef.current; if (r) enterChat(r); }}
                style={{ flex: 1.4, textAlign: 'center', background: '#3b7ff2', color: '#fff', borderRadius: 9, padding: '12px 0', fontSize: 14, fontWeight: 600, cursor: 'pointer' }}
              >
                이어서 대화하기
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
