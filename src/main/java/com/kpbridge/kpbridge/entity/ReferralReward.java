package com.kpbridge.kpbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 추천인 피라미드 보상 지급 이력
 */
@Entity
@Table(name = "REFERRAL_REWARD")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralReward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 보상을 받은 사람 (상위 체인의 누군가)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Member recipient;

    // 보상을 유발한 행동의 주인공 (하위 유저)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Member source;

    // 몇 단계 위 (1=직접 추천, 2=추천의 추천, ...)
    private int level;

    // JOIN (가입 보너스) or TRADE (거래 수익 분배)
    @Column(nullable = false)
    private String rewardType;

    @Column(precision = 18, scale = 8, nullable = false)
    private BigDecimal amount;

    // 연관된 거래 ID (TRADE 타입일 때만 값 있음)
    private Long sourceTransactionId;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
