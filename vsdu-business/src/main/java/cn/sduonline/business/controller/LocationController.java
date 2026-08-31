package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.LocationDetailVO;
import cn.sduonline.business.data.vo.LocationFavoriteVO;
import cn.sduonline.business.data.vo.MediaSummaryVO;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.service.LocationService;
import cn.sduonline.business.service.UserFavoriteTargetService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;
    private final UserFavoriteTargetService favoriteTargetService;

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

    /**
     * 收藏地点
     * 将指定地点加入当前用户的收藏列表。
     */
    @PostMapping("/{locationId}/favorites")
    public Result<LocationFavoriteVO> favorite(
            @PathVariable
            @Positive(message = "地点ID必须为正数")
            Long locationId
    ) {
        return Result.success(
                favoriteTargetService.favoriteLocation(CurrentUser.id(), locationId),
                "地点收藏成功"
        );
    }

    /**
     * 取消收藏地点
     * 移除当前用户对指定地点的收藏关系。
     */
    @DeleteMapping("/{locationId}/favorites")
    public Result<LocationFavoriteVO> unfavorite(
            @PathVariable
            @Positive(message = "地点ID必须为正数")
            Long locationId
    ) {
        return Result.success(
                favoriteTargetService.unfavoriteLocation(CurrentUser.id(), locationId),
                "已取消收藏地点"
        );
    }
}
