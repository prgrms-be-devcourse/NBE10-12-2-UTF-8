package com.back.domain.match.matchRequest.repository;

import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.entity.MatchStatus;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, UUID> {
    boolean existsByMemberAndStatus(Member member, MatchStatus status);

    @Query("SELECT r FROM MatchRequest r WHERE r.member.industry = :industry AND r.situation = :situation AND r.status = :status")
    Optional<MatchRequest> findPendingByIndustryAndSituation(
            @Param("industry") String industry,
            @Param("situation") String situation,
            @Param("status") MatchStatus status);

    @Query("SELECT r FROM MatchRequest r WHERE r.member.industry = :industry AND r.status = :status")
    Optional<MatchRequest> findPendingByIndustry(
            @Param("industry") String industry,
            @Param("status") MatchStatus status);
}
