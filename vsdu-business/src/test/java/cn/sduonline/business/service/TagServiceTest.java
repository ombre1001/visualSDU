package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Tag;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {
    @Mock
    private TagMapper tagMapper;
    @InjectMocks
    private TagService service;

    @Test
    void listShouldSupportPopularSortAndPagination() {
        when(tagMapper.countPublicTags("建筑")).thenReturn(2L);
        when(tagMapper.selectPublicTagPage("建筑", "popular", 0, 50))
                .thenReturn(List.of(tag(1L, "建筑"), tag(2L, "古建筑")));

        var result = service.list(" 建筑 ", "POPULAR", 1, 50);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.items()).extracting("id", "name")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, "建筑"),
                        org.assertj.core.groups.Tuple.tuple(2L, "古建筑")
                );
    }

    @Test
    void lookupShouldPreserveRequestedOrder() {
        when(tagMapper.selectByIds(any())).thenReturn(List.of(tag(1L, "建筑"), tag(2L, "晚霞")));

        assertThat(service.lookup(List.of(2L, 1L))).extracting("id")
                .containsExactly(2L, 1L);
    }

    @Test
    void lookupShouldRejectMissingTag() {
        when(tagMapper.selectByIds(any())).thenReturn(List.of(tag(1L, "建筑")));

        assertThatThrownBy(() -> service.lookup(List.of(1L, 2L)))
                .isInstanceOfSatisfying(BizException.class, exception ->
                        assertThat(exception.getBizCode()).isEqualTo(BizCode.TAG_NOT_FOUND));
    }

    private Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }
}
