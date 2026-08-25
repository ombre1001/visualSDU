package cn.sduonline.business.data.vo;

public record AdminSubmissionUploaderVO(
        Long id, String casId, String name, String nickname, String avatarUrl,
        long submissionCount, long approvedCount
) {
}
