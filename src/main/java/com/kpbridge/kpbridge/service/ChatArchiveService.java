package com.kpbridge.kpbridge.service;

import com.kpbridge.kpbridge.entity.ChatMessage;
import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.repository.ChatMessageRepository;
import com.kpbridge.kpbridge.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.*;

/**
 * 매일 자정: 오늘 채팅 내역을 사용자별 txt 파일로 만들어 zip 저장
 * 저장 위치: ./chat-archives/YYYY-MM-DD.zip
 *   └ 압축 내부: userId_YYYY-MM-DD.txt (사용자별 대화 내용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatArchiveService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberRepository memberRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String ARCHIVE_DIR = "./chat-archives";

    /**
     * 매일 자정 00:00 실행
     * 전날 채팅 내역 zip 저장 후 DB에서 제거
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void archiveYesterdayChats() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime dayStart = yesterday.atStartOfDay();
        LocalDateTime dayEnd   = yesterday.atTime(23, 59, 59);

        log.info("📦 채팅 아카이빙 시작: {}", yesterday);

        // 어제 메시지가 있는 회원 목록
        List<Long> memberIds = chatMessageRepository.findDistinctMemberIds();
        if (memberIds.isEmpty()) {
            log.info("📭 아카이빙할 메시지 없음");
            return;
        }

        try {
            Path archiveDir = Paths.get(ARCHIVE_DIR);
            Files.createDirectories(archiveDir);

            String zipName = yesterday.format(DATE_FMT) + ".zip";
            Path zipPath = archiveDir.resolve(zipName);

            int totalMessages = 0;
            int userCount = 0;

            try (ZipOutputStream zos = new ZipOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(zipPath)),
                    StandardCharsets.UTF_8)) {

                for (Long memberId : memberIds) {
                    Member member = memberRepository.findById(memberId).orElse(null);
                    if (member == null) continue;

                    // 해당 회원의 어제 메시지만 조회
                    List<ChatMessage> msgs = chatMessageRepository
                            .findByMemberIdOrderBySentAtAsc(memberId)
                            .stream()
                            .filter(m -> !m.getSentAt().isBefore(dayStart) && !m.getSentAt().isAfter(dayEnd))
                            .toList();

                    if (msgs.isEmpty()) continue;

                    // 파일명: userId_2026-04-07.txt
                    String fileName = member.getUserId() + "_" + yesterday.format(DATE_FMT) + ".txt";
                    zos.putNextEntry(new ZipEntry(fileName));

                    // 헤더
                    String header = String.format(
                            "=== KPBridge 1:1 채팅 아카이브 ===\n" +
                            "회원: %s (%s)\n날짜: %s\n메시지 수: %d건\n%s\n\n",
                            member.getUserName(), member.getUserId(),
                            yesterday.format(DATE_FMT), msgs.size(),
                            "=".repeat(40));
                    zos.write(header.getBytes(StandardCharsets.UTF_8));

                    // 메시지 본문
                    for (ChatMessage msg : msgs) {
                        String who  = "USER".equals(msg.getSenderType()) ? "[" + member.getUserId() + "]" : "[관리자]";
                        String time = msg.getSentAt().format(TIME_FMT);
                        String line = time + " " + who + " " + msg.getContent() + "\n";
                        zos.write(line.getBytes(StandardCharsets.UTF_8));
                    }
                    zos.closeEntry();
                    totalMessages += msgs.size();
                    userCount++;
                }
            }

            if (userCount == 0) {
                // 실제 어제 메시지 없으면 zip 파일 삭제
                Files.deleteIfExists(zipPath);
                log.info("📭 어제({}) 채팅 없음 - 아카이브 생성 안 함", yesterday);
                return;
            }

            log.info("✅ 채팅 아카이브 완료: {} ({} 명, {} 건) → {}",
                    yesterday, userCount, totalMessages, zipPath.toAbsolutePath());

            // 30일 이상 된 오래된 zip 파일 자동 삭제
            deleteOldArchives(archiveDir, 30);

        } catch (IOException e) {
            log.error("❌ 채팅 아카이브 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * N일 이상 지난 아카이브 파일 삭제
     */
    private void deleteOldArchives(Path dir, int keepDays) {
        try {
            LocalDate cutoff = LocalDate.now().minusDays(keepDays);
            Files.list(dir)
                 .filter(p -> p.toString().endsWith(".zip"))
                 .filter(p -> {
                     try {
                         String name = p.getFileName().toString().replace(".zip", "");
                         return LocalDate.parse(name, DATE_FMT).isBefore(cutoff);
                     } catch (Exception e) { return false; }
                 })
                 .forEach(p -> {
                     try {
                         Files.delete(p);
                         log.info("🗑 오래된 아카이브 삭제: {}", p.getFileName());
                     } catch (IOException e) {
                         log.warn("아카이브 삭제 실패: {}", p.getFileName());
                     }
                 });
        } catch (IOException e) {
            log.warn("아카이브 정리 중 오류: {}", e.getMessage());
        }
    }

    /**
     * 브라우저 다운로드용: 특정 날짜 채팅을 zip 바이트로 반환
     * 파일 저장 없이 메모리에서 바로 생성 → 응답으로 전송
     */
    public byte[] buildZipBytes(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd   = date.atTime(23, 59, 59);

        List<Long> memberIds = chatMessageRepository.findDistinctMemberIds();
        if (memberIds.isEmpty()) return null;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            int count = 0;
            for (Long memberId : memberIds) {
                Member member = memberRepository.findById(memberId).orElse(null);
                if (member == null) continue;

                List<ChatMessage> msgs = chatMessageRepository
                        .findByMemberIdOrderBySentAtAsc(memberId)
                        .stream()
                        .filter(m -> !m.getSentAt().isBefore(dayStart) && !m.getSentAt().isAfter(dayEnd))
                        .toList();

                if (msgs.isEmpty()) continue;

                zos.putNextEntry(new ZipEntry(member.getUserId() + "_" + date.format(DATE_FMT) + ".txt"));

                String header = String.format(
                        "=== KPBridge 채팅 아카이브 ===\n" +
                        "회원: %s (%s) | 날짜: %s | 메시지: %d건\n%s\n\n",
                        member.getUserName(), member.getUserId(),
                        date.format(DATE_FMT), msgs.size(), "=".repeat(40));
                zos.write(header.getBytes(StandardCharsets.UTF_8));

                for (ChatMessage msg : msgs) {
                    String who  = "USER".equals(msg.getSenderType()) ? "[" + member.getUserId() + "]" : "[관리자]";
                    String line = msg.getSentAt().format(TIME_FMT) + " " + who + " " + msg.getContent() + "\n";
                    zos.write(line.getBytes(StandardCharsets.UTF_8));
                }
                zos.closeEntry();
                count++;
            }
            if (count == 0) return null;
            zos.finish();
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("zip 생성 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 관리자가 수동으로 특정 날짜 아카이브 서버 저장 (백업용)
     */
    public String archiveDate(LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd   = date.atTime(23, 59, 59);

        List<Long> memberIds = chatMessageRepository.findDistinctMemberIds();
        if (memberIds.isEmpty()) return "해당 날짜에 메시지가 없습니다.";

        try {
            Path archiveDir = Paths.get(ARCHIVE_DIR);
            Files.createDirectories(archiveDir);
            String zipName = date.format(DATE_FMT) + ".zip";
            Path zipPath = archiveDir.resolve(zipName);
            int count = 0;

            try (ZipOutputStream zos = new ZipOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(zipPath)),
                    StandardCharsets.UTF_8)) {

                for (Long memberId : memberIds) {
                    Member member = memberRepository.findById(memberId).orElse(null);
                    if (member == null) continue;

                    List<ChatMessage> msgs = chatMessageRepository
                            .findByMemberIdOrderBySentAtAsc(memberId)
                            .stream()
                            .filter(m -> !m.getSentAt().isBefore(dayStart) && !m.getSentAt().isAfter(dayEnd))
                            .toList();

                    if (msgs.isEmpty()) continue;

                    String fileName = member.getUserId() + "_" + date.format(DATE_FMT) + ".txt";
                    zos.putNextEntry(new ZipEntry(fileName));
                    String header = String.format("=== %s (%s) / %s ===\n\n",
                            member.getUserName(), member.getUserId(), date.format(DATE_FMT));
                    zos.write(header.getBytes(StandardCharsets.UTF_8));

                    for (ChatMessage msg : msgs) {
                        String who  = "USER".equals(msg.getSenderType()) ? "[" + member.getUserId() + "]" : "[관리자]";
                        String line = msg.getSentAt().format(TIME_FMT) + " " + who + " " + msg.getContent() + "\n";
                        zos.write(line.getBytes(StandardCharsets.UTF_8));
                    }
                    zos.closeEntry();
                    count++;
                }
            }

            if (count == 0) {
                Files.deleteIfExists(zipPath);
                return "해당 날짜에 메시지가 없습니다.";
            }
            return zipPath.toAbsolutePath().toString() + " (" + count + "명 저장)";
        } catch (IOException e) {
            log.error("수동 아카이브 실패: {}", e.getMessage());
            return "실패: " + e.getMessage();
        }
    }
}
