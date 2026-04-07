package com.kpbridge.kpbridge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이 메시지가 속하는 회원 (대화 상대방 기준)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 발신자 타입: "USER" or "ADMIN"
    private String senderType;

    // 메시지 내용
    @Column(columnDefinition = "TEXT")
    private String content;

    // 발송 시각
    private LocalDateTime sentAt;

    // 읽음 여부
    private boolean isRead = false;
}
