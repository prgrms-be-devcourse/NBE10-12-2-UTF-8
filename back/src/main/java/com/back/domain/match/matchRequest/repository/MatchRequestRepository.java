package com.back.domain.match.matchRequest.repository;

import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.entity.MatchStatus;
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
            @Param("industry") String industry,
            @Param("situation") String situation,
            @Param("status") MatchStatus status);

    @Query("SELECT r FROM MatchRequest r WHERE r.industry = :industry  AND r.status = :status")
    List<MatchRequest> findPendingByIndustry(
            @Param("industry") String industry,
            @Param("status") MatchStatus status);

    @Query("SELECT COUNT(DISTINCT r.room.id) FROM MatchRequest r WHERE r.status = :status AND r.createdAt BETWEEN :start AND :end")
    long countTodayMatches(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") MatchStatus status
    );
}
