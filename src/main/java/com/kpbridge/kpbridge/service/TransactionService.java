package com.kpbridge.kpbridge.service;

import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.entity.Transaction;
import com.kpbridge.kpbridge.repository.MemberRepository;
import com.kpbridge.kpbridge.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final ReferralService referralService;

    // Telegram 인증 정보 (application.properties에서 관리)
    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.chat-id:}")
    private String chatId;

    @Transactional
    public void charge(String userId, BigDecimal amount) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        member.setMyCoinBalance(member.getMyCoinBalance().add(amount));
        memberRepository.save(member);
        saveLog(member, "충전 (Deposit)", amount, "완료");
        log.info("💰 충전 완료: 사용자={}, 금액={}", userId, amount);
    }

    @Transactional
    public void withdraw(String userId, BigDecimal amount) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        if (member.getMyCoinBalance().compareTo(amount) < 0) {
            throw new RuntimeException("잔액이 부족합니다.");
        }
        member.setMyCoinBalance(member.getMyCoinBalance().subtract(amount));
        memberRepository.save(member);
        saveLog(member, "출금 (Withdraw)", amount.negate(), "완료");
        log.info("💸 출금 완료: 사용자={}, 금액={}", userId, amount);
    }

    @Transactional
    public void executeTrade(String userId, String coinType, String route, BigDecimal investment) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();

        // 수익 계산 (3.5% ~ 5.0%)
        double rate = 0.035 + (Math.random() * 0.015);
        BigDecimal profit = investment.multiply(BigDecimal.valueOf(rate));

        // 자산 증가
        member.setMyCoinBalance(member.getMyCoinBalance().add(profit));
        memberRepository.save(member);

        String description = coinType + " 차익거래 수익";
        Transaction tx = saveLog(member, description, profit, "완료");

        log.info("🚀 거래 실행: 사용자={}, 수익={}", userId, profit);

        // 피라미드 추천 보상 전파 (상위 체인 전체에 TRADE 보상 지급)
        referralService.propagateTradeReward(member, profit, tx.getId());

        // 텔레그램 발송
        String message = String.format(
                "[🚀 거래 알림]\n회원: %s\n코인: %s\n경로: %s\n투자금: %,d KRW\n수익: %,d KRW",
                userId, coinType, route, investment.longValue(), profit.longValue()
        );
        sendTelegramAlert(message);
    }

    public List<Transaction> getHistory(String userId) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        return transactionRepository.findByMemberIdOrderByDateDesc(member.getId());
    }

    private Transaction saveLog(Member member, String type, BigDecimal amount, String status) {
        Transaction tx = new Transaction();
        tx.setMember(member);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(member.getMyCoinBalance());
        tx.setStatus(status);
        tx.setDate(LocalDateTime.now());
        return transactionRepository.save(tx);
    }

    private void sendTelegramAlert(String text) {
        if (botToken == null || botToken.isBlank() || chatId == null || chatId.isBlank()) {
            log.debug("텔레그램 설정 없음, 발송 스킵");
            return;
        }
        new Thread(() -> {
            try {
                String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
                String urlStr = "https://api.telegram.org/bot" + botToken
                        + "/sendMessage?chat_id=" + chatId + "&text=" + encodedText;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestMethod("GET");
                conn.getInputStream().close();
                log.info("📲 텔레그램 발송 성공");
            } catch (Exception e) {
                log.error("🚫 텔레그램 발송 실패: {}", e.getMessage());
            }
        }).start();
    }
}
