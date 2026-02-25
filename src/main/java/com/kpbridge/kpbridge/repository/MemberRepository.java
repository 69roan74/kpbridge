package com.kpbridge.kpbridge.repository;

import com.kpbridge.kpbridge.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    
    // 아이디로 사용자 찾기
    Optional<Member> findByUserId(String userId);

    // 이메일로 사용자 찾기 (이걸 추가해야 서비스의 빨간 줄이 사라집니다!)
    Optional<Member> findByEmail(String email);

    // 전화번호로 사용자 찾기
    Optional<Member> findByPhone(String phone);
}