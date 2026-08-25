package cn.sduonline.business.service;

import cn.sduonline.business.data.enums.SubmissionStatus;
import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.data.po.Submission;
import cn.sduonline.business.data.po.SubmissionAsset;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.business.mapper.SubmissionAssetMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionPublicationServiceTest {
    @Mock
    private SubmissionAssetMapper assetMapper;
    @Mock
    private MediaMapper mediaMapper;
    @InjectMocks
    private SubmissionPublicationService service;

    @Test
    void publishAssetsShouldCreateMediaAndLinkAsset() {
        Submission submission = Submission.builder()
                .id(10L)
                .userId(5L)
                .locationId(8L)
                .description("校园秋景")
                .shotAt(LocalDateTime.of(2026, 8, 1, 10, 0))
                .tags("秋天|校园")
                .status(SubmissionStatus.APPROVED)
                .build();
        SubmissionAsset asset = new SubmissionAsset();
        asset.setId(20L);
        asset.setSubmissionId(10L);
        asset.setObjectKey("submission/5/photo.jpg");
        asset.setOriginalName("photo.jpg");

        when(assetMapper.selectList(any())).thenReturn(List.of(asset));
        when(mediaMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            Media media = invocation.getArgument(0);
            media.setId(30L);
            return 1;
        }).when(mediaMapper).insert(any(Media.class));

        service.publishAssets(submission);

        ArgumentCaptor<Media> mediaCaptor = ArgumentCaptor.forClass(Media.class);
        verify(mediaMapper).insert(mediaCaptor.capture());
        assertThat(mediaCaptor.getValue().getSubmissionId()).isEqualTo(10L);
        assertThat(mediaCaptor.getValue().getUploaderId()).isEqualTo(5L);
        assertThat(mediaCaptor.getValue().getObjectKey()).isEqualTo("submission/5/photo.jpg");
        assertThat(mediaCaptor.getValue().getStatus()).isEqualTo(1);
        assertThat(asset.getMediaId()).isEqualTo(30L);
        verify(assetMapper).updateById(asset);
    }
}
