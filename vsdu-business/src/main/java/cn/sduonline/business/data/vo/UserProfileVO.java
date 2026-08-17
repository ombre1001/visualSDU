package cn.sduonline.business.data.vo;

import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.business.data.enums.UserStatus;

import java.time.LocalDateTime;

public record UserProfileVO(
        Long id,
        String phoneMasked,
        String casId,
        String name,
        String nickname,
        String avatarUrl,
        String bio,
        UserRole role,
        UserStatus status,
        boolean allowUpload,
        boolean allowDownload,
        boolean passwordConfigured,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
