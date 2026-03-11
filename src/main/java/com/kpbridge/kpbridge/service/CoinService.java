package com.kpbridge.kpbridge.service;

import com.kpbridge.kpbridge.dto.CoinPriceDto;
import com.kpbridge.kpbridge.dto.PriceResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoinService {

    private final RestTemplate restTemplate;

    private static final String UPBIT_TICKER_URL =
            "https://api.upbit.com/v1/ticker?markets=KRW-BTC,KRW-ETH,KRW-USDT";
    private static final String OKX_BTC_URL =
            "https://www.okx.com/api/v5/market/ticker?instId=BTC-USDT";
    private static final String OKX_ETH_URL =
            "https://www.okx.com/api/v5/market/ticker?instId=ETH-USDT";

    private static final Map<String, String> COIN_NAMES = Map.of(
            "KRW-BTC", "비트코인",
            "KRW-ETH", "이더리움",
            "KRW-USDT", "테더"
    );
    private static final Map<String, String> COIN_ICONS = Map.of(
            "KRW-BTC", "https://s2.coinmarketcap.com/static/img/coins/64x64/1.png",
            "KRW-ETH", "https://s2.coinmarketcap.com/static/img/coins/64x64/1027.png",
            "KRW-USDT", "https://s2.coinmarketcap.com/static/img/coins/64x64/825.png"
    );

    /** 새로운 통합 API - 코인별 구조화 데이터 반환 */
    public PriceResponseDto getAllPrices() {
        List<Map<String, Object>> upbitData = fetchUpbitTickers();
        double okxBtc = fetchOkxPrice(OKX_BTC_URL);
        double okxEth = fetchOkxPrice(OKX_ETH_URL);

        double exchangeRate = 0;
        for (Map<String, Object> ticker : upbitData) {
            if ("KRW-USDT".equals(ticker.get("market"))) {
                exchangeRate = toDouble(ticker.get("trade_price"));
                break;
            }
        }

        Map<String, Double> globalPrices = Map.of(
                "KRW-BTC", okxBtc,
                "KRW-ETH", okxEth
        );

        List<CoinPriceDto> coins = new ArrayList<>();
        for (Map<String, Object> ticker : upbitData) {
            String market = (String) ticker.get("market");
            String symbol = market.replace("KRW-", "");

            double upbitPrice = toDouble(ticker.get("trade_price"));
            double changeRate = toDouble(ticker.get("signed_change_rate"));
            double changePrice = toDouble(ticker.get("signed_change_price"));
            double tradeVolume = toDouble(ticker.get("acc_trade_price_24h"));

            double kimchiPremium = 0;
            if (!"USDT".equals(symbol) && exchangeRate > 0) {
                Double globalUsdt = globalPrices.get(market);
                if (globalUsdt != null && globalUsdt > 0) {
                    double globalKrw = globalUsdt * exchangeRate;
                    kimchiPremium = ((upbitPrice - globalKrw) / globalKrw) * 100;
                }
            }

            coins.add(CoinPriceDto.builder()
                    .symbol(symbol)
                    .name(COIN_NAMES.getOrDefault(market, symbol))
                    .iconUrl(COIN_ICONS.getOrDefault(market, ""))
                    .currentPrice(upbitPrice)
                    .kimchiPremium(kimchiPremium)
                    .changeRate(changeRate)
                    .changePrice(changePrice)
                    .tradeVolume(tradeVolume)
                    .build());
        }

        return new PriceResponseDto(coins, exchangeRate);
    }

    // --- 기존 메서드 (MemberController.mainPage에서 호출됨) ---

    public String getUpbitBtc() {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> res = restTemplate.getForObject(
                    "https://api.upbit.com/v1/ticker?markets=KRW-BTC", List.class);
            return (res != null && !res.isEmpty()) ? res.get(0).get("trade_price").toString() : "0";
        } catch (Exception e) { return "0"; }
    }

    public String getBithumbBtc() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(
                    "https://api.bithumb.com/public/ticker/BTC_KRW", Map.class);
            return ((Map) res.get("data")).get("closing_price").toString();
        } catch (Exception e) { return "0"; }
    }

    public String getBinanceBtc() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(
                    "https://api3.binance.com/api/v3/ticker/price?symbol=BTCUSDT", Map.class);
            return res.get("price").toString();
        } catch (Exception e) { return "0"; }
    }

    public String getOkxBtc() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(
                    "https://www.okx.com/api/v5/market/ticker?instId=BTC-USDT", Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) res.get("data");
            return (data != null && !data.isEmpty()) ? data.get(0).get("last").toString() : "0";
        } catch (Exception e) { return "0"; }
    }

    public String getEthPrice() {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> res = restTemplate.getForObject(
                    "https://api.upbit.com/v1/ticker?markets=KRW-ETH", List.class);
            return (res != null && !res.isEmpty()) ? res.get(0).get("trade_price").toString() : "0";
        } catch (Exception e) { return "0"; }
    }

    public String getBinanceEthPrice() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(
                    "https://api.bithumb.com/public/ticker/ETH_KRW", Map.class);
            return ((Map) res.get("data")).get("closing_price").toString();
        } catch (Exception e) { return "0"; }
    }

    public String getOkxEthPrice() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(
                    "https://www.okx.com/api/v5/market/ticker?instId=ETH-USDT", Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) res.get("data");
            return (data != null && !data.isEmpty()) ? data.get(0).get("last").toString() : "0";
        } catch (Exception e) { return "0"; }
    }

    // --- 내부 헬퍼 ---

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchUpbitTickers() {
        try {
            List<Map<String, Object>> result = restTemplate.getForObject(UPBIT_TICKER_URL, List.class);
            return result != null ? result : List.of();
        } catch (Exception e) {
            log.error("Upbit API 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private double fetchOkxPrice(String url) {
        try {
            Map<String, Object> res = restTemplate.getForObject(url, Map.class);
            if (res != null && res.get("data") != null) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) res.get("data");
                if (data != null && !data.isEmpty()) {
                    return Double.parseDouble(data.get(0).get("last").toString());
                }
            }
        } catch (Exception e) {
            log.error("OKX API 호출 실패 ({}): {}", url, e.getMessage());
        }
        return 0;
    }

    private double toDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); }
        catch (NumberFormatException e) { return 0; }
    }
}
