package cn.sduonline.business.data.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum ReportStatus implements IEnum<Integer> {
    PENDING(0),
    PROCESSING(1),
    CONFIRMED(2),
    DISMISSED(3),
    CLOSED(4);

    private final Integer value;

    public static ReportStatus valueOf(Integer value) {
        for (ReportStatus status : values()) {
            if (Objects.equals(status.value, value)) return status;
        }
        throw new IllegalArgumentException("无效的举报状态");
    }

    public boolean isActive() {
        return this == PENDING || this == PROCESSING;
    }
}
