package com.kpbridge.kpbridge.repository;

import com.kpbridge.kpbridge.entity.ReferralReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ReferralRewardRepository extends JpaRepository<ReferralReward, Long> {

    // 특정 회원이 받은 보상 이력 (최신 순)
    List<ReferralReward> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    // 특정 회원이 유발한 보상 이력
    List<ReferralReward> findBySourceIdOrderByCreatedAtDesc(Long sourceId);

    // 특정 회원이 받은 총 보상액
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM ReferralReward r WHERE r.recipient.id = :recipientId")
    BigDecimal sumAmountByRecipientId(@Param("recipientId") Long recipientId);

    // 전체 지급된 추천 보상 총액 (관리자 통계용)
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM ReferralReward r")
    BigDecimal sumAllRewards();
}
