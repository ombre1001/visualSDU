package cn.sduonline.business.service;

import cn.sduonline.business.data.po.LocationCategory;
import cn.sduonline.business.mapper.LocationCategoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationCategoryServiceTest {

    @Mock private LocationCategoryMapper locationCategoryMapper;
    @InjectMocks private LocationCategoryService service;

    @Test
    void listEnabledOptionsShouldReturnCodeAndName() {
        LocationCategory category = new LocationCategory();
        category.setId(1L);
        category.setCode("BUILDING");
        category.setName("教学及办公建筑");

        when(locationCategoryMapper.selectList(any())).thenReturn(List.of(category));

        assertThat(service.listEnabledOptions())
                .singleElement()
                .satisfies(option -> {
                    assertThat(option.code()).isEqualTo("BUILDING");
                    assertThat(option.name()).isEqualTo("教学及办公建筑");
                });
    }
}
