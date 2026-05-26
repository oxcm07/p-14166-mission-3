package com.mysite.sbb.user;

import com.mysite.sbb.DataNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService {
    private static final String KAKAO_PROVIDER = "kakao";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SiteUser create(String username, String email, String password) {
        SiteUser user = new SiteUser();
        user.setUsername(username);
        user.setEmail(email); //email.toLowerCase() 설정 시 대소문자 중복 문제 차단 가능
        user.setPassword(passwordEncoder.encode(password)); //비밀번호 암호화
        this.userRepository.save(user);
        return user;
    }

    @Transactional
    public SiteUser findOrCreateKakaoUser(String providerId, String email) {
        return this.userRepository.findByProviderAndProviderId(KAKAO_PROVIDER, providerId)
                .orElseGet(() -> createKakaoUser(providerId, email));
    }

    public SiteUser getUser(String username) {
        Optional<SiteUser> siteUser = this.userRepository.findByUsername(username);
        if (siteUser.isPresent()) {
            return siteUser.get();
        } else {
            throw new DataNotFoundException("siteuser not found");
        }
    }

    private SiteUser createKakaoUser(String providerId, String email) {
        SiteUser user = new SiteUser();
        user.setUsername(KAKAO_PROVIDER + "_" + providerId);
        user.setEmail(resolveKakaoEmail(providerId, email));
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setProvider(KAKAO_PROVIDER);
        user.setProviderId(providerId);
        return this.userRepository.save(user);
    }

    private String resolveKakaoEmail(String providerId, String email) {
        if (email != null && !email.isBlank() && this.userRepository.findByEmail(email).isEmpty()) {
            return email;
        }
        return KAKAO_PROVIDER + "_" + providerId + "@kakao.local";
    }
}
