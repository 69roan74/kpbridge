package com.kpbridge.kpbridge.controller;

import com.kpbridge.kpbridge.entity.Transaction;
import com.kpbridge.kpbridge.service.ChatService;
import com.kpbridge.kpbridge.service.CoinService;
import com.kpbridge.kpbridge.service.SiteConfigService;
import com.kpbridge.kpbridge.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/trans")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final ChatService chatService;
    private final CoinService coinService;
    private final SiteConfigService siteConfigService;

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

    // 거래 가능 금액 조회 (KRW 원금 / USDT 원금 분리)
    @GetMapping("/tradable-balance")
    public ResponseEntity<?> tradableBalance(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        BigDecimal krw  = transactionService.getTradableKrwBalance(principal.getName());
        BigDecimal usdt = transactionService.getTradableUsdtBalance(principal.getName());
        return ResponseEntity.ok(Map.of("tradableKrw", krw, "tradableUsdt", usdt));
    }

    // 거래 가능 시간대 조회
    @GetMapping("/trade-hours")
    public ResponseEntity<?> tradeHours() {
        String enabled   = siteConfigService.get("trade.hours.enabled", "N");
        String startHour = siteConfigService.get("trade.start.hour", "0");
        String endHour   = siteConfigService.get("trade.end.hour", "23");
        return ResponseEntity.ok(Map.of(
            "enabled",   "Y".equals(enabled),
            "startHour", Integer.parseInt(startHour),
            "endHour",   Integer.parseInt(endHour)
        ));
    }

    // 3. 거래 주문 접수 (main.html 거래하기 버튼)
    @PostMapping("/trade")
    public ResponseEntity<?> submitTrade(@RequestBody Map<String, Object> req, Principal principal) {
        try {
            // 거래 시간 서버 측 검증
            String enabled   = siteConfigService.get("trade.hours.enabled", "N");
            if ("Y".equals(enabled)) {
                int startHour = Integer.parseInt(siteConfigService.get("trade.start.hour", "0"));
                int endHour   = Integer.parseInt(siteConfigService.get("trade.end.hour", "23"));
                int nowHour   = LocalTime.now().getHour();
                if (nowHour < startHour || nowHour >= endHour) {
                    return ResponseEntity.badRequest()
                        .body("⏰ 거래 가능 시간이 아닙니다. (" + startHour + "시 ~ " + endHour + "시)");
                }
            }

            String coin      = (String) req.get("coin");
            String route     = (String) req.get("route");
            String tradeType = String.valueOf(req.getOrDefault("tradeType", "KRW"));
            BigDecimal amount = new BigDecimal(String.valueOf(req.get("amount")).replace(",", ""));

            Double kimchiRate = req.get("kimchiRate") != null ? Double.valueOf(String.valueOf(req.get("kimchiRate"))) : null;
            Transaction tx = transactionService.submitOrder(principal.getName(), coin, route, amount, tradeType, kimchiRate);

            String chatMsg = String.format(
                "📋 거래 주문이 접수되었습니다.\n\n코인: %s\n경로: %s\n금액: %,d KRW (%s)\n\n담당자 확인 후 거래를 진행합니다.",
                coin, route, amount.longValue(), tradeType
            );
            chatService.sendSystemMessage(principal.getName(), chatMsg);

            return ResponseEntity.ok("거래 주문이 접수되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("주문 실패: " + e.getMessage());
        }
    }
}
