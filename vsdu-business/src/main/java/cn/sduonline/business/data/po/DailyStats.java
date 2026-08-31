package cn.sduonline.business.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@TableName("daily_stats")
public class DailyStats {

    @TableId(type = IdType.INPUT)
    private LocalDate statDate;
    private Long pendingReviewCount;
    private Long dailyUploadedMediaCount;
    private Long visibleMediaTotal;
    private Long dailyRegisteredUserCount;
    private Long registeredUserTotal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static final DailyStats ZERO = DailyStats.builder()
            .pendingReviewCount(0L)
            .dailyUploadedMediaCount(0L)
            .visibleMediaTotal(0L)
            .dailyRegisteredUserCount(0L)
            .registeredUserTotal(0L)
            .build();
}
