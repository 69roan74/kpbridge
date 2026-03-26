package com.kpbridge.kpbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 추천인 피라미드 요율 설정
 * - targetType=GLOBAL: 전체 기본 요율
 * - targetType=INDIVIDUAL: 특정 회원(targetMemberId)에 대한 개인별 오버라이드
 */
@Entity
@Table(name = "REFERRAL_CONFIG")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferralConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // GLOBAL or INDIVIDUAL
    @Column(nullable = false)
    private String targetType;

    // INDIVIDUAL일 때만 사용, GLOBAL이면 null
    private Long targetMemberId;

    // 가입 시 1회 지급 보너스 (KP)
    @Column(precision = 18, scale = 8)
    private BigDecimal joinBonus;

    // 레벨별 거래 수익 분배율 (JSON 형식)
    // 예: {"1": "5.0", "2": "3.0", "default": "1.0"}
    @Column(columnDefinition = "TEXT")
    private String tradeRateJson;

    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
