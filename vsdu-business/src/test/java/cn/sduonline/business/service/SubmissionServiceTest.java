package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.CreateSubmissionRequest;
import cn.sduonline.business.data.dto.UpdateSubmissionRequest;
import cn.sduonline.business.data.enums.ImageScene;
import cn.sduonline.business.data.enums.SubmissionStatus;
import cn.sduonline.business.data.enums.UserStatus;
import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.po.Submission;
import cn.sduonline.business.data.po.User;
import cn.sduonline.business.mapper.LocationMapper;
import cn.sduonline.business.mapper.SubmissionAssetMapper;
import cn.sduonline.business.mapper.SubmissionMapper;
import cn.sduonline.business.mapper.UserMapper;
import cn.sduonline.infrastructure.file.image.ImageFileUpload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {
    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private SubmissionAssetMapper assetMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private LocationMapper locationMapper;
    @Mock
    private TagRelationService tagRelationService;
    @Mock
    private SubmissionReviewSettingService reviewSettingService;
    @Mock
    private ImageFileUpload imageFileUpload;
    @InjectMocks
    private SubmissionService service;

    private Location location;

    @BeforeEach
    void setUp() {
        User uploader = new User();
        uploader.setId(5L);
        uploader.setCasId("20260001");
        uploader.setStatus(UserStatus.NORMAL);
        uploader.setAllowUpload(true);
        uploader.setDeleted(false);

        location = new Location();
        location.setId(8L);
        location.setName("中心校区");
        location.setStatus(1);

        when(userMapper.selectById(5L)).thenReturn(uploader);
    }

    @Test
    void createShouldStoreSubmissionTagsInRequestOrder() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(1024L);
        when(locationMapper.selectOne(any())).thenReturn(location);
        when(imageFileUpload.uploadImageFile(ImageScene.SUBMISSION, 5L, file)).thenReturn("submission/5/photo.jpg");
        when(reviewSettingService.isReviewEnabled()).thenReturn(true);
        when(assetMapper.selectList(any())).thenReturn(List.of());
        when(tagRelationService.listSubmissionTagNames(10L)).thenReturn(List.of("建筑", "晚霞"));
        doAnswer(invocation -> {
            Submission submission = invocation.getArgument(0);
            submission.setId(10L);
            return 1;
        }).when(submissionMapper).insert(any(Submission.class));

        CreateSubmissionRequest request = new CreateSubmissionRequest();
        request.setFiles(List.of(file));
        request.setLocationId(8L);
        request.setTagIds(List.of(1L, 2L));
        request.setCopyrightConfirmed(true);

        var result = service.create(5L, request);

        verify(tagRelationService).replaceSubmissionTags(10L, List.of(1L, 2L));
        assertThat(result.tags()).containsExactly("建筑", "晚霞");
    }

    @Test
    void updateShouldKeepTagsWhenTagIdsAreAbsent() {
        stubEditableSubmission();
        UpdateSubmissionRequest request = new UpdateSubmissionRequest();

        service.update(5L, 10L, request);

        verify(tagRelationService, never()).replaceSubmissionTags(anyLong(), anyList());
    }

    @Test
    void updateShouldClearTagsWhenTagIdsAreEmpty() {
        stubEditableSubmission();
        UpdateSubmissionRequest request = new UpdateSubmissionRequest();
        request.setTagIds(List.of());

        service.update(5L, 10L, request);

        verify(tagRelationService).replaceSubmissionTags(10L, List.of());
    }

    private void stubEditableSubmission() {
        Submission submission = Submission.builder()
                .id(10L)
                .userId(5L)
                .locationId(8L)
                .status(SubmissionStatus.PENDING)
                .version(0)
                .deleted(false)
                .build();
        when(submissionMapper.selectById(10L)).thenReturn(submission);
        when(locationMapper.selectById(8L)).thenReturn(location);
        when(assetMapper.selectList(any())).thenReturn(List.of());
        when(submissionMapper.updateEditableWithVersion(any(Submission.class), eq(0), eq(0))).thenReturn(1);
        when(tagRelationService.listSubmissionTagNames(10L)).thenReturn(List.of());
    }
}
