package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.AdminCreateLocationRequest;
import cn.sduonline.business.data.po.Campus;
import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.po.LocationCategory;
import cn.sduonline.business.mapper.CampusMapper;
import cn.sduonline.business.mapper.LocationCategoryMapper;
import cn.sduonline.business.mapper.LocationMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminLocationServiceTest {

    @Mock private LocationMapper locationMapper;
    @Mock private CampusMapper campusMapper;
    @Mock private LocationCategoryMapper locationCategoryMapper;
    @InjectMocks private AdminLocationService service;

    @Test
    void createShouldPersistCoverKey() {
        stubEnabledCampusAndCategory();
        AdminCreateLocationRequest request = request("locations/admin/cover.jpg");

        var result = service.create(request);

        assertThat(result.coverKey()).isEqualTo("locations/admin/cover.jpg");
        verify(locationMapper).insert(argThat((Location location) ->
                "BUILDING".equals(location.getCategoryCode())
                        && "locations/admin/cover.jpg".equals(location.getCoverKey())
        ));
    }

    @Test
    void createShouldUseTemporaryDefaultCoverKeyWhenNotProvided() {
        stubEnabledCampusAndCategory();

        var result = service.create(request(null));

        assertThat(result.coverKey()).isEqualTo("avatars/default.png");
        verify(locationMapper).insert(argThat((Location location) ->
                "avatars/default.png".equals(location.getCoverKey())
        ));
    }

    @Test
    void createShouldRejectUrlAsCoverKey() {
        stubEnabledCampusAndCategory();

        assertThatThrownBy(() -> service.create(request("https://example.com/cover.jpg")))
                .isInstanceOfSatisfying(BizException.class, exception ->
                        assertThat(exception.getBizCode()).isEqualTo(BizCode.BAD_REQUEST)
                );

        verify(locationMapper, never()).insert(any(Location.class));
    }

    @Test
    void createShouldRejectDisabledOrMissingCategory() {
        Campus campus = new Campus();
        campus.setId(1L);
        campus.setStatus(1);
        when(campusMapper.selectOne(any())).thenReturn(campus);
        when(locationCategoryMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.create(request(null)))
                .isInstanceOfSatisfying(BizException.class, exception ->
                        assertThat(exception.getBizCode()).isEqualTo(BizCode.ADMIN_LOCATION_CATEGORY_INVALID)
                );

        verify(locationMapper, never()).insert(any(Location.class));
    }

    private void stubEnabledCampusAndCategory() {
        Campus campus = new Campus();
        campus.setId(1L);
        campus.setStatus(1);
        when(campusMapper.selectOne(any())).thenReturn(campus);

        LocationCategory category = new LocationCategory();
        category.setCode("BUILDING");
        category.setStatus(1);
        when(locationCategoryMapper.selectOne(any())).thenReturn(category);
    }

    private AdminCreateLocationRequest request(String coverKey) {
        return new AdminCreateLocationRequest(
                1L,
                "知新楼",
                "BUILDING",
                "中心校区",
                new BigDecimal("117.0618"),
                new BigDecimal("36.6745"),
                coverKey,
                "教学建筑",
                0,
                1
        );
    }
}
