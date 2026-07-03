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
import java.util.Collection;
import java.util.UUID;
import java.util.List;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, UUID> {
    boolean existsByMemberAndStatus(Member member, MatchStatus status);

    @Query("SELECT r FROM MatchRequest r WHERE r.industry = :industry AND r.situation = :situation AND r.status = :status ORDER BY r.requestedAt ASC")
    List<MatchRequest> findPendingByIndustryAndSituation(
            @Param("industry") Industry industry,
            @Param("situation") Situation situation,
            @Param("status") MatchStatus status);

    // 같은 업종 + 비슷한 상황 여러개
    @Query("SELECT r FROM MatchRequest r WHERE r.industry = :industry AND r.situation IN :situations AND r.status = :status ORDER BY r.requestedAt ASC")
    List<MatchRequest> findPendingByIndustryAndSituations(
            @Param("industry") Industry industry,
            @Param("situations") Collection<Situation> situations,
            @Param("status") MatchStatus status);

    @Query("SELECT r FROM MatchRequest r WHERE r.industry = :industry  AND r.status = :status ORDER BY r.requestedAt ASC")
    List<MatchRequest> findPendingByIndustry(
            @Param("industry") Industry industry,
            @Param("status") MatchStatus status);

    @Query("SELECT r FROM MatchRequest r WHERE r.status = :status ORDER BY r.requestedAt ASC")
    List<MatchRequest> findAllByStatus(@Param("status") MatchStatus status);

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
}
