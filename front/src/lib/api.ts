const BASE = "";

/* ── Token / admin storage ──────────────────────────────────────── */
export const getToken = (): string | null =>
  typeof window !== "undefined" ? localStorage.getItem("accessToken") : null;

export const setTokens = (accessToken: string, refreshToken: string) => {
  localStorage.setItem("accessToken", accessToken);
  localStorage.setItem("refreshToken", refreshToken);
};

export const clearTokens = () => {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  localStorage.removeItem("isAdmin");
};

export const setAdmin = () => localStorage.setItem("isAdmin", "1");
export const isAdmin = () =>
  typeof window !== "undefined" && localStorage.getItem("isAdmin") === "1";

// JWT의 role 클레임을 읽어 관리자 여부를 판별 (백엔드는 별도의 /me role 필드를 내려주지 않음)
export const getRoleFromToken = (token: string): string | null => {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
        .join(""),
    );
    return (JSON.parse(json).role as string) ?? null;
  } catch {
    return null;
  }
};

/* ── Industry mapping ────────────────────────────────────────────── */
// 백엔드 Industry enum @JsonValue 가 한글 라벨을 그대로 직렬화하므로 표시명 = API 값
export const INDUSTRY_CODES: Record<string, string> = {
  "IT/개발": "IT/개발",
  서비스업: "서비스업",
  금융업: "금융업",
  의료서비스: "의료서비스",
  유통: "유통",
  "미디어/디자인": "미디어/디자인",
  사무업: "사무업",
};

export const INDUSTRY_NAMES: Record<string, string> = Object.fromEntries(
  Object.entries(INDUSTRY_CODES).map(([k, v]) => [v, k]),
);

/* ── Base fetch ─────────────────────────────────────────────────── */
async function req<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getToken();
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options?.headers ?? {}),
    },
  });
  if (res.status === 204) return null as T;
  const body = await res.json();
  if (!res.ok)
    throw Object.assign(new Error(body?.msg ?? res.statusText), {
      status: res.status,
    });
  return body.data as T;
}

/* ── Auth ───────────────────────────────────────────────────────── */
export const apiLogin = (email: string, password: string) =>
  req<{
    grantType: string;
    accessToken: string;
    refreshToken: string;
    accessTokenExpiresIn: number;
  }>("/api/v1/members/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });

export const apiSignup = (email: string, password: string, industry: string) =>
  req<{ id: string; email: string; industry: string }>(
    "/api/v1/members/signup",
    { method: "POST", body: JSON.stringify({ email, password, industry }) },
  );

export const apiLogout = () =>
  req<null>("/api/v1/members/logout", { method: "POST" });

export const apiRefreshToken = () =>
  req<{ grantType: string; accessToken: string; refreshToken: string; accessTokenExpiresIn: number }>(
    "/api/v1/members/refresh",
    { method: "POST" },
  );

export const apiGetMe = () =>
  req<{ email: string; industry: string }>("/api/v1/members/me");

export const apiUpdateMe = (industry: string) =>
  req<{ industry: string }>("/api/v1/members/me", {
    method: "PATCH",
    body: JSON.stringify({ industry }),
  });

export const apiDeleteMe = () =>
  req<null>("/api/v1/members/me", { method: "DELETE" });

export type MatchHistoryDto = {
  matchedAt: string;
  industry: string;
  situation: string;
  status: 'ACTIVE' | 'CLOSED';
};

export const apiGetMatchHistory = () =>
  req<MatchHistoryDto[]>("/api/v1/members/me/matches");

/* ── Match ──────────────────────────────────────────────────────── */
export type MatchResponseDto = {
  matchRequestId: string;
  status: 'PENDING' | 'MATCHED';
  requestedAt: string;
  chatRoomId?: string;
};

export const apiCreateMatch = (situation: string) =>
  req<MatchResponseDto>("/api/v1/matches", { method: "POST", body: JSON.stringify({ situation }) });

export const apiGetMatch = (matchRequestId: string) =>
  req<MatchResponseDto>(`/api/v1/matches/${matchRequestId}`);

export const apiCancelMatch = (matchRequestId: string) =>
  req<null>(`/api/v1/matches/${matchRequestId}`, { method: "DELETE" });

/* ── Chat ───────────────────────────────────────────────────────── */
export type ChatRoom = {
  roomId: string;
  status: "ACTIVE" | "CLOSED";
  maxParticipants: number;
  createdAt: string;
  closedAt?: string;
};

export const apiGetRoom = (roomId: string) =>
  req<ChatRoom>(`/api/v1/rooms/${roomId}`);

// 로그인한 회원이 현재 참여 중인 활성 채팅방 조회. 없으면 data: null
export const apiGetActiveRoom = () =>
  req<ChatRoom | null>("/api/v1/rooms/active");

// 채팅방 종료 (status -> CLOSED)
export const apiCloseRoom = (roomId: string) =>
  req<ChatRoom>(`/api/v1/rooms/${roomId}`, { method: "PATCH" });

export const apiSendMessage = (roomId: string, content: string) =>
  req<ChatMsg>(`/api/v1/rooms/${roomId}/messages`, {
    method: "POST",
    body: JSON.stringify({ content }),
  });

export type ChatMsg = {
  messageId: string;
  roomId: string;
  senderNickname: string;
  content: string;
  createdAt: string;
  isMine: boolean;
};

// after: LocalDateTime ISO 문자열 (마지막 수신 메시지의 createdAt). 없으면 전체 조회.
// 백엔드가 종료된 방에 대해 HTTP 200 + resultCode "200-3" 을 반환하므로 closed 플래그로 구분.
export async function apiGetMessages(
  roomId: string,
  after?: string,
): Promise<{ msgs: ChatMsg[] | null; closed: boolean }> {
  const token = getToken();
  const url = `/api/v1/rooms/${roomId}/messages${after ? `?after=${encodeURIComponent(after)}` : ""}`;
  const res = await fetch(url, {
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
  if (res.status === 204) return { msgs: null, closed: false };
  const body = await res.json();
  if (!res.ok)
    throw Object.assign(new Error(body?.msg ?? res.statusText), {
      status: res.status,
    });
  if (body.resultCode === "200-3") return { msgs: null, closed: true };
  return { msgs: body.data as ChatMsg[] | null, closed: false };
}

/* ── Admin ──────────────────────────────────────────────────────── */
export type AdminMember = {
  memberId: string;
  email: string;
  industry: string;
  isSuspended: boolean;
  createdAt: string;
  role?: string;
};

export const apiGetDashboard = () =>
  req<{
    matchStatistics: {
      totalMembers: number;
      todayMatches: number;
      activeChatRooms: number;
    };
    industryStatistics: Array<{ industry: string; count: number }>;
    recentMatchLogs: Array<{ matchedAt: string; industry: string; situation: string }>;
  }>("/api/v1/admin/dashboard");

export const apiGetAdminMembers = (page = 0, size = 10) =>
  req<{
    content: AdminMember[];
    totalPages: number;
    totalElements: number;
    pageable: { pageNumber: number; pageSize: number };
  }>(`/api/v1/admin/members?page=${page}&size=${size}`);

export const apiGetAdminMember = (identifier: string) =>
  req<AdminMember>(`/api/v1/admin/members/${identifier}`);

export const apiSuspendMember = (memberId: string) =>
  req<AdminMember>(`/api/v1/admin/members/${memberId}/suspend`, { method: 'PATCH' });

/* ── Reports ────────────────────────────────────────────────────── */
export type ReportResult = {
  reportId: string;
  status: 'PENDING' | 'PROCESSED';
  createdAt: string;
};

export const apiSubmitReport = (roomId: string, reportedMessageId: string, reason: string) =>
  req<ReportResult>('/api/v1/reports', {
    method: 'POST',
    body: JSON.stringify({ roomId, reportedMessageId, reason }),
  });

export type AdminReport = {
  reportId: string;
  reporterEmail: string;
  reportedEmail: string;
  reason: string;
  status: 'PENDING' | 'PROCESSED';
  createdAt: string;
};

export type AdminReportDetail = {
  reportId: string;
  reporterEmail: string;
  reportedEmail: string;
  status: 'PENDING' | 'PROCESSED';
  reportedMessages: Array<{
    senderNickname: string;
    senderLabel: string;
    content: string;
    sentAt: string;
    isTarget: boolean;
  }>;
};

export const apiGetAdminReports = (page = 0, size = 10) =>
  req<{
    content: AdminReport[];
    totalPages: number;
    totalElements: number;
    pageable: { pageNumber: number; pageSize: number };
  }>(`/api/v1/admin/reports?page=${page}&size=${size}`);

export const apiGetAdminReport = (reportId: string) =>
  req<AdminReportDetail>(`/api/v1/admin/reports/${reportId}`);

export const apiToggleReportStatus = (reportId: string) =>
  req<{ reportId: string; status: 'PENDING' | 'PROCESSED' }>(`/api/v1/admin/reports/${reportId}/status`, { method: 'PATCH' });
