package com.kpbridge.kpbridge.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Caffeine 캐시 설정
     * - coinPrices: 거래소 API 응답 5초 캐싱 (실시간성 vs 성능 균형)
     * - singlePrice: 개별 가격 조회 5초 캐싱
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("coinPrices", "singlePrice");
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.SECONDS)
                        .maximumSize(100)
        );
        return cacheManager;
    }
}
