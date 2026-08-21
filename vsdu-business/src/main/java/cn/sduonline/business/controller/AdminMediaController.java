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

    @AdminApi @GetMapping
    public Result<PageResult<AdminMediaVO>> list(
            @RequestParam(required = false) @Size(max = 50) String keyword,
            @RequestParam(required = false) @Positive Long locationId,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status,
            @RequestParam(defaultValue = "1") @Positive long page,
            @RequestParam(defaultValue = "20") @Positive long size) {
        return Result.success(service.list(keyword, locationId, status, page, size));
    }

    @AdminApi @PatchMapping("/{mediaId}/classification")
    public Result<AdminMediaVO> classify(@PathVariable @Positive Long mediaId,
                                         @Valid @RequestBody AdminMediaClassificationRequest request) {
        return Result.success(service.classify(mediaId, request), "媒体分类更新成功");
    }

    @AdminApi @PostMapping("/{mediaId}/hide")
    public Result<AdminMediaVO> hide(@PathVariable @Positive Long mediaId) {
        return Result.success(service.hide(mediaId), "媒体已隐藏");
    }

    @AdminApi @PostMapping("/{mediaId}/restore")
    public Result<AdminMediaVO> restore(@PathVariable @Positive Long mediaId) {
        return Result.success(service.restore(mediaId), "媒体已恢复");
    }

    @AdminApi
    @DeleteMapping("/{mediaId}")
    public Result<Void> delete(@PathVariable @Positive Long mediaId) {
        service.delete(mediaId);
        return Result.success(null, "媒体及其存储文件已删除");
    }
}
