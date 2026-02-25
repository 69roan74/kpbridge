package com.kpbridge.kpbridge.service;

import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal; // [중요] BigDecimal 사용을 위해 필요
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(Member member) {
        // 1. 블랙리스트 체크 (기존 기능 유지)
        List<String> fakeNumbers = Arrays.asList("01000000000", "01012345678", "01011111111");
        String cleanPhone = member.getPhone().replace("-", "");
        if (fakeNumbers.contains(cleanPhone)) {
            throw new RuntimeException("비정상적인 전화번호입니다. 진짜 번호를 입력해주세요.");
        }

        // 2. 날짜 비교 (기존 기능 유지)
        String birth = member.getBirthDate().replace("-", "");
        String today = java.time.LocalDate.now().toString().replace("-", "");
        if (birth.length() == 8 && Integer.parseInt(birth) >= Integer.parseInt(today)) {
            throw new RuntimeException("생년월일은 오늘보다 과거여야 합니다.");
        }

        // ★ 중복 검사 (아이디/이메일) ★
        if (memberRepository.findByUserId(member.getUserId()).isPresent()) {
            throw new RuntimeException("이미 사용 중인 아이디입니다.");
        }
        if (memberRepository.findByEmail(member.getEmail()).isPresent()) {
            throw new RuntimeException("이미 사용 중인 이메일 주소입니다.");
        }

        // 3. 비밀번호 암호화 및 150 KP 지급
        member.setPassword(this.passwordEncoder.encode(member.getPassword()));
        
        // [수정 완료] 여기서 에러가 났었습니다! BigDecimal로 감싸서 해결했습니다.
        member.setMyCoinBalance(BigDecimal.valueOf(150));
        
        memberRepository.save(member);
    }
}