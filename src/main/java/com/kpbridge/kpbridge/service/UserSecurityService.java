package com.kpbridge.kpbridge.service;

import com.kpbridge.kpbridge.entity.Member;
import com.kpbridge.kpbridge.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ★ 로그 추가
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j // ★ 지저분한 System.out 제거
@Service
@RequiredArgsConstructor
public class UserSecurityService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("🔐 로그인 시도: {}", username);

        Optional<Member> _member = this.memberRepository.findByUserId(username);
        
        if (_member.isEmpty()) {
            log.warn("❌ 로그인 실패: 존재하지 않는 아이디 ({})", username);
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        Member member = _member.get();
        
        List<GrantedAuthority> authorities = new ArrayList<>();
        if ("ADMIN".equals(member.getRole())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            log.info("👑 관리자(ADMIN) 권한으로 로그인");
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new User(member.getUserId(), member.getPassword(), authorities);
    }
}