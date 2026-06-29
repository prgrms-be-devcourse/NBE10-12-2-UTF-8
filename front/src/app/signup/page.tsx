'use client';

import Link from 'next/link';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiSignup, INDUSTRY_CODES } from '@/lib/api';

const INDUSTRIES = [
  { name: 'IT/개발',       color: '#3b7ff2' },
  { name: '서비스업',      color: '#34a06b' },
  { name: '금융업',        color: '#f5b400' },
  { name: '의료서비스',    color: '#ea4c4c' },
  { name: '유통',          color: '#3b7ff2' },
  { name: '미디어/디자인', color: '#ea4c4c' },
  { name: '사무업',        color: '#34a06b' },
];

const LOGO_CHARS = [
  { c: 'T', color: '#3b7ff2' }, { c: 'a', color: '#ea4c4c' }, { c: 'n', color: '#f5b400' },
  { c: 'g', color: '#3b7ff2' }, { c: 'b', color: '#34a06b' }, { c: 'i', color: '#ea4c4c' },
  { c: 's', color: '#f5b400' }, { c: 'i', color: '#3b7ff2' }, { c: 'l', color: '#34a06b' },
];
function TangbisilLogo({ size = 34 }: { size?: number }) {
  const ls = size >= 35 ? '-1.2px' : '-0.8px';
  return (
    <span style={{ fontFamily: "var(--font-baloo2), 'Baloo 2', sans-serif", fontSize: size, fontWeight: 700, lineHeight: 1, letterSpacing: ls, userSelect: 'none' }}>
      {LOGO_CHARS.map(({ c, color }, i) => <span key={i} style={{ color }}>{c}</span>)}
    </span>
  );
}

export default function SignupPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [selected, setSelected] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const pwOk = password.length >= 8;
  const canSubmit = !!(email && pwOk && password === confirm && selected && !loading);

  const handleSignup = async () => {
    if (!canSubmit) return;
    setError('');
    setLoading(true);
    try {
      await apiSignup(email, password, INDUSTRY_CODES[selected!] ?? selected!);
      router.replace('/login');
    } catch (e: unknown) {
      setError((e as Error)?.message ?? '회원가입에 실패했어요');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', background: '#fff', fontFamily: "Arial, 'Helvetica Neue', sans-serif", padding: '34px 0 30px', overflowY: 'auto' }}>
      <TangbisilLogo size={34} />
      <div style={{ fontSize: 20, color: '#202124', fontWeight: 500, marginBottom: 4, marginTop: 6 }}>계정 만들기</div>
      <div style={{ fontSize: 13, color: '#5f6368', marginBottom: 24 }}>실명·회사명·연락처는 받지 않아요. 익명으로 시작합니다</div>

      <div style={{ width: 560 }}>
        {/* Email */}
        <div style={{ marginBottom: 14 }}>
          <div style={{ fontSize: 12, color: '#5f6368', marginBottom: 6 }}>이메일</div>
          <input
            type="email"
            value={email}
            onChange={e => setEmail(e.target.value)}
            placeholder="work@company.com"
            style={{ width: '100%', height: 46, border: '1px solid #dadce0', borderRadius: 8, padding: '0 14px', fontSize: 15, color: '#202124', outline: 'none', boxSizing: 'border-box' }}
          />
        </div>

        {/* Password row */}
        <div style={{ display: 'flex', gap: 14, marginBottom: 6 }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 12, color: '#5f6368', marginBottom: 6 }}>비밀번호</div>
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••"
              style={{ width: '100%', height: 46, border: '1px solid #dadce0', borderRadius: 8, padding: '0 14px', fontSize: 15, outline: 'none', boxSizing: 'border-box' }}
            />
          </div>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 12, color: '#5f6368', marginBottom: 6 }}>비밀번호 확인</div>
            <input
              type="password"
              value={confirm}
              onChange={e => setConfirm(e.target.value)}
              placeholder="••••••••"
              style={{ width: '100%', height: 46, border: `1px solid ${confirm && confirm !== password ? '#ea4c4c' : '#dadce0'}`, borderRadius: 8, padding: '0 14px', fontSize: 15, outline: 'none', boxSizing: 'border-box' }}
            />
          </div>
        </div>
        <div style={{ fontSize: 11.5, color: pwOk ? '#34a06b' : '#9aa0a6', marginBottom: 22 }}>
          {pwOk ? '✓ 8자 이상 · 안전한 비밀번호예요' : '비밀번호는 8자 이상으로 입력해주세요'}
        </div>

        {/* Industry */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 11 }}>
          <span style={{ fontSize: 13, color: '#3c4043', fontWeight: 600 }}>산업군</span>
          <span style={{ fontSize: 11, color: '#ea4c4c' }}>필수</span>
          <span style={{ fontSize: 11.5, color: '#9aa0a6' }}>같은 업계 사람과 우선 매칭돼요</span>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 9, marginBottom: 14 }}>
          {INDUSTRIES.map((ind) => {
            const sel = selected === ind.name;
            return (
              <span
                key={ind.name}
                onClick={() => setSelected(ind.name)}
                style={{ display: 'inline-flex', alignItems: 'center', gap: 8, padding: '8px 14px', background: sel ? '#e8f0fe' : '#fff', border: `1.5px solid ${sel ? '#3b7ff2' : '#dadce0'}`, borderRadius: 10, fontSize: 14, color: sel ? '#1a56c4' : '#3c4043', fontWeight: 500, cursor: 'pointer' }}
              >
                <span style={{ width: 11, height: 11, borderRadius: 3, background: ind.color, flexShrink: 0 }} />
                {ind.name}
              </span>
            );
          })}
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginBottom: 28, fontSize: 11.5, color: '#9aa0a6' }}>
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="#9aa0a6" strokeWidth="2" />
            <path d="M12 8v5" stroke="#9aa0a6" strokeWidth="2" strokeLinecap="round" />
            <circle cx="12" cy="16.5" r="1" fill="#9aa0a6" />
          </svg>
          현재 상황은 가입 후 매칭할 때 골라요
        </div>

        {error && <div style={{ fontSize: 12, color: '#ea4c4c', marginBottom: 12 }}>{error}</div>}

        <button
          onClick={handleSignup}
          disabled={!canSubmit}
          style={{ width: '100%', height: 48, background: canSubmit ? '#3b7ff2' : '#9aa0a6', color: '#fff', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 15, fontWeight: 600, border: 'none', cursor: canSubmit ? 'pointer' : 'default' }}
        >
          {loading ? '가입 중...' : '가입하고 매칭 시작'}
        </button>
        <div style={{ textAlign: 'center', fontSize: 12, color: '#9aa0a6', marginTop: 14 }}>
          가입 시 약관 및 개인정보 최소 수집(이메일·비밀번호·산업군)에 동의합니다
        </div>
        <div style={{ textAlign: 'center', fontSize: 13, color: '#5f6368', marginTop: 12 }}>
          이미 계정이 있으신가요?{' '}
          <Link href="/login" style={{ color: '#3b7ff2', fontWeight: 600, textDecoration: 'none' }}>로그인</Link>
        </div>
      </div>
    </div>
  );
}
