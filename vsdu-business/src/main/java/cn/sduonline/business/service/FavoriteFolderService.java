package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.BatchFavoriteRequest;
import cn.sduonline.business.data.dto.CreateFavoriteFolderRequest;
import cn.sduonline.business.data.dto.UpdateFavoriteFolderRequest;
import cn.sduonline.business.data.enums.FavoriteBatchAction;
import cn.sduonline.business.data.po.FavoriteFolder;
import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.po.MediaFavorite;
import cn.sduonline.business.data.vo.BatchFavoriteResultVO;
import cn.sduonline.business.data.vo.FavoriteFolderVO;
import cn.sduonline.business.data.vo.MediaSummaryVO;
import cn.sduonline.business.mapper.FavoriteFolderMapper;
import cn.sduonline.business.mapper.MediaFavoriteMapper;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.security.context.CurrentUser;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FavoriteFolderService {

    private static final int DEFAULT_SORT_ORDER = 0;
    private static final int VISIBLE = 1;

    private final FavoriteFolderMapper folderMapper;
    private final MediaFavoriteMapper favoriteMapper;
    private final MediaMapper mediaMapper;
    private final MediaService mediaService;

    /**
     * 查询当前登录用户的全部收藏夹。
     * <p>
     * 如果用户还没有默认收藏夹，则自动创建。
     */
    @Transactional
    public List<FavoriteFolderVO> listFolders() {
        Long userId = CurrentUser.id();
        getOrCreateDefaultFolder(userId);

        return folderMapper.selectList(
                        new LambdaQueryWrapper<FavoriteFolder>()
                                .eq(FavoriteFolder::getUserId, userId)
                                .orderByDesc(FavoriteFolder::getIsDefault)
                                .orderByAsc(FavoriteFolder::getSortOrder)
                                .orderByAsc(FavoriteFolder::getId)
                )
                .stream()
                .map(folder -> toVO(userId, folder))
                .toList();
    }

    /**
     * 创建收藏夹。
     */
    @Transactional
    public FavoriteFolderVO createFolder(CreateFavoriteFolderRequest request) {
        Long userId = CurrentUser.id();
        String name = normalizeRequiredName(request.name());

        requireFolderNameAvailable(userId, name, null);

        FavoriteFolder folder = new FavoriteFolder();
        folder.setUserId(userId);
        folder.setName(name);
        folder.setDescription(normalizeNullableText(request.description()));
        folder.setCoverMediaId(null);
        folder.setIsDefault(false);
        folder.setSortOrder(
                request.sortOrder() == null
                        ? DEFAULT_SORT_ORDER
                        : request.sortOrder()
        );
        folder.setDeleted(false);
        folder.setCreatedAt(LocalDateTime.now());
        folder.setUpdatedAt(LocalDateTime.now());

        folderMapper.insert(folder);
        return toVO(userId, folder);
    }

    /**
     * 修改收藏夹。
     */
    @Transactional
    public FavoriteFolderVO updateFolder(
            Long folderId,
            UpdateFavoriteFolderRequest request
    ) {
        Long userId = CurrentUser.id();
        FavoriteFolder folder = requireOwnedFolder(userId, folderId);

        requireUpdateContent(request);

        if (request.name() != null) {
            String name = normalizeRequiredName(request.name());
            requireFolderNameAvailable(userId, name, folderId);
            folder.setName(name);
        }

        if (request.description() != null) {
            folder.setDescription(normalizeNullableText(request.description()));
        }

        if (request.sortOrder() != null) {
            folder.setSortOrder(request.sortOrder());
        }

        if (Boolean.TRUE.equals(request.clearCover())) {
            if (request.coverMediaId() != null) {
                throw new BizException(
                        BizCode.BAD_REQUEST,
                        "设置封面和清除封面不能同时操作"
                );
            }
            folder.setCoverMediaId(null);
        } else if (request.coverMediaId() != null) {
            requireMediaInFolder(userId, folderId, request.coverMediaId());
            requireVisibleMedia(request.coverMediaId());
            folder.setCoverMediaId(request.coverMediaId());
        }

        folder.setUpdatedAt(LocalDateTime.now());
        folderMapper.updateById(folder);

        return toVO(userId, folder);
    }

    /**
     * 删除收藏夹。
     * <p>
     * 默认收藏夹不允许删除。
     * 删除普通收藏夹时，同时取消其中的全部收藏，并同步媒体收藏数。
     */
    @Transactional
    public void deleteFolder(Long folderId) {
        Long userId = CurrentUser.id();
        FavoriteFolder folder = requireOwnedFolder(userId, folderId);

        if (Boolean.TRUE.equals(folder.getIsDefault())) {
            throw new BizException(
                    BizCode.FAVORITE_DEFAULT_FOLDER_CANNOT_DELETE
            );
        }

        List<MediaFavorite> favorites =
                favoriteMapper.selectByFolder(userId, folderId);

        favoriteMapper.deleteByFolder(userId, folderId);

        // media_favorite通过(user_id, media_id)唯一索引保证一条媒体
        // 对同一用户只有一条收藏关系，因此每条关系减少一次收藏数。
        for (MediaFavorite favorite : favorites) {
            mediaMapper.decreaseFavoriteCount(favorite.getMediaId(), 1);
        }

        folderMapper.deleteById(folder.getId());
    }

    /**
     * 分页查询收藏夹中的媒体。
     */
    public PageResult<MediaSummaryVO> listFolderItems(
            Long folderId,
            long page,
            long size
    ) {
        Long userId = CurrentUser.id();
        requireOwnedFolder(userId, folderId);

        long safePage = Math.max(page, 1);
        long safeSize = Math.clamp(size, 1, 50);
        long offset = (safePage - 1) * safeSize;

        long total = favoriteMapper.countVisibleByFolder(userId, folderId);

        List<MediaSummaryVO> items =
                favoriteMapper.selectVisibleMediaIds(
                                userId,
                                folderId,
                                offset,
                                safeSize
                        )
                        .stream()
                        .map(mediaMapper::selectById)
                        .filter(Objects::nonNull)
                        .filter(media -> media.getStatus() == VISIBLE)
                        .map(mediaService::toSummary)
                        .toList();

        return new PageResult<>(
                total,
                safePage,
                safeSize,
                items
        );
    }

    /**
     * 批量添加、移除或移动收藏。
     */
    @Transactional
    public BatchFavoriteResultVO batch(BatchFavoriteRequest request) {
        Long userId = CurrentUser.id();

        List<Long> mediaIds = List.copyOf(
                new LinkedHashSet<>(request.mediaIds())
        );

        return switch (request.action()) {
            case ADD -> batchAdd(
                    userId,
                    request.folderId(),
                    mediaIds
            );
            case REMOVE -> batchRemove(
                    userId,
                    mediaIds
            );
            case MOVE -> batchMove(
                    userId,
                    request.folderId(),
                    request.targetFolderId(),
                    mediaIds
            );
        };
    }

    private BatchFavoriteResultVO batchAdd(
            Long userId,
            Long requestedFolderId,
            List<Long> mediaIds
    ) {
        FavoriteFolder folder = requestedFolderId == null
                ? getOrCreateDefaultFolder(userId)
                : requireOwnedFolder(userId, requestedFolderId);

        int affected = 0;

        for (Long mediaId : mediaIds) {
            requireVisibleMedia(mediaId);

            int inserted = favoriteMapper.insertIgnore(
                    userId,
                    folder.getId(),
                    mediaId
            );

            if (inserted > 0) {
                mediaMapper.increaseFavoriteCount(mediaId, 1);
                affected++;
            }
        }

        return new BatchFavoriteResultVO(
                FavoriteBatchAction.ADD,
                mediaIds.size(),
                affected,
                folder.getId()
        );
    }

    private BatchFavoriteResultVO batchRemove(
            Long userId,
            List<Long> mediaIds
    ) {
        int affected = 0;

        for (Long mediaId : mediaIds) {
            int removed = favoriteMapper.deleteAllForUser(userId, mediaId);

            if (removed > 0) {
                mediaMapper.decreaseFavoriteCount(mediaId, removed);
                affected += removed;
            }
        }

        return new BatchFavoriteResultVO(
                FavoriteBatchAction.REMOVE,
                mediaIds.size(),
                affected,
                null
        );
    }

    private BatchFavoriteResultVO batchMove(
            Long userId,
            Long sourceFolderId,
            Long targetFolderId,
            List<Long> mediaIds
    ) {
        if (sourceFolderId == null) {
            throw new BizException(
                    BizCode.FAVORITE_BATCH_FOLDER_REQUIRED,
                    "移动收藏时必须指定源收藏夹"
            );
        }

        if (targetFolderId == null) {
            throw new BizException(
                    BizCode.FAVORITE_BATCH_FOLDER_REQUIRED,
                    "移动收藏时必须指定目标收藏夹"
            );
        }

        if (Objects.equals(sourceFolderId, targetFolderId)) {
            throw new BizException(
                    BizCode.BAD_REQUEST,
                    "源收藏夹和目标收藏夹不能相同"
            );
        }

        FavoriteFolder sourceFolder =
                requireOwnedFolder(userId, sourceFolderId);

        FavoriteFolder targetFolder =
                requireOwnedFolder(userId, targetFolderId);

        int affected = 0;

        for (Long mediaId : mediaIds) {
            /*
             * 如果目标收藏夹已经存在该媒体，则删除源收藏夹中的重复关系。
             *
             * 删除一条收藏关系后，需要同步减少media.favorite_count。
             */
            int removed = favoriteMapper.deleteSourceWhenTargetExists(
                    userId,
                    sourceFolder.getId(),
                    targetFolder.getId(),
                    mediaId
            );

            if (removed > 0) {
                mediaMapper.decreaseFavoriteCount(mediaId, removed);
                affected += removed;
                continue;
            }

            /*
             * 目标收藏夹不存在该媒体时，直接将源收藏关系移动过去。
             * 这里只改变folder_id，收藏关系总数没有变化，
             * 因此不修改media.favorite_count。
             */
            int moved = favoriteMapper.moveToFolder(
                    userId,
                    sourceFolder.getId(),
                    targetFolder.getId(),
                    mediaId
            );

            affected += moved;
        }

        return new BatchFavoriteResultVO(
                FavoriteBatchAction.MOVE,
                mediaIds.size(),
                affected,
                targetFolder.getId()
        );
    }

    /**
     * 查询并校验收藏夹属于当前用户。
     */
    private FavoriteFolder requireOwnedFolder(
            Long userId,
            Long folderId
    ) {
        FavoriteFolder folder = folderMapper.selectOne(
                new LambdaQueryWrapper<FavoriteFolder>()
                        .eq(FavoriteFolder::getId, folderId)
                        .eq(FavoriteFolder::getUserId, userId)
        );

        if (folder == null) {
            throw new BizException(
                    BizCode.FAVORITE_FOLDER_NOT_FOUND
            );
        }

        return folder;
    }

    /**
     * 获取或创建用户的默认收藏夹。
     */
    private FavoriteFolder getOrCreateDefaultFolder(Long userId) {
        FavoriteFolder folder = folderMapper.selectOne(
                new LambdaQueryWrapper<FavoriteFolder>()
                        .eq(FavoriteFolder::getUserId, userId)
                        .eq(FavoriteFolder::getIsDefault, true)
        );

        if (folder != null) {
            return folder;
        }

        FavoriteFolder defaultFolder = new FavoriteFolder();
        defaultFolder.setUserId(userId);
        defaultFolder.setName("默认收藏夹");
        defaultFolder.setDescription("系统默认收藏夹");
        defaultFolder.setCoverMediaId(null);
        defaultFolder.setIsDefault(true);
        defaultFolder.setSortOrder(DEFAULT_SORT_ORDER);
        defaultFolder.setDeleted(false);
        defaultFolder.setCreatedAt(LocalDateTime.now());
        defaultFolder.setUpdatedAt(LocalDateTime.now());

        folderMapper.insert(defaultFolder);
        return defaultFolder;
    }

    /**
     * 判断媒体是否属于指定收藏夹。
     */
    private void requireMediaInFolder(
            Long userId,
            Long folderId,
            Long mediaId
    ) {
        long count = favoriteMapper.countFolderMedia(
                userId,
                folderId,
                mediaId
        );

        if (count == 0) {
            throw new BizException(
                    BizCode.FAVORITE_COVER_NOT_IN_FOLDER
            );
        }
    }

    private void requireVisibleMedia(Long mediaId) {
        Media media = mediaMapper.selectOne(
                new LambdaQueryWrapper<Media>()
                        .eq(Media::getId, mediaId)
                        .eq(Media::getStatus, VISIBLE)
        );

        if (media == null) {
            throw new BizException(BizCode.MEDIA_NOT_FOUND);
        }
    }

    private void requireFolderNameAvailable(
            Long userId,
            String name,
            Long excludedFolderId
    ) {
        long count = folderMapper.selectCount(
                new LambdaQueryWrapper<FavoriteFolder>()
                        .eq(FavoriteFolder::getUserId, userId)
                        .eq(FavoriteFolder::getName, name)
                        .ne(excludedFolderId != null,
                                FavoriteFolder::getId,
                                excludedFolderId)
        );

        if (count > 0) {
            throw new BizException(
                    BizCode.FAVORITE_FOLDER_NAME_EXISTS
            );
        }
    }

    private void requireUpdateContent(
            UpdateFavoriteFolderRequest request
    ) {
        boolean empty = request.name() == null
                && request.description() == null
                && request.coverMediaId() == null
                && request.clearCover() == null
                && request.sortOrder() == null;

        if (empty) {
            throw new BizException(
                    BizCode.BAD_REQUEST,
                    "至少需要提供一个要修改的字段"
            );
        }
    }

    private FavoriteFolderVO toVO(
            Long userId,
            FavoriteFolder folder
    ) {
        long itemCount = favoriteMapper.countVisibleByFolder(
                userId,
                folder.getId()
        );

        Long coverMediaId = resolveCoverMediaId(userId, folder);
        String coverUrl = resolveCoverUrl(coverMediaId);

        return new FavoriteFolderVO(
                folder.getId(),
                folder.getName(),
                folder.getDescription(),
                coverMediaId,
                coverUrl,
                itemCount,
                Boolean.TRUE.equals(folder.getIsDefault()),
                Objects.requireNonNullElse(
                        folder.getSortOrder(),
                        DEFAULT_SORT_ORDER
                ),
                folder.getCreatedAt(),
                folder.getUpdatedAt()
        );
    }

    private Long resolveCoverMediaId(
            Long userId,
            FavoriteFolder folder
    ) {
        Long configuredCoverId = folder.getCoverMediaId();

        if (configuredCoverId != null
                && favoriteMapper.countFolderMedia(
                userId,
                folder.getId(),
                configuredCoverId
        ) > 0) {

            Media cover = mediaMapper.selectById(configuredCoverId);
            if (cover != null && cover.getStatus() == VISIBLE) {
                return configuredCoverId;
            }
        }

        return favoriteMapper.selectLatestVisibleMediaId(
                userId,
                folder.getId()
        );
    }

    private String resolveCoverUrl(Long mediaId) {
        if (mediaId == null) {
            return null;
        }

        Media media = mediaMapper.selectById(mediaId);
        if (media == null || media.getStatus() != VISIBLE) {
            return null;
        }

        return mediaService.toSummary(media).thumbnailUrl();
    }

    private String normalizeRequiredName(String value) {
        String name = value == null ? null : value.trim();

        if (name == null || name.isEmpty()) {
            throw new BizException(
                    BizCode.BAD_REQUEST,
                    "收藏夹名称不能为空"
            );
        }

        return name;
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
