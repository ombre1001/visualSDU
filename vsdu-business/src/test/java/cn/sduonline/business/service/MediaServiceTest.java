package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Tag;
import cn.sduonline.business.data.projection.MediaSummaryRow;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {
    @Mock private FileStorage fileStorage;
    @Mock private TagRelationService tagRelationService;
    @InjectMocks private MediaService service;

    @Test
    void summariesShouldLoadTagsOnceAndReturnTagIds() {
        MediaSummaryRow first = row(10L, "第一张");
        MediaSummaryRow second = row(20L, "第二张");
        when(tagRelationService.listMediaTags(List.of(10L, 20L))).thenReturn(Map.of(
                10L, List.of(tag(2L, "晚霞"), tag(1L, "建筑")),
                20L, List.of(tag(1L, "建筑"))
        ));
        when(fileStorage.getUrl("thumb-10")).thenReturn("url-10");
        when(fileStorage.getUrl("thumb-20")).thenReturn("url-20");

        var result = service.toSummaries(List.of(first, second));

        assertThat(result.getFirst().tags()).extracting("id", "name")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2L, "晚霞"),
                        org.assertj.core.groups.Tuple.tuple(1L, "建筑")
                );
        assertThat(result.getLast().tags()).extracting("id").containsExactly(1L);
        verify(tagRelationService).listMediaTags(List.of(10L, 20L));
    }

    private MediaSummaryRow row(Long id, String title) {
        MediaSummaryRow row = new MediaSummaryRow();
        row.setId(id);
        row.setTitle(title);
        row.setLocationId(1L);
        row.setLocationName("中心校区");
        row.setThumbnailKey("thumb-" + id);
        return row;
    }

    private Tag tag(Long id, String name) {
        Tag tag = new Tag();
        tag.setId(id);
        tag.setName(name);
        return tag;
    }
}
