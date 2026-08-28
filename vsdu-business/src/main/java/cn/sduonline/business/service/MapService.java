package cn.sduonline.business.service;

import cn.sduonline.business.data.dto.MapMarkerQueryDTO;
import cn.sduonline.business.data.po.Campus;
import cn.sduonline.business.data.po.City;
import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.vo.MapMarkerVO;
import cn.sduonline.business.mapper.CampusMapper;
import cn.sduonline.business.mapper.CityMapper;
import cn.sduonline.business.mapper.LocationMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import cn.sduonline.infrastructure.file.storage.FileStorage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MapService {

    private static final int ENABLED = 1;
    private static final String CAMPUS_MARKER = "CAMPUS";
    private static final String LOCATION_MARKER = "LOCATION";

    private final CityMapper cityMapper;
    private final CampusMapper campusMapper;
    private final LocationMapper locationMapper;
    private final FileStorage fileStorage;

    /**
     * cityId不为空：查询该城市下的校区点位。
     * campusId不为空：查询该校区下的地点点位。
     */
    public List<MapMarkerVO> listMarkers(MapMarkerQueryDTO queryDTO) {
        Long cityId = queryDTO.getCityId();
        Long campusId = queryDTO.getCampusId();

        boolean hasCityId = cityId != null;
        boolean hasCampusId = campusId != null;

        if (hasCityId == hasCampusId) {
            throw new BizException(BizCode.MAP_QUERY_SCOPE_INVALID);
        }

        if (hasCityId) {
            return listCampusMarkers(cityId);
        }

        return listLocationMarkers(campusId);
    }

    private List<MapMarkerVO> listCampusMarkers(Long cityId) {
        if (cityId <= 0) {
            throw new BizException(BizCode.CITY_NOT_FOUND);
        }

        City city = cityMapper.selectOne(
                new LambdaQueryWrapper<City>()
                        .eq(City::getId, cityId)
                        .eq(City::getStatus, ENABLED)
        );

        if (city == null) {
            throw new BizException(BizCode.CITY_NOT_FOUND);
        }

        return campusMapper.selectList(
                        new LambdaQueryWrapper<Campus>()
                                .eq(Campus::getCityId, cityId)
                                .eq(Campus::getStatus, ENABLED)
                                .orderByAsc(Campus::getSortOrder)
                                .orderByAsc(Campus::getId)
                )
                .stream()
                .map(campus -> MapMarkerVO.builder()
                        .id(campus.getId())
                        .markerType(CAMPUS_MARKER)
                        .campusId(campus.getId())
                        .name(campus.getName())
                        .longitude(campus.getLongitude())
                        .latitude(campus.getLatitude())
                        .coverUrl(campus.getCoverUrl())
                        .build())
                .toList();
    }

    private List<MapMarkerVO> listLocationMarkers(Long campusId) {
        if (campusId <= 0) {
            throw new BizException(BizCode.CAMPUS_NOT_FOUND);
        }

        Campus campus = campusMapper.selectOne(
                new LambdaQueryWrapper<Campus>()
                        .eq(Campus::getId, campusId)
                        .eq(Campus::getStatus, ENABLED)
        );

        if (campus == null) {
            throw new BizException(BizCode.CAMPUS_NOT_FOUND);
        }

        return locationMapper.selectList(
                        new LambdaQueryWrapper<Location>()
                                .eq(Location::getCampusId, campusId)
                                .eq(Location::getStatus, ENABLED)
                                .orderByAsc(Location::getSortOrder)
                                .orderByAsc(Location::getId)
                )
                .stream()
                .map(location -> MapMarkerVO.builder()
                        .id(location.getId())
                        .markerType(LOCATION_MARKER)
                        .campusId(location.getCampusId())
                        .name(location.getName())
                        .longitude(location.getLongitude())
                        .latitude(location.getLatitude())
                        .coverUrl(url(location.getCoverKey()))
                        .build())
                .toList();
    }

    private String url(String coverKey) {
        return coverKey == null || coverKey.isBlank() ? null : fileStorage.getUrl(coverKey);
    }
}
