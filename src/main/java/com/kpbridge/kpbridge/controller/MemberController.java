package com.kpbridge.kpbridge.controller;

import com.kpbridge.kpbridge.dto.MemberRequestDto;
import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.repository.MemberRepository;
import com.kpbridge.kpbridge.repository.ReferralRewardRepository;
import com.kpbridge.kpbridge.service.CoinService;
import com.kpbridge.kpbridge.service.MemberService;
import com.kpbridge.kpbridge.service.ReferralService;
import com.kpbridge.kpbridge.service.SiteConfigService;
import com.kpbridge.kpbridge.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final CoinService coinService;
    private final TransactionService transactionService;
    private final ReferralService referralService;
    private final ReferralRewardRepository referralRewardRepository;
    private final PasswordEncoder passwordEncoder;
    private final SiteConfigService siteConfigService;

    @GetMapping("/")
    public String mainPage(Model model, Principal principal) {
        if (principal != null) {
            Member member = memberRepository.findByUserId(principal.getName()).orElse(null);
            model.addAttribute("user", member);
            model.addAttribute("historyList", transactionService.getHistory(principal.getName()));
        }

        try {
            model.addAttribute("btcUpbit", coinService.getUpbitBtc());
            model.addAttribute("btcBithumb", coinService.getBithumbBtc());
            model.addAttribute("btcBinance", coinService.getBinanceBtc());
            model.addAttribute("btcOkx", coinService.getOkxBtc());
            model.addAttribute("ethUpbit", coinService.getEthPrice());
            model.addAttribute("ethBithumb", coinService.getBinanceEthPrice());
            model.addAttribute("ethBinance", "0");
            model.addAttribute("ethOkx", coinService.getOkxEthPrice());
        } catch (Exception e) {
            log.error("시세 조회 중 오류 발생: {}", e.getMessage());
        }
        return "main";
    }

    @GetMapping("/register")
    public String registerForm() { return "register"; }

    @PostMapping("/register")
    public String register(MemberRequestDto dto, Model model) {
        try {
            log.info("📝 회원가입 요청 - ID: {}, Name: {}", dto.getUserId(), dto.getUserName());

            Member member = new Member();
            member.setUserId(dto.getUserId());
            member.setPassword(dto.getPassword());
            member.setUserName(dto.getUserName());
            member.setPhone(dto.getPhone());
            member.setEmail(dto.getEmail());
            member.setBirthDate(dto.getBirthDate());

            // 추천인 코드는 inviteCode로 전달 (member의 referralCode는 나의 코드)
            memberService.register(member, dto.getReferralCode());
            log.info("🎉 회원가입 성공: {}", dto.getUserId());
            return "redirect:/login";
        } catch (RuntimeException e) {
            log.warn("⚠️ 회원가입 실패: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginForm() { return "login"; }

    @GetMapping("/profit")
    public String profitPage(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        Member member = memberRepository.findByUserId(principal.getName()).orElseThrow();
        model.addAttribute("member", member);

        // 거래 수익 목록 (거래완료 상태의 거래 주문)
        var tradeProfits = transactionService.getHistory(principal.getName()).stream()
                .filter(tx -> "거래완료".equals(tx.getTradeStatus()))
                .toList();
        model.addAttribute("tradeProfits", tradeProfits);

        // 거래 수익 합계
        BigDecimal totalTradeProfit = tradeProfits.stream()
                .map(tx -> tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalTradeProfit", totalTradeProfit);

        // 추천 수익 목록 및 합계
        model.addAttribute("referralRewards",
                referralRewardRepository.findByRecipientIdOrderByCreatedAtDesc(member.getId()));
        BigDecimal totalReferralEarned = referralRewardRepository.sumAmountByRecipientId(member.getId());
        model.addAttribute("totalReferralEarned", totalReferralEarned != null ? totalReferralEarned : BigDecimal.ZERO);

        // 총 수익
        BigDecimal grandTotal = totalTradeProfit.add(totalReferralEarned != null ? totalReferralEarned : BigDecimal.ZERO);
        model.addAttribute("grandTotal", grandTotal);

        return "profit";
    }

    @GetMapping("/mypage")
    public String myPage(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        Member member = memberRepository.findByUserId(principal.getName()).orElseThrow();
        model.addAttribute("member", member);
        model.addAttribute("historyList", transactionService.getHistory(principal.getName()));

        // 진행 중인 거래 주문 (거래대기중, 거래중만 표시)
        model.addAttribute("orderList",
                transactionService.getHistory(principal.getName()).stream()
                        .filter(tx -> tx.getTradeStatus() != null && !"거래완료".equals(tx.getTradeStatus()))
                        .toList());

        // 추천 보상 이력
        model.addAttribute("referralRewards",
                referralRewardRepository.findByRecipientIdOrderByCreatedAtDesc(member.getId()));
        model.addAttribute("totalReferralEarned",
                referralRewardRepository.sumAmountByRecipientId(member.getId()));
        // 내가 직접 추천한 사람 수
        model.addAttribute("directReferralCount",
                memberRepository.findByReferredById(member.getId()).size());

        // 입금 정보 (DB에서 동적으로 가져옴)
        model.addAttribute("companyWallet", siteConfigService.get("company.wallet", "TKpBridge9xCzFm2nHqRsUvWy3D7K"));
        model.addAttribute("companyBank", siteConfigService.get("company.bank", "기업은행 123-45-6789012"));

        return "mypage";
    }

    @GetMapping("/member/edit")
    public String editForm(Principal principal, Model model) {
        Member member = memberRepository.findByUserId(principal.getName()).get();
        model.addAttribute("member", member);
        return "member-edit";
    }

    @PostMapping("/member/update")
    public String updateMember(Member member, Principal principal) {
        Member existingMember = memberRepository.findByUserId(principal.getName()).get();
        existingMember.setUserName(member.getUserName());
        existingMember.setEmail(member.getEmail());
        existingMember.setPhone(member.getPhone());
        memberRepository.save(existingMember);
        return "redirect:/mypage";
    }

    @PostMapping("/member/apply-referral")
    public String applyReferral(@RequestParam("refCode") String refCode, Principal principal) {
        Member member = memberRepository.findByUserId(principal.getName()).get();
        if ("N".equals(member.getReferralAppliedYn())) {
            memberRepository.findByReferralCode(refCode.trim()).ifPresent(referrer -> {
                member.setReferredBy(referrer);
                member.setReferralAppliedYn("Y");
                memberRepository.save(member);
                // 체인 보상 전파
                referralService.propagateJoinReward(member);
                log.info("🎁 추천인 코드 적용: {} → {}", principal.getName(), referrer.getUserId());
            });
        }
        return "redirect:/mypage";
    }

    @PostMapping("/member/change-password")
    public String changePassword(@RequestParam("currentPassword") String currentPassword,
                                  @RequestParam("newPassword") String newPassword,
                                  @RequestParam("confirmPassword") String confirmPassword,
                                  Principal principal,
                                  org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("pwError", "새 비밀번호와 확인이 일치하지 않습니다.");
            return "redirect:/member/edit";
        }
        if (newPassword.length() < 8) {
            ra.addFlashAttribute("pwError", "새 비밀번호는 8자 이상이어야 합니다.");
            return "redirect:/member/edit";
        }
        try {
            memberService.changePassword(principal.getName(), currentPassword, newPassword);
            ra.addFlashAttribute("pwSuccess", "비밀번호가 변경되었습니다.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("pwError", e.getMessage());
        }
        return "redirect:/member/edit";
    }

    @PostMapping("/member/delete")
    public String deleteMember(Principal principal) {
        Member member = memberRepository.findByUserId(principal.getName()).get();
        memberRepository.delete(member);
        log.info("👋 회원 탈퇴: {}", principal.getName());
        return "redirect:/logout";
    }

    @GetMapping("/about")
    public String aboutPage() { return "about"; }

    /**
     * 추천 네트워크 계보도 JSON API
     * GET /api/referral/tree          → 본인 계보도
     * GET /api/referral/tree?userId=X → 특정 회원 계보도 (ADMIN 전용)
     */
    @GetMapping("/api/referral/tree")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReferralTree(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String userId,
            Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();

        Member requester = memberRepository.findByUserId(principal.getName()).orElseThrow();

        Member target;
        if (userId != null && !userId.isBlank() && "ADMIN".equals(requester.getRole())) {
            target = memberRepository.findByUserId(userId).orElse(requester);
        } else {
            target = requester;
        }

        Map<String, Object> tree = referralService.getReferralTree(target, 10);
        return ResponseEntity.ok(tree);
    }
}
