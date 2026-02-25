package com.kpbridge.kpbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 거래 종류: CHARGE(충전), WITHDRAW(출금), PROFIT(수익)
    private String type; 

    // 금액
    private BigDecimal amount;

    // 거래 후 잔액 (당시 잔액 기록용)
    private BigDecimal balanceAfter;

    // 상태: PENDING(대기), COMPLETED(완료)
    private String status;

    // 날짜
    private LocalDateTime date;

    // 누구의 거래인가?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}