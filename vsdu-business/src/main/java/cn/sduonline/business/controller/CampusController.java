package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.CampusDetailVO;
import cn.sduonline.business.data.vo.LocationListVO;
import cn.sduonline.business.service.CampusService;
import cn.sduonline.business.service.LocationService;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/campuses")
public class CampusController {

    private final CampusService campusService;
    private final LocationService locationService;

    /**
     * 校区详情。
     */
    @GetMapping("/{campusId}")
    public Result<CampusDetailVO> getCampusDetail(
            @PathVariable Long campusId
    ) {
        return Result.success(
                campusService.getDetail(campusId),
                "查询校区详情成功"
        );
    }

    /**
     * 校区地点列表。
     * <p>
     * categoryCode可以不传。
     */
    @GetMapping("/{campusId}/locations")
    public Result<List<LocationListVO>> listLocations(
            @PathVariable Long campusId,
            @RequestParam(required = false) String categoryCode
    ) {
        return Result.success(
                locationService.listByCampusId(
                        campusId,
                        categoryCode
                ),
                "查询校区地点列表成功"
        );
    }
}