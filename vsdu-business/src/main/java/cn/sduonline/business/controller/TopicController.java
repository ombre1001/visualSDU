package cn.sduonline.business.controller;

import cn.sduonline.business.data.vo.MediaSummaryVO;
import cn.sduonline.business.data.vo.TopicDetailVO;
import cn.sduonline.business.data.vo.TopicSummaryVO;
import cn.sduonline.business.security.anno.PublicApi;
import cn.sduonline.business.service.TopicService;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/topics")
public class TopicController {

    private final TopicService topicService;

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
            @PathVariable Long topicId
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
            @PathVariable Long topicId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return Result.success(
                topicService.media(topicId, page, size)
        );
    }
}
