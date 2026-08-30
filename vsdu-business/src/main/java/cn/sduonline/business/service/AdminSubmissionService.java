package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.AdminBatchReviewSubmissionItemRequest;
import cn.sduonline.business.data.dto.AdminBatchReviewSubmissionsRequest;
import cn.sduonline.business.data.dto.AdminReviewSubmissionRequest;
import cn.sduonline.business.data.enums.SubmissionStatus;
import cn.sduonline.business.data.po.Submission;
import cn.sduonline.business.data.po.SubmissionAsset;
import cn.sduonline.business.data.projection.AdminSubmissionDetailRow;
import cn.sduonline.business.data.projection.AdminSubmissionReviewLogRow;
import cn.sduonline.business.data.projection.AdminSubmissionSummaryRow;
import cn.sduonline.business.data.vo.*;
import cn.sduonline.business.mapper.SubmissionAssetMapper;
import cn.sduonline.business.mapper.SubmissionMapper;
import cn.sduonline.business.mapper.SubmissionReviewLogMapper;
import cn.sduonline.business.util.TagCodec;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSubmissionService {
    private static final int MAX_PAGE_SIZE = 100;
    private static final int RECENT_LOG_LIMIT = 5;

    private final SubmissionMapper submissionMapper;
    private final SubmissionAssetMapper assetMapper;
    private final SubmissionReviewLogMapper reviewLogMapper;
    private final AdminSubmissionReviewExecutor reviewExecutor;
    private final FileStorage fileStorage;

    public PageResult<AdminSubmissionSummaryVO> list(
            SubmissionStatus status,
            String keyword,
            Long userId,
            Long locationId,
            LocalDateTime submittedFrom,
            LocalDateTime submittedTo,
            String sort,
            long page,
            long size
    ) {
        if (submittedFrom != null && submittedTo != null && submittedFrom.isAfter(submittedTo)) {
            throw new BizException(BizCode.BAD_REQUEST, "投稿开始时间不能晚于结束时间");
        }
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedSort = normalizeSort(sort);
        long safePage = Math.max(1, page);
        long safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        Integer statusValue = status == null ? SubmissionStatus.PENDING.getValue() : status.getValue();

        long total = submissionMapper.countAdminSubmissions(
                statusValue, normalizedKeyword, userId, locationId, submittedFrom, submittedTo
        );
        long offset = (safePage - 1) * safeSize;
        List<AdminSubmissionSummaryVO> items = total == 0
                ? List.of()
                : submissionMapper.selectAdminSubmissionPage(
                        statusValue, normalizedKeyword, userId, locationId,
                        submittedFrom, submittedTo, normalizedSort, offset, safeSize
                ).stream().map(this::toSummaryVO).toList();
        return new PageResult<>(total, safePage, safeSize, items);
    }

    public AdminSubmissionDetailVO detail(Long submissionId) {
        AdminSubmissionDetailRow row = submissionMapper.selectAdminSubmissionDetail(submissionId);
        if (row == null) {
            throw new BizException(BizCode.ADMIN_SUBMISSION_NOT_FOUND);
        }

        List<SubmissionAssetVO> assets = assetMapper.selectList(
                new LambdaQueryWrapper<SubmissionAsset>()
                        .eq(SubmissionAsset::getSubmissionId, submissionId)
                        .orderByAsc(SubmissionAsset::getSortOrder)
                        .orderByAsc(SubmissionAsset::getId)
        ).stream().map(this::toAssetVO).toList();
        List<AdminSubmissionReviewLogVO> recentLogs = reviewLogMapper.selectPageBySubmission(
                submissionId, 0, RECENT_LOG_LIMIT
        ).stream().map(this::toReviewLogVO).toList();

        AdminSubmissionUploaderVO uploader = new AdminSubmissionUploaderVO(
                row.getUserId(), row.getUploaderCasId(), row.getUploaderName(),
                row.getUploaderNickname(), url(row.getUploaderAvatarKey()),
                Objects.requireNonNullElse(row.getUploaderSubmissionCount(), 0L),
                Objects.requireNonNullElse(row.getUploaderApprovedCount(), 0L)
        );
        return new AdminSubmissionDetailVO(
                row.getId(), uploader, row.getLocationId(), row.getLocationName(),
                row.getDescription(), row.getShotAt(), TagCodec.decode(row.getTags()),
                row.getStatus(), row.getReviewReason(), row.getSubmittedAt(),
                row.getReviewedBy(), row.getReviewerName(), row.getReviewedAt(),
                row.getCreatedAt(), row.getUpdatedAt(), row.getVersion(), assets, recentLogs
        );
    }

    public AdminSubmissionReviewResultVO review(
            Long reviewerId,
            Long submissionId,
            AdminReviewSubmissionRequest request
    ) {
        return reviewExecutor.review(reviewerId, submissionId, request);
    }

    public AdminBatchReviewResultVO batchReview(
            Long reviewerId,
            AdminBatchReviewSubmissionsRequest request
    ) {
        requireUniqueSubmissionIds(request.items());
        List<AdminBatchReviewItemResultVO> results = new ArrayList<>(request.items().size());
        int successCount = 0;

        for (AdminBatchReviewSubmissionItemRequest item : request.items()) {
            try {
                AdminSubmissionReviewResultVO reviewed = reviewExecutor.review(
                        reviewerId,
                        item.submissionId(),
                        item.toReviewRequest()
                );
                successCount++;
                results.add(new AdminBatchReviewItemResultVO(
                        item.submissionId(), true, BizCode.OK.getCode(), "审核成功",
                        reviewed.status(), reviewed.version()
                ));
            } catch (BizException exception) {
                BizCode code = exception.getBizCode();
                results.add(new AdminBatchReviewItemResultVO(
                        item.submissionId(), false, code.getCode(), exception.getMessage(), null, null
                ));
            } catch (RuntimeException exception) {
                log.error("批量审核稿件失败，submissionId={}", item.submissionId(), exception);
                results.add(new AdminBatchReviewItemResultVO(
                        item.submissionId(), false, BizCode.INTERNAL_SERVER_ERROR.getCode(),
                        BizCode.INTERNAL_SERVER_ERROR.getMsg(), null, null
                ));
            }
        }

        return new AdminBatchReviewResultVO(
                request.items().size(),
                successCount,
                request.items().size() - successCount,
                List.copyOf(results)
        );
    }

    public PageResult<AdminSubmissionReviewLogVO> reviewLogs(
            Long submissionId,
            long page,
            long size
    ) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BizException(BizCode.ADMIN_SUBMISSION_NOT_FOUND);
        }
        long safePage = Math.max(1, page);
        long safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        long total = reviewLogMapper.countBySubmission(submissionId);
        long offset = (safePage - 1) * safeSize;
        List<AdminSubmissionReviewLogVO> items = total == 0
                ? List.of()
                : reviewLogMapper.selectPageBySubmission(submissionId, offset, safeSize)
                        .stream().map(this::toReviewLogVO).toList();
        return new PageResult<>(total, safePage, safeSize, items);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.strip();
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank() || "oldest".equalsIgnoreCase(sort)) {
            return "oldest";
        }
        if ("newest".equalsIgnoreCase(sort)) {
            return "newest";
        }
        throw new BizException(BizCode.BAD_REQUEST, "sort只允许oldest或newest");
    }

    private void requireUniqueSubmissionIds(List<AdminBatchReviewSubmissionItemRequest> items) {
        Set<Long> ids = new HashSet<>();
        for (AdminBatchReviewSubmissionItemRequest item : items) {
            if (!ids.add(item.submissionId())) {
                throw new BizException(BizCode.BAD_REQUEST, "批量审核的稿件ID不能重复");
            }
        }
    }

    private AdminSubmissionSummaryVO toSummaryVO(AdminSubmissionSummaryRow row) {
        return new AdminSubmissionSummaryVO(
                row.getId(), row.getUserId(), row.getUploaderName(),
                row.getLocationId(), row.getLocationName(), row.getDescription(),
                row.getShotAt(), TagCodec.decode(row.getTags()), row.getStatus(),
                row.getReviewReason(), Objects.requireNonNullElse(row.getAssetCount(), 0),
                url(row.getCoverKey()), row.getSubmittedAt(), row.getReviewedAt(),
                row.getUpdatedAt(), row.getVersion()
        );
    }

    private SubmissionAssetVO toAssetVO(SubmissionAsset asset) {
        return new SubmissionAssetVO(
                asset.getId(), asset.getOriginalName(), asset.getContentType(),
                asset.getSizeBytes(), asset.getSortOrder(), asset.getMediaId(),
                url(asset.getObjectKey())
        );
    }

    private AdminSubmissionReviewLogVO toReviewLogVO(AdminSubmissionReviewLogRow row) {
        return new AdminSubmissionReviewLogVO(
                row.getId(), row.getSubmissionId(), row.getRoundNo(), row.getSubmissionVersion(),
                row.getDecision(), row.getReason(), row.getBeforeStatus(), row.getAfterStatus(),
                row.getReviewedBy(), row.getReviewerName(), row.getReviewedAt()
        );
    }

    private String url(String objectKey) {
        return objectKey == null || objectKey.isBlank() ? null : fileStorage.getUrl(objectKey);
    }
}
