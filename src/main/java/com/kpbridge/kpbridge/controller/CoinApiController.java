package com.kpbridge.kpbridge.controller;

import com.kpbridge.kpbridge.service.CoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CoinApiController {

    private final CoinService coinService;

    // [중요] 자바스크립트가 2초마다 이 주소로 데이터를 요청
    @GetMapping("/api/prices")
    public Map<String, Object> getPrices() {
        Map<String, Object> prices = new HashMap<>();
        
        // 1. 비트코인(BTC) 데이터 포장
        prices.put("btcUpbit", coinService.getUpbitBtc());
        prices.put("btcBithumb", coinService.getBithumbBtc());
        prices.put("btcBinance", "0");
        prices.put("btcOkx", coinService.getOkxBtc());
        
        // 2. 이더리움(ETH) 데이터 포장
        prices.put("ethUpbit", coinService.getEthPrice());
        prices.put("ethBithumb", coinService.getBinanceEthPrice()); // 빗썸 로직 사용
        prices.put("ethBinance", "0");
        prices.put("ethOkx", coinService.getOkxEthPrice());
        
        return prices;
    }
}