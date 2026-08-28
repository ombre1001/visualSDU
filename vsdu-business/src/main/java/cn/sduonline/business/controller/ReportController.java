package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.CreateReportRequest;
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

    /** 查询当前可用的举报理由，供客户端构建举报理由下拉列表。 */
    @PublicApi
    @GetMapping("/reasons")
    public Result<List<ReportReasonTypeVO>> reasons() {
        return Result.success(reportService.reasons());
    }

    /** 当前登录用户提交内容举报。 */
    @PostMapping
    public Result<ReportVO> create(
            @Valid @RequestBody CreateReportRequest request,
            HttpServletRequest servletRequest
    ) {
        return Result.success(
                reportService.create(CurrentUser.id(), request, servletRequest.getRemoteAddr()),
                "举报提交成功"
        );
    }
}
