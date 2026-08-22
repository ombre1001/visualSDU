package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.AdminCreateLocationRequest;
import cn.sduonline.business.data.dto.AdminUpdateLocationRequest;
import cn.sduonline.business.data.vo.AdminLocationVO;
import cn.sduonline.business.security.anno.AdminApi;
import cn.sduonline.business.service.AdminLocationService;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/locations")
public class AdminLocationController {
    private final AdminLocationService service;

    /**
     * 创建地点
     * 在指定校区下创建地点并配置其基础信息。
     */
    @AdminApi
    @PostMapping
    public Result<AdminLocationVO> create(@Valid @RequestBody AdminCreateLocationRequest request) {
        return Result.success(service.create(request), "地点创建成功");
    }

    /**
     * 修改地点
     * 修改指定地点的基础信息或启用状态。
     */
    @AdminApi
    @PatchMapping("/{locationId}")
    public Result<AdminLocationVO> update(@PathVariable @Positive Long locationId,
                                          @Valid @RequestBody AdminUpdateLocationRequest request) {
        return Result.success(service.update(locationId, request), "地点修改成功");
    }
}
