package com.kpbridge.kpbridge.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class CoinService {

    // API 주소 설정 (BTC 및 ETH 추가)
    private final String UPBIT_BTC = "https://api.upbit.com/v1/ticker?markets=KRW-BTC";
    private final String UPBIT_ETH = "https://api.upbit.com/v1/ticker?markets=KRW-ETH";
    
    private final String BITHUMB_BTC = "https://api.bithumb.com/public/ticker/BTC_KRW";
    private final String BITHUMB_ETH = "https://api.bithumb.com/public/ticker/ETH_KRW";
    
    private final String BINANCE_BTC = "https://api3.binance.com/api/v3/ticker/price?symbol=BTCUSDT";
    private final String BINANCE_ETH = "https://api3.binance.com/api/v3/ticker/price?symbol=ETHUSDT";
    
    private final String OKX_BTC = "https://www.okx.com/api/v5/market/ticker?instId=BTC-USDT";
    private final String OKX_ETH = "https://www.okx.com/api/v5/market/ticker?instId=ETH-USDT";

    // [BTC] 가격 조회 메서드
    public String getUpbitBtc() { return fetchUpbit(UPBIT_BTC); }
    public String getBithumbBtc() { return fetchBithumb(BITHUMB_BTC); }
    public String getBinanceBtc() { return fetchBinance(BINANCE_BTC); }
    public String getOkxBtc() { return fetchOkx(OKX_BTC); }

    // [ETH] 가격 조회 메서드 (이 부분이 있어야 화면에 데이터가 뜹니다!)
    public String getEthPrice() { return fetchUpbit(UPBIT_ETH); }
    public String getBinanceEthPrice() { return fetchBithumb(BITHUMB_ETH); } // 빗썸 로직 사용
    public String getOkxEthPrice() { return fetchOkx(OKX_ETH); }

    // --- 헬퍼 메서드 (데이터 파싱) ---
    private String fetchUpbit(String url) {
        try {
            RestTemplate rt = new RestTemplate();
            List<Map<String, Object>> res = rt.getForObject(url, List.class);
            return (res != null && !res.isEmpty()) ? res.get(0).get("trade_price").toString() : "0";
        } catch (Exception e) { return "0"; }
    }

    private String fetchBithumb(String url) {
        try {
            RestTemplate rt = new RestTemplate();
            Map res = rt.getForObject(url, Map.class);
            return ((Map) res.get("data")).get("closing_price").toString();
        } catch (Exception e) { return "0"; }
    }

    private String fetchBinance(String url) {
        try {
            RestTemplate rt = new RestTemplate();
            Map res = rt.getForObject(url, Map.class);
            return res.get("price").toString();
        } catch (Exception e) { return "0"; }
    }

    private String fetchOkx(String url) {
        try {
            RestTemplate rt = new RestTemplate();
            Map res = rt.getForObject(url, Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) res.get("data");
            return (data != null && !data.isEmpty()) ? data.get(0).get("last").toString() : "0";
        } catch (Exception e) { return "0"; }
    }
}