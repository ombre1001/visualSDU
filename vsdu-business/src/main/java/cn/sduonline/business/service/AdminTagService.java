package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.po.Tag;
import cn.sduonline.business.data.projection.MediaTagPatch;
import cn.sduonline.business.data.vo.AdminTagVO;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.TagMapper;
import cn.sduonline.business.util.TagCodec;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminTagService {
    private final TagMapper tagMapper;
    private final MediaMapper mediaMapper;

    public List<AdminTagVO> list(String keyword) {
        String q = text(keyword) ? keyword.strip() : null;
        return tagMapper.selectAdminTagStats(q).stream()
                .map(row -> new AdminTagVO(
                        row.getId(), row.getName(), row.getMediaCount(),
                        row.getCreatedAt(), row.getUpdatedAt()
                ))
                .toList();
    }

    @Transactional
    public AdminTagVO create(String rawName) {
        String name = normalizeName(rawName);
        requireUnique(name, null);
        LocalDateTime now = LocalDateTime.now();
        Tag tag = new Tag();
        tag.setName(name); tag.setCreatedAt(now); tag.setUpdatedAt(now);
        tagMapper.insert(tag);
        return toVO(tag, 0);
    }

    @Transactional
    public AdminTagVO update(Long tagId, String rawName) {
        Tag tag = requireTag(tagId);
        String name = normalizeName(rawName);
        if (Objects.equals(tag.getName(), name)) return toVO(tag, countMedia(loadMedia(tag.getName()), tag.getName()));
        requireUnique(name, tagId);
        replaceMediaTag(tag.getName(), name);
        tag.setName(name); tag.setUpdatedAt(LocalDateTime.now());
        tagMapper.updateById(tag);
        return toVO(tag, countMedia(loadMedia(name), name));
    }

    @Transactional
    public void mergeOrDelete(Long sourceTagId, Long targetTagId) {
        Tag source = requireTag(sourceTagId);
        String targetName = null;
        if (targetTagId != null) {
            if (Objects.equals(sourceTagId, targetTagId)) throw new BizException(BizCode.ADMIN_TAG_MERGE_SELF);
            targetName = requireTag(targetTagId).getName();
        }
        replaceMediaTag(source.getName(), targetName);
        tagMapper.deleteById(sourceTagId);
    }

    private void replaceMediaTag(String source, String target) {
        List<MediaTagPatch> patches = new ArrayList<>();
        for (Media media : loadMedia(source)) {
            List<String> oldTags = TagCodec.decode(media.getTags());
            if (!oldTags.contains(source)) continue;
            List<String> updated = new ArrayList<>();
            for (String tag : oldTags) {
                String value = Objects.equals(tag, source) ? target : tag;
                if (text(value) && !updated.contains(value)) updated.add(value);
            }
            String encoded = TagCodec.encode(updated);
            patches.add(new MediaTagPatch(media.getId(), encoded));
        }
        if (!patches.isEmpty()) mediaMapper.batchUpdateTags(patches, LocalDateTime.now());
    }

    private List<Media> loadMedia(String tagName) {
        return mediaMapper.selectByExactTag(tagName);
    }

    private long countMedia(List<Media> media, String name) {
        return media.stream().filter(item -> TagCodec.decode(item.getTags()).contains(name)).count();
    }

    private Tag requireTag(Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) throw new BizException(BizCode.ADMIN_TAG_NOT_FOUND);
        return tag;
    }

    private void requireUnique(String name, Long excludedId) {
        Long count = tagMapper.selectCount(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getName, name).ne(excludedId != null, Tag::getId, excludedId));
        if (count != null && count > 0) throw new BizException(BizCode.ADMIN_TAG_NAME_EXISTS);
    }

    private String normalizeName(String value) {
        String name = value == null ? null : value.strip().replace("|", "");
        if (!text(name)) throw new BizException(BizCode.BAD_REQUEST, "标签名称不能为空");
        return name;
    }

    private AdminTagVO toVO(Tag tag, long count) {
        return new AdminTagVO(tag.getId(), tag.getName(), count, tag.getCreatedAt(), tag.getUpdatedAt());
    }

    private boolean text(String value) { return value != null && !value.isBlank(); }
}
