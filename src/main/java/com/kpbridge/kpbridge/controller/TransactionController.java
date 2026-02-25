package com.kpbridge.kpbridge.controller;

import com.kpbridge.kpbridge.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/trans")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // 1. 충전 요청
    @PostMapping("/charge")
    public ResponseEntity<?> charge(@RequestBody Map<String, BigDecimal> req, Principal principal) {
        transactionService.charge(principal.getName(), req.get("amount"));
        return ResponseEntity.ok("충전이 완료되었습니다.");
    }

    // 2. 출금 요청
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody Map<String, BigDecimal> req, Principal principal) {
        try {
            transactionService.withdraw(principal.getName(), req.get("amount"));
            return ResponseEntity.ok("출금 신청이 처리되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // [여기가 중요!] 빨간 줄 났던 addProfit을 지우고, 이 코드로 바꾸세요!
    @PostMapping("/trade")
    public ResponseEntity<?> executeTrade(@RequestBody Map<String, Object> req, Principal principal) {
        try {
            String coin = (String) req.get("coin");
            String route = (String) req.get("route");
            
            // 콤마가 제거된 숫자 문자열을 받아서 BigDecimal로 변환
            BigDecimal amount = new BigDecimal(String.valueOf(req.get("amount")));

            // 서비스의 새로운 메서드 호출!
            transactionService.executeTrade(principal.getName(), coin, route, amount);
            
            return ResponseEntity.ok("거래가 성공적으로 체결되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("거래 실패: " + e.getMessage());
        }
    }
}