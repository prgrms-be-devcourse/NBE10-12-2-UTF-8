'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { apiGetAdminReports, isAdmin, type AdminReport } from '@/lib/api';
import AdminHeader from '@/components/AdminHeader';

const PAGE_SIZE = 10;

type StatusFilter = 'ALL' | 'PENDING' | 'PROCESSED';

const STATUS_LABEL: Record<string, string> = { PENDING: '처리 전', PROCESSED: '처리 완료' };
const STATUS_STYLE: Record<string, { background: string; color: string }> = {
  PENDING:   { background: '#fce8e6', color: '#c5221f' },
  PROCESSED: { background: '#e6f4ea', color: '#137333' },
};

const FILTER_TABS: { key: StatusFilter; label: string }[] = [
  { key: 'ALL',       label: '전체' },
  { key: 'PENDING',   label: '처리 전' },
  { key: 'PROCESSED', label: '처리 완료' },
];

function fmtDate(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

export default function AdminReportsPage() {
  const router = useRouter();
  const [reports, setReports]             = useState<AdminReport[]>([]);
  const [page, setPage]                   = useState(0);
  const [totalPages, setTotalPages]       = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading]             = useState(true);
  const [error, setError]                 = useState('');
  const [filter, setFilter]               = useState<StatusFilter>('ALL');

  useEffect(() => {
    if (!isAdmin()) { router.replace('/login'); return; }
    setLoading(true);
    setError('');
    const statusParam = filter === 'ALL' ? undefined : filter;
    apiGetAdminReports(page, PAGE_SIZE, statusParam)
      .then(data => {
        setReports(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch(() => setError('데이터를 불러오지 못했어요'))
      .finally(() => setLoading(false));
  }, [page, filter, router]);

  const handleFilterChange = (next: StatusFilter) => {
    setFilter(next);
    setPage(0);
  };

  const renderRow = (r: AdminReport) => (
    <div
      key={r.reportId}
      onClick={() => router.push(`/admin/reports/${r.reportId}`)}
      style={{ display: 'grid', gridTemplateColumns: '1.4fr 1.4fr 2fr 0.9fr 0.9fr', alignItems: 'center', padding: '13px 18px', borderTop: '1px solid #f3f3f3', fontSize: 13, color: '#202124', cursor: 'pointer' }}
      onMouseEnter={e => (e.currentTarget.style.background = '#f8f9fa')}
      onMouseLeave={e => (e.currentTarget.style.background = 'transparent')}
    >
      <span style={{ fontSize: 12, color: '#5f6368', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.reporterEmail}</span>
      <span style={{ fontSize: 12, color: '#5f6368', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.reportedEmail}</span>
      <span style={{ fontSize: 12, color: '#3c4043', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.reason}</span>
      <span>
        <span style={{ display: 'inline-block', padding: '3px 8px', borderRadius: 9, fontSize: 11, fontWeight: 600, ...(STATUS_STYLE[r.status] ?? {}) }}>
          {STATUS_LABEL[r.status] ?? r.status}
        </span>
      </span>
      <span style={{ textAlign: 'right', fontSize: 11.5, color: '#9aa0a6' }}>{fmtDate(r.createdAt)}</span>
    </div>
  );

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: '#f8f9fa', fontFamily: "Arial, 'Helvetica Neue', sans-serif" }}>
      <AdminHeader active="reports" />

      <div style={{ flex: 1, padding: '24px 26px', overflowY: 'auto' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
          {/* 필터 탭 */}
          <div style={{ display: 'flex', gap: 6 }}>
            {FILTER_TABS.map(tab => {
              const active = filter === tab.key;
              return (
                <button
                  key={tab.key}
                  onClick={() => handleFilterChange(tab.key)}
                  style={{
                    padding: '6px 14px', borderRadius: 20, fontSize: 12, fontWeight: active ? 700 : 400,
                    border: active ? 'none' : '1px solid #dadce0',
                    background: active
                      ? tab.key === 'PENDING' ? '#c5221f' : tab.key === 'PROCESSED' ? '#137333' : '#3b7ff2'
                      : '#fff',
                    color: active ? '#fff' : '#5f6368',
                    cursor: 'pointer',
                  }}
                >
                  {tab.label}
                </button>
              );
            })}
          </div>
          <span style={{ fontSize: 13, color: '#5f6368' }}>총 {totalElements.toLocaleString('ko-KR')}건</span>
        </div>

        <div style={{ border: '1px solid #ebebeb', borderRadius: 12, overflow: 'hidden', background: '#fff' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1.4fr 2fr 0.9fr 0.9fr', padding: '12px 18px', background: '#f8f9fa', fontSize: 12, color: '#5f6368', fontWeight: 600 }}>
            <span>신고자</span><span>피신고자</span><span>사유</span><span>상태</span><span style={{ textAlign: 'right' }}>일시</span>
          </div>

          {loading ? (
            <div style={{ padding: '30px 18px', textAlign: 'center', fontSize: 13, color: '#9aa0a6' }}>로딩 중...</div>
          ) : error ? (
            <div style={{ padding: '30px 18px', textAlign: 'center', fontSize: 13, color: '#ea4c4c' }}>{error}</div>
          ) : reports.length === 0 ? (
            <div style={{ padding: '30px 18px', textAlign: 'center', fontSize: 13, color: '#9aa0a6' }}>신고 내역 없음</div>
          ) : (
            reports.map(renderRow)
          )}
        </div>

        {totalPages > 1 && (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, marginTop: 16 }}>
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
              style={{ width: 30, height: 30, border: '1px solid #dadce0', borderRadius: 7, background: 'none', cursor: page === 0 ? 'default' : 'pointer', fontSize: 13, color: page === 0 ? '#dadce0' : '#9aa0a6' }}>‹</button>
            {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
              const p = Math.max(0, Math.min(page - 2, totalPages - 5)) + i;
              return (
                <button key={p} onClick={() => setPage(p)}
                  style={{ width: 30, height: 30, border: p === page ? 'none' : '1px solid #dadce0', background: p === page ? '#3b7ff2' : 'none', color: p === page ? '#fff' : '#3c4043', borderRadius: 7, cursor: 'pointer', fontSize: 13, fontWeight: p === page ? 600 : 400 }}>
                  {p + 1}
                </button>
              );
            })}
            <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
              style={{ width: 30, height: 30, border: '1px solid #dadce0', borderRadius: 7, background: 'none', cursor: page >= totalPages - 1 ? 'default' : 'pointer', fontSize: 13, color: page >= totalPages - 1 ? '#dadce0' : '#9aa0a6' }}>›</button>
          </div>
        )}
      </div>
    </div>
  );
}
