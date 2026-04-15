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

    // 거래 종류: 충전, 출금, 거래 주문 등
    private String type;

    // 금액
    private BigDecimal amount;

    // 거래 후 잔액 (당시 잔액 기록용)
    private BigDecimal balanceAfter;

    // 상태 (기존 호환): 완료, 거래대기중, 거래중, 거래완료
    private String status;

    // 거래 주문 전용 상태: 거래대기중 → 거래중 → 거래완료
    private String tradeStatus;

    // 코인 종류 (거래 주문 시)
    private String coinType;

    // 거래 경로 (예: Binance→Upbit)
    private String route;

    // 충전 방식: "USDT" 또는 "KRW" (충전 신청 시)
    private String chargeMethod;

    // 출금처 정보 (출금 신청 시 사용자가 입력한 지갑 주소 또는 은행 계좌)
    @Column(length = 500)
    private String memo;

    // 날짜
    private LocalDateTime date;

    // 누구의 거래인가?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}
