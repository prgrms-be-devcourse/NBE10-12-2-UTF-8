'use client';

import { Suspense, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import {
  apiOAuthExchange,
  apiGetActiveRoom,
  setTokens,
  setAdmin,
  getRoleFromToken,
  SUSPENDED_STORAGE_KEY,
} from '@/lib/api';

function OAuthCallbackInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [error, setError] = useState('');
  const hasFetched = useRef(false);

  useEffect(() => {
    const code = searchParams.get('code');
    if (!code) {
      setError('로그인 코드가 없어요');
      return;
    }
    // OAuthCodeStore의 code는 1회용이라, StrictMode의 이펙트 2회 실행 시 재요청되면 실패함
    if (hasFetched.current) return;
    hasFetched.current = true;

    (async () => {
      try {
        const data = await apiOAuthExchange(code);
        setTokens(data.accessToken, data.refreshToken);

        if (data.needsOnboarding) {
          router.replace('/me');
          return;
        }

        if (getRoleFromToken(data.accessToken) === 'ADMIN') {
          setAdmin();
          router.replace('/admin/stats');
          return;
        }

        try {
          await apiGetActiveRoom();
        } catch (checkErr: unknown) {
          if ((checkErr as { status?: number })?.status === 403) {
            localStorage.setItem(SUSPENDED_STORAGE_KEY, '1');
            router.replace('/me');
            return;
          }
        }
        router.replace('/');
      } catch (err) {
        console.error('OAuth Exchange Error:', err);
        setError('소셜 로그인에 실패했어요');
      }
    })();
  }, [searchParams, router]);

  if (error) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 16, fontFamily: "Arial, 'Helvetica Neue', sans-serif" }}>
        <div style={{ fontSize: 14, color: '#ea4c4c' }}>{error}</div>
        <Link href="/login" style={{ color: '#3b7ff2', fontWeight: 600, textDecoration: 'none', fontSize: 14 }}>
          로그인으로 돌아가기
        </Link>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: "Arial, 'Helvetica Neue', sans-serif" }}>
      <span style={{ color: '#9aa0a6', fontSize: 13 }}>로그인 처리 중...</span>
    </div>
  );
}

export default function OAuthCallbackPage() {
  return (
    <Suspense
      fallback={
        <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: "Arial, 'Helvetica Neue', sans-serif" }}>
          <span style={{ color: '#9aa0a6', fontSize: 13 }}>로그인 처리 중...</span>
        </div>
      }
    >
      <OAuthCallbackInner />
    </Suspense>
  );
}
