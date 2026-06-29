'use client';

import { useRouter } from 'next/navigation';
import { useState, useEffect } from 'react';
import { apiGetMe, apiUpdateMe, apiLogout, apiDeleteMe, clearTokens, INDUSTRY_NAMES, INDUSTRY_CODES } from '@/lib/api';

const INDUSTRIES = [
  { name: 'IT/개발',       color: '#3b7ff2' },
  { name: '서비스업',      color: '#34a06b' },
  { name: '금융업',        color: '#f5b400' },
  { name: '의료서비스',    color: '#ea4c4c' },
  { name: '유통',          color: '#3b7ff2' },
  { name: '미디어/디자인', color: '#ea4c4c' },
  { name: '사무업',        color: '#34a06b' },
];

function BisilLogo({ size = 24 }: { size?: number }) {
  return (
    <span style={{ fontFamily: "'Baloo 2', sans-serif", fontSize: size, fontWeight: 700, lineHeight: 1, letterSpacing: '-.8px', userSelect: 'none' }}>
      <span style={{ color: '#3b7ff2' }}>B</span><span style={{ color: '#ea4c4c' }}>i</span>
      <span style={{ color: '#f5b400' }}>s</span><span style={{ color: '#3b7ff2' }}>i</span>
      <span style={{ color: '#34a06b' }}>l</span>
    </span>
  );
}

export default function MyPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [current, setCurrent] = useState('IT/개발');
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiGetMe()
      .then(data => {
        setEmail(data.email);
        setCurrent(INDUSTRY_NAMES[data.industry] ?? data.industry);
      })
      .catch(() => router.replace('/login'))
      .finally(() => setLoading(false));
  }, [router]);

  const handleSave = async () => {
    setSaving(true);
    try {
      await apiUpdateMe(INDUSTRY_CODES[current] ?? current);
      setSaved(true);
    } finally {
      setSaving(false);
    }
  };

  const handleLogout = async () => {
    try { await apiLogout(); } catch {}
    clearTokens();
    router.replace('/login');
  };

  const handleDelete = async () => {
    if (!window.confirm('탈퇴 시 계정과 매칭 기록이 즉시 삭제됩니다. 계속하시겠어요?')) return;
    try { await apiDeleteMe(); } catch {}
    clearTokens();
    router.replace('/login');
  };

  if (loading) return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: "Arial, 'Helvetica Neue', sans-serif" }}>
      <span style={{ color: '#9aa0a6', fontSize: 13 }}>로딩 중...</span>
    </div>
  );

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: '#fff', fontFamily: "Arial, 'Helvetica Neue', sans-serif" }}>
      {/* Header */}
      <div style={{ height: 54, flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 30px', borderBottom: '1px solid #ebebeb' }}>
        <button onClick={() => router.push('/')} style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}>
          <BisilLogo size={24} />
        </button>
        <span style={{ fontSize: 13, color: '#5f6368' }}>내 계정</span>
      </div>

      <div style={{ flex: 1, padding: '34px 40px', maxWidth: 700, margin: '0 auto', width: '100%' }}>
        {/* Profile */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 18, marginBottom: 30 }}>
          <div style={{ width: 64, height: 64, borderRadius: '50%', background: '#e8f0fe', color: '#3b7ff2', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 24, fontWeight: 700 }}>동</div>
          <div>
            <div style={{ fontSize: 20, color: '#202124', fontWeight: 600 }}>익명의 동료</div>
            <div style={{ fontSize: 13, color: '#9aa0a6', marginTop: 3 }}>채팅 상대에게는 늘 '익명의 동료'로만 표시됩니다</div>
          </div>
        </div>

        {/* Info card */}
        <div style={{ border: '1px solid #ebebeb', borderRadius: 12, overflow: 'hidden', marginBottom: 22 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 20px', borderBottom: '1px solid #f1f1f1' }}>
            <span style={{ fontSize: 13, color: '#5f6368' }}>이메일</span>
            <span style={{ fontSize: 14, color: '#202124' }}>{email}</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 20px' }}>
            <span style={{ fontSize: 13, color: '#5f6368' }}>가입일</span>
            <span style={{ fontSize: 14, color: '#202124' }}>2026-06-21</span>
          </div>
        </div>

        {/* Industry change */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 11 }}>
          <span style={{ fontSize: 13, color: '#3c4043', fontWeight: 600 }}>산업군 변경</span>
          <span style={{ fontSize: 11.5, color: '#9aa0a6' }}>현재 · {current}</span>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 9, marginBottom: 16 }}>
          {INDUSTRIES.map((ind) => {
            const sel = current === ind.name;
            return (
              <span
                key={ind.name}
                onClick={() => { setCurrent(ind.name); setSaved(false); }}
                style={{ display: 'inline-flex', alignItems: 'center', gap: 8, padding: '8px 14px', background: sel ? '#e8f0fe' : '#fff', border: `1.5px solid ${sel ? '#3b7ff2' : '#dadce0'}`, borderRadius: 10, fontSize: 14, color: sel ? '#1a56c4' : '#3c4043', fontWeight: 500, cursor: 'pointer' }}
              >
                <span style={{ width: 11, height: 11, borderRadius: 3, background: ind.color, flexShrink: 0 }} />
                {ind.name}
              </span>
            );
          })}
        </div>
        <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 30 }}>
          <button
            onClick={handleSave}
            disabled={saving}
            style={{ padding: '9px 22px', background: saved ? '#34a06b' : '#3b7ff2', color: '#fff', borderRadius: 8, fontSize: 14, fontWeight: 600, border: 'none', cursor: 'pointer', transition: 'background .2s' }}
          >
            {saving ? '저장 중...' : saved ? '저장됨 ✓' : '변경 저장'}
          </button>
        </div>

        {/* Actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <button
            onClick={handleLogout}
            style={{ padding: '11px 20px', border: '1px solid #dadce0', borderRadius: 8, fontSize: 14, color: '#3c4043', background: '#fff', cursor: 'pointer' }}
          >
            로그아웃
          </button>
          <button
            onClick={handleDelete}
            style={{ padding: '11px 20px', border: '1px solid #f3c0bb', background: '#fef6f5', borderRadius: 8, fontSize: 14, color: '#c5221f', fontWeight: 500, cursor: 'pointer' }}
          >
            회원 탈퇴
          </button>
          <span style={{ fontSize: 11.5, color: '#9aa0a6' }}>탈퇴 시 계정과 매칭 기록이 즉시 삭제됩니다</span>
        </div>
      </div>
    </div>
  );
}
