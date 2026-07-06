package com.back.domain.match.matchRequest.repository;

import com.back.domain.chat.chatRoom.entity.ChatRoomStatus;
import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.entity.MatchStatus;
import com.back.domain.match.matchRequest.entity.Situation;
import com.back.domain.member.member.entity.Industry;
import com.back.domain.member.member.entity.Member;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    // 동시성 문제의 안전장치: PENDING 상태일 때만 MATCHED로 바꾸는 원자적(compare-and-swap) 업데이트.
    // 두 트랜잭션이 같은 row를 동시에 노려도 DB가 UPDATE 문을 순서대로 처리해줘서,
    // 딱 한쪽만 1(성공)을 받고 나머지는 0(이미 남이 채감)을 받는다.
    // 참고: 이 벌크 UPDATE는 @Version(낙관적 락)을 우회한다.
    // CAS(WHERE status='PENDING') 조건 자체가 동시성 안전장치 역할을 한다.
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE match_request SET status = 'MATCHED', modified_at = CURRENT_TIMESTAMP " + "WHERE id = :id AND status = 'PENDING'", nativeQuery = true)
    int claimPending(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE match_request SET room_id = :roomId WHERE id = :id", nativeQuery = true)
    void assignRoom(@Param("id") UUID id, @Param("roomId") UUID roomId);

    // 둘 중 하나만 선점 성공하고 나머지가 실패했을 때, 성공한 쪽을 다시 PENDING으로 되돌리는 보정 동작.
    // 이게 없으면 "선점은 됐는데 방은 없는" 상태로 영구히 남을 수 있다.
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE match_request SET status = 'PENDING' WHERE id = :id", nativeQuery = true)
    void revertToPending(@Param("id") UUID id);

    // 매칭 성사 시 양쪽 MatchRequest가 거의 동시에 modifiedAt이 갱신되므로,
    // room 기준 중복 제거는 서비스 레이어에서 처리한다 (넉넉히 pageable로 가져와 추림)
    // room은 LAZY라 getId()만 쓰면 프록시 초기화 없이 값을 얻지만(N+1 안 남),
    // 이후 room의 다른 필드를 참조하게 되는 실수를 방지하기 위해 fetch join을 명시해둔다
    @Query("SELECT mr FROM MatchRequest mr JOIN FETCH mr.room WHERE mr.status = :status ORDER BY mr.modifiedAt DESC")
    List<MatchRequest> findRecentByStatus(@Param("status") MatchStatus status, Pageable pageable);
}