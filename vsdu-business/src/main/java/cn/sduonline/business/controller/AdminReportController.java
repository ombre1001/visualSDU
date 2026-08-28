package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.AdminReportDecisionRequest;
import cn.sduonline.business.data.enums.ReportStatus;
import cn.sduonline.business.data.enums.ReportTargetType;
import cn.sduonline.business.data.vo.AdminReportDecisionResultVO;
import cn.sduonline.business.data.vo.AdminReportDetailVO;
import cn.sduonline.business.data.vo.AdminReportSummaryVO;
import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.service.AdminReportService;
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
@RequestMapping("/admin/reports")
public class AdminReportController {
    private final AdminReportService reportService;

    /** 按状态、目标类型、理由、举报人和时间范围分页查询举报。 */
    @AdminApi
    @GetMapping
    public Result<PageResult<AdminReportSummaryVO>> list(
            @RequestParam(defaultValue = "PENDING") ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) @Size(max = 32) String reasonType,
            @RequestParam(required = false) @Positive Long reporterId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo,
            @RequestParam(defaultValue = "1") @Positive long page,
            @RequestParam(defaultValue = "20") @Positive long size
    ) {
        return Result.success(reportService.list(
                status, targetType, reasonType, reporterId,
                createdFrom, createdTo, page, size
        ));
    }

    /** 查询举报人、举报目标、相关举报数量和完整处理历史。 */
    @AdminApi
    @GetMapping("/{reportId}")
    public Result<AdminReportDetailVO> detail(
            @PathVariable @Positive Long reportId
    ) {
        return Result.success(reportService.detail(reportId));
    }

    /** 使用期望版本提交举报处理决定并执行对应资源处置。 */
    @AdminApi
    @PostMapping("/{reportId}/decision")
    public Result<AdminReportDecisionResultVO> decide(
            @PathVariable @Positive Long reportId,
            @Valid @RequestBody AdminReportDecisionRequest request
    ) {
        return Result.success(
                reportService.decide(CurrentUser.id(), reportId, request),
                "举报处理成功"
        );
    }
}
