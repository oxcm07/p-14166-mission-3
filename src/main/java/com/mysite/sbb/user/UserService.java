package com.mysite.sbb.user;

import com.mysite.sbb.DataNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final PasswordEmailSender passwordEmailSender;

    public SiteUser create(String username, String email, String password) {
        SiteUser user = new SiteUser();
        user.setUsername(username);
        user.setEmail(email); //email.toLowerCase() 설정 시 대소문자 중복 문제 차단 가능
        user.setPassword(passwordEncoder.encode(password)); //비밀번호 암호화
        this.userRepository.save(user);
        return user;
    }

    public SiteUser getUser(String username) {
        Optional<SiteUser> siteUser = this.userRepository.findByUsername(username);
        if (siteUser.isPresent()) {
            return siteUser.get();
        } else {
            throw new DataNotFoundException("siteuser not found");
        }
    }

    @Transactional
    public void resetPassword(String email) {
        SiteUser user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException("siteuser not found"));
        String temporaryPassword = this.temporaryPasswordGenerator.generate();
        this.passwordEmailSender.sendTemporaryPassword(user.getEmail(), temporaryPassword);
        user.setPassword(this.passwordEncoder.encode(temporaryPassword));
        this.userRepository.save(user);
    }

    @Transactional
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        SiteUser user = getUser(username);
        if (!this.passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }
        user.setPassword(this.passwordEncoder.encode(newPassword));
        this.userRepository.save(user);
        return true;
    }
}
