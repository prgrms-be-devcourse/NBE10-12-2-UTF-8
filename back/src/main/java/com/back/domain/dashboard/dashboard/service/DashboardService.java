package com.back.domain.dashboard.dashboard.service;

import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.chat.chatRoom.repository.ChatRoomRepository;
import com.back.domain.dashboard.dashboard.dto.DashboardResponseDto;
import com.back.domain.dashboard.dashboard.dto.IndustryStatisticsDto;
import com.back.domain.dashboard.dashboard.dto.MatchStatisticsDto;
import com.back.domain.match.matchRequest.entity.MatchStatus;
import com.back.domain.match.matchRequest.repository.MatchRequestRepository;
import com.back.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {
    private final MemberRepository memberRepository;
    private final MatchRequestRepository matchRequestRepository;
    private final ChatRoomRepository chatRoomRepository;

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

        return new DashboardResponseDto(
                new MatchStatisticsDto(totalMembers, todayMatches, activeChatRooms),
                industryStatistics
        );
    }
}