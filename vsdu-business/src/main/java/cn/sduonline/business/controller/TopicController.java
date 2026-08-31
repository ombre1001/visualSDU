package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.MediaSummaryVO;
import cn.sduonline.business.data.vo.TopicDetailVO;
import cn.sduonline.business.data.vo.TopicFavoriteVO;
import cn.sduonline.business.data.vo.TopicSummaryVO;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.service.TopicService;
import cn.sduonline.business.service.UserFavoriteTargetService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/topics")
public class TopicController {

    private final TopicService topicService;
    private final UserFavoriteTargetService favoriteTargetService;

    /**
     * 话题列表
     * 查询全部启用话题的摘要信息。
     */
    @PublicApi
    @GetMapping
    public Result<List<TopicSummaryVO>> list() {
        return Result.success(topicService.list());
    }

    /**
     * 话题详情
     * 查询指定启用话题的详情。
     */
    @PublicApi
    @GetMapping("/{topicId}")
    public Result<TopicDetailVO> detail(
            @PathVariable
            @Positive(message = "话题ID必须为正数")
            Long topicId
    ) {
        return Result.success(
                topicService.detail(topicId)
        );
    }

    /**
     * 话题媒体
     * 分页查询指定启用话题下的可见媒体。
     */
    @PublicApi
    @GetMapping("/{topicId}/media")
    public Result<PageResult<MediaSummaryVO>> media(
            @PathVariable
            @Positive(message = "话题ID必须为正数")
            Long topicId,
            @RequestParam(defaultValue = "1")
            @Positive(message = "页码必须为正数")
            long page,
            @RequestParam(defaultValue = "20")
            @Positive(message = "每页数量必须为正数")
            long size
    ) {
        return Result.success(
                topicService.media(topicId, page, size)
        );
    }

    /**
     * 收藏话题
     * 将指定话题加入当前用户的收藏列表。
     */
    @PostMapping("/{topicId}/favorites")
    public Result<TopicFavoriteVO> favorite(
            @PathVariable
            @Positive(message = "话题ID必须为正数")
            Long topicId
    ) {
        return Result.success(
                favoriteTargetService.favoriteTopic(CurrentUser.id(), topicId),
                "话题收藏成功"
        );
    }

    /**
     * 取消收藏话题
     * 移除当前用户对指定话题的收藏关系。
     */
    @DeleteMapping("/{topicId}/favorites")
    public Result<TopicFavoriteVO> unfavorite(
            @PathVariable
            @Positive(message = "话题ID必须为正数")
            Long topicId
    ) {
        return Result.success(
                favoriteTargetService.unfavoriteTopic(CurrentUser.id(), topicId),
                "已取消收藏话题"
        );
    }
}
