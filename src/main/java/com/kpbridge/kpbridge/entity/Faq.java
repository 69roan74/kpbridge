package com.kpbridge.kpbridge.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Faq {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String question; // 질문

    @Column(length = 1000)
    private String answer;   // 답변

    private LocalDateTime createdDate = LocalDateTime.now();
}