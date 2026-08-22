package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.LocationDetailVO;
import cn.sduonline.business.data.vo.MediaSummaryVO;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.service.LocationService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    /**
     * 地点详情
     */
    @PublicApi
    @GetMapping("/{locationId}")
    public Result<LocationDetailVO> getLocationDetail(
            @PathVariable
            @Positive(message = "地点ID必须为正数")
            Long locationId
    ) {
        return Result.success(
                locationService.getDetail(locationId),
                "查询地点详情成功"
        );
    }

    /**
     * 地点下的媒体资源
     */
    @PublicApi
    @GetMapping("/{locationId}/media")
    public Result<PageResult<MediaSummaryVO>> media(
            @PathVariable
            @Positive(message = "地点ID必须为正数")
            Long locationId,
            @RequestParam(defaultValue = "1")
            @Positive(message = "页码必须为正数")
            long page,
            @RequestParam(defaultValue = "20")
            @Positive(message = "每页数量必须为正数")
            long size
    ) {
        return Result.success(
                locationService.media(locationId, page, size),
                "查询地点媒体成功"
        );
    }
}
