package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Media;
import cn.sduonline.business.mapper.*;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminMediaServiceTest {
    @Mock private MediaMapper mediaMapper;
    @Mock private MediaLikeMapper mediaLikeMapper;
    @Mock private MediaFavoriteMapper mediaFavoriteMapper;
    @Mock private MediaDownloadMapper mediaDownloadMapper;
    @Mock private UserBrowseHistoryMapper browseHistoryMapper;
    @Mock private TopicMediaMapper topicMediaMapper;
    @Mock private FavoriteFolderMapper favoriteFolderMapper;
    @Mock private SubmissionAssetMapper submissionAssetMapper;
    @Mock private TimeComparisonItemMapper timeComparisonItemMapper;
    @Mock private TimeComparisonMapper timeComparisonMapper;
    @Mock private ReportMapper reportMapper;
    @Mock private FileStorage fileStorage;
    @InjectMocks private AdminMediaService service;

    @Test
    void deleteShouldRejectMediaWithActiveReportBeforeDeletingRelations() {
        Media media = new Media();
        media.setId(5L);
        when(mediaMapper.selectByIdForUpdate(5L)).thenReturn(media);
        when(reportMapper.existsActiveByTarget("MEDIA", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOfSatisfying(BizException.class, exception ->
                        assertThat(exception.getBizCode()).isEqualTo(BizCode.MEDIA_ACTIVE_REPORT_EXISTS)
                );

        verify(mediaMapper, never()).deleteById(anyLong());
        verifyNoInteractions(
                mediaLikeMapper, mediaFavoriteMapper, mediaDownloadMapper,
                browseHistoryMapper, topicMediaMapper, favoriteFolderMapper,
                submissionAssetMapper, timeComparisonItemMapper, timeComparisonMapper,
                fileStorage
        );
    }
}
