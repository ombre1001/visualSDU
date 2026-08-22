package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.CreateAnnouncementRequest;
import cn.sduonline.business.data.dto.UpdateAnnouncementRequest;
import cn.sduonline.business.data.dto.UpdateAnnouncementStatusRequest;
import cn.sduonline.business.data.enums.AnnouncementStatus;
import cn.sduonline.business.data.vo.AdminAnnouncementVO;
import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.service.AnnouncementService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/announcements")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 管理员公告列表
     * 按状态和关键词分页查询全部公告。
     */
    @AdminApi
    @GetMapping
    public Result<PageResult<AdminAnnouncementVO>> list(
            @RequestParam(required = false) AnnouncementStatus status,

            @RequestParam(required = false)
            @Size(max = 100, message = "搜索关键词不能超过100个字符")
            String keyword,

            @RequestParam(defaultValue = "1")
            @Positive(message = "页码必须为正数")
            @Max(value = 10000, message = "页码不能超过10000")
            long page,

            @RequestParam(defaultValue = "20")
            @Positive(message = "每页数量必须为正数")
            @Max(value = 50, message = "每页数量不能超过50")
            long size
    ) {
        return Result.success(
                announcementService.adminList(status, keyword, page, size)
        );
    }

    /**
     * 创建公告
     * 创建一条草稿公告，发布需另行调用状态切换接口。
     */
    @AdminApi
    @PostMapping
    public Result<AdminAnnouncementVO> create(
            @Valid @RequestBody CreateAnnouncementRequest request
    ) {
        return Result.success(
                announcementService.create(request),
                "公告创建成功"
        );
    }

    /**
     * 修改公告
     * 修改指定公告的标题、摘要、正文、置顶及排序信息。
     */
    @AdminApi
    @PatchMapping("/{announcementId}")
    public Result<AdminAnnouncementVO> update(
            @PathVariable
            @Positive(message = "公告ID必须为正数")
            Long announcementId,

            @Valid @RequestBody UpdateAnnouncementRequest request
    ) {
        return Result.success(
                announcementService.update(announcementId, request),
                "公告修改成功"
        );
    }

    /**
     * 发布或下线公告
     * 将指定公告切换为已发布或已下线状态。
     */
    @AdminApi
    @PostMapping("/{announcementId}/status")
    public Result<AdminAnnouncementVO> changeStatus(
            @PathVariable
            @Positive(message = "公告ID必须为正数")
            Long announcementId,

            @Valid @RequestBody UpdateAnnouncementStatusRequest request
    ) {
        AdminAnnouncementVO result = announcementService.changeStatus(
                announcementId,
                request.status()
        );
        String message = result.status() == AnnouncementStatus.PUBLISHED
                ? "公告发布成功"
                : "公告下线成功";
        return Result.success(result, message);
    }
}
