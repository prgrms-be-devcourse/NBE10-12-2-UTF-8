package com.back.domain.bot;

import com.back.domain.match.matchRequest.entity.MatchRequest;
import com.back.domain.match.matchRequest.entity.MatchStatus;
import com.back.domain.match.matchRequest.entity.Situation;
import com.back.domain.match.matchRequest.repository.MatchRequestRepository;
import com.back.domain.match.matchRequest.service.MatchRequestService;
import com.back.domain.member.member.entity.Industry;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import com.back.domain.member.member.service.MemberService;
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
class BotMatchIntegrationTest {

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private MatchRequestService matchRequestService;
    @Autowired
    private MatchRequestRepository matchRequestRepository;

    private MatchRequest createPendingRequest(Member member, Situation situation, long secondsAgo) {
        MatchRequest matchRequest = matchRequestRepository.save(new MatchRequest(member, situation));
        ReflectionTestUtils.setField(matchRequest, "requestedAt", LocalDateTime.now().minusSeconds(secondsAgo));
        return matchRequestRepository.saveAndFlush(matchRequest);
    }

    @Test
    @DisplayName("40초가 안 지났으면 실제 유저가 없어도 봇과 매칭되지 않는다")
    void 그레이스_기간_전에는_봇_매칭_안됨() {
        // Given - 30초 전 요청 (팀의 Tier2 임계값은 지났지만, 봇 폴백 임계값 40초는 아직)
        Member user = memberService.join("bot_grace_early@test.com", "1234", Industry.IT, "USER");
        MatchRequest userRequest = createPendingRequest(user, Situation.NIGHT_WORK, 35);

        // When
        matchRequestService.tryMatch(userRequest);

        // Then - 다른 실제 유저가 없으니 여전히 PENDING
        MatchRequest refreshed = matchRequestRepository.findById(userRequest.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(MatchStatus.PENDING);
    }

    @Test
    @DisplayName("40초가 지나면 실제 유저를 못 찾은 요청이 봇과 매칭된다")
    void 그레이스_기간_지나면_봇과_매칭() {
        // Given - 41초 전 요청 (봇 폴백 임계값 40초 초과)
        Member user = memberService.join("bot_grace_fallback@test.com", "1234", Industry.IT, "USER");
        MatchRequest userRequest = createPendingRequest(user, Situation.NIGHT_WORK, 41);

        // When
        matchRequestService.tryMatch(userRequest);

        // Then
        MatchRequest refreshed = matchRequestRepository.findById(userRequest.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(MatchStatus.MATCHED);
        assertThat(refreshed.getRoom()).isNotNull();
    }

    @Test
    @DisplayName("실제 유저가 있으면 40초가 지나도 봇이 아니라 실제 유저와 매칭된다")
    void 실유저_있으면_봇보다_우선() {
        // Given - 둘 다 41초 전 요청, 같은 산업군
        Member userA = memberService.join("bot_priority_a@test.com", "1234", Industry.IT, "USER");
        Member userB = memberService.join("bot_priority_b@test.com", "1234", Industry.IT, "USER");
        MatchRequest reqA = createPendingRequest(userA, Situation.NIGHT_WORK, 41);
        MatchRequest reqB = createPendingRequest(userB, Situation.MEETING_BOMB, 41);

        // When
        matchRequestService.retryPendingMatches();

        // Then - 봇이 아니라 서로 매칭돼야 함
        MatchRequest refreshedA = matchRequestRepository.findById(reqA.getId()).orElseThrow();
        MatchRequest refreshedB = matchRequestRepository.findById(reqB.getId()).orElseThrow();

        assertThat(refreshedA.getStatus()).isEqualTo(MatchStatus.MATCHED);
        assertThat(refreshedB.getStatus()).isEqualTo(MatchStatus.MATCHED);
        assertThat(refreshedA.getRoom().getId()).isEqualTo(refreshedB.getRoom().getId());

        // 봇 계정은 매칭에 관여 안 했어야 함 (봇 요청 자체가 새로 안 생김)
        Member bot = memberRepository.findByEmail(BotAccounts.emailFor(Industry.IT)).orElseThrow();
        boolean botHasMatchRequest = matchRequestRepository.findAll().stream()
                .anyMatch(r -> r.getMember().getId().equals(bot.getId()));
        assertThat(botHasMatchRequest).isFalse();
    }
}