package cn.sduonline.business.data.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Getter
@RequiredArgsConstructor
public enum UserRole implements IEnum<Integer> {

    USER(0),
    ADMIN(1);

    private final Integer value;

    public static UserRole valueOf(Integer code) {
        for (UserRole role : values()) {
            if (Objects.equals(role.value, code)) return role;
        }
        throw new IllegalArgumentException("无效的UserRole Code");
    }
}