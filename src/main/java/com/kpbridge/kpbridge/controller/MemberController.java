package com.kpbridge.kpbridge.controller;

import com.kpbridge.kpbridge.dto.MemberRequestDto;
import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.repository.MemberRepository;
import com.kpbridge.kpbridge.service.CoinService;
import com.kpbridge.kpbridge.service.MemberService;
import com.kpbridge.kpbridge.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ★ 로그 사용
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.security.Principal;

@Slf4j // ★ System.out 대신 log 사용
@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final CoinService coinService;
    private final TransactionService transactionService;
    private final PasswordEncoder passwordEncoder;

    // 🚨 [비상구] 관리자 계정 생성/복구
    @GetMapping("/emergency-fix")
    @ResponseBody
    public String emergencyFix() {
        String targetId = "admin";
        String targetPw = "1q2w3e4r%T";

        Member member = memberRepository.findByUserId(targetId).orElse(new Member());
        
        if (member.getId() == null) {
            member.setUserId(targetId);
            member.setUserName("총관리자");
            member.setEmail("admin@kpbridge.com");
            member.setPhone("010-0000-0000");
            member.setBirthDate("19990101");
            member.setMyCoinBalance(BigDecimal.ZERO);
            member.setReferralAppliedYn("N");
        }

        member.setPassword(passwordEncoder.encode(targetPw));
        member.setRole("ADMIN"); // ★ 관리자 권한 부여

        memberRepository.save(member);
        log.warn("🚨 비상구 코드 실행됨! 관리자 계정({})이 복구되었습니다.", targetId);

        return "<h1>✅ 복구 완료!</h1><a href='/login'>로그인 이동</a>";
    }

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
            member.setReferralCode(dto.getReferralCode());

            memberService.register(member);
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

    @GetMapping("/mypage")
    public String myPage(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        Member member = memberRepository.findByUserId(principal.getName()).orElseThrow();
        model.addAttribute("member", member);
        model.addAttribute("historyList", transactionService.getHistory(principal.getName()));
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
            member.setMyCoinBalance(member.getMyCoinBalance().add(BigDecimal.valueOf(50)));
            member.setReferralAppliedYn("Y");
            member.setReferralCode(refCode);
            memberRepository.save(member);
            log.info("🎁 추천인 코드 적용: {}", principal.getName());
        }
        return "redirect:/mypage";
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
}