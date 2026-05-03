package com.kpbridge.kpbridge.service;

import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.entity.Transaction;
import com.kpbridge.kpbridge.repository.MemberRepository;
import com.kpbridge.kpbridge.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final ReferralService referralService;
    private final CoinService coinService;

    /** 충전 신청 - 잔액 미반영, 관리자 승인 대기 */
    @Transactional
    public void charge(String userId, BigDecimal amount, String chargeMethod) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        Transaction tx = saveLog(member, "충전 (Deposit)", amount, "입금대기", null, null, null);
        tx.setChargeMethod(chargeMethod != null ? chargeMethod.toUpperCase() : "USDT");
        transactionRepository.save(tx);
        log.info("💰 충전 신청 접수: 사용자={}, 금액={}, 방식={}", userId, amount, chargeMethod);
    }

    /** 출금 신청 - 수익금 범위 내에서만 가능, 관리자 승인 대기 */
    @Transactional
    public void withdraw(String userId, BigDecimal amount, String memo) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        BigDecimal withdrawable = getWithdrawableBalance(userId);
        if (withdrawable.compareTo(amount) < 0) {
            throw new RuntimeException("출금 가능 금액(수익금)을 초과했습니다. 출금 가능: " + withdrawable.toPlainString() + " KRW");
        }
        Transaction tx = saveLog(member, "출금 (Withdraw)", amount, "출금대기", null, null, null);
        tx.setMemo(memo);
        transactionRepository.save(tx);
        log.info("💸 출금 신청 접수: 사용자={}, 금액={}, 출금처={}", userId, amount, memo);
    }

    /** 관리자: 충전 승인 → 잔액 증가 + 원금 추적 */
    @Transactional
    public void approveDeposit(Long txId) {
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        Member member = tx.getMember();
        // 원금 추적: USDT 충전은 KRW 환산해서 잔고에 반영 + usdtPrincipal(USDT 단위) 기록
        if ("USDT".equalsIgnoreCase(tx.getChargeMethod())) {
            double rate = coinService.getUsdtKrwRate();
            BigDecimal krwEquivalent = tx.getAmount().multiply(BigDecimal.valueOf(rate));
            member.setMyCoinBalance(member.getMyCoinBalance().add(krwEquivalent));
            if (member.getUsdtPrincipal() == null) member.setUsdtPrincipal(BigDecimal.ZERO);
            member.setUsdtPrincipal(member.getUsdtPrincipal().add(tx.getAmount()));
        } else {
            member.setMyCoinBalance(member.getMyCoinBalance().add(tx.getAmount()));
            if (member.getKrwPrincipal() == null) member.setKrwPrincipal(BigDecimal.ZERO);
            member.setKrwPrincipal(member.getKrwPrincipal().add(tx.getAmount()));
        }
        memberRepository.save(member);
        tx.setStatus("완료");
        tx.setBalanceAfter(member.getMyCoinBalance());
        transactionRepository.save(tx);
        log.info("✅ 충전 승인: txId={}, 사용자={}, 금액={}, 방식={}", txId, member.getUserId(), tx.getAmount(), tx.getChargeMethod());
    }

    /** 관리자: 출금 승인 → 잔액 차감 + KRW 원금 차감 */
    @Transactional
    public void approveWithdraw(Long txId) {
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        Member member = tx.getMember();
        if (member.getMyCoinBalance().compareTo(tx.getAmount()) < 0) {
            throw new RuntimeException("잔액 부족으로 출금 승인 불가");
        }
        member.setMyCoinBalance(member.getMyCoinBalance().subtract(tx.getAmount()));
        // 출금은 KRW 원금에서 차감 (0 이하로는 내려가지 않음)
        if (member.getKrwPrincipal() == null) member.setKrwPrincipal(BigDecimal.ZERO);
        BigDecimal newKrw = member.getKrwPrincipal().subtract(tx.getAmount());
        member.setKrwPrincipal(newKrw.max(BigDecimal.ZERO));
        memberRepository.save(member);
        tx.setStatus("완료");
        tx.setAmount(tx.getAmount().negate());
        tx.setBalanceAfter(member.getMyCoinBalance());
        transactionRepository.save(tx);
        log.info("✅ 출금 승인: txId={}, 사용자={}, 금액={}", txId, member.getUserId(), tx.getAmount());
    }

    /** 관리자: 충전/출금 거절 */
    @Transactional
    public void rejectRequest(Long txId) {
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        tx.setStatus("거절");
        transactionRepository.save(tx);
        log.info("❌ 신청 거절: txId={}", txId);
    }

    /** 대기 중인 충전 신청 목록 */
    public List<Transaction> getPendingDeposits() {
        return transactionRepository.findByTypeContainingAndStatusOrderByDateDesc("충전", "입금대기");
    }

    /** 대기 중인 출금 신청 목록 */
    public List<Transaction> getPendingWithdraws() {
        return transactionRepository.findByTypeContainingAndStatusOrderByDateDesc("출금", "출금대기");
    }

    /** KRW 거래 가능 금액 (KRW 원금) */
    public BigDecimal getTradableKrwBalance(String userId) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        return member.getKrwPrincipal() != null ? member.getKrwPrincipal() : BigDecimal.ZERO;
    }

    /** USDT 거래 가능 금액 (USDT 원금, USDT 단위) */
    public BigDecimal getTradableUsdtBalance(String userId) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        return member.getUsdtPrincipal() != null ? member.getUsdtPrincipal() : BigDecimal.ZERO;
    }

    /** 출금 가능 금액 = 수익금(잔고 - 원금) */
    public BigDecimal getWithdrawableBalance(String userId) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        BigDecimal krwP = member.getKrwPrincipal() != null ? member.getKrwPrincipal() : BigDecimal.ZERO;
        BigDecimal usdtP = member.getUsdtPrincipal() != null ? member.getUsdtPrincipal() : BigDecimal.ZERO;
        double usdtRate = coinService.getUsdtKrwRate();
        BigDecimal principal = krwP.add(usdtP.multiply(BigDecimal.valueOf(usdtRate)));
        return member.getMyCoinBalance().subtract(principal).max(BigDecimal.ZERO);
    }

    /** 하위 호환용 — 합산 원금 반환 */
    public BigDecimal getTradableBalance(String userId) {
        double rate = coinService.getUsdtKrwRate();
        return getTradableKrwBalance(userId)
                .add(getTradableUsdtBalance(userId).multiply(BigDecimal.valueOf(rate)));
    }

    /**
     * 거래 주문 접수 - KRW/USDT 원금 분리 검증 + 투자금 선차감
     * tradeType: "KRW" → KRW 원금 한도, "USDT" → USDT 원금(KRW 환산) 한도
     */
    @Transactional
    public Transaction submitOrder(String userId, String coinType, String route, BigDecimal investment, String tradeType) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();

        if (investment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("투자 금액은 0보다 커야 합니다.");
        }

        BigDecimal maxAllowed;
        if ("USDT".equalsIgnoreCase(tradeType)) {
            double rate = coinService.getUsdtKrwRate();
            maxAllowed = getTradableUsdtBalance(userId).multiply(BigDecimal.valueOf(rate));
        } else {
            maxAllowed = getTradableKrwBalance(userId);
        }
        if (investment.compareTo(maxAllowed) > 0) {
            String label = "USDT".equalsIgnoreCase(tradeType) ? "USDT 원금 한도" : "KRW 원금 한도";
            throw new RuntimeException("거래 가능 금액을 초과했습니다. (" + label + ": " + maxAllowed.toPlainString() + " KRW)");
        }
        if (investment.compareTo(member.getMyCoinBalance()) > 0) {
            throw new RuntimeException("잔고가 부족합니다.");
        }

        // 투자금 선차감
        member.setMyCoinBalance(member.getMyCoinBalance().subtract(investment));
        memberRepository.save(member);

        Transaction tx = new Transaction();
        tx.setMember(member);
        tx.setType("거래 주문");
        tx.setAmount(investment);
        tx.setInvestmentAmount(investment);
        tx.setBalanceAfter(member.getMyCoinBalance());
        tx.setStatus("거래진행중");
        tx.setTradeStatus("거래진행중");
        tx.setCoinType(coinType);
        tx.setRoute(route);
        tx.setDate(LocalDateTime.now());
        transactionRepository.save(tx);

        log.info("📋 거래 주문 접수: 사용자={}, 코인={}, 경로={}, 금액={}, 잔고={}", userId, coinType, route, investment, member.getMyCoinBalance());
        return tx;
    }

    /**
     * 관리자가 거래 완료 처리 시 호출 - 투자금 반환 + 수익 추가
     */
    @Transactional
    public void completeOrder(Long txId) {
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        Member member = tx.getMember();

        BigDecimal investment = tx.getInvestmentAmount() != null ? tx.getInvestmentAmount() : tx.getAmount();
        double rate = 0.035 + (Math.random() * 0.015);
        BigDecimal profit = investment.multiply(BigDecimal.valueOf(rate));

        // 투자금 반환 + 수익금 추가
        member.setMyCoinBalance(member.getMyCoinBalance().add(investment).add(profit));
        memberRepository.save(member);

        tx.setTradeStatus("거래완료");
        tx.setStatus("거래완료");
        tx.setAmount(profit);
        tx.setBalanceAfter(member.getMyCoinBalance());
        transactionRepository.save(tx);

        referralService.propagateTradeReward(member, profit, tx.getId());
        log.info("✅ 거래 완료 처리: txId={}, 투자금={}, 수익={}", txId, investment, profit);
    }

    /**
     * 관리자가 거래 취소 처리 - 투자금 환불
     */
    @Transactional
    public void cancelOrder(Long txId) {
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        Member member = tx.getMember();

        BigDecimal investment = tx.getInvestmentAmount() != null ? tx.getInvestmentAmount() : tx.getAmount();
        member.setMyCoinBalance(member.getMyCoinBalance().add(investment));
        memberRepository.save(member);

        tx.setTradeStatus("거래취소");
        tx.setStatus("거래취소");
        tx.setBalanceAfter(member.getMyCoinBalance());
        transactionRepository.save(tx);
        log.info("❌ 거래 취소: txId={}, 투자금 환불={}", txId, investment);
    }

    /**
     * 관리자가 거래 상태 변경
     */
    @Transactional
    public void updateTradeStatus(Long txId, String newStatus) {
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        tx.setTradeStatus(newStatus);
        tx.setStatus(newStatus);
        transactionRepository.save(tx);
        log.info("🔄 거래 상태 변경: txId={}, 상태={}", txId, newStatus);
    }

    public List<Transaction> getHistory(String userId) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        return transactionRepository.findByMemberIdOrderByDateDesc(member.getId());
    }

    public List<Transaction> getPendingOrders() {
        return transactionRepository.findByTradeStatusOrderByDateDesc("거래진행중");
    }

    public List<Transaction> getActiveOrders() {
        return transactionRepository.findByTradeStatusOrderByDateDesc("거래중");
    }

    private Transaction saveLog(Member member, String type, BigDecimal amount, String status,
                                 String tradeStatus, String coinType, String route) {
        Transaction tx = new Transaction();
        tx.setMember(member);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(member.getMyCoinBalance());
        tx.setStatus(status);
        tx.setTradeStatus(tradeStatus);
        tx.setCoinType(coinType);
        tx.setRoute(route);
        tx.setDate(LocalDateTime.now());
        return transactionRepository.save(tx);
    }
}
