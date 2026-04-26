package com.kpbridge.kpbridge.service;

import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.repository.MemberRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReferralService referralService;

    /** 부팅 시 referralCode가 비어있는 회원에게 자동 발급 (구버전 데이터 보정) */
    @PostConstruct
    public void backfillMissingReferralCodes() {
        List<Member> missing = memberRepository.findAll().stream()
                .filter(m -> m.getReferralCode() == null || m.getReferralCode().isBlank())
                .toList();
        for (Member m : missing) {
            m.setReferralCode(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
            memberRepository.save(m);
            log.info("🔧 referralCode 자동 발급: {} → {}", m.getUserId(), m.getReferralCode());
        }
    }

    /** 비밀번호 변경 - 현재 비밀번호 검증 후 새 비밀번호로 교체 */
    public void changePassword(String userId, String currentPassword, String newPassword) {
        Member member = memberRepository.findByUserId(userId).orElseThrow();
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }

    /**
     * @param member      가입할 회원 객체 (referralCode 필드에 추천인 코드가 담겨 올 수 있음)
     * @param inviteCode  가입 시 입력한 추천인 코드 (optional)
     */
    public void register(Member member, String inviteCode) {
        // 1. 블랙리스트 체크
        List<String> fakeNumbers = Arrays.asList("01000000000", "01012345678", "01011111111");
        String cleanPhone = member.getPhone().replace("-", "");
        if (fakeNumbers.contains(cleanPhone)) {
            throw new RuntimeException("비정상적인 전화번호입니다. 진짜 번호를 입력해주세요.");
        }

        // 2. 날짜 비교
        String birth = member.getBirthDate().replace("-", "");
        String today = java.time.LocalDate.now().toString().replace("-", "");
        if (birth.length() == 8 && Integer.parseInt(birth) >= Integer.parseInt(today)) {
            throw new RuntimeException("생년월일은 오늘보다 과거여야 합니다.");
        }

        // 3. 중복 검사
        if (memberRepository.findByUserId(member.getUserId()).isPresent()) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }
        if (memberRepository.findByEmail(member.getEmail()).isPresent()) {
            throw new RuntimeException("이미 사용 중인 이메일 주소입니다.");
        }

        // 4. 비밀번호 암호화
        member.setPassword(passwordEncoder.encode(member.getPassword()));

        // 5. 기본 잔액 0 KP (포인트 자동 지급 없음)
        member.setMyCoinBalance(BigDecimal.ZERO);

        // 6. 나만의 추천 코드 자동 생성 (8자리 UUID 앞부분)
        String myCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        member.setReferralCode(myCode);

        // 7. 추천인 코드 필수 검증 및 연결
        if (inviteCode == null || inviteCode.isBlank()) {
            throw new RuntimeException("추천인 코드는 필수입니다. 추천인에게 코드를 받아 입력해주세요.");
        }
        Member referrer = memberRepository.findByReferralCode(inviteCode.trim())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 추천인 코드입니다. 다시 확인해주세요."));
        member.setReferredBy(referrer);
        member.setReferralAppliedYn("Y");

        // 8. 저장
        Member saved = memberRepository.save(member);

        // 9. 가입 보너스 체인 전파 (추천인이 있을 때만)
        if (saved.getReferredBy() != null) {
            referralService.propagateJoinReward(saved);
        }
    }

    // 기존 호환성을 위해 inviteCode 없는 버전도 유지
    public void register(Member member) {
        register(member, member.getReferralCode());
    }
}
