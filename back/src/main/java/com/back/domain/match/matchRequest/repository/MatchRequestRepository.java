package com.back.domain.match.matchRequest.repository;

import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.entity.MatchStatus;
import com.back.domain.match.matchRequest.entity.Situation;
import com.back.domain.member.member.entity.Industry;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, UUID> {
    boolean existsByMemberAndStatus(Member member, MatchStatus status);

    @Query("SELECT r FROM MatchRequest r WHERE r.industry = :industry AND r.situation = :situation AND r.status = :status")
    List<MatchRequest> findPendingByIndustryAndSituation(
            @Param("industry") Industry industry,
            @Param("situation") Situation situation,
            @Param("status") MatchStatus status);

    @Query("SELECT r FROM MatchRequest r WHERE r.industry = :industry  AND r.status = :status")
    List<MatchRequest> findPendingByIndustry(
            @Param("industry") Industry industry,
            @Param("status") MatchStatus status);

    @Query("SELECT COUNT(DISTINCT r.room.id) FROM MatchRequest r WHERE r.status = :status AND r.createdAt BETWEEN :start AND :end")
    long countTodayMatches(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") MatchStatus status
    );

    @Query("SELECT r FROM MatchRequest r WHERE r.status = :status AND r.requestedAt < :expiredBefore")
    List<MatchRequest> findExpiredPending(
            @Param("status") MatchStatus status,
            @Param("expiredBefore") LocalDateTime expiredBefore);

    List<MatchRequest> findByMemberAndRoomStatus(Member member, ChatRoomStatus status);

    // 매칭 성사 시 양쪽 MatchRequest가 거의 동시에 modifiedAt이 갱신되므로,
    // room 기준 중복 제거는 서비스 레이어에서 처리한다 (넉넉히 20개 가져와 10개로 추림)
    List<MatchRequest> findTop20ByStatusOrderByModifiedAtDesc(MatchStatus status);

}
