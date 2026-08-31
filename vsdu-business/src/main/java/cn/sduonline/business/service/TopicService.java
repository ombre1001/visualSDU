package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Topic;
import cn.sduonline.business.data.projection.TopicSummaryRow;
import cn.sduonline.business.data.vo.MediaSummaryVO;
import cn.sduonline.business.data.vo.TopicDetailVO;
import cn.sduonline.business.data.vo.TopicSummaryVO;
import cn.sduonline.business.mapper.TopicMapper;
import cn.sduonline.business.mapper.TopicMediaMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private static final int ENABLED = 1;

    private final TopicMapper topicMapper;
    private final TopicMediaMapper topicMediaMapper;
    private final MediaService mediaService;
    private final UserFavoriteTargetService favoriteTargetService;

    public List<TopicSummaryVO> list() {
        Long userId = favoriteTargetService.currentFormalUserIdOrNull();
        return topicMapper.selectEnabledSummaries()
                .stream()
                .map(topic -> toSummary(topic, userId))
                .toList();
    }

    public TopicDetailVO detail(Long topicId) {
        Topic topic = requireEnabled(topicId);
        long mediaCount =
                topicMediaMapper.countVisibleMedia(topic.getId());
        Long userId = favoriteTargetService.currentFormalUserIdOrNull();

        return new TopicDetailVO(
                topic.getId(),
                topic.getName(),
                topic.getSlug(),
                topic.getDescription(),
                topic.getCoverUrl(),
                mediaCount,
                favoriteTargetService.countTopicFavorites(topic.getId()),
                favoriteTargetService.isTopicFavorited(userId, topic.getId()),
                topic.getCreatedAt(),
                topic.getUpdatedAt()
        );
    }

    public PageResult<MediaSummaryVO> media(
            Long topicId,
            long page,
            long size
    ) {
        Topic topic = requireEnabled(topicId);

        long safePage = Math.max(page, 1);
        long safeSize = Math.clamp(size, 1, 50);
        long offset = (safePage - 1) * safeSize;

        long total =
                topicMediaMapper.countVisibleMedia(topic.getId());

        if (total == 0) {
            return new PageResult<>(
                    0,
                    safePage,
                    safeSize,
                    List.of()
            );
        }

        List<MediaSummaryVO> items =
                topicMediaMapper.selectVisibleMedia(
                                topic.getId(),
                                offset,
                                safeSize
                        )
                        .stream()
                        .map(mediaService::toSummary)
                        .toList();

        return new PageResult<>(
                total,
                safePage,
                safeSize,
                items
        );
    }

    private TopicSummaryVO toSummary(TopicSummaryRow topic, Long userId) {
        return new TopicSummaryVO(
                topic.getId(), topic.getName(), topic.getSlug(), topic.getDescription(),
                topic.getCoverUrl(), topic.getMediaCount(),
                favoriteTargetService.countTopicFavorites(topic.getId()),
                favoriteTargetService.isTopicFavorited(userId, topic.getId())
        );
    }

    private Topic requireEnabled(Long topicId) {
        if (topicId == null || topicId <= 0) {
            throw new BizException(BizCode.TOPIC_NOT_FOUND);
        }

        Topic topic = topicMapper.selectOne(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getId, topicId)
                        .eq(Topic::getStatus, ENABLED)
        );

        if (topic == null) {
            throw new BizException(BizCode.TOPIC_NOT_FOUND);
        }

        return topic;
    }
}
