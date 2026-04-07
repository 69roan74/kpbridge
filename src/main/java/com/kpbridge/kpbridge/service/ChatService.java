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
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    /** 채팅 목록 아이템 (회원 + 미확인 수 + 마지막 메시지) */
    public record ChatListItem(Member member, long unreadCount, ChatMessage lastMessage) {}

    @Transactional
    public ChatMessage sendUserMessage(String userId, String content) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        return saveMessage(member, "USER", content);
    }

    @Transactional
    public ChatMessage sendAdminMessage(Long memberId, String content) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        return saveMessage(member, "ADMIN", content);
    }

    @Transactional
    public ChatMessage sendSystemMessage(String userId, String content) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        return saveMessage(member, "ADMIN", content);
    }

    public List<ChatMessage> getMessages(Long memberId) {
        return chatMessageRepository.findByMemberIdOrderBySentAtAsc(memberId);
    }

    @Transactional
    public List<ChatMessage> getMessagesForUser(String userId) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        List<ChatMessage> messages = chatMessageRepository.findByMemberIdOrderBySentAtAsc(member.getId());
        messages.stream()
                .filter(m -> "ADMIN".equals(m.getSenderType()) && !m.isRead())
                .forEach(m -> m.setRead(true));
        return messages;
    }

    @Transactional
    public List<ChatMessage> getMessagesForAdmin(Long memberId) {
        List<ChatMessage> messages = chatMessageRepository.findByMemberIdOrderBySentAtAsc(memberId);
        messages.stream()
                .filter(m -> "USER".equals(m.getSenderType()) && !m.isRead())
                .forEach(m -> m.setRead(true));
        return messages;
    }

    /**
     * 채팅 목록 - 미확인 메시지 많은 순 → 최근 메시지 순 정렬
     */
    public List<ChatListItem> getChatList() {
        List<Long> memberIds = chatMessageRepository.findDistinctMemberIds();
        return memberIds.stream()
                .distinct()
                .map(id -> memberRepository.findById(id).orElse(null))
                .filter(m -> m != null)
                .map(member -> {
                    long unread = chatMessageRepository
                            .countByMemberIdAndSenderTypeAndIsReadFalse(member.getId(), "USER");
                    List<ChatMessage> msgs = chatMessageRepository
                            .findByMemberIdOrderBySentAtAsc(member.getId());
                    ChatMessage last = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
                    return new ChatListItem(member, unread, last);
                })
                // 미확인 있는 것 먼저, 그 다음 최신 메시지 순
                .sorted(Comparator
                        .comparingLong(ChatListItem::unreadCount).reversed()
                        .thenComparing(item -> item.lastMessage() != null
                                ? item.lastMessage().getSentAt()
                                : LocalDateTime.MIN, Comparator.reverseOrder()))
                .toList();
    }

    public long getUnreadCount(Long memberId) {
        return chatMessageRepository.countByMemberIdAndSenderTypeAndIsReadFalse(memberId, "USER");
    }

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
