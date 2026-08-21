package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;

public record AdminTagVO(Long id, String name, long mediaCount,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
}
