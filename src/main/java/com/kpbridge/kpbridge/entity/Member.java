package com.kpbridge.kpbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "MEMBER")
@Getter @Setter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String userId;

    @Column(nullable = false)
    private String password;

    private String userName;
    private String phone;
    private String email;
    private String birthDate;

    // 나의 추천 코드 (가입 시 자동 생성, unique)
    @Column(unique = true, nullable = true)
    private String referralCode;

    private String referralAppliedYn = "N";

    // 나를 추천한 사람 (피라미드 상위 노드)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_by_id")
    private Member referredBy;

    // ★ [수정됨] 기본값을 "ROLE_USER" -> "USER"로 변경! (깔끔하게)
    private String role = "USER";

    // 네트워크 직급: 사원 → 주임 → 대리 → 과장 → 차장 → 부장
    private String rank = "사원";

    @Column(precision = 18, scale = 8)
    private BigDecimal myCoinBalance = BigDecimal.ZERO;

    // 원금 추적 (수익 제외): KRW 입금 원금
    @Column(precision = 18, scale = 8)
    private BigDecimal krwPrincipal = BigDecimal.ZERO;

    // 원금 추적 (수익 제외): USDT 입금 원금 (USDT 단위)
    @Column(precision = 18, scale = 8)
    private BigDecimal usdtPrincipal = BigDecimal.ZERO;

    private LocalDateTime joinDate = LocalDateTime.now();
}