package cn.sduonline.business.data.vo;

public record AdminReportReporterVO(
        Long id,
        String casId,
        String name,
        String nickname,
        String avatarUrl,
        long reportCount,
        long confirmedCount
) {
}
