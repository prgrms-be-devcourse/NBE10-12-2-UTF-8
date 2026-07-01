'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { apiGetAdminMembers, isAdmin, INDUSTRY_NAMES, type AdminMember } from '@/lib/api';
import AdminHeader from '@/components/AdminHeader';

const PAGE_SIZE = 10;

export default function AdminMembersPage() {
  const router = useRouter();
  const [members, setMembers] = useState<AdminMember[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isAdmin()) { router.replace('/login'); return; }
    setLoading(true);
    setError('');
    apiGetAdminMembers(page, PAGE_SIZE)
      .then(data => {
        setMembers(data.content);
        setTotalPages(data.totalPages);
        setTotalElements(data.totalElements);
      })
      .catch(() => setError('데이터를 불러오지 못했어요'))
      .finally(() => setLoading(false));
  }, [page, router]);

  const fmtDate = (iso: string) => iso.slice(0, 10);

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: '#fff', fontFamily: "Arial, 'Helvetica Neue', sans-serif" }}>
      <AdminHeader active="members" />

      <div style={{ flex: 1, padding: '24px 26px', overflowY: 'auto' }}>
        {/* Filters */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 18 }}>
          <div style={{ flex: 1, height: 40, maxWidth: 340, border: '1px solid #dadce0', borderRadius: 20, display: 'flex', alignItems: 'center', gap: 10, padding: '0 16px' }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <circle cx="11" cy="11" r="7" stroke="#9aa0a6" strokeWidth="2" />
              <line x1="16.5" y1="16.5" x2="21" y2="21" stroke="#9aa0a6" strokeWidth="2" strokeLinecap="round" />
            </svg>
            <span style={{ fontSize: 13, color: '#9aa0a6' }}>이메일 또는 UUID로 검색</span>
          </div>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, padding: '8px 14px', background: '#f3f8ff', color: '#3b7ff2', borderRadius: 18, fontSize: 13, fontWeight: 600 }}>
            전체 산업군 <span style={{ fontSize: 9 }}>▾</span>
          </span>
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: 5, padding: '8px 14px', border: '1px solid #dadce0', color: '#5f6368', borderRadius: 18, fontSize: 13 }}>
            상태 <span style={{ fontSize: 9 }}>▾</span>
          </span>
          <span style={{ marginLeft: 'auto', fontSize: 12.5, color: '#9aa0a6' }}>총 {totalElements.toLocaleString('ko-KR')}명</span>
        </div>

        {/* Table */}
        <div style={{ border: '1px solid #ebebeb', borderRadius: 12, overflow: 'hidden' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1.5fr 1fr 1fr 0.7fr', padding: '12px 18px', background: '#f8f9fa', fontSize: 12, color: '#5f6368', fontWeight: 600 }}>
            <span>UUID</span><span>이메일</span><span>산업군</span><span>가입일</span><span style={{ textAlign: 'right' }}>상태</span>
          </div>
          {loading ? (
            <div style={{ padding: '30px 18px', textAlign: 'center', fontSize: 13, color: '#9aa0a6' }}>로딩 중...</div>
          ) : error ? (
            <div style={{ padding: '30px 18px', textAlign: 'center', fontSize: 13, color: '#ea4c4c' }}>{error}</div>
          ) : members.length === 0 ? (
            <div style={{ padding: '30px 18px', textAlign: 'center', fontSize: 13, color: '#9aa0a6' }}>회원 없음</div>
          ) : members.map((m) => {
            const displayInd = INDUSTRY_NAMES[m.industry] ?? m.industry;
            const statusText = m.isSuspended ? '정지' : '활성';
            const statusBg = m.isSuspended ? '#fce8e6' : '#e6f4ea';
            const statusColor = m.isSuspended ? '#c5221f' : '#137333';
            return (
              <div key={m.memberId} style={{ display: 'grid', gridTemplateColumns: '1.4fr 1.5fr 1fr 1fr 0.7fr', alignItems: 'center', padding: '13px 18px', borderTop: '1px solid #f3f3f3', fontSize: 13, color: '#202124' }}>
                <span style={{ fontFamily: 'monospace', fontSize: 11, color: '#80868b', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{m.memberId}</span>
                <span>{m.email}</span>
                <span style={{ color: '#3c4043' }}>{displayInd}</span>
                <span style={{ color: '#5f6368' }}>{fmtDate(m.createdAt)}</span>
                <span style={{ textAlign: 'right' }}>
                  <span style={{ display: 'inline-block', padding: '3px 10px', borderRadius: 10, fontSize: 11, fontWeight: 600, background: statusBg, color: statusColor }}>{statusText}</span>
                </span>
              </div>
            );
          })}
        </div>

        {/* Pagination */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 16 }}>
          <span style={{ fontSize: 12, color: '#9aa0a6' }}>
            {page * PAGE_SIZE + 1}–{Math.min((page + 1) * PAGE_SIZE, totalElements)} / {totalElements.toLocaleString('ko-KR')}
          </span>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              style={{ width: 30, height: 30, border: '1px solid #dadce0', borderRadius: 7, background: 'none', cursor: page === 0 ? 'default' : 'pointer', fontSize: 13, color: page === 0 ? '#dadce0' : '#9aa0a6' }}
            >‹</button>
            {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
              const p = Math.max(0, Math.min(page - 2, totalPages - 5)) + i;
              return (
                <button
                  key={p}
                  onClick={() => setPage(p)}
                  style={{ width: 30, height: 30, border: p === page ? 'none' : '1px solid #dadce0', background: p === page ? '#3b7ff2' : 'none', color: p === page ? '#fff' : '#3c4043', borderRadius: 7, cursor: 'pointer', fontSize: 13, fontWeight: p === page ? 600 : 400 }}
                >
                  {p + 1}
                </button>
              );
            })}
            <button
              onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              style={{ width: 30, height: 30, border: '1px solid #dadce0', borderRadius: 7, background: 'none', cursor: page >= totalPages - 1 ? 'default' : 'pointer', fontSize: 13, color: page >= totalPages - 1 ? '#dadce0' : '#9aa0a6' }}
            >›</button>
          </div>
        </div>
      </div>
    </div>
  );
}
