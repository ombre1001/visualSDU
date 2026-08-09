package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.CampusListVO;
import cn.sduonline.business.data.vo.CityVO;
import cn.sduonline.business.service.CampusService;
import cn.sduonline.business.service.CityService;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cities")
public class CityController {

    private final CityService cityService;
    private final CampusService campusService;

    /**
     * 城市列表。
     */
    @GetMapping
    public Result<List<CityVO>> listCities() {
        return Result.success(
                cityService.listCities(),
                "查询城市列表成功"
        );
    }

    /**
     * 城市下校区列表。
     */
    @GetMapping("/{cityId}/campuses")
    public Result<List<CampusListVO>> listCampuses(
            @PathVariable Long cityId
    ) {
        return Result.success(
                campusService.listByCityId(cityId),
                "查询城市下校区列表成功"
        );
    }
}