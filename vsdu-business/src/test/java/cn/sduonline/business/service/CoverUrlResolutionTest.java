package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.MapMarkerQueryDTO;
import cn.sduonline.business.data.po.Campus;
import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.projection.CitySummaryRow;
import cn.sduonline.business.mapper.CampusMapper;
import cn.sduonline.business.mapper.CityMapper;
import cn.sduonline.business.mapper.LocationMapper;
import cn.sduonline.business.mapper.MediaMapper;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CoverUrlResolutionTest {

    @Test
    void cityListShouldResolveCoverKeyToUrl() {
        CityMapper cityMapper = mock(CityMapper.class);
        FileStorage fileStorage = mock(FileStorage.class);
        CitySummaryRow row = new CitySummaryRow();
        row.setCoverKey("cities/jinan.jpg");
        when(cityMapper.selectEnabledSummaries()).thenReturn(List.of(row));
        when(fileStorage.getUrl("cities/jinan.jpg")).thenReturn("https://signed.example/city");

        CityService service = new CityService(cityMapper, fileStorage);

        assertThat(service.listCities()).singleElement()
                .extracting("coverUrl")
                .isEqualTo("https://signed.example/city");
    }

    @Test
    void locationListShouldResolveCoverKeyToUrl() {
        CityMapper cityMapper = mock(CityMapper.class);
        CampusMapper campusMapper = mock(CampusMapper.class);
        LocationMapper locationMapper = mock(LocationMapper.class);
        MediaMapper mediaMapper = mock(MediaMapper.class);
        MediaService mediaService = mock(MediaService.class);
        FileStorage fileStorage = mock(FileStorage.class);

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setStatus(1);
        Location location = new Location();
        location.setId(2L);
        location.setCampusId(1L);
        location.setCoverKey("locations/zhixin.jpg");
        when(campusMapper.selectOne(any())).thenReturn(campus);
        when(locationMapper.selectList(any())).thenReturn(List.of(location));
        when(fileStorage.getUrl("locations/zhixin.jpg")).thenReturn("https://signed.example/location");

        LocationService service = new LocationService(
                cityMapper, campusMapper, locationMapper, mediaMapper, mediaService, fileStorage
        );

        assertThat(service.listByCampusId(1L, null)).singleElement()
                .extracting("coverUrl")
                .isEqualTo("https://signed.example/location");
    }

    @Test
    void locationMarkerShouldResolveCoverKeyToUrl() {
        CityMapper cityMapper = mock(CityMapper.class);
        CampusMapper campusMapper = mock(CampusMapper.class);
        LocationMapper locationMapper = mock(LocationMapper.class);
        FileStorage fileStorage = mock(FileStorage.class);

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setStatus(1);
        Location location = new Location();
        location.setId(2L);
        location.setCampusId(1L);
        location.setCoverKey("locations/marker.jpg");
        when(campusMapper.selectOne(any())).thenReturn(campus);
        when(locationMapper.selectList(any())).thenReturn(List.of(location));
        when(fileStorage.getUrl("locations/marker.jpg")).thenReturn("https://signed.example/marker");

        MapService service = new MapService(cityMapper, campusMapper, locationMapper, fileStorage);
        MapMarkerQueryDTO query = new MapMarkerQueryDTO();
        query.setCampusId(1L);

        assertThat(service.listMarkers(query)).singleElement()
                .extracting("coverUrl")
                .isEqualTo("https://signed.example/marker");
    }
}
