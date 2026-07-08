'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { apiGetDashboard, isAdmin, INDUSTRY_NAMES } from '@/lib/api';
import AdminHeader from '@/components/AdminHeader';

// 백엔드 Industry enum 라벨(한글 전체) 기준으로 색상 매핑
const INDUSTRY_COLORS: Record<string, string> = {
  'IT/개발': '#3b7ff2', 서비스업: '#34a06b', 금융업: '#f5b400',
  의료서비스: '#ea4c4c', 유통: '#3b7ff2', '미디어/디자인': '#ea4c4c', 사무업: '#34a06b',
};

type MatchLog = { matchedAt: string; industry: string; situation: string };

function fmtLogTime(iso: string) {
  const d = new Date(iso);
  return d.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
}

export default function AdminStatsPage() {
  const router = useRouter();
  const [stats, setStats]         = useState({ totalMembers: 0, todayMatches: 0, activeChatRooms: 0, pendingMatches: 0 });
  const [bars, setBars]           = useState<Array<{ key: string; name: string; count: number; color: string; pct: string }>>([]);
  const [matchLogs, setMatchLogs] = useState<MatchLog[]>([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState('');

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
        setMatchLogs(data.recentMatchLogs ?? []);
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
                { label: '대기 중 매칭', value: (stats.pendingMatches ?? 0).toLocaleString('ko-KR'), color: '#f5b400' },
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

              {/* Match log */}
              <div style={{ background: '#fff', border: '1px solid #ebebeb', borderRadius: 12, padding: '18px 20px' }}>
                <div style={{ fontSize: 13, color: '#3c4043', fontWeight: 600, marginBottom: 14 }}>최근 매칭 로그</div>
                <div style={{ display: 'grid', gridTemplateColumns: '0.9fr 1.1fr 1.3fr', fontSize: 11, color: '#9aa0a6', paddingBottom: 9, borderBottom: '1px solid #f1f1f1' }}>
                  <span>시각</span><span>산업군</span><span>상황</span>
                </div>
                {matchLogs.length === 0 ? (
                  <div style={{ fontSize: 13, color: '#9aa0a6', textAlign: 'center', padding: '20px 0' }}>데이터 없음</div>
                ) : matchLogs.map((r, i) => (
                  <div key={i} style={{ display: 'grid', gridTemplateColumns: '0.9fr 1.1fr 1.3fr', alignItems: 'center', fontSize: 12, color: '#3c4043', padding: '8px 0', borderBottom: '1px solid #f6f6f6' }}>
                    <span style={{ color: '#80868b', fontFamily: 'monospace', fontSize: 11 }}>{fmtLogTime(r.matchedAt)}</span>
                    <span>{INDUSTRY_NAMES[r.industry] ?? r.industry}</span>
                    <span style={{ color: '#5f6368' }}>{r.situation}</span>
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
