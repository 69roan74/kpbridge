package com.kpbridge.kpbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PriceResponseDto {
    private List<CoinPriceDto> coins;
    private double exchangeRate;
}
