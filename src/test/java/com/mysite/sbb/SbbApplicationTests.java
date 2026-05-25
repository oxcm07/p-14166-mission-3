package com.mysite.sbb;

import com.mysite.sbb.question.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class SbbApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private QuestionRepository questionRepository;

	@Test
	void sbb() throws Exception {
		mockMvc.perform(get("/sbb"))
				.andExpect(status().isOk())
				.andExpect(content().string("안녕하세요 sbb에 오신 것을 환영합니다."));
	}

	@Test
	void testInitDataIsNotCreatedDuringTests() {
		assertThat(questionRepository.count()).isZero();
	}

}
