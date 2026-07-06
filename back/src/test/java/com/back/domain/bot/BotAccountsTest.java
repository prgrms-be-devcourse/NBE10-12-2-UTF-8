package com.back.domain.bot;

import com.back.domain.member.member.entity.Industry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BotAccountsTest {

    @Test
    @DisplayName("emailFor - 산업군별로 고유한 봇 이메일을 생성한다")
    void emailFor_생성() {
        assertThat(BotAccounts.emailFor(Industry.IT)).isEqualTo("bot.it@tangbisil.bot");
        assertThat(BotAccounts.emailFor(Industry.FINANCE)).isEqualTo("bot.finance@tangbisil.bot");
    }

    @Test
    @DisplayName("isBotEmail - 봇 이메일 패턴을 정확히 인식한다")
    void isBotEmail_인식() {
        assertThat(BotAccounts.isBotEmail("bot.it@tangbisil.bot")).isTrue();
        assertThat(BotAccounts.isBotEmail("user1@test.com")).isFalse();
        assertThat(BotAccounts.isBotEmail("admin@test.com")).isFalse();
        assertThat(BotAccounts.isBotEmail(null)).isFalse();
    }
}