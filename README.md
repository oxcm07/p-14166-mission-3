## 비밀번호 찾기 메일 설정

SMTP 설정 없이 실행하면 임시 비밀번호 메일 발송은 실패 처리됩니다.
실제 메일을 발송하려면 `mail` 프로필과 아래 환경 변수를 설정하세요.

```bash
SPRING_PROFILES_ACTIVE=mail
MAIL_USERNAME=<gmail-address>
MAIL_PASSWORD=<google-app-password>
```

Gmail 기본값은 `smtp.gmail.com:587`과 STARTTLS입니다.
Gmail을 사용하는 경우 계정 비밀번호가 아니라 Google 앱 비밀번호를 사용해야 합니다.
