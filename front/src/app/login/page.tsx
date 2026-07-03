'use client';

import Link from 'next/link';
import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiLogin, apiGetActiveRoom, setTokens, setAdmin, getRoleFromToken } from '@/lib/api';

const LOGO_CHARS = [
  { c: 'T', color: '#3b7ff2' }, { c: 'a', color: '#ea4c4c' }, { c: 'n', color: '#f5b400' },
  { c: 'g', color: '#3b7ff2' }, { c: 'b', color: '#34a06b' }, { c: 'i', color: '#ea4c4c' },
  { c: 's', color: '#f5b400' }, { c: 'i', color: '#3b7ff2' }, { c: 'l', color: '#34a06b' },
];
function TangbisilLogo({ size = 42 }: { size?: number }) {
  const ls = size >= 35 ? '-1.2px' : '-0.8px';
  return (
    <span style={{ fontFamily: "var(--font-baloo2), 'Baloo 2', sans-serif", fontSize: size, fontWeight: 700, lineHeight: 1, letterSpacing: ls, userSelect: 'none' }}>
      {LOGO_CHARS.map(({ c, color }, i) => <span key={i} style={{ color }}>{c}</span>)}
    </span>
  );
}

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleLogin = async () => {
    if (!email || !password) return;
    setError('');
    setLoading(true);
    try {
      const data = await apiLogin(email, password);
      setTokens(data.accessToken, data.refreshToken);
      if (getRoleFromToken(data.accessToken) === 'ADMIN') {
        setAdmin();
        router.replace('/admin/stats');
        return;
      }
      try {
        await apiGetActiveRoom();
      } catch (checkErr: unknown) {
        if ((checkErr as { status?: number })?.status === 403) {
          localStorage.setItem('tangbisil_suspended', '1');
          router.replace('/me');
          return;
        }
      }
      router.replace('/');
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않아요');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', background: '#fff', fontFamily: "Arial, 'Helvetica Neue', sans-serif" }}>
      <TangbisilLogo size={42} />
      <div style={{ fontSize: 14, color: '#5f6368', marginBottom: 26, marginTop: 8 }}>
        검색하듯 로그인하고, 익명으로 동료와 연결되세요
      </div>

      <div style={{ width: 400, border: '1px solid #dadce0', borderRadius: 14, padding: '30px 30px 26px' }}>
        <div style={{ fontSize: 20, color: '#202124', fontWeight: 500, marginBottom: 22 }}>로그인</div>

        <div style={{ fontSize: 12, color: '#5f6368', marginBottom: 6 }}>이메일</div>
        <input
          type="email"
          value={email}
          onChange={e => setEmail(e.target.value)}
          onKeyDown={e => e.key === 'Enter' && handleLogin()}
          placeholder="work@company.com"
          style={{ width: '100%', height: 46, border: '1px solid #dadce0', borderRadius: 8, padding: '0 14px', fontSize: 15, color: '#202124', marginBottom: 16, outline: 'none', boxSizing: 'border-box' }}
        />

        <div style={{ fontSize: 12, color: '#5f6368', marginBottom: 6 }}>비밀번호</div>
        <div style={{ width: '100%', height: 46, border: `2px solid ${error ? '#ea4c4c' : '#3b7ff2'}`, borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 14px', marginBottom: 10, boxSizing: 'border-box' }}>
          <input
            type={showPw ? 'text' : 'password'}
            value={password}
            onChange={e => setPassword(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleLogin()}
            placeholder="••••••••"
            style={{ border: 'none', outline: 'none', fontSize: 16, color: '#202124', letterSpacing: 2, flex: 1, background: 'transparent' }}
          />
          <button onClick={() => setShowPw(v => !v)} style={{ fontSize: 12, color: '#3b7ff2', background: 'none', border: 'none', cursor: 'pointer', flexShrink: 0 }}>
            {showPw ? '숨기기' : '표시'}
          </button>
        </div>

        {error && (
          <div style={{ fontSize: 12, color: '#ea4c4c', marginBottom: 10 }}>{error}</div>
        )}

        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 22 }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: 7, fontSize: 12.5, color: '#5f6368', cursor: 'pointer' }}>
            <input type="checkbox" style={{ width: 15, height: 15, accentColor: '#3b7ff2' }} />
            로그인 유지
          </label>
          <span style={{ fontSize: 12.5, color: '#3b7ff2', cursor: 'pointer' }}>비밀번호 찾기</span>
        </div>

        <button
          onClick={handleLogin}
          disabled={loading}
          style={{ width: '100%', height: 46, background: loading ? '#9aa0a6' : '#3b7ff2', color: '#fff', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 15, fontWeight: 600, border: 'none', cursor: loading ? 'default' : 'pointer' }}
        >
          {loading ? '로그인 중...' : '로그인'}
        </button>

        <div style={{ textAlign: 'center', fontSize: 13, color: '#5f6368', marginTop: 18 }}>
          계정이 없으신가요?{' '}
          <Link href="/signup" style={{ color: '#3b7ff2', fontWeight: 600, textDecoration: 'none' }}>회원가입</Link>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginTop: 18, fontSize: 11.5, color: '#9aa0a6' }}>
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none">
          <rect x="4" y="10" width="16" height="11" rx="2" stroke="#9aa0a6" strokeWidth="2" />
          <path d="M8 10V7a4 4 0 0 1 8 0v3" stroke="#9aa0a6" strokeWidth="2" />
        </svg>
        Access Token 30분 · Refresh Token 1달 · BCrypt 암호화 저장
      </div>
    </div>
  );
}
