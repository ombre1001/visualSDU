package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.AdminCreateTimeComparisonRequest;
import cn.sduonline.business.data.vo.AdminTimeComparisonVO;
import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.service.AdminTimeComparisonService;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/time-comparisons")
public class AdminTimeComparisonController {
    private final AdminTimeComparisonService service;

    /**
     * 创建时光对比
     * 创建一组同一地点不同时间的媒体对比项。
     */
    @AdminApi
    @PostMapping
    public Result<AdminTimeComparisonVO> create(@Valid @RequestBody AdminCreateTimeComparisonRequest request) {
        return Result.success(service.create(request), "时光对比创建成功");
    }
}
