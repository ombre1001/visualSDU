package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.AdminCreateTopicRequest;
import cn.sduonline.business.data.dto.AdminUpdateTopicRequest;
import cn.sduonline.business.data.po.Topic;
import cn.sduonline.business.data.vo.AdminTopicVO;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.TopicMapper;
import cn.sduonline.business.mapper.TopicMediaMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminTopicService {
    private static final int ENABLED = 1;
    private final TopicMapper topicMapper;
    private final TopicMediaMapper topicMediaMapper;
    private final MediaMapper mediaMapper;

    @Transactional
    public AdminTopicVO create(AdminCreateTopicRequest r) {
        String slug = r.slug().strip(); requireUniqueSlug(slug, null);
        LocalDateTime now = LocalDateTime.now();
        Topic topic = new Topic();
        topic.setName(r.name().strip()); topic.setSlug(slug); topic.setDescription(nullable(r.description()));
        topic.setCoverUrl(nullable(r.coverUrl())); topic.setStatus(Objects.requireNonNullElse(r.status(), ENABLED));
        topic.setSortOrder(Objects.requireNonNullElse(r.sortOrder(), 0)); topic.setCreatedAt(now); topic.setUpdatedAt(now);
        topicMapper.insert(topic);
        replaceMedia(topic.getId(), Objects.requireNonNullElse(r.mediaIds(), List.of()));
        return toVO(topic);
    }

    @Transactional
    public AdminTopicVO update(Long id, AdminUpdateTopicRequest r) {
        Topic topic = topicMapper.selectById(id);
        if (topic == null) throw new BizException(BizCode.ADMIN_TOPIC_NOT_FOUND);
        if (r.name() == null && r.slug() == null && r.description() == null && r.coverUrl() == null
                && r.status() == null && r.sortOrder() == null && r.mediaIds() == null) {
            throw new BizException(BizCode.ADMIN_TOPIC_UPDATE_EMPTY);
        }
        if (r.name() != null) { topic.setName(required(r.name(), "专题名称不能为空")); }
        if (r.slug() != null) { String v = r.slug().strip(); requireUniqueSlug(v, id); topic.setSlug(v); }
        if (r.description() != null) { topic.setDescription(nullable(r.description())); }
        if (r.coverUrl() != null) { topic.setCoverUrl(nullable(r.coverUrl())); }
        if (r.status() != null) { topic.setStatus(r.status()); }
        if (r.sortOrder() != null) { topic.setSortOrder(r.sortOrder()); }
        LocalDateTime now = LocalDateTime.now(); topic.setUpdatedAt(now);
        topicMapper.updatePartial(id, r, topic, now);
        if (r.mediaIds() != null) replaceMedia(id, r.mediaIds());
        return toVO(topic);
    }

    private void replaceMedia(Long topicId, List<Long> mediaIds) {
        if (new HashSet<>(mediaIds).size() != mediaIds.size()) throw new BizException(BizCode.BAD_REQUEST, "媒体ID不能重复");
        if (!mediaIds.isEmpty() && mediaMapper.selectBatchIds(mediaIds).size() != mediaIds.size()) {
            throw new BizException(BizCode.ADMIN_MEDIA_NOT_FOUND);
        }
        topicMediaMapper.deleteByTopic(topicId);
        for (int i = 0; i < mediaIds.size(); i++) topicMediaMapper.upsertRelation(topicId, mediaIds.get(i), i);
    }

    private void requireUniqueSlug(String slug, Long excludedId) {
        Long count = topicMapper.selectCount(new LambdaQueryWrapper<Topic>()
                .eq(Topic::getSlug, slug).ne(excludedId != null, Topic::getId, excludedId));
        if (count != null && count > 0) throw new BizException(BizCode.ADMIN_TOPIC_SLUG_EXISTS);
    }

    private AdminTopicVO toVO(Topic t) {
        return new AdminTopicVO(t.getId(), t.getName(), t.getSlug(), t.getDescription(), t.getCoverUrl(),
                t.getStatus(), t.getSortOrder(), topicMediaMapper.selectAllMediaIds(t.getId()), t.getCreatedAt(), t.getUpdatedAt());
    }

    private String required(String v, String message) {
        String result = nullable(v); if (result == null) throw new BizException(BizCode.BAD_REQUEST, message); return result;
    }
    private String nullable(String v) { return v == null || v.isBlank() ? null : v.strip(); }
}
