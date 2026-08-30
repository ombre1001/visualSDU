package cn.sduonline.business.controller;

import cn.sduonline.business.data.dto.FavoriteMediaRequest;
import cn.sduonline.business.data.dto.StreamDownloadFile;
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
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/media")
public class MediaController {
    private final MediaService mediaService;

    /**
     * 媒体详情
     * 查询可见媒体的详情；携带有效令牌时同时返回当前用户的点赞和收藏状态。
     */
    @PublicApi
    @GetMapping("/{mediaId}")
    public Result<MediaDetailVO> detail(@PathVariable Long mediaId) {
        return Result.success(mediaService.detail(mediaId, optionalUserId()));
    }

    /**
     * 记录一次浏览
     * 增加媒体浏览量，登录用户还会新增或更新浏览足迹；前端应避免重复调用造成重复计数。
     */
    @PublicApi
    @PostMapping("/{mediaId}/views")
    public Result<MediaInteractionVO> recordView(@PathVariable Long mediaId) {
        return Result.success(mediaService.recordView(mediaId, optionalUserId()));
    }

    /**
     * 点赞
     * 为当前用户点赞指定媒体并返回最新互动状态。
     */
    @PostMapping("/{mediaId}/likes")
    public Result<MediaInteractionVO> like(@PathVariable Long mediaId) {
        return Result.success(mediaService.like(CurrentUser.id(), mediaId), "点赞成功");
    }

    /**
     * 取消点赞
     * 取消当前用户对指定媒体的点赞并返回最新互动状态。
     */
    @DeleteMapping("/{mediaId}/likes")
    public Result<MediaInteractionVO> unlike(@PathVariable Long mediaId) {
        return Result.success(mediaService.unlike(CurrentUser.id(), mediaId), "已取消点赞");
    }

    /**
     * 收藏
     * 将媒体加入指定收藏夹；不传请求体或收藏夹 ID 时使用默认收藏夹。
     */
    @PostMapping("/{mediaId}/favorites")
    public Result<MediaInteractionVO> favorite(
            @PathVariable Long mediaId,
            @Valid @RequestBody(required = false) FavoriteMediaRequest request
    ) {
        Long folderId = request == null ? null : request.folderId();
        return Result.success(mediaService.favorite(CurrentUser.id(), mediaId, folderId), "收藏成功");
    }

    /**
     * 取消收藏
     * 删除当前用户对指定媒体的全部收藏关系，而非仅从某个收藏夹移除。
     */
    @DeleteMapping("/{mediaId}/favorites")
    public Result<MediaInteractionVO> unfavorite(@PathVariable Long mediaId) {
        return Result.success(mediaService.unfavorite(CurrentUser.id(), mediaId), "已取消收藏");
    }

    /**
     * 请求原图下载
     * 为允许下载的媒体生成短期有效的原图下载地址，前端不应长期缓存该地址。
     */
    @PostMapping("/{mediaId}/downloads")
    public Result<MediaDownloadVO> requestDownload(@PathVariable Long mediaId) {
        return Result.success(mediaService.requestDownload(CurrentUser.id(), mediaId), "下载地址已生成");
    }

    @PostMapping("/{mediaId}/downloads/ticket")
    public Result<String> mediaDownloadTicket(@PathVariable Long mediaId) {
        return Result.success(mediaService.getMediaDownloadTicket(mediaId));
    }

    @PublicApi
    @GetMapping("/downloads/stream")
    public ResponseEntity<StreamingResponseBody> downloadStream(@RequestParam(name = "ticket") String ticket) {
        StreamDownloadFile downloadFile = mediaService.streamDownloadFile(ticket);

        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = downloadFile.inputStream()) {
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloadFile.contentType()))
                .contentLength(downloadFile.size())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + downloadFile.encodedFilename()
                )
                .cacheControl(CacheControl.noStore())
                .body(body);
    }

    /**
     * 相关媒体
     * 按地点和标签相关性查询指定媒体的相关推荐。
     */
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
