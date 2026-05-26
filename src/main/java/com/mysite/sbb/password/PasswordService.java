package com.mysite.sbb.password;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.user.SiteUser;
import com.mysite.sbb.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class PasswordService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TemporaryPasswordGenerator temporaryPasswordGenerator;
    private final PasswordEmailSender passwordEmailSender;

    public void resetPassword(String email) {
        SiteUser user = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new DataNotFoundException("siteuser not found"));
        String previousPassword = user.getPassword();
        String temporaryPassword = this.temporaryPasswordGenerator.generate();
        user.setPassword(this.passwordEncoder.encode(temporaryPassword));
        this.userRepository.saveAndFlush(user);
        try {
            this.passwordEmailSender.sendTemporaryPassword(user.getEmail(), temporaryPassword);
        } catch (RuntimeException e) {
            user.setPassword(previousPassword);
            this.userRepository.saveAndFlush(user);
            throw e;
        }
    }

    @Transactional
    public boolean changePassword(String username, String currentPassword, String newPassword) {
        SiteUser user = this.userRepository.findByUsername(username)
                .orElseThrow(() -> new DataNotFoundException("siteuser not found"));
        if (!this.passwordEncoder.matches(currentPassword, user.getPassword())) {
            return false;
        }
        user.setPassword(this.passwordEncoder.encode(newPassword));
        this.userRepository.save(user);
        return true;
    }
}
