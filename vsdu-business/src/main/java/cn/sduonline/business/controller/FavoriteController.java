package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.BatchFavoriteRequest;
import cn.sduonline.business.data.vo.BatchFavoriteResultVO;
import cn.sduonline.business.data.vo.LocationListVO;
import cn.sduonline.business.data.vo.TopicSummaryVO;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.service.FavoriteFolderService;
import cn.sduonline.business.service.UserFavoriteTargetService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/favorites")
public class FavoriteController {

    private final FavoriteFolderService favoriteFolderService;
    private final UserFavoriteTargetService favoriteTargetService;

    /**
     * 分页查询当前用户收藏的地点
     * 按收藏时间倒序分页返回当前用户收藏的可用地点。
     */
    @GetMapping("/locations")
    public Result<PageResult<LocationListVO>> locations(
            @RequestParam(defaultValue = "1")
            @Positive(message = "页码必须为正数")
            long page,
            @RequestParam(defaultValue = "20")
            @Positive(message = "每页数量必须为正数")
            long size
    ) {
        return Result.success(
                favoriteTargetService.listFavoriteLocations(CurrentUser.id(), page, size)
        );
    }

    /**
     * 分页查询当前用户收藏的话题
     * 按收藏时间倒序分页返回当前用户收藏的可用话题。
     */
    @GetMapping("/topics")
    public Result<PageResult<TopicSummaryVO>> topics(
            @RequestParam(defaultValue = "1")
            @Positive(message = "页码必须为正数")
            long page,
            @RequestParam(defaultValue = "20")
            @Positive(message = "每页数量必须为正数")
            long size
    ) {
        return Result.success(
                favoriteTargetService.listFavoriteTopics(CurrentUser.id(), page, size)
        );
    }

    /**
     * 批量管理收藏
     * <p>
     * 不加@PublicApi，因此必须登录。
     */
    @PostMapping("/batch")
    public Result<BatchFavoriteResultVO> batch(
            @Valid @RequestBody BatchFavoriteRequest request
    ) {
        return Result.success(
                favoriteFolderService.batch(request),
                "批量收藏操作完成"
        );
    }
}
