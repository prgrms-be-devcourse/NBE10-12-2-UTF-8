package com.back.domain.dashboard.dashboard.service;

import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.back.domain.dashboard.dashboard.dto.DashboardResponseDto;
import com.back.domain.dashboard.dashboard.dto.IndustryStatisticsDto;
import com.back.domain.dashboard.dashboard.dto.MatchStatisticsDto;
import com.back.domain.dashboard.dashboard.dto.RecentMatchLogDto;
import com.back.domain.match.matchRequest.entity.MatchStatus;
import com.back.domain.match.matchRequest.repository.MatchRequestRepository;
import com.back.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private final MemberRepository memberRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final ChatRoomRepository chatRoomRepository;

    private static final int RECENT_MATCH_LOG_SIZE = 10;
    // room 기준 중복 제거를 하고도 목표 개수(10)를 채울 수 있게 넉넉히 가져오는 배치 크기
    private static final int RECENT_MATCH_FETCH_BATCH_SIZE = 20;


    @Cacheable(value = "dashboard", key = "'getDashboard'")
    public DashboardResponseDto getDashboard() {
        long totalMembers = memberRepository.count();
        long todayMatches = matchRequestRepository.countTodayMatches(
                LocalDateTime.now().toLocalDate().atStartOfDay(),
                LocalDateTime.now(),
                MatchStatus.MATCHED
        );
        long activeChatRooms = chatRoomRepository.countByStatus(ChatRoomStatus.ACTIVE);

        List<IndustryStatisticsDto> industryStatistics =
                memberRepository.countByIndustry();

        List<RecentMatchLogDto> recentMatchLogs = getRecentMatchLogs();

        return new DashboardResponseDto(
                new MatchStatisticsDto(totalMembers, todayMatches, activeChatRooms),
                industryStatistics,
                recentMatchLogs
        );
    }
    // 매칭 성사 시 참여자 양쪽에 MatchRequest가 하나씩 생겨서 같은 room이 두 번 잡힌다.
    // room 기준으로 먼저 나온 것만 남기고, 회원 식별 정보 없이 날짜/산업군/상황만 노출한다.
    private List<RecentMatchLogDto> getRecentMatchLogs() {
        Set<UUID> seenRoomIds = new HashSet<>();
        return matchRequestRepository
                .findRecentByStatus(MatchStatus.MATCHED, PageRequest.of(0, RECENT_MATCH_FETCH_BATCH_SIZE))
                .stream()
                .filter(r -> seenRoomIds.add(r.getRoom().getId()))
                .limit(RECENT_MATCH_LOG_SIZE)
                .map(r -> new RecentMatchLogDto(r.getModifiedAt(), r.getIndustry(), r.getSituation()))
                .toList();
    }
}