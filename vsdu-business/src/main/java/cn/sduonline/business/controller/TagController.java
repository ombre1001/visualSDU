package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.TagVO;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.service.TagService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/tags")
public class TagController {
    private final TagService tagService;

    @PublicApi
    @GetMapping
    public Result<PageResult<TagVO>> list(
            @RequestParam(required = false) @Size(max = 32) String keyword,
            @RequestParam(defaultValue = "name") @Size(max = 16) String sort,
            @RequestParam(defaultValue = "1") @Min(1) @Max(10000) long page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) long size
    ) {
        return Result.success(tagService.list(keyword, sort, page, size));
    }

    @PublicApi
    @GetMapping("/lookup")
    public Result<List<TagVO>> lookup(
            @RequestParam
            @NotEmpty(message = "标签ID不能为空")
            @Size(max = 20, message = "一次最多查询20个标签")
            List<@Positive(message = "标签ID必须为正数") Long> ids
    ) {
        return Result.success(tagService.lookup(ids));
    }
}
