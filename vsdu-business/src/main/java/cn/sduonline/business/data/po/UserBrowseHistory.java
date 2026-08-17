package cn.sduonline.business.data.po;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserBrowseHistory {
    private Long userId;
    private Long mediaId;
    private Long viewCount;
    private LocalDateTime lastViewedAt;
}
