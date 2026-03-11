package com.kpbridge.kpbridge.controller;

import com.kpbridge.kpbridge.dto.PriceResponseDto;
import com.kpbridge.kpbridge.service.CoinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CoinApiController {

    private final CoinService coinService;

    @GetMapping("/api/prices")
    public PriceResponseDto getPrices(
            @RequestParam(defaultValue = "upbit") String domestic,
            @RequestParam(defaultValue = "okx") String foreign) {
        return coinService.getAllPrices(domestic, foreign);
    }
}
