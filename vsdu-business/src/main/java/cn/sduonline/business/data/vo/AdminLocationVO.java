package cn.sduonline.business.data.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminLocationVO(
        Long id, Long campusId, String name, String categoryCode, String address,
        BigDecimal longitude, BigDecimal latitude, String coverUrl, String description,
        Integer sortOrder, Integer status, LocalDateTime createdAt, LocalDateTime updatedAt
) {
}
