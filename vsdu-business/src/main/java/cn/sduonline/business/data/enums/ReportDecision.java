package cn.sduonline.business.data.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum ReportDecision implements IEnum<Integer> {
    CONFIRM(1, ReportStatus.CONFIRMED),
    DISMISS(2, ReportStatus.DISMISSED),
    CLOSE(3, ReportStatus.CLOSED);

    private final Integer value;
    private final ReportStatus targetStatus;

    public static ReportDecision valueOf(Integer value) {
        for (ReportDecision decision : values()) {
            if (Objects.equals(decision.value, value)) return decision;
        }
        throw new IllegalArgumentException("无效的举报处理决定");
    }
}
