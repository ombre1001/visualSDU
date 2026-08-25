package cn.sduonline.business.data.vo;

import java.util.List;

public record AdminBatchReviewResultVO(
        int requestedCount, int successCount, int failureCount,
        List<AdminBatchReviewItemResultVO> items
) {
}
