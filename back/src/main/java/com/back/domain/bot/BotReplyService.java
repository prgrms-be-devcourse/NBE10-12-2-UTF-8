package com.back.domain.bot;

import com.back.domain.chat.chatRoomMessage.entity.ChatMessage;
import com.back.domain.chat.chatRoomMessage.service.ChatMessageService;
import com.back.domain.member.member.entity.Industry;
import com.back.domain.member.member.entity.Member;
import com.back.domain.member.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotReplyService {
    private final ChatMessageService chatMessageService;
    private final MemberRepository memberRepository;
    private final BotAiClient botAiClient;

    private static final int MIN_DELAY_MS = 1500;
    private static final int MAX_EXTRA_DELAY_MS = 3000;
    private static final int HISTORY_LIMIT = 12;

    // 매칭 트랜잭션/메시지 전송 트랜잭션이 실제로 커밋된 뒤에만 실행 - 커밋 전에 실행하면
    // 다른 스레드(비동기)가 아직 존재하지 않는 채팅방/메시지를 조회하게 되어 실패한다
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReplyTrigger(BotReplyTriggerEvent event) {
        try {
            Member bot = memberRepository.findById(event.botMemberId())
                    .orElseThrow(() -> new IllegalStateException("봇 계정을 찾을 수 없습니다: " + event.botMemberId()));

            Thread.sleep(MIN_DELAY_MS + new Random().nextInt(MAX_EXTRA_DELAY_MS));

            String reply = generateReply(event.roomId(), bot);
            chatMessageService.sendMessage(event.roomId(), bot, reply);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[BotReplyService] 응답 대기 중 인터럽트 발생 - roomId: {}", event.roomId());
        } catch (Exception e) {
            // 채팅방이 그 사이 종료됐거나 하는 등의 이유로 실패해도 서비스 전체엔 영향 없게 로그만 남김
            log.error("[BotReplyService] 응답 실패 - roomId: {}", event.roomId(), e);
        }
    }

    private String generateReply(java.util.UUID roomId, Member bot) {
        Industry industry = bot.getIndustry();

        // 최근 대화 이력을 시간순으로 정렬해 프롬프트 맥락으로 사용
        List<ChatMessage> recentDesc = chatMessageService.getMessagesByRoom(roomId).stream()
                .limit(HISTORY_LIMIT)
                .toList();
        List<Map.Entry<Boolean, String>> conversation = recentDesc.reversed().stream()
                .map(m -> new AbstractMap.SimpleEntry<>(
                        m.getParticipant().getMember().getId().equals(bot.getId()),
                        m.getContent()
                ))
                .map(e -> (Map.Entry<Boolean, String>) e)
                .toList();

        String systemInstruction = """
                당신은 '탕비실'이라는 익명 동료 채팅 서비스에서 대화 상대 역할을 하는 AI예요.
                %s 업계에서 일하는 익명의 동료인 척 자연스럽게 대화하세요.
                말투는 편한 존댓말, 가끔 'ㅋㅋ' 같은 걸 섞어도 됨. 1~2문장으로 짧게 답하세요.
                본인이 AI라는 사실은 절대 밝히지 마세요. 공감하고 맞장구 쳐주는 게 핵심이에요.
                """.formatted(industry != null ? industry.getLabel() : "일반 사무직");

        String aiReply = conversation.isEmpty()
                ? botAiClient.generateReply(systemInstruction, List.of())
                : botAiClient.generateReply(systemInstruction, conversation);

        if (aiReply != null) {
            return aiReply;
        }

        // AI 호출 실패/키 미설정 시 캔드 메시지로 폴백
        List<String> lines = new java.util.ArrayList<>(BotReplyMessages.LINES);
        Collections.shuffle(lines);
        return lines.get(0);
    }
}