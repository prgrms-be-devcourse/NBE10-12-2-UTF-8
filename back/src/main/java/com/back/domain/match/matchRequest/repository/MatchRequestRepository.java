package com.back.domain.match.matchRequest.repository;

import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.entity.MatchStatus;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, UUID> {
    boolean existsByMemberAndStatus(Member member, MatchStatus status);
    Optional<MatchRequest> findFirstByMember_IndustryAndSituationAndStatus(
            String industry, String situation, MatchStatus status);
    Optional<MatchRequest> findFirstByMember_IndustryAndStatus(
            String industry, MatchStatus status);



}
