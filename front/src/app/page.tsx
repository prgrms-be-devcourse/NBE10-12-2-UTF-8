'use client';

import { useState, useEffect, useRef, useCallback } from 'react';
import ChromeWindow from '@/components/ChromeWindow';

type Phase = 'search' | 'matching' | 'matched' | 'chatting';

interface Message {
  id: number;
  from: 'me' | 'partner';
  text: string;
}

interface Industry {
  name: string;
  color: string;
  count: number;
}

interface Topic {
  name: string;
  count: string;
}

const INDUSTRIES: Industry[] = [
  { name: 'IT/개발', color: '#3b7ff2', count: 3210 },
  { name: '서비스직', color: '#34a06b', count: 2740 },
  { name: '금융업', color: '#f5b400', count: 1180 },
  { name: '의료서비스', color: '#ea4c4c', count: 960 },
  { name: '유통', color: '#3b7ff2', count: 1520 },
  { name: '미디어/디자인', color: '#ea4c4c', count: 1340 },
  { name: '사무직', color: '#34a06b', count: 2980 },
];

const TOPICS: Topic[] = [
  { name: '야근 중', count: '3.2천' },
  { name: '퇴사 충동', count: '2.1천' },
  { name: '사내 갑질 토론', count: '1.8천' },
  { name: '이직 뻘짓', count: '2.6천' },
  { name: '사내 정치 피로', count: '1.4천' },
  { name: '이직 말려요', count: '2.9천' },
  { name: '연봉 협상 앞둔', count: '980' },
  { name: '몰래 루팡 중', count: '1.7천' },
  { name: '기타', count: '540' },
];

const PARTNER_NAMES = ['두꺼비', '판다', '사막여우', '고양이', '새우', '항규', '고이돌치'];

const OPENER_MESSAGES = [
  '한 오늘도 야근이다 ㅠㅠ 반가워',
  '안녕 ㅋㅋ 해근하고 싶다',
  '드디어 매칭됐다ㅋㅋ 안녕',
  '반가워~ 나도 힘든 하루였어',
  '안녕하세요 처음이에요',
  '헬 회사 탈출 고민 중ㅠ 반가워요',
  '야근 3일째인데 같이 하소연해도 돼요?',
  '오늘 팀장한테 또 혼났어요... 반가워요',
];

const REPLY_MESSAGES = [
  '나도 비슷해요 ㅠㅠ',
  'ㅋㅋㅋ 공감',
  '헐 저도요...',
  '우리 회사도 마찬가지예요',
  '진짜 힘드시겠다',
  '이직 생각해보셨어요?',
  '저도 요즘 번아웃이에요',
  '그거 완전 공감...',
  '어느 업종이세요?',
  '저희 팀장도 그래요 ㅎ',
  '오늘따라 더 힘드네요',
  '연봉은 얼마나 받으세요..?',
];

const LOGO_CHARS = [
  { char: 'f', color: '#3b7ff2' },
  { char: 'i', color: '#ea4c4c' },
  { char: 'n', color: '#f5b400' },
  { char: 'd', color: '#3b7ff2' },
  { char: 'l', color: '#34a06b' },
  { char: 'e', color: '#ea4c4c' },
];

function FindleLogo({ size = 'lg' }: { size?: 'sm' | 'lg' }) {
  const fontSize = size === 'lg' ? '72px' : '22px';
  return (
    <span style={{ fontFamily: 'var(--font-baloo2), sans-serif', fontSize, fontWeight: 700, lineHeight: 1 }}>
      {LOGO_CHARS.map(({ char, color }) => (
        <span key={char + color} style={{ color }}>{char}</span>
      ))}
    </span>
  );
}

function IndustryBadge({ industry, size = 'sm' }: { industry: Industry; size?: 'sm' | 'xs' }) {
  return (
    <span
      className={`inline-flex items-center rounded-full text-white font-medium ${size === 'xs' ? 'px-1.5 py-0 text-[10px]' : 'px-2 py-0.5 text-xs'}`}
      style={{ backgroundColor: industry.color }}
    >
      {industry.name}
    </span>
  );
}

function LiveDot({ style }: { style?: React.CSSProperties }) {
  return (
    <span
      className="w-2 h-2 rounded-full inline-block flex-shrink-0"
      style={{ backgroundColor: '#34a06b', animation: 'livePulse 2s ease-in-out infinite', ...style }}
    />
  );
}

export default function FindlePage() {
  const [phase, setPhase] = useState<Phase>('search');
  const [industry, setIndustry] = useState<Industry>(INDUSTRIES[0]);
  const [industryMenuOpen, setIndustryMenuOpen] = useState(false);
  const [topic, setTopic] = useState<Topic | null>(null);
  const [elapsed, setElapsed] = useState(0);
  const [partner, setPartner] = useState('');
  const [partnerIndustry, setPartnerIndustry] = useState<Industry>(INDUSTRIES[0]);
  const [partnerTyping, setPartnerTyping] = useState(false);
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');

  const msgIdRef = useRef(0);
  const elapsedTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const matchTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const matchedTransitionRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const typingTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const chatEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const totalOnline = INDUSTRIES.reduce((sum, ind) => sum + ind.count, 0);

  const clearAllTimers = useCallback(() => {
    if (elapsedTimerRef.current) { clearInterval(elapsedTimerRef.current); elapsedTimerRef.current = null; }
    if (matchTimerRef.current) { clearTimeout(matchTimerRef.current); matchTimerRef.current = null; }
    if (matchedTransitionRef.current) { clearTimeout(matchedTransitionRef.current); matchedTransitionRef.current = null; }
    if (typingTimerRef.current) { clearTimeout(typingTimerRef.current); typingTimerRef.current = null; }
  }, []);

  const addMessage = useCallback((from: 'me' | 'partner', text: string) => {
    setMessages(prev => [...prev, { id: ++msgIdRef.current, from, text }]);
  }, []);

  const respond = useCallback((isOpener: boolean) => {
    if (typingTimerRef.current) clearTimeout(typingTimerRef.current);
    setPartnerTyping(true);
    const delay = 900 + Math.random() * 700;
    typingTimerRef.current = setTimeout(() => {
      setPartnerTyping(false);
      const pool = isOpener ? OPENER_MESSAGES : REPLY_MESSAGES;
      addMessage('partner', pool[Math.floor(Math.random() * pool.length)]);
    }, delay);
  }, [addMessage]);

  const startMatch = useCallback(() => {
    if (phase !== 'search') return;
    setPhase('matching');
    setElapsed(0);

    elapsedTimerRef.current = setInterval(() => {
      setElapsed(prev => prev + 1);
    }, 1000);

    const matchDelay = 3000 + Math.random() * 4000;
    matchTimerRef.current = setTimeout(() => {
      if (elapsedTimerRef.current) { clearInterval(elapsedTimerRef.current); elapsedTimerRef.current = null; }

      const name = `익명의 ${PARTNER_NAMES[Math.floor(Math.random() * PARTNER_NAMES.length)]}`;
      const pInd = INDUSTRIES[Math.floor(Math.random() * INDUSTRIES.length)];
      setPartner(name);
      setPartnerIndustry(pInd);
      setMessages([]);
      setPhase('matched');
      respond(true);

      matchedTransitionRef.current = setTimeout(() => setPhase('chatting'), 1900);
    }, matchDelay);
  }, [phase, respond]);

  const leave = useCallback(() => {
    clearAllTimers();
    setPhase('search');
    setElapsed(0);
    setPartner('');
    setPartnerTyping(false);
    setMessages([]);
    setInput('');
  }, [clearAllTimers]);

  const send = useCallback(() => {
    const trimmed = input.trim();
    if (!trimmed || phase !== 'chatting') return;
    addMessage('me', trimmed);
    setInput('');
    respond(false);
  }, [input, phase, addMessage, respond]);

  useEffect(() => {
    const handle = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && phase !== 'search') leave();
    };
    window.addEventListener('keydown', handle);
    return () => window.removeEventListener('keydown', handle);
  }, [phase, leave]);

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, partnerTyping]);

  useEffect(() => {
    if (phase === 'chatting') inputRef.current?.focus();
  }, [phase]);

  useEffect(() => () => clearAllTimers(), [clearAllTimers]);

  useEffect(() => {
    if (!industryMenuOpen) return;
    const handle = () => setIndustryMenuOpen(false);
    document.addEventListener('click', handle);
    return () => document.removeEventListener('click', handle);
  }, [industryMenuOpen]);

  const formatTime = (secs: number) => {
    const m = Math.floor(secs / 60).toString().padStart(2, '0');
    const s = (secs % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  };

  const partnerInitial = partner.replace('익명의 ', '')[0] ?? '?';

  return (
    <div className="min-h-screen bg-[#f1f3f4] flex items-center justify-center p-4 md:p-8">
      <ChromeWindow>
        <div style={{ minHeight: '560px' }} className="flex flex-col">

          {/* ── SEARCH PHASE ── */}
          {phase === 'search' && (
            <div className="flex-1 flex flex-col">
              {/* Top nav */}
              <div className="flex items-center justify-between px-6 py-3 text-sm text-[#5f6368]">
                <FindleLogo size="sm" />
                <div className="flex items-center gap-4">
                  <span className="hover:text-[#3c4043] cursor-pointer">Gmail</span>
                  <span className="hover:text-[#3c4043] cursor-pointer">이미지</span>
                  <div className="flex items-center gap-1.5">
                    <LiveDot />
                    <span className="text-[11px] font-semibold text-[#34a06b] tracking-wider">LIVE</span>
                  </div>
                  <button className="bg-[#3b7ff2] text-white px-4 py-1.5 rounded text-sm hover:bg-[#2d6de0] transition-colors">
                    로그인
                  </button>
                </div>
              </div>

              {/* Main */}
              <div className="flex-1 flex flex-col items-center justify-center px-6 pb-20 gap-7">
                {/* Logo + subtitle */}
                <div className="flex flex-col items-center gap-2">
                  <FindleLogo size="lg" />
                  <p className="text-[#5f6368] text-[15px]">익명 직장인 랜덤 매칭 서비스</p>
                  <div className="flex items-center gap-1.5 text-[13px] text-[#5f6368] mt-0.5">
                    <LiveDot />
                    <span>
                      현재{' '}
                      <strong className="text-[#3c4043]">{totalOnline.toLocaleString()}명</strong>{' '}
                      접속 중
                    </span>
                  </div>
                </div>

                {/* Search controls */}
                <div className="w-full max-w-[584px] flex flex-col gap-3">
                  <div className="flex items-stretch gap-2">
                    {/* Industry dropdown */}
                    <div className="relative" onClick={(e) => e.stopPropagation()}>
                      <button
                        className="flex items-center gap-2 px-3 py-2.5 border border-[#dfe1e5] rounded-full bg-white hover:shadow-md hover:border-transparent text-sm transition-shadow h-full whitespace-nowrap"
                        onClick={(e) => { e.stopPropagation(); setIndustryMenuOpen(v => !v); }}
                      >
                        <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: industry.color }} />
                        <span className="text-[#3c4043]">{industry.name}</span>
                        <svg className="w-3 h-3 text-[#9aa0a6] ml-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M19 9l-7 7-7-7" />
                        </svg>
                      </button>
                      {industryMenuOpen && (
                        <div className="absolute top-full left-0 mt-1 bg-white rounded-lg shadow-xl border border-[#e0e0e0] z-20 w-[190px] py-1">
                          {INDUSTRIES.map((ind) => (
                            <button
                              key={ind.name}
                              className="flex items-center gap-2.5 w-full px-3 py-2 text-sm text-[#3c4043] hover:bg-[#f8f9fa] text-left"
                              onClick={(e) => { e.stopPropagation(); setIndustry(ind); setIndustryMenuOpen(false); }}
                            >
                              <span className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: ind.color }} />
                              <span className="flex-1">{ind.name}</span>
                              <span className="text-[11px] text-[#9aa0a6]">{ind.count.toLocaleString()}</span>
                            </button>
                          ))}
                        </div>
                      )}
                    </div>

                    {/* Search bar */}
                    <div
                      className="flex-1 flex items-center border border-[#dfe1e5] rounded-full bg-white hover:shadow-md hover:border-transparent px-4 gap-3 transition-shadow cursor-text"
                      onClick={startMatch}
                    >
                      <svg className="w-5 h-5 text-[#9aa0a6] flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                        <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                      </svg>
                      <span className="flex-1 py-2.5 text-[#9aa0a6] text-base select-none">
                        {topic ? `"${topic.name}" 대화상대 찾기...` : '대화상대 찾기...'}
                      </span>
                      <div className="w-px h-6 bg-[#e0e0e0]" />
                      <button
                        className="flex-shrink-0 hover:bg-[#f1f3f4] rounded-full p-1 transition-colors"
                        onClick={(e) => { e.stopPropagation(); startMatch(); }}
                        title="매칭 시작"
                      >
                        <svg className="w-5 h-5 text-[#3b7ff2]" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                          <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                        </svg>
                      </button>
                    </div>
                  </div>

                  {/* Action buttons */}
                  <div className="flex justify-center gap-3">
                    <button
                      className="px-5 py-2 bg-[#f8f9fa] text-[#3c4043] text-sm rounded border border-[#f8f9fa] hover:border-[#dadce0] hover:shadow-sm transition-all"
                      onClick={startMatch}
                    >
                      랜덤 매칭 시작
                    </button>
                    <button
                      className="px-5 py-2 bg-[#f8f9fa] text-[#3c4043] text-sm rounded border border-[#f8f9fa] hover:border-[#dadce0] hover:shadow-sm transition-all"
                      onClick={startMatch}
                    >
                      운 좋은 매칭
                    </button>
                  </div>
                </div>

                {/* Topics */}
                <div className="w-full max-w-[584px]">
                  <p className="text-[11px] text-[#9aa0a6] mb-2 font-medium uppercase tracking-wide">실시간 토픽</p>
                  <div className="flex flex-wrap gap-2">
                    {TOPICS.map((t) => {
                      const active = topic?.name === t.name;
                      return (
                        <button
                          key={t.name}
                          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-full border text-sm transition-all ${
                            active
                              ? 'border-[#3b7ff2] bg-[#e8f0fe] text-[#3b7ff2]'
                              : 'border-[#dfe1e5] bg-white text-[#3c4043] hover:border-[#bfbfbf] hover:shadow-sm'
                          }`}
                          onClick={() => setTopic(prev => prev?.name === t.name ? null : t)}
                        >
                          {t.name}
                          <span className={`text-[11px] ${active ? 'text-[#3b7ff2]' : 'text-[#9aa0a6]'}`}>
                            {t.count}
                          </span>
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
                <FindleLogo size="sm" />
                <button
                  className="text-sm text-[#ea4c4c] hover:text-[#c0392b] font-medium"
                  onClick={leave}
                >
                  취소
                </button>
              </div>
              <div className="flex-1 flex flex-col items-center justify-center gap-6 pb-16">
                <FindleLogo size="lg" />
                <div className="flex items-center gap-0.5 text-[26px] text-[#5f6368] font-medium">
                  <span>매칭 중</span>
                  {[0, 0.3, 0.6].map((delay, i) => (
                    <span key={i} style={{ animation: `blink 1.2s ease-in-out ${delay}s infinite` }}>.</span>
                  ))}
                </div>
                <div className="flex items-center gap-2">
                  <IndustryBadge industry={industry} />
                  {topic && (
                    <span className="px-2.5 py-0.5 bg-[#f1f3f4] text-[#5f6368] text-xs rounded-full border border-[#e0e0e0]">
                      {topic.name}
                    </span>
                  )}
                </div>
                <p className="text-[#9aa0a6] text-sm font-mono tabular-nums">{formatTime(elapsed)} 경과</p>
                <button
                  className="mt-2 px-7 py-2.5 border border-[#ea4c4c] text-[#ea4c4c] rounded-full text-sm hover:bg-[#fce8e6] transition-colors"
                  onClick={leave}
                >
                  매칭 취소
                </button>
              </div>
            </div>
          )}

          {/* ── MATCHED / CHATTING PHASE ── */}
          {(phase === 'matched' || phase === 'chatting') && (
            <div className="flex-1 flex flex-col" style={{ minHeight: '560px' }}>
              {/* Header */}
              <div className="flex items-center justify-between px-4 py-2.5 border-b border-[#e8eaed]">
                <FindleLogo size="sm" />
                <div className="flex items-center gap-2">
                  {phase === 'matched' ? (
                    <span className="text-[#34a06b] text-sm font-semibold" style={{ animation: 'livePulse 1s ease-in-out infinite' }}>
                      연결됨!
                    </span>
                  ) : (
                    <>
                      <div
                        className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0"
                        style={{ backgroundColor: partnerIndustry.color }}
                      >
                        {partnerInitial}
                      </div>
                      <span className="font-medium text-[#3c4043] text-sm">{partner}</span>
                      <IndustryBadge industry={partnerIndustry} size="xs" />
                    </>
                  )}
                </div>
                <button
                  className="p-1.5 hover:bg-[#f1f3f4] rounded-full text-[#9aa0a6] hover:text-[#5f6368] transition-colors"
                  onClick={leave}
                  title="나가기 (ESC)"
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              {/* Messages */}
              <div
                className="flex-1 overflow-y-auto px-4 py-4 flex flex-col gap-3 bg-[#fafafa]"
                style={{ maxHeight: '390px' }}
              >
                {messages.length === 0 && (
                  <div className="flex-1 flex items-center justify-center text-[#bdc1c6] text-sm select-none">
                    {phase === 'matched' ? '연결 중...' : '대화를 시작해보세요 👋'}
                  </div>
                )}
                {messages.map((msg) => (
                  <div key={msg.id} className={`flex items-end gap-2 ${msg.from === 'me' ? 'justify-end' : 'justify-start'}`}>
                    {msg.from === 'partner' && (
                      <div
                        className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0 self-end"
                        style={{ backgroundColor: partnerIndustry.color }}
                      >
                        {partnerInitial}
                      </div>
                    )}
                    <div
                      className={`max-w-[68%] px-3.5 py-2 text-sm leading-relaxed ${
                        msg.from === 'me'
                          ? 'bg-[#3b7ff2] text-white rounded-[18px] rounded-br-[4px]'
                          : 'bg-white text-[#3c4043] rounded-[18px] rounded-bl-[4px] border border-[#e8eaed]'
                      }`}
                    >
                      {msg.text}
                    </div>
                  </div>
                ))}

                {/* Typing indicator */}
                {partnerTyping && (
                  <div className="flex items-end gap-2 justify-start">
                    <div
                      className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold flex-shrink-0"
                      style={{ backgroundColor: partnerIndustry.color }}
                    >
                      {partnerInitial}
                    </div>
                    <div className="bg-white border border-[#e8eaed] px-3.5 py-2.5 rounded-[18px] rounded-bl-[4px] flex items-center gap-1">
                      {[0, 0.25, 0.5].map((delay, i) => (
                        <span
                          key={i}
                          className="w-1.5 h-1.5 rounded-full bg-[#bdc1c6]"
                          style={{ animation: `blink 1.2s ease-in-out ${delay}s infinite` }}
                        />
                      ))}
                    </div>
                  </div>
                )}
                <div ref={chatEndRef} />
              </div>

              {/* Input */}
              <div className="px-4 py-3 border-t border-[#e8eaed] flex items-center gap-2">
                <input
                  ref={inputRef}
                  type="text"
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); } }}
                  placeholder={phase === 'chatting' ? '메시지를 입력하세요...' : '연결 중...'}
                  disabled={phase !== 'chatting'}
                  className="flex-1 border border-[#dfe1e5] rounded-full px-4 py-2 text-sm outline-none focus:border-[#3b7ff2] focus:shadow-[0_0_0_2px_rgba(59,127,242,0.15)] disabled:bg-[#f8f9fa] disabled:text-[#bdc1c6] transition-all"
                />
                <button
                  onClick={send}
                  disabled={phase !== 'chatting' || !input.trim()}
                  className="w-9 h-9 flex items-center justify-center rounded-full bg-[#3b7ff2] text-white hover:bg-[#2d6de0] disabled:bg-[#e8eaed] disabled:text-[#bdc1c6] disabled:cursor-not-allowed transition-colors flex-shrink-0"
                >
                  <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                  </svg>
                </button>
              </div>
            </div>
          )}

        </div>
      </ChromeWindow>
    </div>
  );
}
