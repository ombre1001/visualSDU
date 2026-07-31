package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Campus;
import cn.sduonline.business.data.po.City;
import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.vo.LocationDetailVO;
import cn.sduonline.business.data.vo.LocationListVO;
import cn.sduonline.business.mapper.CampusMapper;
import cn.sduonline.business.mapper.CityMapper;
import cn.sduonline.business.mapper.LocationMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private static final int ENABLED = 1;

    private final CityMapper cityMapper;
    private final CampusMapper campusMapper;
    private final LocationMapper locationMapper;

    /**
     * 查询指定校区下的地点。
     */
    public List<LocationListVO> listByCampusId(
            Long campusId,
            String categoryCode
    ) {
        requireEnabledCampus(campusId);

        LambdaQueryWrapper<Location> queryWrapper =
                new LambdaQueryWrapper<Location>()
                        .eq(Location::getCampusId, campusId)
                        .eq(Location::getStatus, ENABLED)
                        .eq(
                                StringUtils.hasText(categoryCode),
                                Location::getCategoryCode,
                                categoryCode
                        )
                        .orderByAsc(Location::getSortOrder)
                        .orderByAsc(Location::getId);

        return locationMapper.selectList(queryWrapper)
                .stream()
                .map(this::convertToListVO)
                .toList();
    }

    /**
     * 查询地点详情。
     */
    public LocationDetailVO getDetail(Long locationId) {
        Location location = requireEnabledLocation(locationId);
        Campus campus = requireEnabledCampus(location.getCampusId());
        City city = requireEnabledCity(campus.getCityId());

        return LocationDetailVO.builder()
                .id(location.getId())
                .campusId(campus.getId())
                .campusName(campus.getName())
                .cityId(city.getId())
                .cityName(city.getName())
                .name(location.getName())
                .categoryCode(location.getCategoryCode())
                .address(location.getAddress())
                .longitude(location.getLongitude())
                .latitude(location.getLatitude())
                .coverUrl(location.getCoverUrl())
                .description(location.getDescription())
                .build();
    }

    private Location requireEnabledLocation(Long locationId) {
        if (locationId == null || locationId <= 0) {
            throw new BizException(BizCode.LOCATION_NOT_FOUND);
        }

        Location location = locationMapper.selectOne(
                new LambdaQueryWrapper<Location>()
                        .eq(Location::getId, locationId)
                        .eq(Location::getStatus, ENABLED)
        );

        if (location == null) {
            throw new BizException(BizCode.LOCATION_NOT_FOUND);
        }

        return location;
    }

    private Campus requireEnabledCampus(Long campusId) {
        if (campusId == null || campusId <= 0) {
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

        return campus;
    }

    private City requireEnabledCity(Long cityId) {
        City city = cityMapper.selectOne(
                new LambdaQueryWrapper<City>()
                        .eq(City::getId, cityId)
                        .eq(City::getStatus, ENABLED)
        );

        if (city == null) {
            throw new BizException(BizCode.CITY_NOT_FOUND);
        }

        return city;
    }

    private LocationListVO convertToListVO(Location location) {
        return LocationListVO.builder()
                .id(location.getId())
                .campusId(location.getCampusId())
                .name(location.getName())
                .categoryCode(location.getCategoryCode())
                .address(location.getAddress())
                .longitude(location.getLongitude())
                .latitude(location.getLatitude())
                .coverUrl(location.getCoverUrl())
                .build();
    }
}