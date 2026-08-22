package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.CreateSubmissionRequest;
import cn.sduonline.business.data.dto.UpdateSubmissionRequest;
import cn.sduonline.business.data.enums.ImageScene;
import cn.sduonline.business.data.enums.SubmissionStatus;
import cn.sduonline.business.data.enums.UserRole;
import cn.sduonline.business.data.enums.UserStatus;
import cn.sduonline.business.data.po.*;
import cn.sduonline.business.data.projection.SubmissionSummaryRow;
import cn.sduonline.business.data.vo.SubmissionAssetVO;
import cn.sduonline.business.data.vo.SubmissionDetailVO;
import cn.sduonline.business.data.vo.SubmissionSummaryVO;
import cn.sduonline.business.mapper.*;
import cn.sduonline.business.util.TagCodec;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.common.result.PageResult;
import cn.sduonline.infrastructure.file.exception.BadFileException;
import cn.sduonline.infrastructure.file.image.ImageFileUpload;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private static final int MAX_FILES = 9;
    private static final int ENABLED = 1;

    private final SubmissionMapper submissionMapper;
    private final SubmissionAssetMapper assetMapper;
    private final MediaMapper mediaMapper;
    private final UserMapper userMapper;
    private final LocationMapper locationMapper;
    private final TagMapper tagMapper;
    private final SubmissionReviewSettingService reviewSettingService;
    private final ImageFileUpload imageFileUpload;
    private final FileStorage fileStorage;

    @Transactional
    public SubmissionDetailVO create(Long userId, CreateSubmissionRequest request) {
        requireUploader(userId);
        Location location = requireLocation(request.getLocationId());
        List<MultipartFile> files = normalizedFiles(request.getFiles());
        requireFileCount(files, 0);

        Submission submission = Submission.builder()
                .userId(userId)
                .locationId(location.getId())
                .description(request.getDescription())
                .shotAt(request.getShotAt())
                .tags(encodeTagIds(request.getTagIds()))
                .status(SubmissionStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .deleted(false)
                .build();
        submissionMapper.insert(submission);

        List<String> newKeys = uploadAndInsertAssets(submission.getId(), userId, files, 0);
        deleteKeysOnRollback(newKeys);
        autoPublishWhenReviewDisabled(submission);
        return buildDetail(submission, location);
    }

    public PageResult<SubmissionSummaryVO> mine(
            Long userId,
            SubmissionStatus status,
            long page,
            long size
    ) {
        requireFormalUser(userId);
        long safePage = Math.max(page, 1);
        long safeSize = Math.clamp(size, 1, 50);
        Integer statusValue = status == null ? null : status.getValue();
        long total = submissionMapper.countMine(userId, statusValue);
        long offset = (safePage - 1) * safeSize;
        List<SubmissionSummaryVO> items = (total == 0
                ? List.<SubmissionSummaryRow>of()
                : submissionMapper.selectMinePage(userId, statusValue, offset, safeSize))
                .stream()
                .map(this::buildSummary)
                .toList();
        return new PageResult<>(total, safePage, safeSize, items);
    }

    public SubmissionDetailVO detail(Long operatorId, UserRole role, Long submissionId) {
        Submission submission = requireAccessible(operatorId, role, submissionId);
        return buildDetail(submission, requireLocationIncludingDisabled(submission.getLocationId()));
    }

    @Transactional
    public SubmissionDetailVO update(
            Long userId,
            Long submissionId,
            UpdateSubmissionRequest request
    ) {
        requireUploader(userId);
        Submission submission = requireOwned(userId, submissionId);
        requireEditable(submission);

        Location location = request.getLocationId() == null
                ? requireLocationIncludingDisabled(submission.getLocationId())
                : requireLocation(request.getLocationId());

        if (request.getLocationId() != null) submission.setLocationId(location.getId());
        if (request.getShotAt() != null) submission.setShotAt(request.getShotAt());
        if (request.getTags() != null) submission.setTags(TagCodec.encode(request.getTags()));
        if (request.getDescription() != null) submission.setDescription(request.getDescription());

        List<MultipartFile> files = normalizedFiles(request.getFiles());
        List<SubmissionAsset> oldAssets = listAssets(submissionId);
        boolean replace = Boolean.TRUE.equals(request.getReplaceFiles());
        int retainedCount = replace ? 0 : oldAssets.size();
        if (!files.isEmpty()) requireFileCount(files, retainedCount);
        if (replace && files.isEmpty()) throw new BizException(BizCode.SUBMISSION_FILE_REQUIRED);

        List<String> newKeys;
        if (!files.isEmpty()) {
            if (replace) {
                assetMapper.delete(new LambdaQueryWrapper<SubmissionAsset>()
                        .eq(SubmissionAsset::getSubmissionId, submissionId));
            }
            newKeys = uploadAndInsertAssets(submissionId, userId, files, retainedCount);
            deleteKeysOnRollback(newKeys);
            if (replace) deleteKeysAfterCommit(oldAssets.stream().map(SubmissionAsset::getObjectKey).toList());
        }

        submission.setUpdatedAt(LocalDateTime.now());
        submissionMapper.updateById(submission);
        return buildDetail(submission, location);
    }

    @Transactional
    public SubmissionDetailVO resubmit(Long userId, Long submissionId) {
        requireUploader(userId);
        Submission submission = requireOwned(userId, submissionId);
        if (submission.getStatus() != SubmissionStatus.REJECTED) {
            throw new BizException(BizCode.SUBMISSION_STATUS_INVALID, "只有被退回的稿件可以重新提交");
        }
        submission.setStatus(SubmissionStatus.PENDING);
        submission.setReviewReason(null);
        submission.setReviewedAt(null);
        submission.setReviewedBy(null);
        submission.setSubmittedAt(LocalDateTime.now());
        if (reviewSettingService.isReviewEnabled()) {
            submissionMapper.updateById(submission);
        } else {
            publishSubmission(submission);
        }
        return buildDetail(submission, requireLocationIncludingDisabled(submission.getLocationId()));
    }

    @Transactional
    public void withdraw(Long userId, Long submissionId) {
        requireFormalUser(userId);
        Submission submission = requireOwned(userId, submissionId);
        if (submission.getStatus() != SubmissionStatus.PENDING) {
            throw new BizException(BizCode.SUBMISSION_STATUS_INVALID, "只有待审核稿件可以撤回");
        }
        submission.setStatus(SubmissionStatus.WITHDRAWN);
        submissionMapper.updateById(submission);
    }

    private User requireFormalUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            throw new BizException(BizCode.AUTH_USER_NOT_FOUND);
        }
        if (user.getCasId() == null || user.getCasId().isBlank()) {
            throw new BizException(BizCode.FORMAL_USER_REQUIRED);
        }
        if (user.getStatus() != UserStatus.NORMAL) {
            throw new BizException(BizCode.UNAUTHORIZED, "账号已停用或冻结");
        }
        return user;
    }

    private void requireUploader(Long userId) {
        User user = requireFormalUser(userId);
        if (!Boolean.TRUE.equals(user.getAllowUpload())) {
            throw new BizException(BizCode.SUBMISSION_UPLOAD_FORBIDDEN);
        }
    }

    private Location requireLocation(Long locationId) {
        Location location = locationMapper.selectOne(new LambdaQueryWrapper<Location>()
                .eq(Location::getId, locationId)
                .eq(Location::getStatus, ENABLED));
        if (location == null) throw new BizException(BizCode.LOCATION_NOT_FOUND);
        return location;
    }

    private Location requireLocationIncludingDisabled(Long locationId) {
        Location location = locationMapper.selectById(locationId);
        if (location == null) throw new BizException(BizCode.LOCATION_NOT_FOUND);
        return location;
    }

    private Submission requireOwned(Long userId, Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null || !Objects.equals(submission.getUserId(), userId)) {
            throw new BizException(BizCode.SUBMISSION_NOT_FOUND);
        }
        return submission;
    }

    private Submission requireAccessible(Long operatorId, UserRole role, Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null || (role != UserRole.ADMIN && !Objects.equals(submission.getUserId(), operatorId))) {
            throw new BizException(BizCode.SUBMISSION_NOT_FOUND);
        }
        return submission;
    }

    private void requireEditable(Submission submission) {
        if (submission.getStatus() != SubmissionStatus.PENDING
                && submission.getStatus() != SubmissionStatus.REJECTED) {
            throw new BizException(BizCode.SUBMISSION_STATUS_INVALID, "只有待审或退回稿件可以修改");
        }
    }

    private List<MultipartFile> normalizedFiles(List<MultipartFile> files) {
        if (files == null) return List.of();
        return files.stream().filter(Objects::nonNull).filter(file -> !file.isEmpty()).toList();
    }

    private void requireFileCount(List<MultipartFile> files, int retainedCount) {
        if (files.isEmpty() && retainedCount == 0) throw new BizException(BizCode.SUBMISSION_FILE_REQUIRED);
        if (files.size() + retainedCount > MAX_FILES) {
            throw new BizException(BizCode.SUBMISSION_FILE_COUNT_EXCEEDED);
        }
    }

    private List<String> uploadAndInsertAssets(
            Long submissionId,
            Long userId,
            List<MultipartFile> files,
            int startSortOrder
    ) {
        List<String> uploadedKeys = new ArrayList<>();
        try {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String key = imageFileUpload.uploadImageFile(ImageScene.SUBMISSION, userId, file);
                uploadedKeys.add(key);

                SubmissionAsset asset = new SubmissionAsset();
                asset.setSubmissionId(submissionId);
                asset.setObjectKey(key);
                asset.setOriginalName(file.getOriginalFilename());
                asset.setContentType(file.getContentType());
                asset.setSizeBytes(file.getSize());
                asset.setSortOrder(startSortOrder + i);
                assetMapper.insert(asset);
            }
            return List.copyOf(uploadedKeys);
        } catch (BadFileException exception) {
            uploadedKeys.forEach(fileStorage::deleteQuietly);
            BizCode code = switch (exception.getErrorCode()) {
                case FILE_EMPTY -> BizCode.SUBMISSION_FILE_EMPTY;
                case FILE_TOO_LARGE -> BizCode.SUBMISSION_FILE_TOO_LARGE;
                case FILE_TYPE_NOT_SUPPORT -> BizCode.SUBMISSION_FILE_TYPE_NOT_SUPPORT;
            };
            throw new BizException(code);
        } catch (RuntimeException exception) {
            uploadedKeys.forEach(fileStorage::deleteQuietly);
            throw exception;
        }
    }

    private List<SubmissionAsset> listAssets(Long submissionId) {
        return assetMapper.selectList(new LambdaQueryWrapper<SubmissionAsset>()
                .eq(SubmissionAsset::getSubmissionId, submissionId)
                .orderByAsc(SubmissionAsset::getSortOrder)
                .orderByAsc(SubmissionAsset::getId));
    }

    private void autoPublishWhenReviewDisabled(Submission submission) {
        if (!reviewSettingService.isReviewEnabled()) {
            publishSubmission(submission);
        }
    }

    /**
     * 将一个稿件中的每张图片发布为一条媒体记录。
     * 调用者必须处于事务中，确保稿件状态、图片关联和媒体记录同时成功或回滚。
     */
    private void publishSubmission(Submission submission) {
        List<SubmissionAsset> assets = listAssets(submission.getId());
        if (assets.isEmpty()) {
            throw new BizException(BizCode.SUBMISSION_FILE_REQUIRED);
        }

        for (SubmissionAsset asset : assets) {
            if (asset.getMediaId() != null) {
                continue;
            }

            Media media = mediaMapper.selectOne(new LambdaQueryWrapper<Media>()
                    .eq(Media::getObjectKey, asset.getObjectKey()));
            if (media == null) {
                media = new Media();
                media.setSubmissionId(submission.getId());
                media.setUploaderId(submission.getUserId());
                media.setLocationId(submission.getLocationId());
                media.setObjectKey(asset.getObjectKey());
                media.setThumbnailKey(null);
                media.setTitle(mediaTitle(asset.getOriginalName()));
                media.setDescription(submission.getDescription());
                media.setShotAt(submission.getShotAt());
                media.setTags(submission.getTags());
                media.setStatus(ENABLED);
                media.setViewCount(0L);
                media.setLikeCount(0L);
                media.setFavoriteCount(0L);
                media.setDownloadCount(0L);
                mediaMapper.insert(media);
            }

            asset.setMediaId(media.getId());
            assetMapper.updateById(asset);
        }

        submission.setStatus(SubmissionStatus.APPROVED);
        submission.setReviewReason(null);
        submission.setReviewedBy(null);
        submission.setReviewedAt(LocalDateTime.now());
        submission.setUpdatedAt(LocalDateTime.now());
        submissionMapper.updateById(submission);
    }

    private String mediaTitle(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return null;
        }
        return originalName.length() <= 150 ? originalName : originalName.substring(0, 150);
    }

    private SubmissionDetailVO buildDetail(Submission submission, Location location) {
        List<SubmissionAssetVO> assets = listAssets(submission.getId()).stream()
                .map(asset -> new SubmissionAssetVO(
                        asset.getId(), asset.getOriginalName(), asset.getContentType(),
                        asset.getSizeBytes(), asset.getSortOrder(), asset.getMediaId(),
                        fileStorage.getUrl(asset.getObjectKey())
                ))
                .toList();
        return new SubmissionDetailVO(
                submission.getId(), submission.getUserId(), submission.getLocationId(), location.getName(),
                submission.getDescription(), submission.getShotAt(), TagCodec.decode(submission.getTags()),
                submission.getStatus(), submission.getReviewReason(), submission.getSubmittedAt(),
                submission.getReviewedBy(), submission.getReviewedAt(), submission.getCreatedAt(),
                submission.getUpdatedAt(), assets
        );
    }

    private SubmissionSummaryVO buildSummary(SubmissionSummaryRow row) {
        String coverUrl = row.getCoverKey() == null ? null : fileStorage.getUrl(row.getCoverKey());
        return new SubmissionSummaryVO(
                row.getId(), row.getLocationId(), row.getLocationName(), row.getDescription(),
                row.getShotAt(), row.getStatus(), row.getReviewReason(),
                Objects.requireNonNullElse(row.getAssetCount(), 0), coverUrl,
                row.getSubmittedAt(), row.getUpdatedAt()
        );
    }

    /**
     * 创建稿件接口接收标签 ID 数组；数据库仍保存标签名称编码，兼容现有搜索和展示逻辑。
     */
    private String encodeTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return null;

        LinkedHashSet<Long> uniqueTagIds = new LinkedHashSet<>(tagIds);
        Map<Long, Tag> tagsById = tagMapper.selectBatchIds(uniqueTagIds)
                .stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity()));

        List<String> tagNames = uniqueTagIds.stream()
                .map(tagId -> {
                    Tag tag = tagsById.get(tagId);
                    if (tag == null) {
                        throw new BizException(BizCode.ADMIN_TAG_NOT_FOUND);
                    }
                    return tag.getName();
                })
                .toList();

        return TagCodec.encode(tagNames);
    }

    private void deleteKeysOnRollback(List<String> keys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive() || keys.isEmpty()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) keys.forEach(fileStorage::deleteQuietly);
            }
        });
    }

    private void deleteKeysAfterCommit(List<String> keys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive() || keys.isEmpty()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                keys.forEach(fileStorage::deleteQuietly);
            }
        });
    }
}
