package cn.sduonline.business.data.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus implements IEnum<Integer> {

    DISABLED(0),
    NORMAL(1),
    FROZEN(2);

    private final Integer value;

    @Override
    public Integer getValue() {
        return value;
    }
}