'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { apiGetDashboard, isAdmin, INDUSTRY_NAMES } from '@/lib/api';
import AdminHeader from '@/components/AdminHeader';

const INDUSTRY_COLORS: Record<string, string> = {
  IT: '#3b7ff2', 서비스: '#34a06b', 금융: '#f5b400',
  의료: '#ea4c4c', 유통: '#3b7ff2', 미디어: '#ea4c4c', 사무: '#34a06b',
};

const MATCH_LOG = [
  { t: '14:32:08', ind: 'IT/개발',      sit: '야근 중',        st: 'MATCHED' },
  { t: '14:31:55', ind: '서비스업',     sit: '상사 억까',      st: 'MATCHED' },
  { t: '14:31:40', ind: '사무업',       sit: '회의 폭탄',      st: 'PENDING' },
  { t: '14:31:22', ind: '금융업',       sit: '연봉 협상 앞둠', st: 'MATCHED' },
  { t: '14:30:58', ind: '미디어/디자인', sit: '이직 마려움',    st: 'CLOSED'  },
  { t: '14:30:31', ind: '유통',         sit: '사내 정치 피로', st: 'MATCHED' },
].map(r => ({
  ...r,
  stBg:    r.st === 'MATCHED' ? '#e6f4ea' : r.st === 'PENDING' ? '#fef7e0' : '#f1f3f4',
  stColor: r.st === 'MATCHED' ? '#137333' : r.st === 'PENDING' ? '#b06000' : '#5f6368',
}));

export default function AdminStatsPage() {
  const router = useRouter();
  const [stats, setStats] = useState({ totalMembers: 0, todayMatches: 0, activeChatRooms: 0 });
  const [bars, setBars] = useState<Array<{ key: string; name: string; count: number; color: string; pct: string }>>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isAdmin()) { router.replace('/login'); return; }
    setLoading(true);
    setError('');
    apiGetDashboard()
      .then(data => {
        setStats(data.matchStatistics);
        const sorted = [...data.industryStatistics].sort((a, b) => b.count - a.count);
        const max = sorted[0]?.count ?? 1;
        setBars(sorted.map(s => ({
          key: s.industry,
          name: INDUSTRY_NAMES[s.industry] ?? s.industry,
          count: s.count,
          color: INDUSTRY_COLORS[s.industry] ?? '#9aa0a6',
          pct: `${Math.round(s.count / max * 100)}%`,
        })));
      })
      .catch(() => setError('데이터를 불러오지 못했어요'))
      .finally(() => setLoading(false));
  }, [router]);

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: '#f8f9fa', fontFamily: "Arial, 'Helvetica Neue', sans-serif" }}>
      <AdminHeader
        active="stats"
        right={
          <span style={{ fontSize: 12, color: '#137333', display: 'flex', alignItems: 'center', gap: 6 }}>
            <span style={{ width: 7, height: 7, borderRadius: '50%', background: '#34a06b', display: 'inline-block' }} />
            실시간
          </span>
        }
      />

      <div style={{ flex: 1, padding: '22px 26px', overflowY: 'auto' }}>
        {loading ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 200, color: '#9aa0a6', fontSize: 13 }}>
            로딩 중...
          </div>
        ) : error ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: 200, color: '#ea4c4c', fontSize: 13 }}>
            {error}
          </div>
        ) : (
          <>
            {/* Stats cards */}
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 14, marginBottom: 22 }}>
              {[
                { label: '전체 가입자',  value: stats.totalMembers.toLocaleString('ko-KR'),  color: '#202124' },
                { label: '오늘 매칭',   value: stats.todayMatches.toLocaleString('ko-KR'),   color: '#3b7ff2' },
                { label: '활성 채팅방', value: stats.activeChatRooms.toLocaleString('ko-KR'), color: '#34a06b' },
                { label: '대기 중 매칭', value: '–', color: '#f5b400' },
              ].map(c => (
                <div key={c.label} style={{ background: '#fff', border: '1px solid #ebebeb', borderRadius: 12, padding: '16px 18px' }}>
                  <div style={{ fontSize: 12, color: '#5f6368' }}>{c.label}</div>
                  <div style={{ fontSize: 28, color: c.color, fontWeight: 700, marginTop: 6 }}>{c.value}</div>
                </div>
              ))}
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1.1fr 1fr', gap: 16 }}>
              {/* Bar chart */}
              <div style={{ background: '#fff', border: '1px solid #ebebeb', borderRadius: 12, padding: '18px 20px' }}>
                <div style={{ fontSize: 13, color: '#3c4043', fontWeight: 600, marginBottom: 16 }}>산업군별 매칭 현황</div>
                {bars.length === 0 ? (
                  <div style={{ fontSize: 13, color: '#9aa0a6', textAlign: 'center', padding: '20px 0' }}>데이터 없음</div>
                ) : bars.map(b => (
                  <div key={b.key} style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 11 }}>
                    <span style={{ width: 84, fontSize: 12.5, color: '#5f6368', flexShrink: 0 }}>{b.name}</span>
                    <span style={{ flex: 1, height: 14, background: '#f1f3f4', borderRadius: 7, overflow: 'hidden' }}>
                      <span style={{ display: 'block', height: '100%', width: b.pct, background: b.color, borderRadius: 7 }} />
                    </span>
                    <span style={{ width: 48, textAlign: 'right', fontSize: 12, color: '#3c4043', fontWeight: 600, flexShrink: 0 }}>{b.count.toLocaleString('ko-KR')}</span>
                  </div>
                ))}
              </div>

              {/* Match log (mock — no API yet) */}
              <div style={{ background: '#fff', border: '1px solid #ebebeb', borderRadius: 12, padding: '18px 20px' }}>
                <div style={{ fontSize: 13, color: '#3c4043', fontWeight: 600, marginBottom: 14 }}>최근 매칭 로그</div>
                <div style={{ display: 'grid', gridTemplateColumns: '0.9fr 1.1fr 1.3fr 0.9fr', fontSize: 11, color: '#9aa0a6', paddingBottom: 9, borderBottom: '1px solid #f1f1f1' }}>
                  <span>시각</span><span>산업군</span><span>상황</span><span style={{ textAlign: 'right' }}>상태</span>
                </div>
                {MATCH_LOG.map((r, i) => (
                  <div key={i} style={{ display: 'grid', gridTemplateColumns: '0.9fr 1.1fr 1.3fr 0.9fr', alignItems: 'center', fontSize: 12, color: '#3c4043', padding: '8px 0', borderBottom: '1px solid #f6f6f6' }}>
                    <span style={{ color: '#80868b', fontFamily: 'monospace', fontSize: 11 }}>{r.t}</span>
                    <span>{r.ind}</span>
                    <span style={{ color: '#5f6368' }}>{r.sit}</span>
                    <span style={{ textAlign: 'right' }}>
                      <span style={{ display: 'inline-block', padding: '2px 8px', borderRadius: 9, fontSize: 10.5, fontWeight: 600, background: r.stBg, color: r.stColor }}>{r.st}</span>
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
