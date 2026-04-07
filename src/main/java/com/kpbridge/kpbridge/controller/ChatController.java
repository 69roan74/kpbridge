package com.kpbridge.kpbridge.controller;

import com.kpbridge.kpbridge.entity.ChatMessage;
import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.repository.MemberRepository;
import com.kpbridge.kpbridge.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final MemberRepository memberRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    // ===== 사용자 채팅 페이지 =====

    @GetMapping("/chat")
    public String chatPage(Model model, Principal principal) {
        Member member = memberRepository.findByUserId(principal.getName()).orElseThrow();
        List<ChatMessage> messages = chatService.getMessagesForUser(principal.getName());
        model.addAttribute("member", member);
        model.addAttribute("messages", messages);
        return "chat";
    }

    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<?> sendMessage(@RequestBody Map<String, String> req, Principal principal) {
        String content = req.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body("내용을 입력해주세요.");
        }
        ChatMessage msg = chatService.sendUserMessage(principal.getName(), content.trim());
        return ResponseEntity.ok(Map.of(
                "id", msg.getId(),
                "content", msg.getContent(),
                "senderType", msg.getSenderType(),
                "sentAt", msg.getSentAt().format(FMT)
        ));
    }

    @GetMapping("/api/chat/messages")
    @ResponseBody
    public ResponseEntity<?> getMessages(Principal principal) {
        List<ChatMessage> messages = chatService.getMessagesForUser(principal.getName());
        return ResponseEntity.ok(toDto(messages));
    }

    // ===== 관리자 채팅 =====

    @GetMapping("/admin/chat")
    public String adminChatList(Model model) {
        List<Member> members = chatService.getMembersWithChat();
        model.addAttribute("members", members);
        model.addAttribute("totalUnread", chatService.getTotalUnread());
        return "admin-chat";
    }

    @GetMapping("/admin/chat/{memberId}")
    public String adminChatRoom(@PathVariable Long memberId, Model model) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        List<ChatMessage> messages = chatService.getMessagesForAdmin(memberId);
        model.addAttribute("chatMember", member);
        model.addAttribute("messages", messages);
        return "admin-chat-room";
    }

    @PostMapping("/api/admin/chat/send")
    @ResponseBody
    public ResponseEntity<?> adminSendMessage(@RequestBody Map<String, Object> req) {
        Long memberId = Long.valueOf(req.get("memberId").toString());
        String content = req.get("content").toString();
        if (content.isBlank()) return ResponseEntity.badRequest().body("내용을 입력해주세요.");
        ChatMessage msg = chatService.sendAdminMessage(memberId, content.trim());
        return ResponseEntity.ok(Map.of(
                "id", msg.getId(),
                "content", msg.getContent(),
                "senderType", msg.getSenderType(),
                "sentAt", msg.getSentAt().format(FMT)
        ));
    }

    @GetMapping("/api/admin/chat/messages/{memberId}")
    @ResponseBody
    public ResponseEntity<?> adminGetMessages(@PathVariable Long memberId) {
        List<ChatMessage> messages = chatService.getMessagesForAdmin(memberId);
        return ResponseEntity.ok(toDto(messages));
    }

    private List<Map<String, Object>> toDto(List<ChatMessage> messages) {
        return messages.stream().map(m -> Map.<String, Object>of(
                "id", m.getId(),
                "content", m.getContent(),
                "senderType", m.getSenderType(),
                "sentAt", m.getSentAt().format(FMT),
                "read", m.isRead()
        )).collect(Collectors.toList());
    }
}
