package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.CreateSubmissionRequest;
import cn.sduonline.business.data.dto.UpdateSubmissionRequest;
import cn.sduonline.business.data.enums.SubmissionStatus;
import cn.sduonline.business.data.vo.PageResult;
import cn.sduonline.business.data.vo.SubmissionDetailVO;
import cn.sduonline.business.data.vo.SubmissionSummaryVO;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.service.SubmissionService;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/submissions")
public class SubmissionController {
    private final SubmissionService submissionService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<SubmissionDetailVO> create(
            @Valid @ModelAttribute CreateSubmissionRequest request
    ) {
        SubmissionDetailVO result = submissionService.create(CurrentUser.id(), request);
        String message = result.status() == SubmissionStatus.APPROVED
                ? "稿件已自动发布"
                : "稿件已提交审核";
        return Result.success(result, message);
    }

    @GetMapping("/mine")
    public Result<PageResult<SubmissionSummaryVO>> mine(
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.success(submissionService.mine(CurrentUser.id(), status, page, size));
    }

    @GetMapping("/{submissionId}")
    public Result<SubmissionDetailVO> detail(@PathVariable Long submissionId) {
        return Result.success(submissionService.detail(CurrentUser.id(), CurrentUser.role(), submissionId));
    }

    @PutMapping(value = "/{submissionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<SubmissionDetailVO> update(
            @PathVariable Long submissionId,
            @Valid @ModelAttribute UpdateSubmissionRequest request
    ) {
        return Result.success(
                submissionService.update(CurrentUser.id(), submissionId, request),
                "稿件修改成功"
        );
    }

    @PostMapping("/{submissionId}/resubmit")
    public Result<SubmissionDetailVO> resubmit(@PathVariable Long submissionId) {
        SubmissionDetailVO result = submissionService.resubmit(CurrentUser.id(), submissionId);
        String message = result.status() == SubmissionStatus.APPROVED
                ? "稿件已自动发布"
                : "稿件已重新提交审核";
        return Result.success(result, message);
    }

    @PostMapping("/{submissionId}/withdraw")
    public Result<Void> withdraw(@PathVariable Long submissionId) {
        submissionService.withdraw(CurrentUser.id(), submissionId);
        return Result.success(null, "稿件已撤回");
    }
}