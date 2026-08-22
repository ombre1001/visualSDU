package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.AdminMediaClassificationRequest;
import cn.sduonline.business.data.vo.AdminMediaVO;
import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.service.AdminMediaService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequiredArgsConstructor @RequestMapping("/admin/media")
public class AdminMediaController {
    private final AdminMediaService service;

    /**
     * 管理员媒体列表
     * 按关键词、地点和状态分页筛选媒体。
     */
    @AdminApi @GetMapping
    public Result<PageResult<AdminMediaVO>> list(
            @RequestParam(required = false) @Size(max = 50) String keyword,
            @RequestParam(required = false) @Positive Long locationId,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status,
            @RequestParam(defaultValue = "1") @Positive long page,
            @RequestParam(defaultValue = "20") @Positive long size) {
        return Result.success(service.list(keyword, locationId, status, page, size));
    }

    /**
     * 修改媒体分类
     * 更新指定媒体所属的地点和标签。
     */
    @AdminApi @PatchMapping("/{mediaId}/classification")
    public Result<AdminMediaVO> classify(@PathVariable @Positive Long mediaId,
                                         @Valid @RequestBody AdminMediaClassificationRequest request) {
        return Result.success(service.classify(mediaId, request), "媒体分类更新成功");
    }

    /**
     * 隐藏媒体
     * 将指定媒体设为隐藏，使其不再出现在公开查询中。
     */
    @AdminApi @PostMapping("/{mediaId}/hide")
    public Result<AdminMediaVO> hide(@PathVariable @Positive Long mediaId) {
        return Result.success(service.hide(mediaId), "媒体已隐藏");
    }

    /**
     * 恢复媒体
     * 将已隐藏的媒体恢复为公开可见状态。
     */
    @AdminApi @PostMapping("/{mediaId}/restore")
    public Result<AdminMediaVO> restore(@PathVariable @Positive Long mediaId) {
        return Result.success(service.restore(mediaId), "媒体已恢复");
    }

    /**
     * 永久删除媒体
     * 删除媒体记录及其关联的存储文件，此操作不可恢复。
     */
    @AdminApi
    @DeleteMapping("/{mediaId}")
    public Result<Void> delete(@PathVariable @Positive Long mediaId) {
        service.delete(mediaId);
        return Result.success(null, "媒体及其存储文件已删除");
    }
}
