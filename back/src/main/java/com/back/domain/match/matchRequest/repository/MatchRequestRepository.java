package com.back.domain.match.matchRequest.repository;

import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.entity.MatchStatus;
import com.back.domain.match.matchRequest.entity.Situation;
import com.back.domain.member.member.entity.Industry;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.domain.Pageable;
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
    // 매칭 성사 시 양쪽 MatchRequest가 거의 동시에 modifiedAt이 갱신되므로,
    // room 기준 중복 제거는 서비스 레이어에서 처리한다 (넉넉히 pageable로 가져와 추림)
    // room은 LAZY라 getId()만 쓰면 프록시 초기화 없이 값을 얻지만(N+1 안 남),
    // 이후 room의 다른 필드를 참조하게 되는 실수를 방지하기 위해 fetch join을 명시해둔다
    @Query("SELECT mr FROM MatchRequest mr JOIN FETCH mr.room WHERE mr.status = :status ORDER BY mr.modifiedAt DESC")
    List<MatchRequest> findRecentByStatus(@Param("status") MatchStatus status, Pageable pageable);
}
