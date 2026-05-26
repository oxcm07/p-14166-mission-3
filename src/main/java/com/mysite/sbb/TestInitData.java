package com.mysite.sbb;

import com.mysite.sbb.category.Category;
import com.mysite.sbb.category.CategoryService;
import com.mysite.sbb.question.Question;
import com.mysite.sbb.question.QuestionRepository;
import com.mysite.sbb.user.SiteUser;
import com.mysite.sbb.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Configuration
public class TestInitData {
    private static final String INIT_USERNAME = "testuser";
    private static final String INIT_EMAIL = "testuser@sbb.local";
    private static final String INIT_PASSWORD = "1234";

    @Autowired
    @Lazy
    private TestInitData self;
    private final QuestionRepository questionRepository;
    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public TestInitData(
            QuestionRepository questionRepository,
            CategoryService categoryService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${sbb.test-init-data.enabled:false}") boolean enabled) {
        this.questionRepository = questionRepository;
        this.categoryService = categoryService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Bean
    @Order(1)
    public ApplicationRunner testInitDataApplicationRunner() {
        return args -> {
            self.work1();
        };
    }

    @Transactional
    void work1() {
        if (!this.enabled) return;
        if (questionRepository.count() > 0) return;

        Category questionAnswer = this.categoryService.getDefaultCategory();
        SiteUser initUser = this.getInitUser();
        LocalDateTime baseDateTime = LocalDateTime.now();

        for (int i = 0; i <= 150; i++) {
            Question q = new Question();
            q.setSubject("테스트 제목입니다: " + i);
            q.setContent("테스트 데이터 내용입니다: " + i);
            q.setCreateDate(baseDateTime.plusSeconds(i - 1));
            q.setCategory(questionAnswer);
            q.setAuthor(initUser);
            questionRepository.save(q);
        }

        Question q1 = new Question();
        q1.setSubject("sbb가 무엇인가요?");
        q1.setContent("sbb에 대해서 알고 싶습니다.");
        q1.setCreateDate(baseDateTime.plusSeconds(150));
        q1.setCategory(questionAnswer);
        q1.setAuthor(initUser);
        questionRepository.save(q1); // 첫번째 질문 저장

        Question q2 = new Question();
        q2.setSubject("스프링부트 모델 질문입니다.");
        q2.setContent("id는 자동으로 생성되나요?");
        q2.setCreateDate(baseDateTime.plusSeconds(151));
        q2.setCategory(questionAnswer);
        q2.setAuthor(initUser);

        q2.addAnswer("네 자동으로 생성됩니다.");
        q2.addAnswer("따로 생성할 필요가 없습니다.");
        q2.getAnswerList().forEach(answer -> answer.setAuthor(initUser));

        questionRepository.save(q2); // 두번째 질문 저장
    }

    private SiteUser getInitUser() {
        return this.userRepository.findByUsername(INIT_USERNAME)
                .orElseGet(() -> {
                    SiteUser user = new SiteUser();
                    user.setUsername(INIT_USERNAME);
                    user.setEmail(INIT_EMAIL);
                    user.setPassword(this.passwordEncoder.encode(INIT_PASSWORD));
                    return this.userRepository.save(user);
                });
    }
}
