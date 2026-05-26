package com.mysite.sbb.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class KakaoOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private static final String KAKAO_REGISTRATION_ID = "kakao";

    private final UserService userService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!KAKAO_REGISTRATION_ID.equals(registrationId)) {
            return oauth2User;
        }

        KakaoUserInfo kakaoUserInfo = extractKakaoUserInfo(oauth2User.getAttributes());
        SiteUser siteUser = this.userService.findOrCreateKakaoUser(
                kakaoUserInfo.providerId(),
                kakaoUserInfo.email());

        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put("username", siteUser.getUsername());
        attributes.put("email", siteUser.getEmail());

        return new OAuth2UserPrincipal(
                siteUser.getUsername(),
                List.of(new SimpleGrantedAuthority(UserRole.USER.getValue())),
                attributes);
    }

    private KakaoUserInfo extractKakaoUserInfo(Map<String, Object> attributes) {
        Object id = attributes.get("id");
        if (id == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_kakao_id"),
                    "카카오 사용자 ID를 찾을 수 없습니다.");
        }

        Map<String, Object> kakaoAccount = getMap(attributes.get("kakao_account"));
        return new KakaoUserInfo(String.valueOf(id), getString(kakaoAccount.get("email")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Object value) {
        if (value instanceof Map<?, ?>) {
            return (Map<String, Object>) value;
        }
        return Map.of();
    }

    private String getString(Object value) {
        if (value instanceof String stringValue) {
            return stringValue;
        }
        return null;
    }

    private record KakaoUserInfo(String providerId, String email) {
    }
}
