package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.po.Submission;
import cn.sduonline.business.data.po.SubmissionAsset;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.SubmissionAssetMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionPublicationService {
    private static final int VISIBLE = 1;

    private final SubmissionAssetMapper assetMapper;
    private final MediaMapper mediaMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishAssets(Submission submission) {
        List<SubmissionAsset> assets = assetMapper.selectList(
                new LambdaQueryWrapper<SubmissionAsset>()
                        .eq(SubmissionAsset::getSubmissionId, submission.getId())
                        .orderByAsc(SubmissionAsset::getSortOrder)
                        .orderByAsc(SubmissionAsset::getId)
        );
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
                media = buildMedia(submission, asset);
                mediaMapper.insert(media);
            }

            asset.setMediaId(media.getId());
            assetMapper.updateById(asset);
        }
    }

    private Media buildMedia(Submission submission, SubmissionAsset asset) {
        Media media = new Media();
        media.setSubmissionId(submission.getId());
        media.setUploaderId(submission.getUserId());
        media.setLocationId(submission.getLocationId());
        media.setObjectKey(asset.getObjectKey());
        media.setThumbnailKey(null);
        media.setTitle(mediaTitle(asset.getOriginalName()));
        media.setDescription(submission.getDescription());
        media.setShotAt(submission.getShotAt());
        media.setTags(submission.getTags());
        media.setStatus(VISIBLE);
        media.setViewCount(0L);
        media.setLikeCount(0L);
        media.setFavoriteCount(0L);
        media.setDownloadCount(0L);
        return media;
    }

    private String mediaTitle(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return null;
        }
        return originalName.length() <= 150 ? originalName : originalName.substring(0, 150);
    }
}
