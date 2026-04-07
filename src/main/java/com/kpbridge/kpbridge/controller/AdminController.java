package com.kpbridge.kpbridge.controller;

import com.kpbridge.kpbridge.entity.Faq;
import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.entity.ReferralConfig;
import com.kpbridge.kpbridge.repository.FaqRepository;
import com.kpbridge.kpbridge.repository.MemberRepository;
import com.kpbridge.kpbridge.repository.ReferralConfigRepository;
import com.kpbridge.kpbridge.repository.ReferralRewardRepository;
import com.kpbridge.kpbridge.repository.TransactionRepository;
import com.kpbridge.kpbridge.service.ChatService;
import com.kpbridge.kpbridge.service.ReferralService;
import com.kpbridge.kpbridge.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;
    private final FaqRepository faqRepository;
    private final ReferralConfigRepository referralConfigRepository;
    private final ReferralRewardRepository referralRewardRepository;
    private final ReferralService referralService;
    private final TransactionService transactionService;
    private final ChatService chatService;

    // 1. 대시보드 메인
    @GetMapping("")
    public String adminPage(Model model) {
        model.addAttribute("members", memberRepository.findAll());
        model.addAttribute("transactions", transactionRepository.findAllByOrderByDateDesc());
        model.addAttribute("faqs", faqRepository.findAll());

        // 통계 - SQL 집계 쿼리 사용 (N+1 제거)
        long totalMembers = memberRepository.count();
        BigDecimal totalAssets = memberRepository.sumAllBalances();
        BigDecimal totalReferralRewards = referralRewardRepository.sumAllRewards();
        long activeReferrers = memberRepository.findAll().stream()
                .filter(m -> !memberRepository.findByReferredById(m.getId()).isEmpty())
                .count();

        model.addAttribute("totalMembers", totalMembers);
        model.addAttribute("totalAssets", totalAssets);
        model.addAttribute("totalReferralRewards", totalReferralRewards);
        model.addAttribute("activeReferrers", activeReferrers);

        // 거래 주문 현황
        model.addAttribute("pendingOrders", transactionService.getPendingOrders());
        model.addAttribute("activeOrders", transactionService.getActiveOrders());

        // 채팅 미확인 메시지 수
        model.addAttribute("totalUnread", chatService.getTotalUnread());

        // 추천인 요율 설정 (관리자 페이지용)
        Optional<ReferralConfig> globalConfig = referralConfigRepository.findByTargetType("GLOBAL");
        model.addAttribute("globalConfig", globalConfig.orElse(null));
        model.addAttribute("globalJoinBonus", referralService.getGlobalJoinBonus());

        return "admin";
    }

    // 2. 회원 삭제 (강퇴)
    @PostMapping("/member/delete")
    public String deleteMember(@RequestParam Long id) {
        memberRepository.deleteById(id);
        return "redirect:/admin";
    }

    // 3. 회원 정보 수정 (권한 변경 포함)
    @PostMapping("/member/update")
    public String updateMember(@RequestParam Long id,
                               @RequestParam String userName,
                               @RequestParam BigDecimal balance,
                               @RequestParam String role) {
        Member member = memberRepository.findById(id).orElseThrow();
        member.setUserName(userName);
        member.setMyCoinBalance(balance);
        member.setRole(role);
        memberRepository.save(member);
        return "redirect:/admin";
    }

    // 4. 거래 장부 삭제
    @PostMapping("/transaction/delete")
    public String deleteTransaction(@RequestParam Long id) {
        transactionRepository.deleteById(id);
        return "redirect:/admin";
    }

    // 5. FAQ 등록 및 수정
    @PostMapping("/faq/save")
    public String saveFaq(Faq faq) {
        faqRepository.save(faq);
        return "redirect:/admin";
    }

    // 6. FAQ 삭제
    @PostMapping("/faq/delete")
    public String deleteFaq(@RequestParam Long id) {
        faqRepository.deleteById(id);
        return "redirect:/admin";
    }

    // ===== 추천인 요율 관리 =====

    /**
     * 전체 기본 요율 저장/수정
     * POST /admin/referral/config/global
     * 파라미터: joinBonus, tradeRateJson (예: {"1":"5.0","2":"3.0","default":"1.0"})
     */
    @PostMapping("/referral/config/global")
    public String saveGlobalReferralConfig(
            @RequestParam BigDecimal joinBonus,
            @RequestParam String tradeRateJson) {

        ReferralConfig config = referralConfigRepository.findByTargetType("GLOBAL")
                .orElse(ReferralConfig.builder().targetType("GLOBAL").build());

        config.setJoinBonus(joinBonus);
        config.setTradeRateJson(tradeRateJson);
        referralConfigRepository.save(config);
        return "redirect:/admin";
    }

    /**
     * 특정 회원 개인별 요율 오버라이드
     * POST /admin/referral/config/individual
     */
    @PostMapping("/referral/config/individual")
    public String saveIndividualReferralConfig(
            @RequestParam Long memberId,
            @RequestParam BigDecimal joinBonus,
            @RequestParam String tradeRateJson) {

        ReferralConfig config = referralConfigRepository
                .findByTargetTypeAndTargetMemberId("INDIVIDUAL", memberId)
                .orElse(ReferralConfig.builder().targetType("INDIVIDUAL").targetMemberId(memberId).build());

        config.setJoinBonus(joinBonus);
        config.setTradeRateJson(tradeRateJson);
        referralConfigRepository.save(config);
        return "redirect:/admin";
    }

    /**
     * 전체 기본 요율 조회 (JSON API)
     */
    @GetMapping("/referral/config")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReferralConfig() {
        Optional<ReferralConfig> global = referralConfigRepository.findByTargetType("GLOBAL");
        return ResponseEntity.ok(Map.of(
                "joinBonus", referralService.getGlobalJoinBonus(),
                "tradeRateJson", global.map(ReferralConfig::getTradeRateJson).orElse("{\"1\":\"5.0\",\"2\":\"3.0\",\"default\":\"1.0\"}")
        ));
    }

    /**
     * 개인별 요율 삭제 (초기화)
     */
    @PostMapping("/referral/config/individual/delete")
    public String deleteIndividualConfig(@RequestParam Long memberId) {
        referralConfigRepository.findByTargetTypeAndTargetMemberId("INDIVIDUAL", memberId)
                .ifPresent(referralConfigRepository::delete);
        return "redirect:/admin";
    }

    // ===== 거래 상태 관리 =====

    /**
     * 거래 상태 변경 (거래대기중 → 거래중 → 거래완료)
     */
    @PostMapping("/order/status")
    @ResponseBody
    public ResponseEntity<?> updateOrderStatus(@RequestBody Map<String, Object> req) {
        Long txId = Long.valueOf(req.get("txId").toString());
        String newStatus = req.get("status").toString();

        if ("거래완료".equals(newStatus)) {
            transactionService.completeOrder(txId);
        } else {
            transactionService.updateTradeStatus(txId, newStatus);
        }
        return ResponseEntity.ok(Map.of("result", "ok", "status", newStatus));
    }
}
