package com.kpbridge.kpbridge.service;

import com.kpbridge.kpbridge.dto.CoinPriceDto;
import com.kpbridge.kpbridge.dto.PriceResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoinService {

    private final RestTemplate restTemplate;

    // 비동기 외부 API 호출용 스레드풀
    private final ExecutorService apiExecutor = Executors.newFixedThreadPool(6);

    // --- 국내 거래소 URL ---
    private static final String UPBIT_TICKER_URL =
            "https://api.upbit.com/v1/ticker?markets=KRW-BTC,KRW-ETH,KRW-USDT";
    private static final String BITHUMB_ALL_URL =
            "https://api.bithumb.com/public/ticker/ALL_KRW";

    // --- 해외 거래소 URL ---
    private static final String OKX_BTC_URL =
            "https://www.okx.com/api/v5/market/ticker?instId=BTC-USDT";
    private static final String OKX_ETH_URL =
            "https://www.okx.com/api/v5/market/ticker?instId=ETH-USDT";

    private static final Map<String, String> COIN_NAMES = Map.of(
            "KRW-BTC", "비트코인",
            "KRW-ETH", "이더리움",
            "KRW-USDT", "테더"
    );
    private static final Map<String, String> COIN_NAMES_EN = Map.of(
            "KRW-BTC", "Bitcoin",
            "KRW-ETH", "Ethereum",
            "KRW-USDT", "Tether"
    );
    private static final Map<String, String> COIN_ICONS = Map.of(
            "KRW-BTC", "https://s2.coinmarketcap.com/static/img/coins/64x64/1.png",
            "KRW-ETH", "https://s2.coinmarketcap.com/static/img/coins/64x64/1027.png",
            "KRW-USDT", "https://s2.coinmarketcap.com/static/img/coins/64x64/825.png"
    );

    private static final List<String> TARGET_COINS = List.of("BTC", "ETH");

    /**
     * 통합 API - 거래소 선택 지원 + 5초 캐싱
     * 국내/해외 API 병렬 호출로 응답속도 최적화
     */
    @Cacheable(value = "coinPrices", key = "#domestic + '_' + #foreign")
    public PriceResponseDto getAllPrices(String domestic, String foreign) {
        // 국내/해외 API 병렬 호출
        CompletableFuture<List<Map<String, Object>>> domesticFuture = CompletableFuture.supplyAsync(() -> {
            if ("bithumb".equals(domestic)) return fetchBithumbTickers();
            return fetchUpbitTickers();
        }, apiExecutor);

        CompletableFuture<double[]> foreignFuture = CompletableFuture.supplyAsync(() -> {
            double btc, eth;
            switch (foreign != null ? foreign : "okx") {
                case "htx":
                    btc = fetchHtxPrice("btcusdt");
                    eth = fetchHtxPrice("ethusdt");
                    break;
                case "gate":
                    btc = fetchGatePrice("BTC_USDT");
                    eth = fetchGatePrice("ETH_USDT");
                    break;
                case "binance":
                    btc = fetchBinancePrice("BTCUSDT");
                    eth = fetchBinancePrice("ETHUSDT");
                    break;
                default: // okx
                    btc = fetchOkxPrice(OKX_BTC_URL);
                    eth = fetchOkxPrice(OKX_ETH_URL);
            }
            return new double[]{btc, eth};
        }, apiExecutor);

        // 실제 USD/KRW 외환 환율도 병렬 호출
        CompletableFuture<Double> forexFuture = CompletableFuture.supplyAsync(
                this::fetchForexUsdKrwRate, apiExecutor);

        // 세 결과 병합 대기
        List<Map<String, Object>> domesticData;
        double globalBtc, globalEth, forexRate;
        try {
            CompletableFuture.allOf(domesticFuture, foreignFuture, forexFuture).join();
            domesticData = domesticFuture.get();
            double[] foreignPrices = foreignFuture.get();
            globalBtc = foreignPrices[0];
            globalEth = foreignPrices[1];
            forexRate = forexFuture.get();
        } catch (Exception e) {
            log.error("병렬 API 호출 실패: {}", e.getMessage());
            domesticData = List.of();
            globalBtc = 0;
            globalEth = 0;
            forexRate = 0;
        }

        // 업비트 USDT 시세 (UI 표시용)
        double exchangeRate = 0;
        for (Map<String, Object> ticker : domesticData) {
            if ("KRW-USDT".equals(ticker.get("market"))) {
                exchangeRate = toDouble(ticker.get("trade_price"));
                break;
            }
        }
        // 외환 환율 fallback: 업비트 USDT/KRW 시세를 USD/KRW 대용으로 사용
        if (forexRate <= 0) forexRate = exchangeRate > 0 ? exchangeRate : 1499.0;

        Map<String, Double> globalPrices = Map.of(
                "KRW-BTC", globalBtc,
                "KRW-ETH", globalEth
        );

        // 코인별 데이터 조합
        List<CoinPriceDto> coins = new ArrayList<>();
        for (Map<String, Object> ticker : domesticData) {
            String market = (String) ticker.get("market");
            String symbol = market.replace("KRW-", "");

            double domesticPrice = toDouble(ticker.get("trade_price"));
            double kimchiPremium = 0;
            double buyPriceKrw = 0;
            double sellPriceKrw = 0;

            if ("USDT".equals(symbol) && forexRate > 0) {
                // USDT 김프: 업비트 USDT/KRW 가격 vs 실제 USD/KRW 환율
                kimchiPremium = ((domesticPrice - forexRate) / forexRate) * 100;
                buyPriceKrw = forexRate;
                sellPriceKrw = domesticPrice;
            } else {
                Double globalUsdt = globalPrices.get(market);
                if (globalUsdt != null && globalUsdt > 0 && forexRate > 0) {
                    // 김프 계산: 실제 USD/KRW 환율 사용 → 더 정확한 차익 반영
                    double globalKrw = globalUsdt * forexRate;
                    kimchiPremium = ((domesticPrice - globalKrw) / globalKrw) * 100;
                    buyPriceKrw = globalKrw;
                    sellPriceKrw = domesticPrice;
                }
            }

            coins.add(CoinPriceDto.builder()
                    .symbol(symbol)
                    .name(COIN_NAMES.getOrDefault(market, symbol))
                    .iconUrl(COIN_ICONS.getOrDefault(market, ""))
                    .currentPrice(domesticPrice)
                    .kimchiPremium(kimchiPremium)
                    .buyPriceKrw(buyPriceKrw)
                    .sellPriceKrw(sellPriceKrw)
                    .build());
        }

        return new PriceResponseDto(coins, exchangeRate);
    }

    /** USDT/KRW 환율 (Upbit KRW-USDT 시세) */
    @Cacheable(value = "singlePrice", key = "'usdt_krw_rate'")
    public double getUsdtKrwRate() {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> res = restTemplate.getForObject(
                    "https://api.upbit.com/v1/ticker?markets=KRW-USDT", List.class);
            return (res != null && !res.isEmpty()) ? toDouble(res.get(0).get("trade_price")) : 1400.0;
        } catch (Exception e) { return 1400.0; }
    }

    // --- 기존 메서드 (MemberController.mainPage에서 호출됨) - 캐싱 적용 ---

    @Cacheable(value = "singlePrice", key = "'upbit_btc'")
    public String getUpbitBtc() {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> res = restTemplate.getForObject(
                    "https://api.upbit.com/v1/ticker?markets=KRW-BTC", List.class);
            return (res != null && !res.isEmpty()) ? res.get(0).get("trade_price").toString() : "0";
        } catch (Exception e) { return "0"; }
    }

    @Cacheable(value = "singlePrice", key = "'bithumb_btc'")
    public String getBithumbBtc() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(
                    "https://api.bithumb.com/public/ticker/BTC_KRW", Map.class);
            return ((Map) res.get("data")).get("closing_price").toString();
        } catch (Exception e) { return "0"; }
    }

    @Cacheable(value = "singlePrice", key = "'binance_btc'")
    public String getBinanceBtc() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(
                    "https://api3.binance.com/api/v3/ticker/price?symbol=BTCUSDT", Map.class);
            return res.get("price").toString();
        } catch (Exception e) { return "0"; }
    }

    @Cacheable(value = "singlePrice", key = "'okx_btc'")
    public String getOkxBtc() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(
                    "https://www.okx.com/api/v5/market/ticker?instId=BTC-USDT", Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) res.get("data");
            return (data != null && !data.isEmpty()) ? data.get(0).get("last").toString() : "0";
        } catch (Exception e) { return "0"; }
    }

    @Cacheable(value = "singlePrice", key = "'upbit_eth'")
    public String getEthPrice() {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> res = restTemplate.getForObject(
                    "https://api.upbit.com/v1/ticker?markets=KRW-ETH", List.class);
            return (res != null && !res.isEmpty()) ? res.get(0).get("trade_price").toString() : "0";
        } catch (Exception e) { return "0"; }
    }

    @Cacheable(value = "singlePrice", key = "'bithumb_eth'")
    public String getBinanceEthPrice() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(
                    "https://api.bithumb.com/public/ticker/ETH_KRW", Map.class);
            return ((Map) res.get("data")).get("closing_price").toString();
        } catch (Exception e) { return "0"; }
    }

    @Cacheable(value = "singlePrice", key = "'okx_eth'")
    public String getOkxEthPrice() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(
                    "https://www.okx.com/api/v5/market/ticker?instId=ETH-USDT", Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) res.get("data");
            return (data != null && !data.isEmpty()) ? data.get(0).get("last").toString() : "0";
        } catch (Exception e) { return "0"; }
    }

    // ========== 국내 거래소 fetch ==========

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
    private List<Map<String, Object>> fetchBithumbTickers() {
        try {
            Map<String, Object> res = restTemplate.getForObject(BITHUMB_ALL_URL, Map.class);
            if (res == null || res.get("data") == null) return List.of();

            Map<String, Object> data = (Map<String, Object>) res.get("data");
            List<Map<String, Object>> tickers = new ArrayList<>();

            for (String coin : TARGET_COINS) {
                Object coinData = data.get(coin);
                if (coinData instanceof Map) {
                    Map<String, Object> coinMap = (Map<String, Object>) coinData;
                    Map<String, Object> ticker = new HashMap<>();
                    ticker.put("market", "KRW-" + coin);
                    ticker.put("trade_price", toDouble(coinMap.get("closing_price")));
                    tickers.add(ticker);
                }
            }
            return tickers;
        } catch (Exception e) {
            log.error("Bithumb API 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }

    // ========== 해외 거래소 fetch ==========

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

    @SuppressWarnings("unchecked")
    private double fetchHtxPrice(String symbol) {
        try {
            String url = "https://api.huobi.pro/market/detail/merged?symbol=" + symbol;
            Map<String, Object> res = restTemplate.getForObject(url, Map.class);
            if (res != null && "ok".equals(res.get("status"))) {
                Map<String, Object> tick = (Map<String, Object>) res.get("tick");
                if (tick != null) return toDouble(tick.get("close"));
            }
        } catch (Exception e) {
            log.error("HTX API 호출 실패 ({}): {}", symbol, e.getMessage());
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private double fetchGatePrice(String pair) {
        try {
            String url = "https://api.gateio.ws/api/v4/spot/tickers?currency_pair=" + pair;
            List<Map<String, Object>> res = restTemplate.getForObject(url, List.class);
            if (res != null && !res.isEmpty()) {
                return Double.parseDouble(res.get(0).get("last").toString());
            }
        } catch (Exception e) {
            log.error("Gate.io API 호출 실패 ({}): {}", pair, e.getMessage());
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private double fetchBinancePrice(String symbol) {
        try {
            String url = "https://api3.binance.com/api/v3/ticker/price?symbol=" + symbol;
            Map<String, Object> res = restTemplate.getForObject(url, Map.class);
            if (res != null) return toDouble(res.get("price"));
        } catch (Exception e) {
            log.error("Binance API 호출 실패 ({}): {}", symbol, e.getMessage());
        }
        return 0;
    }

    // ========== 외환 환율 fetch (USD/KRW) ==========

    @SuppressWarnings("unchecked")
    private double fetchForexUsdKrwRate() {
        // 1차: ExchangeRate-API (무료, 키 불필요)
        try {
            Map<String, Object> res = restTemplate.getForObject(
                    "https://api.exchangerate-api.com/v4/latest/USD", Map.class);
            if (res != null && res.get("rates") instanceof Map) {
                Object krw = ((Map<String, Object>) res.get("rates")).get("KRW");
                if (krw != null) return toDouble(krw);
            }
        } catch (Exception e) {
            log.warn("ExchangeRate-API 호출 실패, 2차 시도: {}", e.getMessage());
        }
        // 2차: Frankfurter (ECB 기준)
        try {
            Map<String, Object> res = restTemplate.getForObject(
                    "https://api.frankfurter.app/latest?from=USD&to=KRW", Map.class);
            if (res != null && res.get("rates") instanceof Map) {
                Object krw = ((Map<String, Object>) res.get("rates")).get("KRW");
                if (krw != null) return toDouble(krw);
            }
        } catch (Exception e) {
            log.warn("Frankfurter API 호출 실패: {}", e.getMessage());
        }
        return 0;
    }

    // ========== 유틸 ==========

    private double toDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); }
        catch (NumberFormatException e) { return 0; }
    }
}
