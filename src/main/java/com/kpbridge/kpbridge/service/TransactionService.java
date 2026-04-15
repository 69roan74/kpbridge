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

    /** 충전 신청 - 잔액 미반영, 관리자 승인 대기 */
    @Transactional
    public void charge(String userId, BigDecimal amount, String chargeMethod) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        Transaction tx = saveLog(member, "충전 (Deposit)", amount, "입금대기", null, null, null);
        tx.setChargeMethod(chargeMethod != null ? chargeMethod.toUpperCase() : "USDT");
        transactionRepository.save(tx);
        log.info("💰 충전 신청 접수: 사용자={}, 금액={}, 방식={}", userId, amount, chargeMethod);
    }

    /** 출금 신청 - 잔액 미차감, 관리자 승인 대기 */
    @Transactional
    public void withdraw(String userId, BigDecimal amount, String memo) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        if (member.getMyCoinBalance().compareTo(amount) < 0) {
            throw new RuntimeException("잔액이 부족합니다.");
        }
        Transaction tx = saveLog(member, "출금 (Withdraw)", amount, "출금대기", null, null, null);
        tx.setMemo(memo);
        transactionRepository.save(tx);
        log.info("💸 출금 신청 접수: 사용자={}, 금액={}, 출금처={}", userId, amount, memo);
    }

    /** 관리자: 충전 승인 → 잔액 증가 */
    @Transactional
    public void approveDeposit(Long txId) {
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        Member member = tx.getMember();
        member.setMyCoinBalance(member.getMyCoinBalance().add(tx.getAmount()));
        memberRepository.save(member);
        tx.setStatus("완료");
        tx.setBalanceAfter(member.getMyCoinBalance());
        transactionRepository.save(tx);
        log.info("✅ 충전 승인: txId={}, 사용자={}, 금액={}", txId, member.getUserId(), tx.getAmount());
    }

    /** 관리자: 출금 승인 → 잔액 차감 */
    @Transactional
    public void approveWithdraw(Long txId) {
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        Member member = tx.getMember();
        if (member.getMyCoinBalance().compareTo(tx.getAmount()) < 0) {
            throw new RuntimeException("잔액 부족으로 출금 승인 불가");
        }
        member.setMyCoinBalance(member.getMyCoinBalance().subtract(tx.getAmount()));
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

    /**
     * 거래 주문 접수 - 상태: 거래대기중
     * 실제 수익 계산은 관리자가 거래완료 처리 시 수행
     */
    @Transactional
    public Transaction submitOrder(String userId, String coinType, String route, BigDecimal investment) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();

        Transaction tx = new Transaction();
        tx.setMember(member);
        tx.setType("거래 주문");
        tx.setAmount(investment);
        tx.setBalanceAfter(member.getMyCoinBalance());
        tx.setStatus("거래진행중");
        tx.setTradeStatus("거래진행중");
        tx.setCoinType(coinType);
        tx.setRoute(route);
        tx.setDate(LocalDateTime.now());
        transactionRepository.save(tx);

        log.info("📋 거래 주문 접수: 사용자={}, 코인={}, 경로={}, 금액={}", userId, coinType, route, investment);
        return tx;
    }

    /**
     * 관리자가 거래 완료 처리 시 호출 - 수익 계산 및 잔액 반영
     */
    @Transactional
    public void completeOrder(Long txId) {
        Transaction tx = transactionRepository.findById(txId).orElseThrow();
        Member member = tx.getMember();

        double rate = 0.035 + (Math.random() * 0.015);
        BigDecimal profit = tx.getAmount().multiply(BigDecimal.valueOf(rate));

        member.setMyCoinBalance(member.getMyCoinBalance().add(profit));
        memberRepository.save(member);

        tx.setTradeStatus("거래완료");
        tx.setStatus("거래완료");
        tx.setAmount(profit);
        tx.setBalanceAfter(member.getMyCoinBalance());
        transactionRepository.save(tx);

        referralService.propagateTradeReward(member, profit, tx.getId());
        log.info("✅ 거래 완료 처리: txId={}, 수익={}", txId, profit);
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
