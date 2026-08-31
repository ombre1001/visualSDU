package cn.sduonline.business.data.vo;

import lombok.Builder;

@Builder
public record StatsResponse(
        long totalMediaCount,
        long monthlyDeltaMediaCount,
        long dailyDeltaMediaCount,
        long pendingSubmissionsCount,
        long dailyDeltaPendingSubmissionsCount,
        long dayUploadedMediaCount,
        long dailyDeltaDayUploadedMediaCount,
        long registeredUsers,
        long dailyDeltaRegisteredUsers
) {
}
