package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.AnnouncementDetailVO;
import cn.sduonline.business.data.vo.AnnouncementSummaryVO;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.service.AnnouncementService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 公告列表
     * 分页查询当前有效且已发布的公告摘要。
     */
    @PublicApi
    @GetMapping
    public Result<PageResult<AnnouncementSummaryVO>> list(
            @RequestParam(defaultValue = "1")
            @Positive(message = "页码必须为正数")
            @Max(value = 10000, message = "页码不能超过10000")
            long page,

            @RequestParam(defaultValue = "20")
            @Positive(message = "每页数量必须为正数")
            @Max(value = 50, message = "每页数量不能超过50")
            long size
    ) {
        return Result.success(announcementService.listPublished(page, size));
    }

    /**
     * 公告详情
     * 查询当前有效且已发布的指定公告。
     */
    @PublicApi
    @GetMapping("/{announcementId}")
    public Result<AnnouncementDetailVO> detail(
            @PathVariable
            @Positive(message = "公告ID必须为正数")
            Long announcementId
    ) {
        return Result.success(announcementService.publishedDetail(announcementId));
    }
}
