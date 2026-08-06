package cn.sduonline.business.data.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum UserStatus implements IEnum<Integer> {

    DISABLED(0),
    NORMAL(1),
    FROZEN(2);

    private final Integer value;

    public static UserStatus valueOf(Integer code) {
        for (UserStatus status : values()) {
            if (Objects.equals(status.value, code)) return status;
        }
        throw new IllegalArgumentException("无效的UserStatus Code");
    }
}