package com.kpbridge.kpbridge.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CoinPriceDto {
    private String symbol;
    private String name;
    private String iconUrl;
    private double currentPrice;
    private double kimchiPremium;
    private double buyPriceKrw;
    private double sellPriceKrw;
}
