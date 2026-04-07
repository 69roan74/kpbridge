package com.kpbridge.kpbridge.repository;

import com.kpbridge.kpbridge.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    // 특정 회원의 채팅 내역 (최신순)
    List<ChatMessage> findByMemberIdOrderBySentAtAsc(Long memberId);

    // 관리자 읽지 않은 메시지 수 (사용자가 보낸 것)
    long countByMemberIdAndSenderTypeAndIsReadFalse(Long memberId, String senderType);

    // 전체 읽지 않은 메시지 수 (관리자 대시보드용)
    long countBySenderTypeAndIsReadFalse(String senderType);

    // 채팅이 있는 회원 목록 (가장 최근 메시지 기준)
    @Query("SELECT DISTINCT m.id FROM ChatMessage m ORDER BY m.sentAt DESC")
    List<Long> findDistinctMemberIds();
}
