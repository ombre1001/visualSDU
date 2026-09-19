package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.MediaTicketData;
import cn.sduonline.business.data.dto.StreamDownloadFile;
import cn.sduonline.business.data.enums.UserStatus;
import cn.sduonline.business.data.po.*;
import cn.sduonline.business.data.projection.MediaSummaryRow;
import cn.sduonline.business.data.vo.*;
import cn.sduonline.business.mapper.*;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.business.util.MediaTicketUtils;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.infrastructure.file.model.DownloadFile;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MediaService {
    private static final int VISIBLE = 1;

    private final MediaMapper mediaMapper;
    private final MediaLikeMapper likeMapper;
    private final MediaFavoriteMapper favoriteMapper;
    private final FavoriteFolderMapper folderMapper;
    private final UserBrowseHistoryMapper historyMapper;
    private final MediaDownloadMapper downloadMapper;
    private final UserMapper userMapper;
    private final LocationMapper locationMapper;
    private final FileStorage fileStorage;
    private final MediaTicketUtils mediaTicketUtils;
    private final TagRelationService tagRelationService;

    public MediaDetailVO detail(Long mediaId, Long optionalUserId) {
        Media media = requireVisible(mediaId);
        return toDetail(
                media,
                formalUserIdOrNull(optionalUserId),
                tagRelationService.listMediaTags(mediaId)
        );
    }

    List<MediaDetailVO> details(List<Long> mediaIds, Long optionalUserId) {
        if (mediaIds.isEmpty()) return List.of();
        Long formalUserId = formalUserIdOrNull(optionalUserId);
        Map<Long, Media> mediaById = mediaMapper.selectByIds(new LinkedHashSet<>(mediaIds)).stream()
                .filter(media -> Objects.equals(media.getStatus(), VISIBLE))
                .collect(Collectors.toMap(Media::getId, Function.identity()));
        List<Media> orderedMedia = mediaIds.stream().map(id -> {
            Media media = mediaById.get(id);
            if (media == null) throw new BizException(BizCode.MEDIA_NOT_FOUND);
            return media;
        }).toList();
        Map<Long, List<Tag>> tagsByMedia = tagRelationService.listMediaTags(mediaIds);
        return orderedMedia.stream()
                .map(media -> toDetail(
                        media,
                        formalUserId,
                        tagsByMedia.getOrDefault(media.getId(), List.of())
                ))
                .toList();
    }

    @Transactional
    public MediaInteractionVO recordView(Long mediaId, Long optionalUserId) {
        Long formalUserId = formalUserIdOrNull(optionalUserId);
        requireVisible(mediaId);
        mediaMapper.increaseViewCount(mediaId);
        // 匿名游客只增加媒体总浏览量，不创建用户，也不写入用户浏览足迹。
        if (formalUserId != null) historyMapper.upsertView(formalUserId, mediaId);
        return interaction(requireVisible(mediaId), formalUserId);
    }

    @Transactional
    public MediaInteractionVO like(Long userId, Long mediaId) {
        requireFormalUser(userId);
        Media media = requireVisible(mediaId);
        if (likeMapper.insertIgnore(userId, mediaId) == 0) {
            throw new BizException(BizCode.MEDIA_ALREADY_LIKED);
        }
        mediaMapper.increaseLikeCount(mediaId, 1);
        media.setLikeCount(value(media.getLikeCount()) + 1);
        return interaction(media, userId);
    }

    @Transactional
    public MediaInteractionVO unlike(Long userId, Long mediaId) {
        requireFormalUser(userId);
        Media media = requireVisible(mediaId);
        if (likeMapper.deleteRelation(userId, mediaId) == 0) {
            throw new BizException(BizCode.MEDIA_NOT_LIKED);
        }
        mediaMapper.decreaseLikeCount(mediaId, 1);
        media.setLikeCount(Math.max(value(media.getLikeCount()) - 1, 0));
        return interaction(media, userId);
    }

    @Transactional
    public MediaInteractionVO favorite(Long userId, Long mediaId, Long requestedFolderId) {
        requireFormalUser(userId);
        Media media = requireVisible(mediaId);
        FavoriteFolder folder = requestedFolderId == null
                ? getOrCreateDefaultFolder(userId)
                : requireOwnedFolder(userId, requestedFolderId);
        if (favoriteMapper.insertIgnore(userId, folder.getId(), mediaId) == 0) {
            throw new BizException(BizCode.MEDIA_ALREADY_FAVORITED);
        }
        mediaMapper.increaseFavoriteCount(mediaId, 1);
        media.setFavoriteCount(value(media.getFavoriteCount()) + 1);
        return interaction(media, userId);
    }

    @Transactional
    public MediaInteractionVO unfavorite(Long userId, Long mediaId) {
        requireFormalUser(userId);
        Media media = requireVisible(mediaId);
        int removed = favoriteMapper.deleteAllForUser(userId, mediaId);
        if (removed == 0) throw new BizException(BizCode.MEDIA_NOT_FAVORITED);
        mediaMapper.decreaseFavoriteCount(mediaId, removed);
        media.setFavoriteCount(Math.max(value(media.getFavoriteCount()) - removed, 0));
        return interaction(media, userId);
    }

    @Transactional
    public MediaDownloadVO requestDownload(Long userId, Long mediaId) {
        User user = requireFormalUser(userId);
        if (!Boolean.TRUE.equals(user.getAllowDownload())) {
            throw new BizException(BizCode.MEDIA_DOWNLOAD_FORBIDDEN);
        }

        Media media = requireVisible(mediaId);
        recordDownload(userId, mediaId, media.getObjectKey());

        return new MediaDownloadVO(mediaId, fileStorage.getUrl(media.getObjectKey()), 600);
    }

    public String getMediaDownloadTicket(Long mediaId) {
        User u = requireFormalUser(CurrentUser.id());
        if (!Boolean.TRUE.equals(u.getAllowDownload())) {
            throw new BizException(BizCode.MEDIA_DOWNLOAD_FORBIDDEN);
        }

        Media media = requireVisible(mediaId);
        return mediaTicketUtils.getStoredMediaTicket(
                MediaTicketData.builder()
                        .userId(CurrentUser.id())
                        .mediaId(mediaId)
                        .objectKey(media.getObjectKey())
                        .build()
        );
    }

    @Transactional
    public StreamDownloadFile streamDownloadFile(String ticket) {
        var data = mediaTicketUtils.consumeStoredMediaTicket(ticket);
        if (data == null) {
            throw new BizException(BizCode.MEDIA_DOWNLOAD_TICKET_INVALID);
        }

        User u = requireFormalUser(data.userId());
        if (!Boolean.TRUE.equals(u.getAllowDownload())) {
            throw new BizException(BizCode.MEDIA_DOWNLOAD_FORBIDDEN);
        }

        Media m = requireVisible(data.mediaId());
        if (!Objects.equals(m.getObjectKey(), data.objectKey())) {
            throw new BizException(BizCode.MEDIA_DOWNLOAD_TICKET_INVALID);
        }
        recordDownload(data.userId(), data.mediaId(), data.objectKey());

        DownloadFile file =  fileStorage.streamDownload(data.objectKey());
        String filename = buildDownloadFilename(m, file.contentType());

        return StreamDownloadFile.builder()
                .contentType(file.contentType())
                .filename(filename)
                .inputStream(file.inputStream())
                .size(file.size())
                .build();
    }

    @Transactional
    void recordDownload(Long userId, Long mediaId, String objectKey) {
        downloadMapper.insertRecord(userId, mediaId, objectKey);
        mediaMapper.increaseDownloadCount(mediaId);
    }

    private String buildDownloadFilename(Media media, String contentType) {
        String base = sanitizeFilenameBase(media.getTitle());

        if (base == null) {
            base = "vsdu-media";
        }

        String ext = extensionFromContentType(contentType);
        return base + "-" + media.getId() + ext;
    }

    private String sanitizeFilenameBase(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }

        String name = title.strip();

        // Windows 禁止字符 + 所有控制字符
        name = name.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");

        // 合并多余空白
        name = name.replaceAll("\\s+", " ");

        // 去掉 Windows 不友好的结尾点号/空格
        name = name.replaceAll("[. ]+$", "");

        if (name.isBlank()) {
            return null;
        }

        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) {
            name = "_" + name;
        }

        int maxLength = 80;
        if (name.length() > maxLength) {
            name = name.substring(0, maxLength).strip();
        }

        return name.isBlank() ? null : name;
    }

    private String extensionFromContentType(String contentType) {
        if (contentType == null) {
            return ".jpg";
        }

        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }

    public List<MediaSummaryVO> related(Long mediaId, int size) {
        Media source = requireVisible(mediaId);
        int safeSize = Math.clamp(size, 1, 30);
        Long tagId = tagRelationService.firstMediaTagId(mediaId);
        return toSummaries(mediaMapper.selectRelatedMedia(
                mediaId, source.getLocationId(), tagId, safeSize
        ));
    }

    Media requireVisible(Long mediaId) {
        Media media = mediaMapper.selectOne(new LambdaQueryWrapper<Media>()
                .eq(Media::getId, mediaId)
                .eq(Media::getStatus, VISIBLE));
        if (media == null) throw new BizException(BizCode.MEDIA_NOT_FOUND);
        return media;
    }

    List<MediaSummaryVO> toMediaSummaries(List<Media> media) {
        if (media.isEmpty()) return List.of();
        Map<Long, List<Tag>> tagsByMedia = tagRelationService.listMediaTags(
                media.stream().map(Media::getId).toList()
        );
        return media.stream()
                .map(item -> toSummary(item, tagsByMedia.getOrDefault(item.getId(), List.of())))
                .toList();
    }

    private MediaSummaryVO toSummary(Media media, List<Tag> tags) {
        Location location = locationMapper.selectById(media.getLocationId());
        return new MediaSummaryVO(
                media.getId(), media.getTitle(), media.getLocationId(),
                location == null ? null : location.getName(), thumbnailUrl(media),
                media.getShotAt(), toTagVOs(tags), value(media.getViewCount()), value(media.getLikeCount()),
                value(media.getFavoriteCount())
        );
    }

    List<MediaSummaryVO> toSummaries(List<MediaSummaryRow> rows) {
        if (rows.isEmpty()) return List.of();
        Map<Long, List<Tag>> tagsByMedia = tagRelationService.listMediaTags(
                rows.stream().map(MediaSummaryRow::getId).toList()
        );
        return rows.stream()
                .map(row -> toSummary(row, tagsByMedia.getOrDefault(row.getId(), List.of())))
                .toList();
    }

    private MediaSummaryVO toSummary(MediaSummaryRow row, List<Tag> tags) {
        return new MediaSummaryVO(
                row.getId(), row.getTitle(), row.getLocationId(), row.getLocationName(),
                fileStorage.getUrl(row.getThumbnailKey()), row.getShotAt(),
                toTagVOs(tags),
                value(row.getViewCount()), value(row.getLikeCount()), value(row.getFavoriteCount())
        );
    }

    String thumbnailUrl(Media media) {
        String thumbnailKey = media.getThumbnailKey() == null || media.getThumbnailKey().isBlank()
                ? media.getObjectKey() : media.getThumbnailKey();
        return fileStorage.getUrl(thumbnailKey);
    }

    private MediaDetailVO toDetail(Media media, Long optionalUserId, List<Tag> tags) {
        Location location = locationMapper.selectById(media.getLocationId());
        User uploader = media.getUploaderId() == null ? null : userMapper.selectById(media.getUploaderId());
        boolean liked = optionalUserId != null && hasLike(optionalUserId, media.getId());
        boolean favorited = optionalUserId != null && hasFavorite(optionalUserId, media.getId());
        String thumbnailKey = media.getThumbnailKey() == null || media.getThumbnailKey().isBlank()
                ? media.getObjectKey() : media.getThumbnailKey();
        return new MediaDetailVO(
                media.getId(), media.getUploaderId(), uploader == null ? null : uploader.getNickname(),
                media.getLocationId(), location == null ? null : location.getName(), media.getTitle(),
                media.getDescription(), fileStorage.getUrl(media.getObjectKey()), fileStorage.getUrl(thumbnailKey),
                media.getShotAt(), toTagVOs(tags), value(media.getViewCount()),
                value(media.getLikeCount()), value(media.getFavoriteCount()), value(media.getDownloadCount()),
                liked, favorited, media.getCreatedAt()
        );
    }

    private MediaInteractionVO interaction(Media media, Long optionalUserId) {
        boolean liked = optionalUserId != null && hasLike(optionalUserId, media.getId());
        boolean favorited = optionalUserId != null && hasFavorite(optionalUserId, media.getId());
        return new MediaInteractionVO(
                media.getId(), value(media.getViewCount()), value(media.getLikeCount()),
                value(media.getFavoriteCount()), liked, favorited
        );
    }

    private boolean hasLike(Long userId, Long mediaId) {
        return likeMapper.selectCount(new LambdaQueryWrapper<MediaLike>()
                .eq(MediaLike::getUserId, userId)
                .eq(MediaLike::getMediaId, mediaId)) > 0;
    }

    private boolean hasFavorite(Long userId, Long mediaId) {
        return favoriteMapper.selectCount(new LambdaQueryWrapper<MediaFavorite>()
                .eq(MediaFavorite::getUserId, userId)
                .eq(MediaFavorite::getMediaId, mediaId)) > 0;
    }

    private FavoriteFolder requireOwnedFolder(Long userId, Long folderId) {
        FavoriteFolder folder = folderMapper.selectOne(new LambdaQueryWrapper<FavoriteFolder>()
                .eq(FavoriteFolder::getId, folderId)
                .eq(FavoriteFolder::getUserId, userId));
        if (folder == null) throw new BizException(BizCode.FAVORITE_FOLDER_NOT_FOUND);
        return folder;
    }

    private FavoriteFolder getOrCreateDefaultFolder(Long userId) {
        FavoriteFolder folder = folderMapper.selectOne(new LambdaQueryWrapper<FavoriteFolder>()
                .eq(FavoriteFolder::getUserId, userId)
                .eq(FavoriteFolder::getIsDefault, true));
        if (folder != null) return folder;

        folder = new FavoriteFolder();
        folder.setUserId(userId);
        folder.setName("默认收藏夹");
        folder.setDescription("系统默认收藏夹");
        folder.setIsDefault(true);
        folder.setSortOrder(0);
        folder.setDeleted(false);
        folderMapper.insert(folder);
        return folder;
    }

    private User requireFormalUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (!isFormalUser(user)) {
            throw new BizException(BizCode.FORMAL_USER_REQUIRED);
        }
        return user;
    }

    /**
     * 公开浏览接口允许不携带 token。只有统一认证产生的正式账号才返回用户维度状态，
     * 并写入 user_browse_history；匿名游客及历史遗留的非正式账号一律按匿名处理。
     */
    private Long formalUserIdOrNull(Long optionalUserId) {
        if (optionalUserId == null) return null;
        User user = userMapper.selectById(optionalUserId);
        return isFormalUser(user) ? user.getId() : null;
    }

    private boolean isFormalUser(User user) {
        return user != null
                && !Boolean.TRUE.equals(user.getDeleted())
                && user.getCasId() != null
                && !user.getCasId().isBlank()
                && user.getRole() != null
                && user.getStatus() == UserStatus.NORMAL;
    }

    private long value(Long count) {
        return Objects.requireNonNullElse(count, 0L);
    }

    private List<TagVO> toTagVOs(List<Tag> tags) {
        return tags.stream().map(tag -> new TagVO(tag.getId(), tag.getName())).toList();
    }


}
