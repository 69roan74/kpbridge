package com.kpbridge.kpbridge.controller;

import com.kpbridge.kpbridge.entity.Transaction;
import com.kpbridge.kpbridge.service.ChatService;
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
    private final ChatService chatService;

    // 1. 충전 요청
    @PostMapping("/charge")
    public ResponseEntity<?> charge(@RequestBody Map<String, Object> req, Principal principal) {
        BigDecimal amount = new BigDecimal(String.valueOf(req.get("amount")));
        String chargeMethod = String.valueOf(req.getOrDefault("chargeMethod", "USDT"));
        transactionService.charge(principal.getName(), amount, chargeMethod);
        return ResponseEntity.ok("충전이 완료되었습니다.");
    }

    // 2. 출금 요청
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody Map<String, Object> req, Principal principal) {
        try {
            BigDecimal amount = new BigDecimal(String.valueOf(req.get("amount")));
            String memo = String.valueOf(req.getOrDefault("memo", ""));
            transactionService.withdraw(principal.getName(), amount, memo);
            return ResponseEntity.ok("출금 신청이 처리되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 거래 가능 금액 조회
    @GetMapping("/tradable-balance")
    public ResponseEntity<?> tradableBalance(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        BigDecimal balance = transactionService.getTradableBalance(principal.getName());
        return ResponseEntity.ok(Map.of("tradableBalance", balance));
    }

    // 3. 거래 주문 접수 (main.html 거래하기 버튼)
    @PostMapping("/trade")
    public ResponseEntity<?> submitTrade(@RequestBody Map<String, Object> req, Principal principal) {
        try {
            String coin = (String) req.get("coin");
            String route = (String) req.get("route");
            BigDecimal amount = new BigDecimal(String.valueOf(req.get("amount")).replace(",", ""));

            // 거래 주문 생성 (상태: 거래대기중)
            Transaction tx = transactionService.submitOrder(principal.getName(), coin, route, amount);

            // 채팅으로 자동 알림 메시지 발송
            String chatMsg = String.format(
                "📋 거래 주문이 접수되었습니다.\n\n코인: %s\n경로: %s\n금액: %,d KRW\n\n담당자 확인 후 거래를 진행합니다.",
                coin, route, amount.longValue()
            );
            chatService.sendSystemMessage(principal.getName(), chatMsg);

            return ResponseEntity.ok("거래 주문이 접수되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("주문 실패: " + e.getMessage());
        }
    }
}
