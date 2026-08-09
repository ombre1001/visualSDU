package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.LocationDetailVO;
import cn.sduonline.business.service.LocationService;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    /**
     * 地点详情。
     */
    @GetMapping("/{locationId}")
    public Result<LocationDetailVO> getLocationDetail(
            @PathVariable Long locationId
    ) {
        return Result.success(
                locationService.getDetail(locationId),
                "查询地点详情成功"
        );
    }
}