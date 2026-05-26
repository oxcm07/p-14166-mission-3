package com.mysite.sbb.question;

import java.time.LocalDateTime;

public record QuestionListDto(
        long number,
        String subject,
        String authorName,
        LocalDateTime createDate,
        int answerCount,
        int commentCount,
        int viewCount,
        int voteCount
) {
}
