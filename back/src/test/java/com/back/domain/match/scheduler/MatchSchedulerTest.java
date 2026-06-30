package com.back.domain.match.scheduler;

import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.entity.MatchStatus;
import com.back.domain.match.matchRequest.repository.MatchRequestRepository;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
public class MatchSchedulerTest {

    @Autowired
    private MatchScheduler matchScheduler;

    @Autowired
    private MatchRequestRepository matchRequestRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member createMember(String email) {
        return memberRepository.save(new Member(email, "1234", "IT", "USER"));
    }

    private MatchRequest createPendingRequest(Member member, LocalDateTime requestedAt) {
        MatchRequest matchRequest = matchRequestRepository.save(new MatchRequest(member, "야근 중"));
        ReflectionTestUtils.setField(matchRequest, "requestedAt", requestedAt);
        return matchRequestRepository.saveAndFlush(matchRequest);
    }

    @Test
    @DisplayName("5분이 지난 PENDING 요청은 자동으로 삭제된다")
    void t1() {
        Member member = createMember("expired@test.com");
        MatchRequest matchRequest = createPendingRequest(member, LocalDateTime.now().minusMinutes(6));

        matchScheduler.cancelExpiredMatchRequests();

        assertThat(matchRequestRepository.findById(matchRequest.getId())).isEmpty();
    }

    @Test
    @DisplayName("5분이 지나지 않은 PENDING 요청은 삭제되지 않는다")
    void t2() {
        Member member = createMember("fresh@test.com");
        MatchRequest matchRequest = createPendingRequest(member, LocalDateTime.now().minusMinutes(4));

        matchScheduler.cancelExpiredMatchRequests();

        assertThat(matchRequestRepository.findById(matchRequest.getId())).isPresent();
    }

    @Test
    @DisplayName("MATCHED 요청은 5분이 지나도 삭제되지 않는다")
    void t3() {
        Member member = createMember("matched@test.com");
        MatchRequest matchRequest = createPendingRequest(member, LocalDateTime.now().minusMinutes(6));
        ReflectionTestUtils.setField(matchRequest, "status", MatchStatus.MATCHED);
        matchRequestRepository.saveAndFlush(matchRequest);

        matchScheduler.cancelExpiredMatchRequests();

        assertThat(matchRequestRepository.findById(matchRequest.getId())).isPresent();
    }
}
