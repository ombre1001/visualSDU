package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.CreateFavoriteFolderRequest;
import cn.sduonline.business.data.dto.UpdateFavoriteFolderRequest;
import cn.sduonline.business.data.vo.FavoriteFolderVO;
import cn.sduonline.business.data.vo.MediaSummaryVO;
import cn.sduonline.business.data.vo.PageResult;
import cn.sduonline.business.service.FavoriteFolderService;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/favorite-folders")
public class FavoriteFolderController {

    private final FavoriteFolderService favoriteFolderService;

    /**
     * 查询当前用户的收藏夹。
     *
     * 不加@PublicApi，因此必须登录。
     */
    @GetMapping
    public Result<List<FavoriteFolderVO>> listFolders() {
        return Result.success(
                favoriteFolderService.listFolders()
        );
    }

    /**
     * 创建收藏夹。
     */
    @PostMapping
    public Result<FavoriteFolderVO> createFolder(
            @Valid @RequestBody CreateFavoriteFolderRequest request
    ) {
        return Result.success(
                favoriteFolderService.createFolder(request),
                "收藏夹创建成功"
        );
    }

    /**
     * 修改收藏夹。
     */
    @PatchMapping("/{folderId}")
    public Result<FavoriteFolderVO> updateFolder(
            @PathVariable
            @Positive(message = "收藏夹ID必须为正数")
            Long folderId,

            @Valid
            @RequestBody
            UpdateFavoriteFolderRequest request
    ) {
        return Result.success(
                favoriteFolderService.updateFolder(folderId, request),
                "收藏夹修改成功"
        );
    }

    /**
     * 删除收藏夹。
     */
    @DeleteMapping("/{folderId}")
    public Result<Void> deleteFolder(
            @PathVariable
            @Positive(message = "收藏夹ID必须为正数")
            Long folderId
    ) {
        favoriteFolderService.deleteFolder(folderId);
        return Result.ok();
    }

    /**
     * 分页查询收藏夹内容。
     */
    @GetMapping("/{folderId}/items")
    public Result<PageResult<MediaSummaryVO>> listFolderItems(
            @PathVariable
            @Positive(message = "收藏夹ID必须为正数")
            Long folderId,

            @RequestParam(defaultValue = "1")
            @Positive(message = "页码必须为正数")
            long page,

            @RequestParam(defaultValue = "20")
            @Positive(message = "每页数量必须为正数")
            long size
    ) {
        return Result.success(
                favoriteFolderService.listFolderItems(
                        folderId,
                        page,
                        size
                )
        );
    }
}