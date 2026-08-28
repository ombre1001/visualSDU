package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.LocationCategoryOptionVO;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.service.LocationCategoryService;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/location-categories")
public class LocationCategoryController {

    private final LocationCategoryService locationCategoryService;

    @PublicApi
    @GetMapping
    public Result<List<LocationCategoryOptionVO>> list() {
        return Result.success(
                locationCategoryService.listEnabledOptions(),
                "查询地点分类列表成功"
        );
    }
}
