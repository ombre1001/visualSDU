package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Tag;
import cn.sduonline.business.data.vo.AdminTagVO;
import cn.sduonline.business.mapper.MediaTagMapper;
import cn.sduonline.business.mapper.SubmissionTagMapper;
import cn.sduonline.business.mapper.TagMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AdminTagService {
    private final TagMapper tagMapper;
    private final MediaTagMapper mediaTagMapper;
    private final SubmissionTagMapper submissionTagMapper;
    private final TagRelationService tagRelationService;

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
        if (Objects.equals(tag.getName(), name)) return toVO(tag, mediaTagMapper.countByTagId(tagId));
        requireUnique(name, tagId);
        tag.setName(name); tag.setUpdatedAt(LocalDateTime.now());
        tagMapper.updateById(tag);
        return toVO(tag, mediaTagMapper.countByTagId(tagId));
    }

    @Transactional
    public void merge(Long sourceTagId, Long targetTagId) {
        if (Objects.equals(sourceTagId, targetTagId)) {
            throw new BizException(BizCode.ADMIN_TAG_MERGE_SELF);
        }
        List<Long> tagIds = Stream.of(sourceTagId, targetTagId).sorted().toList();
        if (tagMapper.selectByIdsForUpdate(tagIds).size() != 2) {
            throw new BizException(BizCode.ADMIN_TAG_NOT_FOUND);
        }
        tagRelationService.mergeTag(sourceTagId, targetTagId);
        tagMapper.deleteById(sourceTagId);
    }

    @Transactional
    public void delete(Long tagId, boolean force) {
        if (tagMapper.selectByIdsForUpdate(List.of(tagId)).isEmpty()) {
            throw new BizException(BizCode.ADMIN_TAG_NOT_FOUND);
        }
        boolean referenced = submissionTagMapper.countByTagId(tagId) > 0
                || mediaTagMapper.countByTagId(tagId) > 0;
        if (referenced && !force) {
            throw new BizException(BizCode.ADMIN_TAG_IN_USE);
        }
        if (referenced) tagRelationService.removeTag(tagId);
        tagMapper.deleteById(tagId);
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
