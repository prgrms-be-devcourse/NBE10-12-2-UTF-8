'use client';

import { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { apiGetAdminReport, isAdmin, type AdminReportDetail } from '@/lib/api';
import AdminHeader from '@/components/AdminHeader';

const STATUS_LABEL: Record<string, string> = { PENDING: '검토 중', PROCESSED: '처리 완료' };
const STATUS_STYLE: Record<string, { background: string; color: string }> = {
  PENDING:   { background: '#fef7e0', color: '#b06000' },
  PROCESSED: { background: '#e6f4ea', color: '#137333' },
};

const LABEL_STYLE: Record<string, { background: string; color: string }> = {
  '신고자':   { background: '#e8f0fe', color: '#1a56c4' },
  '피신고자': { background: '#fce8e6', color: '#c5221f' },
  '참여자 A': { background: '#f1f3f4', color: '#5f6368' },
};

function fmtTime(iso: string) {
  return new Date(iso).toLocaleString('ko-KR', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' });
}

export default function AdminReportDetailPage() {
  const router = useRouter();
  const { reportId } = useParams<{ reportId: string }>();
  const [detail, setDetail] = useState<AdminReportDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!isAdmin()) { router.replace('/login'); return; }
    if (!reportId) return;
    apiGetAdminReport(reportId)
      .then(data => setDetail(data))
      .catch(() => setError('신고 내역을 불러오지 못했어요'))
      .finally(() => setLoading(false));
  }, [reportId, router]);

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', background: '#f8f9fa', fontFamily: "Arial, 'Helvetica Neue', sans-serif" }}>
      <AdminHeader active="reports" />

      <div style={{ flex: 1, padding: '24px 26px', overflowY: 'auto', maxWidth: 860, margin: '0 auto', width: '100%' }}>
        <button onClick={() => router.back()} style={{ border: 'none', background: 'none', color: '#5f6368', fontSize: 13, cursor: 'pointer', marginBottom: 18, padding: 0 }}>← 목록으로</button>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#9aa0a6', fontSize: 13 }}>로딩 중...</div>
        ) : error ? (
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#ea4c4c', fontSize: 13 }}>{error}</div>
        ) : detail && (
          <>
            {/* 신고 요약 카드 */}
            <div style={{ background: '#fff', border: '1px solid #ebebeb', borderRadius: 12, padding: '20px 22px', marginBottom: 20 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 }}>
                <span style={{ fontSize: 15, fontWeight: 700, color: '#202124' }}>신고 정보</span>
                <span style={{ display: 'inline-block', padding: '4px 10px', borderRadius: 9, fontSize: 12, fontWeight: 600, ...(STATUS_STYLE[detail.status] ?? {}) }}>
                  {STATUS_LABEL[detail.status] ?? detail.status}
                </span>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px 20px', fontSize: 13 }}>
                <div><span style={{ color: '#9aa0a6' }}>신고자 </span><span style={{ color: '#202124' }}>{detail.reporterEmail}</span></div>
                <div><span style={{ color: '#9aa0a6' }}>피신고자 </span><span style={{ color: '#202124' }}>{detail.reportedEmail}</span></div>
              </div>
            </div>

            {/* 증거 대화 */}
            <div style={{ background: '#fff', border: '1px solid #ebebeb', borderRadius: 12, padding: '20px 22px' }}>
              <div style={{ fontSize: 14, fontWeight: 700, color: '#202124', marginBottom: 14 }}>증거 대화</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {detail.reportedMessages.map((msg, i) => {
                  const labelStyle = LABEL_STYLE[msg.senderLabel] ?? { background: '#f1f3f4', color: '#5f6368' };
                  return (
                    <div
                      key={i}
                      style={{ display: 'flex', gap: 12, padding: '12px 14px', borderRadius: 10, background: msg.isTarget ? '#fce8e6' : '#f8f9fa', border: msg.isTarget ? '1.5px solid #f3c0bb' : '1px solid transparent' }}
                    >
                      <div style={{ flexShrink: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, minWidth: 60 }}>
                        <span style={{ display: 'inline-block', padding: '2px 8px', borderRadius: 8, fontSize: 11, fontWeight: 600, ...labelStyle }}>
                          {msg.senderLabel}
                        </span>
                        {msg.isTarget && <span style={{ fontSize: 10, color: '#c5221f', fontWeight: 700 }}>신고 대상</span>}
                      </div>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontSize: 14, color: '#202124', lineHeight: 1.5 }}>{msg.content}</div>
                        <div style={{ fontSize: 11, color: '#9aa0a6', marginTop: 4 }}>{fmtTime(msg.sentAt)}</div>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
