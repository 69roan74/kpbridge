package com.kpbridge.kpbridge.controller;

import com.kpbridge.kpbridge.entity.Faq;
import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.repository.FaqRepository;
import com.kpbridge.kpbridge.repository.MemberRepository;
import com.kpbridge.kpbridge.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final MemberRepository memberRepository;
    private final TransactionRepository transactionRepository;
    private final FaqRepository faqRepository;

    // 1. 대시보드 메인
    @GetMapping("")
    public String adminPage(Model model) {
        model.addAttribute("members", memberRepository.findAll());
        model.addAttribute("transactions", transactionRepository.findAllByOrderByDateDesc());
        model.addAttribute("faqs", faqRepository.findAll()); // FAQ 목록 추가

        // 통계
        long totalMembers = memberRepository.count();
        BigDecimal totalAssets = memberRepository.findAll().stream()
                .map(Member::getMyCoinBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalMembers", totalMembers);
        model.addAttribute("totalAssets", totalAssets);

        return "admin";
    }

    // 2. 회원 삭제 (강퇴)
    @PostMapping("/member/delete")
    public String deleteMember(@RequestParam Long id) {
        memberRepository.deleteById(id);
        return "redirect:/admin";
    }

    // 3. 회원 정보 수정 (★ 권한 변경 기능 포함)
    @PostMapping("/member/update")
    public String updateMember(@RequestParam Long id,
                               @RequestParam String userName,
                               @RequestParam BigDecimal balance,
                               @RequestParam String role) { // role 받기
        Member member = memberRepository.findById(id).orElseThrow();
        member.setUserName(userName);
        member.setMyCoinBalance(balance);
        member.setRole(role); // ★ DB에 권한 저장 (USER or ADMIN)
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
}