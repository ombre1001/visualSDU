package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.BatchFavoriteRequest;
import cn.sduonline.business.data.vo.BatchFavoriteResultVO;
import cn.sduonline.business.service.FavoriteFolderService;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/favorites")
public class FavoriteController {

    private final FavoriteFolderService favoriteFolderService;

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