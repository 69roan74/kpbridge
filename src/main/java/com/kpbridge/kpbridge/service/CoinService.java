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

    private static final List<String> TARGET_COINS = List.of("BTC", "ETH", "USDT");

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

        // 두 결과 병합 대기
        List<Map<String, Object>> domesticData;
        double globalBtc, globalEth;
        try {
            CompletableFuture.allOf(domesticFuture, foreignFuture).join();
            domesticData = domesticFuture.get();
            double[] foreignPrices = foreignFuture.get();
            globalBtc = foreignPrices[0];
            globalEth = foreignPrices[1];
        } catch (Exception e) {
            log.error("병렬 API 호출 실패: {}", e.getMessage());
            domesticData = List.of();
            globalBtc = 0;
            globalEth = 0;
        }

        // 환율 추출 (국내 거래소 USDT 가격)
        double exchangeRate = 0;
        for (Map<String, Object> ticker : domesticData) {
            if ("KRW-USDT".equals(ticker.get("market"))) {
                exchangeRate = toDouble(ticker.get("trade_price"));
                break;
            }
        }

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
            double buyPriceUsdt = 0;
            double sellPriceUsdt = 0;

            if (!"USDT".equals(symbol) && exchangeRate > 0) {
                Double globalUsdt = globalPrices.get(market);
                if (globalUsdt != null && globalUsdt > 0) {
                    double globalKrw = globalUsdt * exchangeRate;
                    kimchiPremium = ((domesticPrice - globalKrw) / globalKrw) * 100;
                    buyPriceUsdt = globalUsdt;
                    sellPriceUsdt = domesticPrice / exchangeRate;
                }
            }

            coins.add(CoinPriceDto.builder()
                    .symbol(symbol)
                    .name(COIN_NAMES.getOrDefault(market, symbol))
                    .iconUrl(COIN_ICONS.getOrDefault(market, ""))
                    .currentPrice(domesticPrice)
                    .kimchiPremium(kimchiPremium)
                    .buyPriceUsdt(buyPriceUsdt)
                    .sellPriceUsdt(sellPriceUsdt)
                    .build());
        }

        return new PriceResponseDto(coins, exchangeRate);
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

    // ========== 유틸 ==========

    private double toDouble(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); }
        catch (NumberFormatException e) { return 0; }
    }
}
