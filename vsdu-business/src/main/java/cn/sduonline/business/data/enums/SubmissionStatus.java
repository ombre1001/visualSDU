package cn.sduonline.business.data.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubmissionStatus implements IEnum<Integer> {
    PENDING(0),
    APPROVED(1),
    REJECTED(2),
    WITHDRAWN(3);

    private final Integer value;
}
