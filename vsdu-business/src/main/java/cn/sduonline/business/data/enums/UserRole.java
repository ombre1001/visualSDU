package cn.sduonline.business.data.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole implements IEnum<Integer> {

    USER(0),
    ADMIN(1);

    private final Integer value;

    @Override
    public Integer getValue() {
        return value;
    }
}