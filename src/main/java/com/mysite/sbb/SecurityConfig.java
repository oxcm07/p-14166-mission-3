package com.mysite.sbb;

import com.mysite.sbb.user.KakaoOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.frameoptions.XFrameOptionsHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) //@PreAuthorize 사용 위한 설정
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, KakaoOAuth2UserService kakaoOAuth2UserService) throws Exception {
        PathPatternRequestMatcher.Builder pathMatcher = PathPatternRequestMatcher.withDefaults();

        http
                .authorizeHttpRequests((authorizeHttpRequests) -> authorizeHttpRequests
                        .anyRequest().permitAll()) // AntPathRequestMatcher 제거됨
                .csrf((csrf) -> csrf
                        .ignoringRequestMatchers(pathMatcher.matcher("/h2-console/**")))
                .headers((headers) -> headers
                        .addHeaderWriter(new XFrameOptionsHeaderWriter(
                                XFrameOptionsHeaderWriter.XFrameOptionsMode.SAMEORIGIN)))
                .formLogin((formLogin) -> formLogin
                        .loginPage("/user/login")
                        .defaultSuccessUrl("/"))
                .oauth2Login((oauth2Login) -> oauth2Login
                        .loginPage("/user/login")
                        .defaultSuccessUrl("/")
                        .userInfoEndpoint((userInfoEndpoint) -> userInfoEndpoint
                                .userService(kakaoOAuth2UserService)))
                .logout((logout) -> logout
                        .logoutUrl("/user/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); //암호화 방식 변경 시 사용
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager(); //스프링 시큐리티의 인증을 처리
    }
}
