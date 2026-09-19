package cn.sduonline.business.service;

import cn.sduonline.business.data.po.MediaTag;
import cn.sduonline.business.data.po.SubmissionTag;
import cn.sduonline.business.data.po.Tag;
import cn.sduonline.business.mapper.MediaTagMapper;
import cn.sduonline.business.mapper.SubmissionTagMapper;
import cn.sduonline.business.mapper.TagMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagRelationServiceTest {
    @Mock
    private TagMapper tagMapper;
    @Mock
    private SubmissionTagMapper submissionTagMapper;
    @Mock
    private MediaTagMapper mediaTagMapper;
    @InjectMocks
    private TagRelationService service;

    @Test
    void replaceSubmissionTagsShouldKeepRequestOrder() {
        when(tagMapper.selectByIds(any())).thenReturn(List.of(tag(2L, "校园"), tag(1L, "建筑")));

        service.replaceSubmissionTags(10L, List.of(1L, 2L));

        verify(submissionTagMapper).deleteBySubmissionId(10L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SubmissionTag>> captor = ArgumentCaptor.forClass(List.class);
        verify(submissionTagMapper).insertBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(SubmissionTag::getTagId, SubmissionTag::getSortOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, 0),
                        org.assertj.core.groups.Tuple.tuple(2L, 1)
                );
    }

    @Test
    void resolveTagsShouldRejectDuplicateIds() {
        assertThatThrownBy(() -> service.resolveTags(List.of(1L, 1L)))
                .isInstanceOfSatisfying(BizException.class, exception ->
                        assertThat(exception.getBizCode()).isEqualTo(BizCode.BAD_REQUEST));

        verifyNoInteractions(tagMapper, submissionTagMapper, mediaTagMapper);
    }

    @Test
    void copySubmissionTagsToMediaShouldKeepStoredOrder() {
        SubmissionTag first = submissionTag(10L, 2L, 0);
        SubmissionTag second = submissionTag(10L, 1L, 1);
        when(submissionTagMapper.selectBySubmissionId(10L)).thenReturn(List.of(first, second));

        service.copySubmissionTagsToMedia(10L, 20L);

        verify(mediaTagMapper).deleteByMediaId(20L);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MediaTag>> captor = ArgumentCaptor.forClass(List.class);
        verify(mediaTagMapper).insertBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(MediaTag::getMediaId, MediaTag::getTagId, MediaTag::getSortOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(20L, 2L, 0),
                        org.assertj.core.groups.Tuple.tuple(20L, 1L, 1)
                );
    }

    @Test
    void mergeTagShouldKeepEarliestPositionAndRebuildContinuousOrder() {
        when(submissionTagMapper.selectSubmissionIdsByTagId(1L)).thenReturn(List.of(10L));
        when(submissionTagMapper.selectBySubmissionIdsForUpdate(List.of(10L))).thenReturn(List.of(
                submissionTag(10L, 1L, 0),
                submissionTag(10L, 3L, 1),
                submissionTag(10L, 2L, 2)
        ));
        when(mediaTagMapper.selectMediaIdsByTagId(1L)).thenReturn(List.of(20L));
        when(mediaTagMapper.selectByMediaIdsForUpdate(List.of(20L))).thenReturn(List.of(
                mediaTag(20L, 3L, 0),
                mediaTag(20L, 2L, 1),
                mediaTag(20L, 1L, 2)
        ));

        service.mergeTag(1L, 2L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SubmissionTag>> submissionCaptor = ArgumentCaptor.forClass(List.class);
        verify(submissionTagMapper).deleteBySubmissionIds(List.of(10L));
        verify(submissionTagMapper).insertBatch(submissionCaptor.capture());
        assertThat(submissionCaptor.getValue())
                .extracting(SubmissionTag::getTagId, SubmissionTag::getSortOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, 0),
                        org.assertj.core.groups.Tuple.tuple(3L, 1)
                );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MediaTag>> mediaCaptor = ArgumentCaptor.forClass(List.class);
        verify(mediaTagMapper).deleteByMediaIds(List.of(20L));
        verify(mediaTagMapper).insertBatch(mediaCaptor.capture());
        assertThat(mediaCaptor.getValue())
                .extracting(MediaTag::getTagId, MediaTag::getSortOrder)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(3L, 0),
                        org.assertj.core.groups.Tuple.tuple(2L, 1)
                );
    }

    @Test
    void listMediaTagsShouldLoadRelationsAndTagsInBatches() {
        MediaTag first = mediaTag(10L, 2L, 0);
        MediaTag second = mediaTag(10L, 1L, 1);
        MediaTag third = mediaTag(20L, 1L, 0);
        when(mediaTagMapper.selectByMediaIds(any())).thenReturn(List.of(first, second, third));
        when(tagMapper.selectByIds(any())).thenReturn(List.of(tag(1L, "建筑"), tag(2L, "晚霞")));

        Map<Long, List<Tag>> result = service.listMediaTags(List.of(10L, 20L));

        assertThat(result.get(10L)).extracting(Tag::getId).containsExactly(2L, 1L);
        assertThat(result.get(20L)).extracting(Tag::getId).containsExactly(1L);
        verify(mediaTagMapper, times(1)).selectByMediaIds(any());
        verify(tagMapper, times(1)).selectByIds(any());
    }

    @Test
    void firstMediaTagShouldUseZeroSortOrderQuery() {
        when(mediaTagMapper.selectFirstTagId(10L)).thenReturn(2L);

        assertThat(service.firstMediaTagId(10L)).isEqualTo(2L);

        verify(mediaTagMapper).selectFirstTagId(10L);
        verify(mediaTagMapper, never()).selectByMediaId(anyLong());
    }

    private Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }

    private SubmissionTag submissionTag(Long submissionId, Long tagId, int sortOrder) {
        SubmissionTag relation = new SubmissionTag();
        relation.setSubmissionId(submissionId);
        relation.setTagId(tagId);
        relation.setSortOrder(sortOrder);
        return relation;
    }

    private MediaTag mediaTag(Long mediaId, Long tagId, int sortOrder) {
        MediaTag relation = new MediaTag();
        relation.setMediaId(mediaId);
        relation.setTagId(tagId);
        relation.setSortOrder(sortOrder);
        return relation;
    }
}
