package com.kpbridge.kpbridge.repository;

import com.kpbridge.kpbridge.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqRepository extends JpaRepository<Faq, Long> {
}