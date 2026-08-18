package cn.sduonline.business.data.vo;

import java.time.LocalDateTime;

public record AdminUserVO(
        Long id,
        String phone,
        String casId,
        String name,
        String nickname,
        String avatarUrl,
        String bio,
        Integer role,
        Integer status,
        LocalDateTime frozenUntil,
        String frozenReason,
        boolean allowUpload,
        boolean allowDownload,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
