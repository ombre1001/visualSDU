package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.AdminMediaClassificationRequest;
import cn.sduonline.business.data.enums.ReportTargetType;
import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.po.Tag;
import cn.sduonline.business.data.vo.AdminMediaVO;
import cn.sduonline.business.mapper.*;
import cn.sduonline.business.util.TagCodec;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.infrastructure.file.exception.FileStorageException;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminMediaService {
    private static final int HIDDEN = 0;
    private static final int VISIBLE = 1;
    private final MediaMapper mediaMapper;
    private final LocationMapper locationMapper;
    private final TagMapper tagMapper;
    private final MediaLikeMapper mediaLikeMapper;
    private final MediaFavoriteMapper mediaFavoriteMapper;
    private final MediaDownloadMapper mediaDownloadMapper;
    private final UserBrowseHistoryMapper browseHistoryMapper;
    private final TopicMediaMapper topicMediaMapper;
    private final FavoriteFolderMapper favoriteFolderMapper;
    private final SubmissionAssetMapper submissionAssetMapper;
    private final TimeComparisonItemMapper timeComparisonItemMapper;
    private final TimeComparisonMapper timeComparisonMapper;
    private final ReportMapper reportMapper;
    private final FileStorage fileStorage;

    public PageResult<AdminMediaVO> list(String keyword, Long locationId, Integer status, long page, long size) {
        long p = Math.max(1, page), s = Math.clamp(size, 1, 100);
        String q = keyword == null || keyword.isBlank() ? null : keyword.strip();
        long total = mediaMapper.countAdmin(q, locationId, status);
        long offset = (p - 1) * s;
        List<Media> records = total == 0
                ? List.of()
                : mediaMapper.selectAdminPage(q, locationId, status, offset, s);
        return new PageResult<>(total, p, s, records.stream().map(this::toVO).toList());
    }

    @Transactional
    public AdminMediaVO classify(Long mediaId, AdminMediaClassificationRequest r) {
        Media media = requireMedia(mediaId);
        if (r.locationId() == null && r.tagIds() == null) throw new BizException(BizCode.ADMIN_MEDIA_CLASSIFICATION_EMPTY);
        LambdaUpdateWrapper<Media> update = new LambdaUpdateWrapper<Media>().eq(Media::getId, mediaId);
        if (r.locationId() != null) {
            Location location = locationMapper.selectOne(new LambdaQueryWrapper<Location>()
                    .eq(Location::getId, r.locationId()).eq(Location::getStatus, VISIBLE));
            if (location == null) throw new BizException(BizCode.ADMIN_MEDIA_LOCATION_INVALID);
            media.setLocationId(r.locationId());
            update.set(Media::getLocationId, r.locationId());
        }
        if (r.tagIds() != null) {
            if (new HashSet<>(r.tagIds()).size() != r.tagIds().size()) {
                throw new BizException(BizCode.BAD_REQUEST, "标签ID不能重复");
            }
            List<String> names = new ArrayList<>();
            for (Long tagId : r.tagIds()) {
                Tag tag = tagMapper.selectById(tagId);
                if (tag == null) throw new BizException(BizCode.ADMIN_TAG_NOT_FOUND);
                names.add(tag.getName());
            }
            String tags = TagCodec.encode(names);
            media.setTags(tags);
            update.set(Media::getTags, tags);
        }
        LocalDateTime now = LocalDateTime.now();
        media.setUpdatedAt(now); update.set(Media::getUpdatedAt, now); mediaMapper.update(null, update); return toVO(media);
    }

    @Transactional
    public AdminMediaVO hide(Long id) {
        Media media = requireMedia(id);
        if (Objects.equals(media.getStatus(), HIDDEN)) throw new BizException(BizCode.ADMIN_MEDIA_ALREADY_HIDDEN);
        media.setStatus(HIDDEN); media.setUpdatedAt(LocalDateTime.now()); mediaMapper.updateById(media); return toVO(media);
    }

    @Transactional
    public AdminMediaVO restore(Long id) {
        Media media = requireMedia(id);
        if (Objects.equals(media.getStatus(), VISIBLE)) throw new BizException(BizCode.ADMIN_MEDIA_ALREADY_VISIBLE);
        if (media.getLocationId() == null || locationMapper.selectOne(new LambdaQueryWrapper<Location>()
                .eq(Location::getId, media.getLocationId()).eq(Location::getStatus, VISIBLE)) == null) {
            throw new BizException(BizCode.ADMIN_MEDIA_LOCATION_INVALID);
        }
        media.setStatus(VISIBLE); media.setUpdatedAt(LocalDateTime.now()); mediaMapper.updateById(media); return toVO(media);
    }

    /**
     * 永久删除媒体、所有业务关联以及 R2 中的原图和缩略图。
     * R2 删除使用严格模式，任何一个对象删除失败都会抛出异常并回滚数据库事务。
     */
    @Transactional
    public void delete(Long id) {
        Media media = mediaMapper.selectByIdForUpdate(id);
        if (media == null) throw new BizException(BizCode.ADMIN_MEDIA_NOT_FOUND);
        if (reportMapper.existsActiveByTarget(ReportTargetType.MEDIA.name(), id)) {
            throw new BizException(BizCode.MEDIA_ACTIVE_REPORT_EXISTS);
        }

        favoriteFolderMapper.clearCoverMedia(id);
        mediaLikeMapper.deleteByMedia(id);
        mediaFavoriteMapper.deleteByMedia(id);
        mediaDownloadMapper.deleteByMedia(id);
        browseHistoryMapper.deleteByMedia(id);
        topicMediaMapper.deleteByMedia(id);
        submissionAssetMapper.deleteByMedia(id);
        removeFromTimeComparisons(id);

        if (mediaMapper.deleteById(id) != 1) {
            throw new BizException(BizCode.ADMIN_MEDIA_NOT_FOUND);
        }

        deleteStorageObjects(media);
    }

    private void removeFromTimeComparisons(Long mediaId) {
        List<Long> comparisonIds =
                timeComparisonItemMapper.selectComparisonIdsByMedia(mediaId);
        timeComparisonItemMapper.deleteByMedia(mediaId);

        for (Long comparisonId : comparisonIds) {
            if (timeComparisonItemMapper.countByComparison(comparisonId) < 2) {
                timeComparisonItemMapper.deleteByComparison(comparisonId);
                timeComparisonMapper.deleteById(comparisonId);
            }
        }
    }

    private void deleteStorageObjects(Media media) {
        LinkedHashSet<String> objectKeys = new LinkedHashSet<>();
        addObjectKey(objectKeys, media.getThumbnailKey());
        addObjectKey(objectKeys, media.getObjectKey());

        try {
            for (String objectKey : objectKeys) {
                fileStorage.delete(objectKey);
            }
        } catch (FileStorageException exception) {
            throw new BizException(BizCode.ADMIN_MEDIA_STORAGE_DELETE_FAILED);
        }
    }

    private void addObjectKey(Set<String> objectKeys, String objectKey) {
        if (objectKey != null && !objectKey.isBlank()) {
            objectKeys.add(objectKey);
        }
    }

    private Media requireMedia(Long id) {
        Media media = mediaMapper.selectById(id); if (media == null) throw new BizException(BizCode.ADMIN_MEDIA_NOT_FOUND); return media;
    }

    private AdminMediaVO toVO(Media m) {
        return new AdminMediaVO(m.getId(), m.getSubmissionId(), m.getUploaderId(), m.getLocationId(),
                url(m.getObjectKey()), url(m.getThumbnailKey()), m.getTitle(), m.getDescription(), m.getShotAt(),
                TagCodec.decode(m.getTags()), m.getStatus(), Objects.requireNonNullElse(m.getViewCount(), 0L),
                Objects.requireNonNullElse(m.getLikeCount(), 0L), Objects.requireNonNullElse(m.getFavoriteCount(), 0L),
                Objects.requireNonNullElse(m.getDownloadCount(), 0L), m.getCreatedAt(), m.getUpdatedAt());
    }

    private String url(String key) { return key == null || key.isBlank() ? null : fileStorage.getUrl(key); }
}
