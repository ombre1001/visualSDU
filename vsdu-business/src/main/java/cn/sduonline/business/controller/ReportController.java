package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.CreateMediaReportRequest;
import cn.sduonline.business.data.dto.CreateUserReportRequest;
import cn.sduonline.business.data.vo.ReportReasonTypeVO;
import cn.sduonline.business.data.vo.ReportVO;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.service.ReportService;
import cn.sduonline.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {
    private final ReportService reportService;

    /**
     * 举报理由下拉列表
     * 查询当前可用的举报理由，供客户端构建举报理由下拉列表。
     */
    @PublicApi
    @GetMapping("/reasons")
    public Result<List<ReportReasonTypeVO>> reasons() {
        return Result.success(reportService.reasons());
    }

    @PostMapping("/media")
    public Result<ReportVO> createMediaReport(
            @Valid @RequestBody CreateMediaReportRequest request,
            HttpServletRequest servletRequest
    ) {
        return Result.success(
                reportService.createMediaReport(
                        CurrentUser.id(), request, servletRequest.getRemoteAddr()
                ),
                "举报提交成功"
        );
    }

    @PostMapping("/user")
    public Result<ReportVO> createUserReport(
            @Valid @RequestBody CreateUserReportRequest request,
            HttpServletRequest servletRequest
    ) {
        return Result.success(
                reportService.createUserReport(
                        CurrentUser.id(), request, servletRequest.getRemoteAddr()
                ),
                "举报提交成功"
        );
    }
}
