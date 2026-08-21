package cn.sduonline.business.data.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnnouncementStatus implements IEnum<Integer> {
    DRAFT(0),
    PUBLISHED(1),
    OFFLINE(2);

    private final Integer value;
}
