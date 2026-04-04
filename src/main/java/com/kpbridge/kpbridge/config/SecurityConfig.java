package com.kpbridge.kpbridge.config;

import com.kpbridge.kpbridge.service.UserSecurityService; // ★ 우리가 만든 서비스 import
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor // ★ final 필드 주입을 위해 추가
public class SecurityConfig {

    private final UserSecurityService userSecurityService; // ★ 서비스 주입받기

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ★ [핵심] 스프링에게 "이 서비스와 이 암호화 방식을 써라"고 강제 지정
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userSecurityService); // 우리의 서비스를 등록
        authProvider.setPasswordEncoder(passwordEncoder());      // 암호화 방식 등록
        return authProvider;
    }

    // ★ [핵심] 위 설정을 관리자에 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) 
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN") // 관리자만 접근 가능
                .requestMatchers("/", "/register", "/login", "/about", "/faq", "/css/**", "/js/**", "/api/**", "/emergency-fix", "/favicon.svg", "/favicon.ico").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(login -> login
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .usernameParameter("username") // 혹시 몰라 명시
                .passwordParameter("password") // 혹시 몰라 명시
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}