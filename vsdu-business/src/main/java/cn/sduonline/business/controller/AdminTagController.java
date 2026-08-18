package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.AdminCreateTagRequest;
import cn.sduonline.business.data.dto.AdminMergeTagRequest;
import cn.sduonline.business.data.dto.AdminUpdateTagRequest;
import cn.sduonline.business.data.vo.AdminTagVO;
import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.service.AdminTagService;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/tags")
public class AdminTagController {
    private final AdminTagService service;

    @AdminApi
    @GetMapping
    public Result<List<AdminTagVO>> list(@RequestParam(required = false) @Size(max = 32) String keyword) {
        return Result.success(service.list(keyword));
    }

    @AdminApi
    @PostMapping
    public Result<AdminTagVO> create(@Valid @RequestBody AdminCreateTagRequest request) {
        return Result.success(service.create(request.name()), "标签创建成功");
    }

    @AdminApi
    @PatchMapping("/{tagId}")
    public Result<AdminTagVO> update(@PathVariable @Positive Long tagId,
                                     @Valid @RequestBody AdminUpdateTagRequest request) {
        return Result.success(service.update(tagId, request.name()), "标签修改成功");
    }

    @AdminApi
    @PostMapping("/{tagId}/merge")
    public Result<Void> merge(@PathVariable @Positive Long tagId,
                              @Valid @RequestBody AdminMergeTagRequest request) {
        service.mergeOrDelete(tagId, request.targetTagId());
        return Result.success(null, request.targetTagId() == null ? "标签已删除" : "标签合并成功");
    }
}
