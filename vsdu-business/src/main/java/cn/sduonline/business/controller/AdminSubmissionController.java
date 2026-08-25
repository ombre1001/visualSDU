package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.AdminBatchReviewSubmissionsRequest;
import cn.sduonline.business.data.dto.AdminReviewSubmissionRequest;
import cn.sduonline.business.data.enums.SubmissionStatus;
import cn.sduonline.business.data.vo.*;
import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.service.AdminSubmissionService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/submissions")
public class AdminSubmissionController {
    private final AdminSubmissionService service;

    @AdminApi
    @GetMapping
    public Result<PageResult<AdminSubmissionSummaryVO>> list(
            @RequestParam(defaultValue = "PENDING") SubmissionStatus status,
            @RequestParam(required = false) @Size(max = 50) String keyword,
            @RequestParam(required = false) @Positive Long userId,
            @RequestParam(required = false) @Positive Long locationId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submittedFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime submittedTo,
            @RequestParam(defaultValue = "oldest") String sort,
            @RequestParam(defaultValue = "1") @Positive long page,
            @RequestParam(defaultValue = "20") @Positive long size
    ) {
        return Result.success(service.list(
                status, keyword, userId, locationId, submittedFrom, submittedTo, sort, page, size
        ));
    }

    @AdminApi
    @GetMapping("/{submissionId}")
    public Result<AdminSubmissionDetailVO> detail(
            @PathVariable @Positive Long submissionId
    ) {
        return Result.success(service.detail(submissionId));
    }

    @AdminApi
    @PostMapping("/{submissionId}/review")
    public Result<AdminSubmissionReviewResultVO> review(
            @PathVariable @Positive Long submissionId,
            @Valid @RequestBody AdminReviewSubmissionRequest request
    ) {
        return Result.success(
                service.review(CurrentUser.id(), submissionId, request),
                "稿件审核成功"
        );
    }

    @AdminApi
    @PostMapping("/reviews/batch")
    public Result<AdminBatchReviewResultVO> batchReview(
            @Valid @RequestBody AdminBatchReviewSubmissionsRequest request
    ) {
        return Result.success(
                service.batchReview(CurrentUser.id(), request),
                "批量审核完成"
        );
    }

    @AdminApi
    @GetMapping("/{submissionId}/review-logs")
    public Result<PageResult<AdminSubmissionReviewLogVO>> reviewLogs(
            @PathVariable @Positive Long submissionId,
            @RequestParam(defaultValue = "1") @Positive long page,
            @RequestParam(defaultValue = "20") @Positive long size
    ) {
        return Result.success(service.reviewLogs(submissionId, page, size));
    }
}
