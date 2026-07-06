package com.back.domain.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotReplyService {
    private final BotReplyExecutor botReplyExecutor;

    private static final int MIN_DELAY_MS = 1500;
    private static final int MAX_EXTRA_DELAY_MS = 3000;

    // 매칭 트랜잭션/메시지 전송 트랜잭션이 실제로 커밋된 뒤에만 실행 - 커밋 전에 실행하면
    // 다른 스레드(비동기)가 아직 존재하지 않는 채팅방/메시지를 조회하게 되어 실패한다.
    //
    // sleep은 여기서 트랜잭션 없이 소비한다 - @Transactional 안에서 sleep하면
    // 그동안 DB 커넥션을 붙잡고 있어서 동시 요청이 몰릴 때 HikariCP 풀이 고갈될 수 있다.
    // 실제 DB 조회/전송은 별도 빈(BotReplyExecutor)의 트랜잭션 메서드에서 처리한다.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleReplyTrigger(BotReplyTriggerEvent event) {
        try {
            Thread.sleep(MIN_DELAY_MS + ThreadLocalRandom.current().nextInt(MAX_EXTRA_DELAY_MS));
            botReplyExecutor.execute(event);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[BotReplyService] 응답 대기 중 인터럽트 발생 - roomId: {}", event.roomId());
        } catch (Exception e) {
            // 채팅방이 그 사이 종료됐거나 하는 등의 이유로 실패해도 서비스 전체엔 영향 없게 로그만 남김
            log.error("[BotReplyService] 응답 실패 - roomId: {}", event.roomId(), e);
        }
    }
}