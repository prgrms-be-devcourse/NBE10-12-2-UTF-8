export default function ChromeWindow({ children }: { children: React.ReactNode }) {
  return (
    <div className="w-full max-w-[900px] min-w-[320px] rounded-xl overflow-hidden border border-[#d0d0d0]"
      style={{ boxShadow: '0 8px 40px rgba(0,0,0,0.18), 0 2px 8px rgba(0,0,0,0.08)' }}>
      {/* Title bar */}
      <div className="bg-[#e4e4e4] px-3 pt-2.5 pb-0 select-none">
        <div className="flex items-center gap-1.5 mb-2">
          <div className="w-3 h-3 rounded-full bg-[#ff5f56]" style={{ border: '0.5px solid rgba(0,0,0,0.15)' }} />
          <div className="w-3 h-3 rounded-full bg-[#ffbd2e]" style={{ border: '0.5px solid rgba(0,0,0,0.15)' }} />
          <div className="w-3 h-3 rounded-full bg-[#27c93f]" style={{ border: '0.5px solid rgba(0,0,0,0.15)' }} />
        </div>
        {/* Tab */}
        <div className="flex items-end">
          <div className="flex items-center gap-1.5 bg-white rounded-t-[6px] px-3 py-1.5 text-[11px] text-[#3c4043] max-w-[220px]">
            <svg className="w-3 h-3 flex-shrink-0" viewBox="0 0 16 16" fill="none">
              <circle cx="8" cy="8" r="3" fill="#3b7ff2" />
              <circle cx="8" cy="8" r="6" stroke="#3b7ff2" strokeWidth="1.5" fill="none" />
              <circle cx="8" cy="8" r="2" fill="#ea4c4c" />
            </svg>
            <span className="truncate font-medium">findle — 익명 직장인 랜덤채팅</span>
          </div>
          <div className="flex-1" />
        </div>
      </div>

      {/* Address bar */}
      <div className="bg-[#ebebeb] px-3 py-1.5 flex items-center gap-2 border-b border-[#d0d0d0]">
        <div className="flex items-center gap-0.5 text-[#808080]">
          <button className="p-1 rounded hover:bg-[#d8d8d8] disabled:opacity-30" disabled>
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <button className="p-1 rounded hover:bg-[#d8d8d8] disabled:opacity-30" disabled>
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
            </svg>
          </button>
          <button className="p-1 rounded hover:bg-[#d8d8d8]">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>
        </div>
        <div className="flex-1 flex items-center bg-white rounded-full px-3 py-1 text-[13px] text-[#3c4043] border border-[#d0d0d0] gap-2">
          <svg className="w-3.5 h-3.5 text-[#34a06b] flex-shrink-0" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4z" />
          </svg>
          <span className="text-[#5f6368]">findle.io</span>
        </div>
        <button className="p-1 rounded hover:bg-[#d8d8d8] text-[#808080]">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
          </svg>
        </button>
      </div>

      {/* Content */}
      <div className="bg-white">
        {children}
      </div>
    </div>
  );
}
