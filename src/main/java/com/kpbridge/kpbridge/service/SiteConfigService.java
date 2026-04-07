package com.kpbridge.kpbridge.service;

import com.kpbridge.kpbridge.entity.SiteConfig;
import com.kpbridge.kpbridge.repository.SiteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteConfigService {

    private final SiteConfigRepository siteConfigRepository;

    public String get(String key, String defaultValue) {
        return siteConfigRepository.findById(key)
                .map(SiteConfig::getValue)
                .orElse(defaultValue);
    }

    public void set(String key, String value) {
        siteConfigRepository.save(new SiteConfig(key, value));
    }
}
