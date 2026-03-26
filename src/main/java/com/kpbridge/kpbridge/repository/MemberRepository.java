package com.kpbridge.kpbridge.repository;

import com.kpbridge.kpbridge.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 아이디로 사용자 찾기
    Optional<Member> findByUserId(String userId);

    // 이메일로 사용자 찾기
    Optional<Member> findByEmail(String email);

    // 전화번호로 사용자 찾기
    Optional<Member> findByPhone(String phone);

    // 추천 코드로 추천인 찾기
    Optional<Member> findByReferralCode(String referralCode);

    // 특정 회원이 직접 추천한 하위 목록
    List<Member> findByReferredById(Long referredById);

    // 전체 자산 합계 (N+1 제거용 집계 쿼리)
    @Query("SELECT COALESCE(SUM(m.myCoinBalance), 0) FROM Member m")
    BigDecimal sumAllBalances();
}