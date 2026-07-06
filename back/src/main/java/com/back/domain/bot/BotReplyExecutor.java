package com.back.domain.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class BotReplyExecutor {
    private final BotReplyTransactionalOps ops;
    private final BotAiClient botAiClient;

    public void execute(BotReplyTriggerEvent event) {
        BotReplyTransactionalOps.ReplyContext context = ops.loadContext(event.roomId(), event.botMemberId());
        if (context == null) {
            return;
        }

        String reply = callAi(context);

        ops.persistReply(event.roomId(), context.botId(), reply);
    }

    private String callAi(BotReplyTransactionalOps.ReplyContext context) {
        context.conversation().forEach(c -> log.debug("{} : {}", c.getKey() ? "BOT" : "USER", c.getValue()));

        String aiReply = context.conversation().isEmpty()
                ? botAiClient.generateReply(context.systemInstruction(), List.of())
                : botAiClient.generateReply(context.systemInstruction(), context.conversation());

        log.info("AI 응답 = {}", aiReply);

        if (aiReply != null) {
            return aiReply;
        }
        log.warn("AI 실패 -> 폴백 메시지 사용");
        List<String> lines = BotReplyMessages.LINES;
        return lines.get(ThreadLocalRandom.current().nextInt(lines.size()));
    }
}