package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AdminTimeComparisonVO(
        Long id, Long locationId, String title, String description, Integer status,
        List<AdminTimeComparisonItemVO> items, LocalDateTime createdAt, LocalDateTime updatedAt
) {
}
