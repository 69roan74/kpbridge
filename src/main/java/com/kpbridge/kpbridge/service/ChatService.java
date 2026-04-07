package com.kpbridge.kpbridge.service;

import com.kpbridge.kpbridge.entity.ChatMessage;
import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.repository.ChatMessageRepository;
import com.kpbridge.kpbridge.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    /**
     * 사용자 → 관리자 메시지 전송
     */
    @Transactional
    public ChatMessage sendUserMessage(String userId, String content) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        return saveMessage(member, "USER", content);
    }

    /**
     * 관리자 → 사용자 메시지 전송
     */
    @Transactional
    public ChatMessage sendAdminMessage(Long memberId, String content) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        return saveMessage(member, "ADMIN", content);
    }

    /**
     * 시스템 메시지 (거래 알림 등)
     */
    @Transactional
    public ChatMessage sendSystemMessage(String userId, String content) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        return saveMessage(member, "ADMIN", content);
    }

    /**
     * 특정 회원의 채팅 내역 조회
     */
    public List<ChatMessage> getMessages(Long memberId) {
        return chatMessageRepository.findByMemberIdOrderBySentAtAsc(memberId);
    }

    /**
     * 사용자 본인의 채팅 내역 조회 + 읽음 처리
     */
    @Transactional
    public List<ChatMessage> getMessagesForUser(String userId) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        List<ChatMessage> messages = chatMessageRepository.findByMemberIdOrderBySentAtAsc(member.getId());
        // 관리자 메시지를 읽음 처리
        messages.stream()
                .filter(m -> "ADMIN".equals(m.getSenderType()) && !m.isRead())
                .forEach(m -> m.setRead(true));
        return messages;
    }

    /**
     * 관리자가 특정 회원 채팅 조회 + 읽음 처리
     */
    @Transactional
    public List<ChatMessage> getMessagesForAdmin(Long memberId) {
        List<ChatMessage> messages = chatMessageRepository.findByMemberIdOrderBySentAtAsc(memberId);
        // 사용자 메시지를 읽음 처리
        messages.stream()
                .filter(m -> "USER".equals(m.getSenderType()) && !m.isRead())
                .forEach(m -> m.setRead(true));
        return messages;
    }

    /**
     * 채팅이 있는 회원 목록 (관리자용)
     */
    public List<Member> getMembersWithChat() {
        List<Long> memberIds = chatMessageRepository.findDistinctMemberIds();
        return memberIds.stream()
                .distinct()
                .map(id -> memberRepository.findById(id).orElse(null))
                .filter(m -> m != null)
                .toList();
    }

    /**
     * 읽지 않은 사용자 메시지 수 (관리자용)
     */
    public long getUnreadCount(Long memberId) {
        return chatMessageRepository.countByMemberIdAndSenderTypeAndIsReadFalse(memberId, "USER");
    }

    /**
     * 전체 읽지 않은 메시지 수
     */
    public long getTotalUnread() {
        return chatMessageRepository.countBySenderTypeAndIsReadFalse("USER");
    }

    private ChatMessage saveMessage(Member member, String senderType, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setMember(member);
        msg.setSenderType(senderType);
        msg.setContent(content);
        msg.setSentAt(LocalDateTime.now());
        msg.setRead(false);
        return chatMessageRepository.save(msg);
    }
}
