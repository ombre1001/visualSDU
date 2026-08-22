package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.UpdateSubmissionReviewSettingRequest;
import cn.sduonline.business.data.vo.SubmissionReviewSettingVO;
import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.service.SubmissionReviewSettingService;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/settings/submission-review")
public class AdminSubmissionReviewSettingController {
    private final SubmissionReviewSettingService settingService;

    /**
     * 更新投稿审核开关
     * 设置新投稿是否需要管理员审核后才能发布。
     */
    @AdminApi
    @PutMapping
    public Result<SubmissionReviewSettingVO> update(
            @Valid @RequestBody UpdateSubmissionReviewSettingRequest request
    ) {
        SubmissionReviewSettingVO result = settingService.update(request);
        String message = result.reviewEnabled() ? "稿件审核已开启" : "稿件审核已关闭";
        return Result.success(result, message);
    }
}
