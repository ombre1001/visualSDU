package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.CreateSubmissionRequest;
import cn.sduonline.business.data.dto.UpdateSubmissionRequest;
import cn.sduonline.business.data.enums.SubmissionStatus;
import cn.sduonline.business.data.vo.SubmissionDetailVO;
import cn.sduonline.business.data.vo.SubmissionSummaryVO;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.service.SubmissionService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/submissions")
public class SubmissionController {
    private final SubmissionService submissionService;

    /**
     * 创建投稿
     * 以 multipart 表单提交图片和信息；文件、标签 ID 分别使用同名多值字段 files、tagIds。
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<SubmissionDetailVO> create(
            @Validated @ModelAttribute CreateSubmissionRequest request
    ) {
        SubmissionDetailVO result = submissionService.create(CurrentUser.id(), request);
        String message = result.status() == SubmissionStatus.APPROVED
                ? "稿件已自动发布"
                : "稿件已提交审核";
        return Result.success(result, message);
    }

    /**
     * 我的投稿
     * 按可选状态分页查询当前用户提交的稿件。
     */
    @GetMapping("/mine")
    public Result<PageResult<SubmissionSummaryVO>> mine(
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size
    ) {
        return Result.success(submissionService.mine(CurrentUser.id(), status, page, size));
    }

    /**
     * 投稿详情
     * 查询指定稿件；普通用户只能查看自己的稿件，管理员可查看任意稿件。
     */
    @GetMapping("/{submissionId}")
    public Result<SubmissionDetailVO> detail(@PathVariable Long submissionId) {
        return Result.success(submissionService.detail(CurrentUser.id(), CurrentUser.role(), submissionId));
    }

    /**
     * 修改投稿
     * 修改待审核或已驳回稿件；标签使用同名多值字段 tags，修改后不会自动重新提交。
     */
    @PutMapping(value = "/{submissionId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<SubmissionDetailVO> update(
            @PathVariable Long submissionId,
            @Validated @ModelAttribute UpdateSubmissionRequest request
    ) {
        return Result.success(
                submissionService.update(CurrentUser.id(), submissionId, request),
                "稿件修改成功"
        );
    }

    /**
     * 重新提交
     * 将当前用户已驳回的稿件重新提交审核，审核关闭时会直接发布。
     */
    @PostMapping("/{submissionId}/resubmit")
    public Result<SubmissionDetailVO> resubmit(@PathVariable Long submissionId) {
        SubmissionDetailVO result = submissionService.resubmit(CurrentUser.id(), submissionId);
        String message = result.status() == SubmissionStatus.APPROVED
                ? "稿件已自动发布"
                : "稿件已重新提交审核";
        return Result.success(result, message);
    }

    /**
     * 撤回投稿
     * 撤回当前用户处于待审核状态的稿件。
     */
    @PostMapping("/{submissionId}/withdraw")
    public Result<Void> withdraw(@PathVariable Long submissionId) {
        submissionService.withdraw(CurrentUser.id(), submissionId);
        return Result.success(null, "稿件已撤回");
    }
}
