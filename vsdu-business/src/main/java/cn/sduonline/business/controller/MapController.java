package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.MapMarkerQueryDTO;
import cn.sduonline.business.data.vo.MapMarkerVO;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.service.MapService;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/map")
public class MapController {

    private final MapService mapService;

    /**
     * 地图点位。
     * <p>
     * 查询城市下校区：
     * GET /api/v1/map/markers?cityId=1
     * <p>
     * 查询校区下地点：
     * GET /api/v1/map/markers?campusId=1
     */
    @PublicApi
    @GetMapping("/markers")
    public Result<List<MapMarkerVO>> listMarkers(
            @ModelAttribute MapMarkerQueryDTO queryDTO
    ) {
        return Result.success(
                mapService.listMarkers(queryDTO),
                "查询地图点位成功"
        );
    }
}