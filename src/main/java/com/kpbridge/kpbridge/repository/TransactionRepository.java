package com.kpbridge.kpbridge.repository;

import com.kpbridge.kpbridge.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // [중요] 이 부분이 빠져있어서 Service에서 빨간 줄이 떴던 겁니다!
    // "특정 회원의 거래 내역을 날짜 내림차순(최신순)으로 가져와라" 라는 뜻입니다.
    List<Transaction> findByMemberIdOrderByDateDesc(Long memberId);

    // (관리자용) 모든 회원의 내역 최신순 조회
    List<Transaction> findAllByOrderByDateDesc();
}