package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Tag;
import cn.sduonline.business.mapper.MediaTagMapper;
import cn.sduonline.business.mapper.SubmissionTagMapper;
import cn.sduonline.business.mapper.TagMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTagServiceTest {
    @Mock
    private TagMapper tagMapper;
    @Mock
    private MediaTagMapper mediaTagMapper;
    @Mock
    private SubmissionTagMapper submissionTagMapper;
    @Mock
    private TagRelationService tagRelationService;
    @InjectMocks
    private AdminTagService service;

    @Test
    void updateShouldOnlyRenameTag() {
        Tag tag = tag(1L, "旧名称");
        when(tagMapper.selectById(1L)).thenReturn(tag);
        when(tagMapper.selectCount(any())).thenReturn(0L);
        when(mediaTagMapper.countByTagId(1L)).thenReturn(3L);

        assertThat(service.update(1L, " 新名称 ").name()).isEqualTo("新名称");

        verify(tagMapper).updateById(tag);
        verifyNoInteractions(tagRelationService, submissionTagMapper);
    }

    @Test
    void mergeShouldLockBothTagsThenMoveRelationsAndDeleteSource() {
        when(tagMapper.selectByIdsForUpdate(List.of(1L, 2L)))
                .thenReturn(List.of(tag(1L, "源"), tag(2L, "目标")));

        service.merge(2L, 1L);

        verify(tagRelationService).mergeTag(2L, 1L);
        verify(tagMapper).deleteById(2L);
    }

    @Test
    void deleteShouldRejectReferencedTagByDefault() {
        when(tagMapper.selectByIdsForUpdate(List.of(1L))).thenReturn(List.of(tag(1L, "建筑")));
        when(submissionTagMapper.countByTagId(1L)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L, false))
                .isInstanceOfSatisfying(BizException.class, exception ->
                        assertThat(exception.getBizCode()).isEqualTo(BizCode.ADMIN_TAG_IN_USE));

        verify(tagRelationService, never()).removeTag(anyLong());
        verify(tagMapper, never()).deleteById(anyLong());
    }

    @Test
    void forceDeleteShouldRemoveRelationsBeforeTag() {
        when(tagMapper.selectByIdsForUpdate(List.of(1L))).thenReturn(List.of(tag(1L, "建筑")));
        when(submissionTagMapper.countByTagId(1L)).thenReturn(0L);
        when(mediaTagMapper.countByTagId(1L)).thenReturn(2L);

        service.delete(1L, true);

        verify(tagRelationService).removeTag(1L);
        verify(tagMapper).deleteById(1L);
    }

    private Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }
}
