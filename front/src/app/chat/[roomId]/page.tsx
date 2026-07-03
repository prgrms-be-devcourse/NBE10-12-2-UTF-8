'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import { useRouter, useParams } from 'next/navigation';
import {
  apiGetRoom, apiCloseRoom, apiSendMessage, apiGetMessages, apiGetMe, apiSubmitReport,
  getToken, INDUSTRY_NAMES, type ChatMsg,
} from '@/lib/api';
import { AppShell } from '@/components/AppShell';
import { TangbisilLogo } from '@/components/TangbisilLogo';

const SITUATION_KEY = 'tangbisil_situation';

function SearchIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" style={{ flexShrink: 0 }}>
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

export default function ChatPage() {
  const router = useRouter();
  const { roomId } = useParams<{ roomId: string }>();

  const [messages, setMessages]         = useState<ChatMsg[]>([]);
  const [input, setInput]               = useState('');
  const [chatTimeLeft, setChatTimeLeft] = useState(600);
  const [situation, setSituation]       = useState('');
  const [userIndustry, setUserIndustry] = useState('');
  const [partnerLeft, setPartnerLeft]   = useState(false);
  const [chatExpired, setChatExpired]   = useState(false);

  const [showReport, setShowReport]       = useState(false);
  const [reportMsgId, setReportMsgId]     = useState<string | null>(null);
  const [reportReason, setReportReason]   = useState('');
  const [reportLoading, setReportLoading] = useState(false);
  const [reportError, setReportError]     = useState('');

  const chatTimerRef   = useRef<ReturnType<typeof setInterval> | null>(null);
  const msgPollRef     = useRef<ReturnType<typeof setInterval> | null>(null);
  const seenMsgIds     = useRef<Set<string>>(new Set());
  const lastMsgTimeRef = useRef<string | null>(null);
  const inputRef       = useRef<HTMLInputElement>(null);
  const isLeavingRef   = useRef(false);

  const chatClosed = chatExpired || partnerLeft;

  const stopTimers = useCallback(() => {
    if (chatTimerRef.current) { clearInterval(chatTimerRef.current); chatTimerRef.current = null; }
    if (msgPollRef.current)   { clearInterval(msgPollRef.current);   msgPollRef.current   = null; }
  }, []);

  const notifyPartnerLeft = useCallback(() => {
    stopTimers();
    setPartnerLeft(true);
  }, [stopTimers]);

  const endChat = useCallback(async () => {
    if (isLeavingRef.current) return;
    isLeavingRef.current = true;
    stopTimers();
    localStorage.removeItem(SITUATION_KEY);
    try { await apiCloseRoom(roomId); } catch { /* ignore */ }
    router.push('/');
  }, [roomId, router, stopTimers]);

  const send = useCallback(async () => {
    if (chatClosed) return;
    const content = input.trim();
    if (!content) return;
    setInput('');
    try {
      const sent = await apiSendMessage(roomId, content);
      if (!seenMsgIds.current.has(sent.messageId)) {
        seenMsgIds.current.add(sent.messageId);
        setMessages(prev => [...prev, { ...sent, isMine: true }]);
      }
    } catch (err) {
      const status = (err as { status?: number })?.status;
      if (status === 409) notifyPartnerLeft();
      else setInput(content);
    }
  }, [input, roomId, chatClosed, notifyPartnerLeft]);

  useEffect(() => {
    if (!roomId) { router.push('/'); return; }

    const token = getToken();
    if (!token) { router.push('/login'); return; }

    setSituation(localStorage.getItem(SITUATION_KEY) ?? '');

    apiGetMe()
      .then(me => setUserIndustry(INDUSTRY_NAMES[me.industry] ?? me.industry))
      .catch(() => {});

    apiGetMessages(roomId).then(({ msgs: initial, closed }) => {
      if (closed) { notifyPartnerLeft(); return; }
      if (initial && initial.length > 0) {
        const fresh = initial.filter(m => !seenMsgIds.current.has(m.messageId));
        fresh.forEach(m => seenMsgIds.current.add(m.messageId));
        setMessages(prev => [...prev, ...fresh]);
        lastMsgTimeRef.current = initial[initial.length - 1].createdAt;
      }
    }).catch(() => {});

    msgPollRef.current = setInterval(async () => {
      try {
        const { msgs: newMsgs, closed } = await apiGetMessages(roomId, lastMsgTimeRef.current ?? undefined);
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

    apiGetRoom(roomId)
      .then(room => {
        if (room.status === 'CLOSED') { endChat(); return; }
        const endTime = new Date(room.createdAt).getTime() + 10 * 60 * 1000;
        const remaining = Math.max(0, Math.floor((endTime - Date.now()) / 1000));
        if (remaining <= 0) { stopTimers(); setChatExpired(true); return; }
        setChatTimeLeft(remaining);
        chatTimerRef.current = setInterval(() => {
          const left = Math.max(0, Math.floor((endTime - Date.now()) / 1000));
          setChatTimeLeft(left);
          if (left <= 0) { stopTimers(); setChatExpired(true); }
        }, 1000);
      })
      .catch(() => {
        setChatTimeLeft(600);
        chatTimerRef.current = setInterval(() => {
          setChatTimeLeft(prev => {
            const n = prev - 1;
            if (n <= 0) { stopTimers(); setChatExpired(true); return 0; }
            return n;
          });
        }, 1000);
      });

    return () => stopTimers();
  }, [roomId]);

  useEffect(() => {
    if (!chatClosed) inputRef.current?.focus();
  }, [chatClosed]);

  const submitReport = async () => {
    if (!reportMsgId || !reportReason.trim()) return;
    setReportLoading(true);
    setReportError('');
    try {
      await apiSubmitReport(roomId, reportMsgId, reportReason.trim());
      setShowReport(false);
      setReportMsgId(null);
      setReportReason('');
    } catch (err: unknown) {
      setReportError((err as Error)?.message ?? '신고에 실패했어요');
    } finally {
      setReportLoading(false);
    }
  };

  const fmtTimer = (s: number) =>
    `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;

  const partnerNickname = messages.find(m => !m.isMine)?.senderNickname ?? '익명의 상대';
  const reportableMessages = messages.filter(m => !m.isMine);

  const s = {
    card: { width: 560, background: '#fff', borderRadius: 24, boxShadow: '0 1px 10px rgba(32,33,36,.18)', marginTop: 20 } as const,
    searchRow: { height: 46, display: 'flex', alignItems: 'center', gap: 13, padding: '0 16px' } as const,
    sep: { height: 1, background: '#e8eaed', margin: '0 14px' } as const,
    industryRow: { display: 'flex', alignItems: 'center', gap: 12, padding: '6px 18px', borderBottom: '1px solid #e8eaed' } as const,
    hintRow: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '9px 18px' } as const,
    hintText: { fontSize: 11, color: '#bdc1c6' } as const,
  };

  return (
    <AppShell>
      <div style={{ marginBottom: 18 }}><TangbisilLogo size={52} /></div>
      <div style={s.card}>
        <div style={s.searchRow}>
          <SearchIcon />
          <input
            ref={inputRef}
            type="text"
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && !e.nativeEvent.isComposing) { e.preventDefault(); send(); } }}
            placeholder={chatClosed ? '대화가 종료됐어요' : '메시지를 입력하고 Enter'}
            disabled={chatClosed}
            style={{ flex: 1, border: 'none', outline: 'none', fontSize: 16, color: chatClosed ? '#9aa0a6' : '#3c4043', background: 'transparent', cursor: chatClosed ? 'not-allowed' : 'text' }}
          />
          <div style={{ width: 1, height: 24, background: '#dfe1e5', flexShrink: 0 }} />
          <MicIcon />
        </div>

        <div style={s.sep} />

        <div style={s.industryRow}>
          <span style={{ fontSize: 11.5, color: '#9aa0a6', flexShrink: 0 }}>내 산업군</span>
          {userIndustry ? (
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, padding: '4px 11px', background: '#f3f8ff', color: '#3b7ff2', borderRadius: 14, fontSize: 12.5, fontWeight: 600, flexShrink: 0 }}>
              <LockIcon />{userIndustry}
            </span>
          ) : (
            <span style={{ fontSize: 11.5, color: '#bdc1c6' }}>로그인 후 확인</span>
          )}
          {situation && (
            <span style={{ display: 'inline-flex', alignItems: 'center', padding: '4px 11px', background: '#e8f0fe', border: '1px solid #3b7ff2', color: '#1a56c4', borderRadius: 14, fontSize: 12.5, fontWeight: 600, flexShrink: 0 }}>
              {situation}
            </span>
          )}
          <span style={{ marginLeft: 'auto', fontSize: 11.5, color: '#9aa0a6', flexShrink: 0 }}>지금 13,590명 활동 중</span>
        </div>

        <div style={{ padding: '12px 18px 14px' }}>
          {partnerLeft && (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, background: '#fff8e1', border: '1px solid #f5b400', borderRadius: 10, padding: '10px 14px', marginBottom: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 16 }}>👋</span>
                <span style={{ fontSize: 13, color: '#5f3e00', fontWeight: 500 }}>상대방이 채팅을 종료했습니다</span>
              </div>
              <button onClick={endChat} style={{ flexShrink: 0, padding: '5px 14px', background: '#f5b400', color: '#fff', border: 'none', borderRadius: 8, fontSize: 12, fontWeight: 700, cursor: 'pointer' }}>확인</button>
            </div>
          )}

          {chatExpired && (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, background: '#e8f0fe', border: '1px solid #3b7ff2', borderRadius: 10, padding: '10px 14px', marginBottom: 12 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 16 }}>⏰</span>
                <span style={{ fontSize: 13, color: '#1a56c4', fontWeight: 500 }}>10분이 지나 채팅이 종료됐어요</span>
              </div>
              <button onClick={endChat} style={{ flexShrink: 0, padding: '5px 14px', background: '#3b7ff2', color: '#fff', border: 'none', borderRadius: 8, fontSize: 12, fontWeight: 700, cursor: 'pointer' }}>확인</button>
            </div>
          )}

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, marginBottom: 11 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <span style={{ width: 8, height: 8, borderRadius: '50%', background: chatClosed ? '#9aa0a6' : '#34a06b', display: 'inline-block', animation: chatClosed ? 'none' : 'livePulse 1.8s infinite' }} />
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
              {!chatClosed && (
                <span onClick={endChat} style={{ border: '1px solid #f3c0bb', background: '#fef6f5', color: '#c5221f', borderRadius: 14, padding: '4px 13px', fontSize: 12, fontWeight: 600, cursor: 'pointer' }}>
                  채팅 종료
                </span>
              )}
            </div>
          </div>

          <div style={{ maxHeight: 220, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 1 }}>
            {messages.length === 0 && (
              <div style={{ padding: '20px 0', textAlign: 'center', fontSize: 13, color: '#bdc1c6' }}>대화를 시작해보세요</div>
            )}
            {[...messages].reverse().map(msg => (
              <div
                key={msg.messageId}
                style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '8px 16px', borderRadius: 8, background: msg.isMine ? 'transparent' : '#f8f9fa' }}
              >
                <ClockIcon stroke={msg.isMine ? '#3b7ff2' : '#9aa0a6'} />
                <span style={{ flex: 1, fontSize: 15, color: '#202124' }}>{msg.content}</span>
              </div>
            ))}
          </div>
        </div>

        <div style={s.sep} />

        <div style={s.hintRow}>
          <span style={s.hintText}>10분이 지나면 대화는 자동으로 종료돼요</span>
          {reportableMessages.length > 0 && (
            <span
              onClick={() => { setShowReport(true); setReportError(''); }}
              style={{ ...s.hintText, color: '#ea4c4c', cursor: 'pointer', fontWeight: 600 }}
            >
              ⚑ 신고
            </span>
          )}
        </div>
      </div>

      {showReport && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.4)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ width: 440, background: '#fff', borderRadius: 16, padding: '24px 24px 20px', boxShadow: '0 8px 40px rgba(0,0,0,.2)' }}>
            <div style={{ fontSize: 16, fontWeight: 700, color: '#202124', marginBottom: 4 }}>신고하기</div>
            <div style={{ fontSize: 13, color: '#9aa0a6', marginBottom: 16 }}>신고할 메시지를 선택하고 사유를 입력해주세요</div>

            <div style={{ maxHeight: 180, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 14 }}>
              {reportableMessages.map(msg => (
                <div
                  key={msg.messageId}
                  onClick={() => setReportMsgId(msg.messageId)}
                  style={{ display: 'flex', alignItems: 'flex-start', gap: 10, padding: '10px 12px', borderRadius: 10, border: `1.5px solid ${reportMsgId === msg.messageId ? '#ea4c4c' : '#e8eaed'}`, background: reportMsgId === msg.messageId ? '#fce8e6' : '#f8f9fa', cursor: 'pointer' }}
                >
                  <div style={{ width: 16, height: 16, borderRadius: '50%', border: `2px solid ${reportMsgId === msg.messageId ? '#ea4c4c' : '#dadce0'}`, background: reportMsgId === msg.messageId ? '#ea4c4c' : '#fff', flexShrink: 0, marginTop: 2 }} />
                  <span style={{ fontSize: 14, color: '#202124', lineHeight: 1.5 }}>{msg.content}</span>
                </div>
              ))}
            </div>

            <textarea
              value={reportReason}
              onChange={e => setReportReason(e.target.value)}
              placeholder="신고 사유를 입력해주세요 (최대 500자)"
              maxLength={500}
              style={{ width: '100%', height: 80, border: '1px solid #dadce0', borderRadius: 8, padding: '10px 12px', fontSize: 14, color: '#202124', resize: 'none', outline: 'none', boxSizing: 'border-box', fontFamily: 'inherit' }}
            />

            {reportError && (
              <div style={{ fontSize: 12, color: '#ea4c4c', marginTop: 6 }}>{reportError}</div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10, marginTop: 14 }}>
              <button
                onClick={() => { setShowReport(false); setReportMsgId(null); setReportReason(''); setReportError(''); }}
                style={{ padding: '8px 18px', border: '1px solid #dadce0', borderRadius: 8, fontSize: 14, color: '#5f6368', background: '#fff', cursor: 'pointer' }}
              >
                취소
              </button>
              <button
                onClick={submitReport}
                disabled={reportLoading || !reportMsgId || !reportReason.trim()}
                style={{ padding: '8px 18px', background: reportLoading || !reportMsgId || !reportReason.trim() ? '#9aa0a6' : '#ea4c4c', color: '#fff', border: 'none', borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: reportLoading || !reportMsgId || !reportReason.trim() ? 'default' : 'pointer' }}
              >
                {reportLoading ? '신고 중...' : '신고하기'}
              </button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
}
