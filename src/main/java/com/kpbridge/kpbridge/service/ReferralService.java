package com.kpbridge.kpbridge.service;

import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.entity.ReferralConfig;
import com.kpbridge.kpbridge.entity.ReferralReward;
import com.kpbridge.kpbridge.repository.MemberRepository;
import com.kpbridge.kpbridge.repository.ReferralConfigRepository;
import com.kpbridge.kpbridge.repository.ReferralRewardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferralService {

    private final MemberRepository memberRepository;
    private final ReferralConfigRepository referralConfigRepository;
    private final ReferralRewardRepository referralRewardRepository;

    /**
     * 가입 시 1회성 보너스: 추천인 체인 전체에 JOIN 보너스 지급
     */
    @Transactional
    public void propagateJoinReward(Member newMember) {
        if (newMember.getReferredBy() == null) return;

        BigDecimal joinBonus = getGlobalJoinBonus();
        if (joinBonus.compareTo(BigDecimal.ZERO) <= 0) return;

        Member current = newMember.getReferredBy();
        int level = 1;
        Set<Long> visited = new HashSet<>();

        while (current != null) {
            if (visited.contains(current.getId())) break; // 순환 방지
            visited.add(current.getId());

            current.setMyCoinBalance(current.getMyCoinBalance().add(joinBonus));
            memberRepository.save(current);

            ReferralReward reward = ReferralReward.builder()
                    .recipient(current)
                    .source(newMember)
                    .level(level)
                    .rewardType("JOIN")
                    .amount(joinBonus)
                    .sourceTransactionId(null)
                    .build();
            referralRewardRepository.save(reward);

            log.info("🎁 JOIN 보너스 지급: {} → {} (레벨 {}, {}KP)", newMember.getUserId(), current.getUserId(), level, joinBonus);

            current = current.getReferredBy();
            level++;
        }
    }

    /**
     * 거래 수익 발생 시: 상위 체인 전체에 TRADE 수익 분배
     */
    @Transactional
    public void propagateTradeReward(Member trader, BigDecimal profit, Long transactionId) {
        if (trader.getReferredBy() == null) return;

        Member current = trader.getReferredBy();
        int level = 1;
        Set<Long> visited = new HashSet<>();

        while (current != null) {
            if (visited.contains(current.getId())) break;
            visited.add(current.getId());

            BigDecimal rate = getRateForLevel(current, level);
            if (rate.compareTo(BigDecimal.ZERO) <= 0) {
                current = current.getReferredBy();
                level++;
                continue;
            }

            BigDecimal reward = profit.multiply(rate).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);

            current.setMyCoinBalance(current.getMyCoinBalance().add(reward));
            memberRepository.save(current);

            ReferralReward referralReward = ReferralReward.builder()
                    .recipient(current)
                    .source(trader)
                    .level(level)
                    .rewardType("TRADE")
                    .amount(reward)
                    .sourceTransactionId(transactionId)
                    .build();
            referralRewardRepository.save(referralReward);

            log.info("💰 TRADE 보상 지급: {} → {} (레벨 {}, {}%, {}KP)", trader.getUserId(), current.getUserId(), level, rate, reward);

            current = current.getReferredBy();
            level++;
        }
    }

    /**
     * 계보도 트리 반환 (마이페이지 팝업용)
     * 재귀적으로 하위 구조 조회
     */
    public Map<String, Object> getReferralTree(Member member, int maxDepth) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("userId", maskUserId(member.getUserId()));
        node.put("userName", maskUserName(member.getUserName()));
        node.put("level", 0);
        node.put("totalReward", referralRewardRepository.sumAmountByRecipientId(member.getId()));
        node.put("children", buildChildren(member, 1, maxDepth, new HashSet<>()));
        return node;
    }

    private List<Map<String, Object>> buildChildren(Member parent, int currentLevel, int maxDepth, Set<Long> visited) {
        if (currentLevel > maxDepth) return Collections.emptyList();
        if (visited.contains(parent.getId())) return Collections.emptyList();
        visited.add(parent.getId());

        List<Member> children = memberRepository.findByReferredById(parent.getId());
        List<Map<String, Object>> result = new ArrayList<>();

        for (Member child : children) {
            Map<String, Object> childNode = new LinkedHashMap<>();
            childNode.put("userId", maskUserId(child.getUserId()));
            childNode.put("userName", maskUserName(child.getUserName()));
            childNode.put("level", currentLevel);
            childNode.put("totalReward", referralRewardRepository.sumAmountByRecipientId(parent.getId()));
            childNode.put("children", buildChildren(child, currentLevel + 1, maxDepth, new HashSet<>(visited)));
            result.add(childNode);
        }
        return result;
    }

    /**
     * 전체 기본 가입 보너스 조회
     */
    public BigDecimal getGlobalJoinBonus() {
        return referralConfigRepository.findByTargetType("GLOBAL")
                .map(ReferralConfig::getJoinBonus)
                .filter(b -> b != null)
                .orElse(BigDecimal.valueOf(50)); // 기본값 50 KP
    }

    /**
     * 레벨별 분배율 조회 (개인별 오버라이드 지원)
     */
    public BigDecimal getRateForLevel(Member recipient, int level) {
        // 1. 개인별 설정 우선
        Optional<ReferralConfig> individual = referralConfigRepository
                .findByTargetTypeAndTargetMemberId("INDIVIDUAL", recipient.getId());
        if (individual.isPresent()) {
            BigDecimal rate = parseRateFromJson(individual.get().getTradeRateJson(), level);
            if (rate != null) return rate;
        }

        // 2. 전체 기본 설정
        Optional<ReferralConfig> global = referralConfigRepository.findByTargetType("GLOBAL");
        if (global.isPresent()) {
            BigDecimal rate = parseRateFromJson(global.get().getTradeRateJson(), level);
            if (rate != null) return rate;
        }

        // 3. 하드코딩 기본값: 레벨1=5%, 레벨2=3%, 그 외=1%
        if (level == 1) return BigDecimal.valueOf(5.0);
        if (level == 2) return BigDecimal.valueOf(3.0);
        return BigDecimal.valueOf(1.0);
    }

    /**
     * JSON 문자열에서 레벨별 비율 파싱
     * 형식: {"1":"5.0","2":"3.0","default":"1.0"}
     */
    private BigDecimal parseRateFromJson(String json, int level) {
        if (json == null || json.isBlank()) return null;
        try {
            String key = "\"" + level + "\"";
            int idx = json.indexOf(key);
            if (idx >= 0) {
                int colon = json.indexOf(':', idx);
                int start = json.indexOf('"', colon) + 1;
                int end = json.indexOf('"', start);
                return new BigDecimal(json.substring(start, end));
            }
            // default
            int defIdx = json.indexOf("\"default\"");
            if (defIdx >= 0) {
                int colon = json.indexOf(':', defIdx);
                int start = json.indexOf('"', colon) + 1;
                int end = json.indexOf('"', start);
                return new BigDecimal(json.substring(start, end));
            }
        } catch (Exception e) {
            log.warn("요율 JSON 파싱 실패: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 사용자 ID 마스킹 (보안)
     */
    private String maskUserId(String userId) {
        if (userId == null || userId.length() <= 2) return "**";
        return userId.charAt(0) + "*".repeat(userId.length() - 2) + userId.charAt(userId.length() - 1);
    }

    /**
     * 이름 마스킹
     */
    private String maskUserName(String name) {
        if (name == null || name.length() <= 1) return "*";
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }
}
