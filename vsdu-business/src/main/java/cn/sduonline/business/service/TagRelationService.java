package cn.sduonline.business.service;

import cn.sduonline.business.data.po.MediaTag;
import cn.sduonline.business.data.po.SubmissionTag;
import cn.sduonline.business.data.po.Tag;
import cn.sduonline.business.mapper.MediaTagMapper;
import cn.sduonline.business.mapper.SubmissionTagMapper;
import cn.sduonline.business.mapper.TagMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagRelationService {
    public static final int MAX_TAGS = 20;

    private final TagMapper tagMapper;
    private final SubmissionTagMapper submissionTagMapper;
    private final MediaTagMapper mediaTagMapper;

    public List<Tag> resolveTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return List.of();
        if (tagIds.size() > MAX_TAGS) {
            throw new BizException(BizCode.BAD_REQUEST, "最多选择20个标签");
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long tagId : tagIds) {
            if (tagId == null || tagId <= 0) {
                throw new BizException(BizCode.BAD_REQUEST, "标签ID必须为正数");
            }
            if (!uniqueIds.add(tagId)) {
                throw new BizException(BizCode.BAD_REQUEST, "标签ID不能重复");
            }
        }

        Map<Long, Tag> tagsById = tagMapper.selectByIds(uniqueIds).stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity()));
        return uniqueIds.stream().map(id -> {
            Tag tag = tagsById.get(id);
            if (tag == null) throw new BizException(BizCode.TAG_NOT_FOUND);
            return tag;
        }).toList();
    }

    @Transactional
    public void replaceSubmissionTags(Long submissionId, List<Long> tagIds) {
        List<Tag> tags = resolveTags(tagIds);
        submissionTagMapper.deleteBySubmissionId(submissionId);
        if (tags.isEmpty()) return;

        List<SubmissionTag> relations = new ArrayList<>(tags.size());
        for (int index = 0; index < tags.size(); index++) {
            SubmissionTag relation = new SubmissionTag();
            relation.setSubmissionId(submissionId);
            relation.setTagId(tags.get(index).getId());
            relation.setSortOrder(index);
            relations.add(relation);
        }
        submissionTagMapper.insertBatch(relations);
    }

    @Transactional
    public void replaceMediaTags(Long mediaId, List<Long> tagIds) {
        List<Tag> tags = resolveTags(tagIds);
        mediaTagMapper.deleteByMediaId(mediaId);
        if (tags.isEmpty()) return;

        List<MediaTag> relations = new ArrayList<>(tags.size());
        for (int index = 0; index < tags.size(); index++) {
            MediaTag relation = new MediaTag();
            relation.setMediaId(mediaId);
            relation.setTagId(tags.get(index).getId());
            relation.setSortOrder(index);
            relations.add(relation);
        }
        mediaTagMapper.insertBatch(relations);
    }

    @Transactional
    public void copySubmissionTagsToMedia(Long submissionId, Long mediaId) {
        List<SubmissionTag> source = submissionTagMapper.selectBySubmissionId(submissionId);
        mediaTagMapper.deleteByMediaId(mediaId);
        if (source.isEmpty()) return;

        List<MediaTag> relations = source.stream().map(item -> {
            MediaTag relation = new MediaTag();
            relation.setMediaId(mediaId);
            relation.setTagId(item.getTagId());
            relation.setSortOrder(item.getSortOrder());
            return relation;
        }).toList();
        mediaTagMapper.insertBatch(relations);
    }

    public List<Tag> listSubmissionTags(Long submissionId) {
        return orderedTags(submissionTagMapper.selectBySubmissionId(submissionId).stream()
                .map(SubmissionTag::getTagId).toList());
    }

    public List<Tag> listMediaTags(Long mediaId) {
        return orderedTags(mediaTagMapper.selectByMediaId(mediaId).stream()
                .map(MediaTag::getTagId).toList());
    }

    public List<String> listSubmissionTagNames(Long submissionId) {
        return listSubmissionTags(submissionId).stream().map(Tag::getName).toList();
    }

    public List<String> listMediaTagNames(Long mediaId) {
        return listMediaTags(mediaId).stream().map(Tag::getName).toList();
    }

    public Map<Long, List<String>> listSubmissionTagNames(Collection<Long> submissionIds) {
        return tagNamesByOwner(listSubmissionTags(submissionIds));
    }

    public Map<Long, List<Tag>> listSubmissionTags(Collection<Long> submissionIds) {
        if (submissionIds == null || submissionIds.isEmpty()) return Map.of();
        List<SubmissionTag> relations = submissionTagMapper.selectBySubmissionIds(distinctIds(submissionIds));
        return tagsByOwner(
                submissionIds,
                relations,
                SubmissionTag::getSubmissionId,
                SubmissionTag::getTagId
        );
    }

    public Map<Long, List<String>> listMediaTagNames(Collection<Long> mediaIds) {
        return tagNamesByOwner(listMediaTags(mediaIds));
    }

    public Map<Long, List<Tag>> listMediaTags(Collection<Long> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) return Map.of();
        List<MediaTag> relations = mediaTagMapper.selectByMediaIds(distinctIds(mediaIds));
        return tagsByOwner(mediaIds, relations, MediaTag::getMediaId, MediaTag::getTagId);
    }

    public Long firstMediaTagId(Long mediaId) {
        return mediaTagMapper.selectFirstTagId(mediaId);
    }

    @Transactional
    public void mergeTag(Long sourceTagId, Long targetTagId) {
        mergeSubmissionTags(sourceTagId, targetTagId);
        mergeMediaTags(sourceTagId, targetTagId);
    }

    @Transactional
    public void removeTag(Long tagId) {
        submissionTagMapper.deleteByTagId(tagId);
        mediaTagMapper.deleteByTagId(tagId);
    }

    private void mergeSubmissionTags(Long sourceTagId, Long targetTagId) {
        List<Long> submissionIds = submissionTagMapper.selectSubmissionIdsByTagId(sourceTagId);
        if (submissionIds.isEmpty()) return;

        List<SubmissionTag> current = submissionTagMapper.selectBySubmissionIdsForUpdate(submissionIds);
        Map<Long, LinkedHashSet<Long>> tagsBySubmission = new LinkedHashMap<>();
        for (SubmissionTag relation : current) {
            Long tagId = Objects.equals(relation.getTagId(), sourceTagId)
                    ? targetTagId
                    : relation.getTagId();
            tagsBySubmission.computeIfAbsent(relation.getSubmissionId(), ignored -> new LinkedHashSet<>())
                    .add(tagId);
        }

        List<SubmissionTag> merged = new ArrayList<>();
        tagsBySubmission.forEach((submissionId, tagIds) -> {
            int sortOrder = 0;
            for (Long tagId : tagIds) {
                SubmissionTag relation = new SubmissionTag();
                relation.setSubmissionId(submissionId);
                relation.setTagId(tagId);
                relation.setSortOrder(sortOrder++);
                merged.add(relation);
            }
        });

        submissionTagMapper.deleteBySubmissionIds(submissionIds);
        if (!merged.isEmpty()) submissionTagMapper.insertBatch(merged);
    }

    private void mergeMediaTags(Long sourceTagId, Long targetTagId) {
        List<Long> mediaIds = mediaTagMapper.selectMediaIdsByTagId(sourceTagId);
        if (mediaIds.isEmpty()) return;

        List<MediaTag> current = mediaTagMapper.selectByMediaIdsForUpdate(mediaIds);
        Map<Long, LinkedHashSet<Long>> tagsByMedia = new LinkedHashMap<>();
        for (MediaTag relation : current) {
            Long tagId = Objects.equals(relation.getTagId(), sourceTagId)
                    ? targetTagId
                    : relation.getTagId();
            tagsByMedia.computeIfAbsent(relation.getMediaId(), ignored -> new LinkedHashSet<>())
                    .add(tagId);
        }

        List<MediaTag> merged = new ArrayList<>();
        tagsByMedia.forEach((mediaId, tagIds) -> {
            int sortOrder = 0;
            for (Long tagId : tagIds) {
                MediaTag relation = new MediaTag();
                relation.setMediaId(mediaId);
                relation.setTagId(tagId);
                relation.setSortOrder(sortOrder++);
                merged.add(relation);
            }
        });

        mediaTagMapper.deleteByMediaIds(mediaIds);
        if (!merged.isEmpty()) mediaTagMapper.insertBatch(merged);
    }

    private List<Tag> orderedTags(List<Long> tagIds) {
        if (tagIds.isEmpty()) return List.of();
        Map<Long, Tag> tagsById = tagMapper.selectByIds(new LinkedHashSet<>(tagIds)).stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity()));
        return tagIds.stream().map(tagsById::get).filter(Objects::nonNull).toList();
    }

    private Collection<Long> distinctIds(Collection<Long> ownerIds) {
        return new LinkedHashSet<>(ownerIds);
    }

    private Map<Long, List<String>> tagNamesByOwner(Map<Long, List<Tag>> tagsByOwner) {
        return tagsByOwner.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().map(Tag::getName).toList()
        ));
    }

    private <R> Map<Long, List<Tag>> tagsByOwner(
            Collection<Long> ownerIds,
            List<R> relations,
            Function<R, Long> ownerId,
            Function<R, Long> tagId
    ) {
        LinkedHashSet<Long> tagIds = relations.stream()
                .map(tagId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Tag> tagsById = tagIds.isEmpty() ? Map.of() : tagMapper.selectByIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity()));

        Map<Long, List<Tag>> result = new LinkedHashMap<>();
        ownerIds.forEach(id -> result.put(id, new ArrayList<>()));
        for (R relation : relations) {
            Tag tag = tagsById.get(tagId.apply(relation));
            if (tag != null) result.computeIfAbsent(ownerId.apply(relation), ignored -> new ArrayList<>()).add(tag);
        }
        return result.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> List.copyOf(entry.getValue())
        ));
    }
}
