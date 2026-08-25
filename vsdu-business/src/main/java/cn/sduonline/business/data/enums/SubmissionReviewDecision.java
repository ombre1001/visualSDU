package cn.sduonline.business.data.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 投稿审核决定。 */
@Getter
@RequiredArgsConstructor
public enum SubmissionReviewDecision implements IEnum<Integer> {
    APPROVE(1),
    RETURN(2),
    REJECT(3);

    private final Integer value;
}
