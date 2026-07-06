package com.back.domain.bot;

import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import com.back.domain.chat.chatRoomMessage.service.ChatMessageService;
import com.back.domain.member.member.entity.Industry;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// BotReplyService에서 sleep과 트랜잭션을 분리하기 위해 뺀 실행부.
// 같은 클래스 내 self-invocation은 @Transactional 프록시를 안 타서 별도 빈으로 분리했다.
@Slf4j
@Component
@RequiredArgsConstructor
public class BotReplyExecutor {
    private final ChatMessageService chatMessageService;
    private final MemberRepository memberRepository;
    private final BotAiClient botAiClient;

    private static final int HISTORY_LIMIT = 12;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(BotReplyTriggerEvent event) {
        Member bot = memberRepository.findById(event.botMemberId())
                .orElseThrow(() -> new IllegalStateException("봇 계정을 찾을 수 없습니다: " + event.botMemberId()));

        String reply = generateReply(event.roomId(), bot);
        if (reply == null || reply.isBlank()) {
            return;
        }
        chatMessageService.sendMessage(event.roomId(), bot, reply);
    }

    private String generateReply(UUID roomId, Member bot) {
        // 최근 대화 이력 조회
        List<ChatMessage> recentDesc = chatMessageService.getRecentMessages(roomId, HISTORY_LIMIT);

        // 마지막 메시지가 이미 봇이면 응답 안함
        if (!recentDesc.isEmpty()) {
            ChatMessage latest = recentDesc.get(0);
            boolean lastIsBot = latest.getParticipant()
                    .getMember()
                    .getId()
                    .equals(bot.getId());
            if (lastIsBot) {
                log.info("마지막 메시지가 봇이라 응답 생략");
                return null;
            }
        }

        Industry industry = bot.getIndustry();

        List<Map.Entry<Boolean, String>> conversation = recentDesc.reversed().stream()
                .map(m -> new AbstractMap.SimpleEntry<>(
                        m.getParticipant().getMember().getId().equals(bot.getId()),
                        m.getContent()
                ))
                .map(e -> (Map.Entry<Boolean, String>) e)
                .toList();

        log.info("봇 대화 이력 = {}", conversation);

        String systemInstruction = """
당신은 '탕비실'이라는 익명 직장인 채팅 서비스의 대화 상대입니다.
상대방은 %s 업종에서 일하는 직장인입니다.

반드시 한국어로만 답변하세요. 영어, 태국어, 베트남어, 중국어 등
다른 언어를 절대 섞지 마세요.

규칙:
- 마지막 사용자 메시지에만 답변하세요.
- 사용자가 말하지 않은 경험을 지어내지 마세요.
- '나도 그랬어', '나도 겪어봤어' 같은 표현을 사용하지 마세요.
- 과한 공감을 하지 마세요.
- 친구처럼 가볍게 대화하세요.
- 답변은 1문장 또는 최대 2문장.
- 편한 반말 사용.
- 필요하면 ㅋㅋ 사용 가능.
- 사용자의 감정에만 반응하지 말고 내용에도 반응하세요.

예시:

사용자: 안녕
답변: 하이 ㅋㅋ

사용자: 힘들다
답변: 왜 힘듦?

사용자: 상사한테 혼났어
답변: 왜 혼남? 뭔 일 있었음?

사용자: 오늘 퇴근했다
답변: 오 고생했네 ㅋㅋ

사용자: 개짜증나네
답변: 또 뭔 일인데 ㅋㅋ

사용자: 배고프다
답변: 뭐 먹을 건데?

사용자: 코드리뷰 20개 달림
답변: 와 그건 좀 빡세네 ㅋㅋ

절대로 상담사처럼 말하지 마세요.
절대로 과도하게 공감하지 마세요.
절대로 본인 경험을 말하지 마세요.
""".formatted(industry != null ? industry.getLabel() : "일반 사무직");

        conversation.forEach(c ->
                log.debug("{} : {}", c.getKey() ? "BOT" : "USER", c.getValue())
        );
        String aiReply = conversation.isEmpty()
                ? botAiClient.generateReply(systemInstruction, List.of())
                : botAiClient.generateReply(systemInstruction, conversation);

        log.info("AI 응답 = {}", aiReply);

        if (aiReply != null) {
            return aiReply;
        }
        log.warn("AI 실패 -> 폴백 메시지 사용");

        // AI 호출 실패/키 미설정 시 캔드 메시지로 폴백
        List<String> lines = BotReplyMessages.LINES;
        int randomIndex = java.util.concurrent.ThreadLocalRandom.current().nextInt(lines.size());
        return lines.get(randomIndex);
    }
}