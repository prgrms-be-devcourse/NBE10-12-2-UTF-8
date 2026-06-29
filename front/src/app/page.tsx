'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import Link from 'next/link';
import {
  apiCreateMatch, apiGetMatch, apiCancelMatch,
  apiGetRoom, apiSendMessage, apiGetMessages,
  getToken, type ChatMsg,
} from '@/lib/api';

type Phase = 'search' | 'matching' | 'chatting';

const TOPICS = [
  '야근 중', '퇴사 충동', '사내 갑질 토론', '이직 뻘짓',
  '사내 정치 피로', '이직 말려요', '연봉 협상 앞둔', '몰래 루팡 중',
];

const ROOM_STORAGE_KEY = 'bisil_room_id';

function BisilLogo({ size = 26 }: { size?: number }) {
  return (
    <span style={{ fontFamily: "var(--font-baloo2), 'Baloo 2', sans-serif", fontSize: size, fontWeight: 700, lineHeight: 1, letterSpacing: '-0.5px', userSelect: 'none' }}>
      <span style={{ color: '#3b7ff2' }}>B</span>
      <span style={{ color: '#ea4c4c' }}>i</span>
      <span style={{ color: '#f5b400' }}>s</span>
      <span style={{ color: '#3b7ff2' }}>i</span>
      <span style={{ color: '#34a06b' }}>l</span>
    </span>
  );
}

function LiveDot() {
  return (
    <span
      className="w-2 h-2 rounded-full inline-block flex-shrink-0"
      style={{ backgroundColor: '#34a06b', animation: 'livePulse 2s ease-in-out infinite' }}
    />
  );
}

export default function HomePage() {
  const [phase, setPhase] = useState<Phase>('search');
  const [situation, setSituation] = useState('');
  const [selectedTopic, setSelectedTopic] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMsg[]>([]);
  const [input, setInput] = useState('');
  const [elapsed, setElapsed] = useState(0);
  const [chatTimeLeft, setChatTimeLeft] = useState(600);
  const [showResume, setShowResume] = useState(false);

  const phaseRef = useRef<Phase>('search');
  const matchRequestIdRef = useRef<string | null>(null);
  const chatRoomIdRef = useRef<string | null>(null);
  const seenMsgIds = useRef<Set<string>>(new Set());
  const lastMsgTimeRef = useRef<string | undefined>(undefined);
  const matchPollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const msgPollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const elapsedTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const chatTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const chatEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const setPhaseSync = useCallback((p: Phase) => {
    phaseRef.current = p;
    setPhase(p);
  }, []);

  const stopAll = useCallback(() => {
    [matchPollRef, msgPollRef, elapsedTimerRef, chatTimerRef].forEach(r => {
      if (r.current) { clearInterval(r.current); r.current = null; }
    });
  }, []);

  const endChat = useCallback(() => {
    stopAll();
    chatRoomIdRef.current = null;
    matchRequestIdRef.current = null;
    seenMsgIds.current.clear();
    lastMsgTimeRef.current = undefined;
    localStorage.removeItem(ROOM_STORAGE_KEY);
    setMessages([]);
    setInput('');
    setChatTimeLeft(600);
    setPhaseSync('search');
  }, [stopAll, setPhaseSync]);

  const fetchMessages = useCallback(async () => {
    const roomId = chatRoomIdRef.current;
    if (!roomId || phaseRef.current !== 'chatting') return;
    try {
      const msgs = await apiGetMessages(roomId, lastMsgTimeRef.current);
      if (!msgs || msgs.length === 0) return;
      const newMsgs = msgs.filter(m => !seenMsgIds.current.has(m.messageId));
      if (newMsgs.length === 0) return;
      newMsgs.forEach(m => seenMsgIds.current.add(m.messageId));
      lastMsgTimeRef.current = newMsgs[newMsgs.length - 1].createdAt;
      setMessages(prev => [...prev, ...newMsgs]);
    } catch { /* ignore */ }
  }, []);

  const startMsgPoll = useCallback(() => {
    fetchMessages();
    if (msgPollRef.current) clearInterval(msgPollRef.current);
    msgPollRef.current = setInterval(fetchMessages, 2000);
  }, [fetchMessages]);

  const enterChat = useCallback(async (roomId: string) => {
    chatRoomIdRef.current = roomId;
    localStorage.setItem(ROOM_STORAGE_KEY, roomId);
    setPhaseSync('chatting');
    try {
      const room = await apiGetRoom(roomId);
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
        setChatTimeLeft(prev => {
          const next = prev - 1;
          if (next <= 0) { endChat(); return 0; }
          return next;
        });
      }, 1000);
    }
    startMsgPoll();
  }, [setPhaseSync, endChat, startMsgPoll]);

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
    const sit = situation.trim() || selectedTopic || '대화 중';
    try {
      const data = await apiCreateMatch(sit);
      matchRequestIdRef.current = data.matchRequestId;
      setElapsed(0);
      setPhaseSync('matching');
      elapsedTimerRef.current = setInterval(() => setElapsed(p => p + 1), 1000);
      startMatchPoll(data.matchRequestId);
    } catch (err: unknown) {
      const status = (err as { status?: number })?.status;
      if (status === 401) { window.location.href = '/login'; return; }
      console.error(err);
    }
  }, [situation, selectedTopic, setPhaseSync, startMatchPoll]);

  const startMatch = useCallback(() => {
    if (chatRoomIdRef.current) { setShowResume(true); return; }
    if (!getToken()) { window.location.href = '/login'; return; }
    doStartMatch();
  }, [doStartMatch]);

  const cancelMatch = useCallback(async () => {
    stopAll();
    const id = matchRequestIdRef.current;
    matchRequestIdRef.current = null;
    setPhaseSync('search');
    if (id) { try { await apiCancelMatch(id); } catch { /* ignore */ } }
  }, [stopAll, setPhaseSync]);

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
        const msg: ChatMsg = {
          messageId: sent.messageId,
          senderNickname: sent.senderNickname,
          content: sent.content,
          createdAt: sent.createdAt,
          isMine: true,
        };
        setMessages(prev => [...prev, msg]);
        lastMsgTimeRef.current = sent.createdAt;
      }
    } catch (err) { console.error(err); }
  }, [input]);

  // Check for in-progress chat on mount
  useEffect(() => {
    const savedRoomId = localStorage.getItem(ROOM_STORAGE_KEY);
    if (savedRoomId && getToken()) {
      chatRoomIdRef.current = savedRoomId;
      setShowResume(true);
    }
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

  return (
    <div className="min-h-screen flex flex-col" style={{ background: '#f1f3f4', fontFamily: "Arial, 'Helvetica Neue', sans-serif" }}>

      {/* ── SEARCH PHASE ── */}
      {phase === 'search' && (
        <div className="flex-1 flex flex-col">
          {/* Top nav */}
          <div className="flex items-center justify-between px-6 py-3 text-sm text-[#5f6368]">
            <BisilLogo size={22} />
            <div className="flex items-center gap-5">
              <span className="hover:text-[#3c4043] cursor-pointer select-none">Gmail</span>
              <span className="hover:text-[#3c4043] cursor-pointer select-none">이미지</span>
              <span className="flex items-center gap-1.5">
                <LiveDot />
                <span className="text-[11px] font-semibold text-[#34a06b] tracking-wider">LIVE</span>
              </span>
              <Link
                href="/login"
                className="bg-[#3b7ff2] text-white px-4 py-1.5 rounded text-sm hover:bg-[#2d6de0] transition-colors"
                style={{ textDecoration: 'none' }}
              >
                로그인
              </Link>
            </div>
          </div>

          {/* Center */}
          <div className="flex-1 flex flex-col items-center justify-center px-6 pb-24 gap-8">
            <div className="flex flex-col items-center gap-2.5">
              <BisilLogo size={72} />
              <p className="text-[#5f6368] text-[15px]">익명 직장인 랜덤 매칭 서비스</p>
              <div className="flex items-center gap-1.5 text-[13px] text-[#5f6368]">
                <LiveDot />
                <span>현재 <strong className="text-[#3c4043]">13,590명</strong> 접속 중</span>
              </div>
            </div>

            {/* Input area */}
            <div className="w-full max-w-[584px] flex flex-col gap-3">
              <div
                className="flex items-center border border-[#dfe1e5] rounded-full bg-white hover:shadow-md hover:border-transparent px-4 gap-3 transition-shadow"
                style={{ height: 48 }}
              >
                <svg className="w-5 h-5 text-[#9aa0a6] flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
                <input
                  type="text"
                  value={situation}
                  onChange={e => { setSituation(e.target.value); setSelectedTopic(null); }}
                  onKeyDown={e => {
                    if (e.key === 'Enter' && !e.nativeEvent.isComposing) startMatch();
                  }}
                  placeholder="지금 상황을 입력하세요... (예: 야근 중, 이직 고민)"
                  className="flex-1 py-2.5 text-[#3c4043] text-base outline-none bg-transparent placeholder-[#9aa0a6]"
                />
                {(situation || selectedTopic) && (
                  <button
                    onClick={() => { setSituation(''); setSelectedTopic(null); }}
                    className="p-1 hover:bg-[#f1f3f4] rounded-full transition-colors"
                  >
                    <svg className="w-4 h-4 text-[#9aa0a6]" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                      <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                )}
              </div>

              <div className="flex justify-center gap-3">
                <button
                  onClick={startMatch}
                  className="px-5 py-2 bg-[#f8f9fa] text-[#3c4043] text-sm rounded border border-[#f8f9fa] hover:border-[#dadce0] hover:shadow-sm transition-all"
                >
                  랜덤 매칭 시작
                </button>
                <button
                  onClick={startMatch}
                  className="px-5 py-2 bg-[#f8f9fa] text-[#3c4043] text-sm rounded border border-[#f8f9fa] hover:border-[#dadce0] hover:shadow-sm transition-all"
                >
                  운 좋은 매칭
                </button>
              </div>
            </div>

            {/* Topic chips */}
            <div className="w-full max-w-[584px]">
              <p className="text-[11px] text-[#9aa0a6] mb-2 font-medium uppercase tracking-wide">실시간 토픽</p>
              <div className="flex flex-wrap gap-2">
                {TOPICS.map(t => {
                  const active = selectedTopic === t;
                  return (
                    <button
                      key={t}
                      onClick={() => { setSelectedTopic(prev => prev === t ? null : t); setSituation(''); }}
                      className={`px-3 py-1.5 rounded-full border text-sm transition-all ${
                        active
                          ? 'border-[#3b7ff2] bg-[#e8f0fe] text-[#3b7ff2]'
                          : 'border-[#dfe1e5] bg-white text-[#3c4043] hover:border-[#bfbfbf] hover:shadow-sm'
                      }`}
                    >
                      {t}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ── MATCHING PHASE ── */}
      {phase === 'matching' && (
        <div className="flex-1 flex flex-col">
          <div className="flex items-center justify-between px-6 py-3">
            <BisilLogo size={22} />
            <button
              onClick={cancelMatch}
              className="text-sm text-[#ea4c4c] hover:text-[#c0392b] font-medium"
            >
              취소
            </button>
          </div>
          <div className="flex-1 flex flex-col items-center justify-center gap-6 pb-20">
            <BisilLogo size={72} />
            <div className="flex items-center gap-0.5 text-[26px] text-[#5f6368] font-medium">
              <span>매칭 중</span>
              {[0, 0.3, 0.6].map((delay, i) => (
                <span key={i} style={{ animation: `blink 1.2s ease-in-out ${delay}s infinite` }}>.</span>
              ))}
            </div>
            <p className="text-[#9aa0a6] text-sm font-mono tabular-nums">{fmtTimer(elapsed)} 경과</p>
            <button
              onClick={cancelMatch}
              className="mt-2 px-7 py-2.5 border border-[#ea4c4c] text-[#ea4c4c] rounded-full text-sm hover:bg-[#fce8e6] transition-colors"
            >
              매칭 취소
            </button>
          </div>
        </div>
      )}

      {/* ── CHATTING PHASE ── */}
      {phase === 'chatting' && (
        <div className="flex-1 flex flex-col bg-white" style={{ minHeight: '100vh' }}>
          {/* Header */}
          <div style={{ height: 54, flexShrink: 0, display: 'flex', alignItems: 'center', padding: '0 16px', borderBottom: '1px solid #e8eaed', gap: 12 }}>
            <BisilLogo size={52} />
            <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10 }}>
              <span style={{ fontSize: 13, color: '#5f6368' }}>{partnerNickname}</span>
              <span style={{ fontFamily: 'monospace', fontSize: 15, fontWeight: 700, color: chatTimeLeft <= 60 ? '#ea4c4c' : '#3b7ff2' }}>
                {fmtTimer(chatTimeLeft)}
              </span>
            </div>
            <button
              onClick={endChat}
              style={{ padding: '7px 14px', background: '#fce8e6', color: '#ea4c4c', borderRadius: 20, fontSize: 13, fontWeight: 600, border: 'none', cursor: 'pointer', flexShrink: 0 }}
            >
              채팅 종료
            </button>
          </div>

          {/* Messages */}
          <div className="flex-1 overflow-y-auto px-4 py-4 flex flex-col gap-3" style={{ background: '#fafafa' }}>
            {messages.length === 0 && (
              <div className="flex-1 flex items-center justify-center text-[#bdc1c6] text-sm select-none">
                대화를 시작해보세요 👋
              </div>
            )}
            {messages.map(msg => (
              <div key={msg.messageId} className={`flex items-end gap-2 ${msg.isMine ? 'justify-end' : 'justify-start'}`}>
                {!msg.isMine && (
                  <div
                    className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0 self-end"
                    style={{ background: '#3b7ff2' }}
                  >
                    {msg.senderNickname?.[0] ?? '?'}
                  </div>
                )}
                <div
                  className={`max-w-[68%] px-3.5 py-2 text-sm leading-relaxed ${
                    msg.isMine
                      ? 'bg-[#3b7ff2] text-white rounded-[18px] rounded-br-[4px]'
                      : 'bg-white text-[#3c4043] rounded-[18px] rounded-bl-[4px] border border-[#e8eaed]'
                  }`}
                >
                  {msg.content}
                </div>
              </div>
            ))}
            <div ref={chatEndRef} />
          </div>

          {/* Footer */}
          <div style={{ borderTop: '1px solid #e8eaed', background: '#fff' }}>
            <div className="flex items-center gap-2 px-4 py-2.5">
              <input
                ref={inputRef}
                type="text"
                value={input}
                onChange={e => setInput(e.target.value)}
                onKeyDown={e => {
                  if (e.key === 'Enter' && !e.nativeEvent.isComposing) {
                    e.preventDefault();
                    send();
                  }
                }}
                placeholder="메시지를 입력하세요..."
                className="flex-1 border border-[#dfe1e5] rounded-full px-4 py-2 text-sm outline-none focus:border-[#3b7ff2] focus:shadow-[0_0_0_2px_rgba(59,127,242,0.15)] transition-all"
              />
              <button
                onClick={send}
                disabled={!input.trim()}
                className="w-9 h-9 flex items-center justify-center rounded-full bg-[#3b7ff2] text-white hover:bg-[#2d6de0] disabled:bg-[#e8eaed] disabled:text-[#bdc1c6] disabled:cursor-not-allowed transition-colors flex-shrink-0"
              >
                <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                </svg>
              </button>
            </div>
            <div className="flex items-center justify-center gap-3 px-4 pb-3 text-[11px] text-[#bdc1c6] select-none">
              <span>10분이 지나면 대화는 자동으로 종료돼요</span>
              <span>|</span>
              <span>Enter 전송 · ⚑ 신고</span>
            </div>
          </div>
        </div>
      )}

      {/* ── RESUME MODAL (A4) ── */}
      {showResume && (
        <div
          className="fixed inset-0 flex items-center justify-center z-50"
          style={{ background: 'rgba(0,0,0,0.35)' }}
        >
          <div style={{ width: 340, background: '#fff', borderRadius: 16, padding: '28px 24px', boxShadow: '0 8px 32px rgba(0,0,0,0.18)' }}>
            <div style={{ fontSize: 17, fontWeight: 600, color: '#202124', marginBottom: 8 }}>
              진행 중인 대화가 있어요
            </div>
            <div style={{ fontSize: 13.5, color: '#5f6368', marginBottom: 24, lineHeight: 1.6 }}>
              이전 대화를 종료하고 새로운 매칭을 시작할까요?
            </div>
            <div style={{ display: 'flex', gap: 10 }}>
              <button
                onClick={() => {
                  setShowResume(false);
                  endChat();
                  setTimeout(() => doStartMatch(), 50);
                }}
                style={{ flex: 1, height: 42, background: '#fce8e6', color: '#ea4c4c', borderRadius: 10, fontSize: 14, fontWeight: 600, border: 'none', cursor: 'pointer' }}
              >
                대화 종료
              </button>
              <button
                onClick={() => {
                  setShowResume(false);
                  const roomId = chatRoomIdRef.current;
                  if (roomId) enterChat(roomId);
                }}
                style={{ flex: 1, height: 42, background: '#3b7ff2', color: '#fff', borderRadius: 10, fontSize: 14, fontWeight: 600, border: 'none', cursor: 'pointer' }}
              >
                이어서 대화하기
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
