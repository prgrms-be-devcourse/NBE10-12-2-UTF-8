const BASE = '';

/* ── Token / admin storage ──────────────────────────────────────── */
export const getToken = (): string | null =>
  typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;

export const setTokens = (accessToken: string, refreshToken: string) => {
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
};

export const clearTokens = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('isAdmin');
};

export const setAdmin = () => localStorage.setItem('isAdmin', '1');
export const isAdmin = () =>
  typeof window !== 'undefined' && localStorage.getItem('isAdmin') === '1';

/* ── Industry mapping (display ↔ API code) ──────────────────────── */
export const INDUSTRY_CODES: Record<string, string> = {
  'IT/개발':       'IT',
  '서비스업':      '서비스',
  '금융업':        '금융',
  '의료서비스':    '의료',
  '유통':          '유통',
  '미디어/디자인': '미디어',
  '사무업':        '사무',
};

export const INDUSTRY_NAMES: Record<string, string> = Object.fromEntries(
  Object.entries(INDUSTRY_CODES).map(([k, v]) => [v, k])
);

/* ── Base fetch ─────────────────────────────────────────────────── */
async function req<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getToken();
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options?.headers ?? {}),
    },
  });
  if (res.status === 204) return null as T;
  const body = await res.json();
  if (!res.ok) throw Object.assign(new Error(body?.msg ?? res.statusText), { status: res.status });
  return body.data as T;
}

/* ── Auth ───────────────────────────────────────────────────────── */
export const apiLogin = (email: string, password: string) =>
  req<{ grantType: string; accessToken: string; refreshToken: string; accessTokenExpiresIn: number }>(
    '/api/v1/members/login',
    { method: 'POST', body: JSON.stringify({ email, password }) }
  );

export const apiSignup = (email: string, password: string, industry: string) =>
  req<{ id: string; email: string; industry: string }>(
    '/api/v1/members/signup',
    { method: 'POST', body: JSON.stringify({ email, password, industry }) }
  );

export const apiLogout = () => req<null>('/api/v1/members/logout', { method: 'POST' });

export const apiGetMe = () => req<{ email: string; industry: string }>('/api/v1/members/me');

export const apiUpdateMe = (industry: string) =>
  req<{ industry: string }>('/api/v1/members/me', { method: 'PATCH', body: JSON.stringify({ industry }) });

export const apiDeleteMe = () => req<null>('/api/v1/members/me', { method: 'DELETE' });

/* ── Match ──────────────────────────────────────────────────────── */
export const apiCreateMatch = (situation: string) =>
  req<{ matchRequestId: string; status: string; requestedAt: string }>(
    '/api/v1/matches',
    { method: 'POST', body: JSON.stringify({ situation }) }
  );

export const apiGetMatch = (matchRequestId: string) =>
  req<{ status: string; chatRoomId?: string }>(`/api/v1/matches/${matchRequestId}`);

export const apiCancelMatch = (matchRequestId: string) =>
  req<null>(`/api/v1/matches/${matchRequestId}`, { method: 'DELETE' });

/* ── Chat ───────────────────────────────────────────────────────── */
export type ChatRoom = {
  roomId: string;
  status: 'ACTIVE' | 'CLOSED';
  maxParticipants: number;
  createdAt: string;
  closedAt?: string;
};

export const apiGetRoom = (roomId: string) =>
  req<ChatRoom>(`/api/v1/rooms/${roomId}`);

// 로그인한 회원이 현재 참여 중인 활성 채팅방 조회. 없으면 data: null
export const apiGetActiveRoom = () => req<ChatRoom | null>('/api/v1/rooms/active');

// 채팅방 종료 (status -> CLOSED)
export const apiCloseRoom = (roomId: string) =>
  req<ChatRoom>(`/api/v1/rooms/${roomId}`, { method: 'PATCH' });

export const apiSendMessage = (roomId: string, content: string) =>
  req<{ messageId: string; roomId: string; senderNickname: string; content: string; createdAt: string }>(
    `/api/v1/rooms/${roomId}/messages`,
    { method: 'POST', body: JSON.stringify({ content }) }
  );

export type ChatMsg = {
  messageId: string;
  senderNickname: string;
  content: string;
  createdAt: string;
  isMine: boolean;
};

// 백엔드에 메시지 조회(GET) 엔드포인트가 아직 없어 상대 메시지 폴링은 불가능 — 보낸 메시지만 로컬에 표시

/* ── Admin ──────────────────────────────────────────────────────── */
export type AdminMember = {
  memberId: string;
  email: string;
  industry: string;
  isSuspended: boolean;
  createdAt: string;
};

export const apiGetDashboard = () =>
  req<{
    matchStatistics: { totalMembers: number; todayMatches: number; activeChatRooms: number };
    industryStatistics: Array<{ industry: string; count: number }>;
  }>('/api/v1/adm/dashboard');

export const apiGetAdminMembers = (page = 0, size = 10) =>
  req<{
    content: AdminMember[];
    totalPages: number;
    totalElements: number;
    pageable: { pageNumber: number; pageSize: number };
  }>(`/api/v1/adm/members?page=${page}&size=${size}`);
