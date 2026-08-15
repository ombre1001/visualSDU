package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;

public record FavoriteFolderVO(
        Long id,
        String name,
        String description,
        Long coverMediaId,
        String coverUrl,
        long itemCount,
        boolean isDefault,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
