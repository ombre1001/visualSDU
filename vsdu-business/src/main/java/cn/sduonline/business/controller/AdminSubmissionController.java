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

    /**
     * 管理端稿件列表
     * 按状态、关键词、投稿人、地点及投稿时间分页筛选稿件，并支持按投稿时间排序。
     */
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

    /**
     * 管理端稿件详情
     * 查询指定稿件、投稿人统计、关联资源及最近审核记录。
     */
    @AdminApi
    @GetMapping("/{submissionId}")
    public Result<AdminSubmissionDetailVO> detail(
            @PathVariable @Positive Long submissionId
    ) {
        return Result.success(service.detail(submissionId));
    }

    /**
     * 审核单个稿件
     * 根据审核决定处理待审核稿件，并使用请求中的期望版本执行乐观锁校验。
     */
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

    /**
     * 批量审核稿件
     * 逐项独立审核多个稿件，并汇总每项的成功或失败结果。
     */
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

    /**
     * 稿件审核记录
     * 分页查询指定稿件的历次审核决定、状态变化及审核人信息。
     */
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
