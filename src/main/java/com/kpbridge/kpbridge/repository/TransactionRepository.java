package com.kpbridge.kpbridge.repository;

import com.kpbridge.kpbridge.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByMemberIdOrderByDateDesc(Long memberId);

    List<Transaction> findAllByOrderByDateDesc();

    // 거래 상태별 조회 (관리자용)
    List<Transaction> findByTradeStatusOrderByDateDesc(String tradeStatus);

    // 특정 회원의 거래 주문 조회 (마이페이지용)
    List<Transaction> findByMemberIdAndTradeStatusNotNullOrderByDateDesc(Long memberId);

    // 입금/출금 대기 중인 신청 조회 (관리자용)
    List<Transaction> findByTypeContainingAndStatusOrderByDateDesc(String type, String status);

    // 첫 거래 여부 확인 (완료된 거래 주문 존재 여부)
    boolean existsByMemberIdAndTradeStatus(Long memberId, String tradeStatus);
}
