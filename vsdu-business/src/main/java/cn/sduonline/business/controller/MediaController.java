package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.FavoriteMediaRequest;
import cn.sduonline.business.data.vo.MediaDetailVO;
import cn.sduonline.business.data.vo.MediaDownloadVO;
import cn.sduonline.business.data.vo.MediaInteractionVO;
import cn.sduonline.business.data.vo.MediaSummaryVO;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.service.MediaService;
import cn.sduonline.common.result.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
public class MediaController {
    private final MediaService mediaService;

    @PublicApi
    @GetMapping("/{mediaId}")
    public Result<MediaDetailVO> detail(@PathVariable Long mediaId) {
        return Result.success(mediaService.detail(mediaId, optionalUserId()));
    }

    @PublicApi
    @PostMapping("/{mediaId}/views")
    public Result<MediaInteractionVO> recordView(@PathVariable Long mediaId) {
        return Result.success(mediaService.recordView(mediaId, optionalUserId()));
    }

    @PostMapping("/{mediaId}/likes")
    public Result<MediaInteractionVO> like(@PathVariable Long mediaId) {
        return Result.success(mediaService.like(CurrentUser.id(), mediaId), "点赞成功");
    }

    @DeleteMapping("/{mediaId}/likes")
    public Result<MediaInteractionVO> unlike(@PathVariable Long mediaId) {
        return Result.success(mediaService.unlike(CurrentUser.id(), mediaId), "已取消点赞");
    }

    @PostMapping("/{mediaId}/favorites")
    public Result<MediaInteractionVO> favorite(
            @PathVariable Long mediaId,
            @Valid @RequestBody(required = false) FavoriteMediaRequest request
    ) {
        Long folderId = request == null ? null : request.folderId();
        return Result.success(mediaService.favorite(CurrentUser.id(), mediaId, folderId), "收藏成功");
    }

    @DeleteMapping("/{mediaId}/favorites")
    public Result<MediaInteractionVO> unfavorite(@PathVariable Long mediaId) {
        return Result.success(mediaService.unfavorite(CurrentUser.id(), mediaId), "已取消收藏");
    }

    @PostMapping("/{mediaId}/downloads")
    public Result<MediaDownloadVO> requestDownload(@PathVariable Long mediaId) {
        return Result.success(mediaService.requestDownload(CurrentUser.id(), mediaId), "下载地址已生成");
    }

    @PublicApi
    @GetMapping("/{mediaId}/related")
    public Result<List<MediaSummaryVO>> related(
            @PathVariable Long mediaId,
            @RequestParam(defaultValue = "12") int size
    ) {
        return Result.success(mediaService.related(mediaId, size));
    }

    private Long optionalUserId() {
        return CurrentUser.isLogin() ? CurrentUser.id() : null;
    }
}
