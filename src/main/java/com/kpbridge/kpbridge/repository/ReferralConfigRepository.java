package com.kpbridge.kpbridge.repository;

import com.kpbridge.kpbridge.entity.ReferralConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferralConfigRepository extends JpaRepository<ReferralConfig, Long> {

    // 전체 기본 요율 설정
    Optional<ReferralConfig> findByTargetType(String targetType);

    // 특정 회원 개인별 요율 오버라이드
    Optional<ReferralConfig> findByTargetTypeAndTargetMemberId(String targetType, Long memberId);
}
