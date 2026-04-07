package com.kpbridge.kpbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "SITE_CONFIG")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class SiteConfig {
    @Id
    @Column(name = "config_key", length = 100)
    private String key;

    @Column(name = "config_value", length = 500)
    private String value;
}
