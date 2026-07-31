package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Campus;
import cn.sduonline.business.data.po.City;
import cn.sduonline.business.data.po.Location;
import cn.sduonline.business.data.vo.CampusDetailVO;
import cn.sduonline.business.data.vo.CampusListVO;
import cn.sduonline.business.mapper.CampusMapper;
import cn.sduonline.business.mapper.CityMapper;
import cn.sduonline.business.mapper.LocationMapper;
import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.exception.BizException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampusService {

    private static final int ENABLED = 1;

    private final CityMapper cityMapper;
    private final CampusMapper campusMapper;
    private final LocationMapper locationMapper;

    /**
     * 查询指定城市下的校区。
     */
    public List<CampusListVO> listByCityId(Long cityId) {
        requireEnabledCity(cityId);

        List<Campus> campuses = campusMapper.selectList(
                new LambdaQueryWrapper<Campus>()
                        .eq(Campus::getCityId, cityId)
                        .eq(Campus::getStatus, ENABLED)
                        .orderByAsc(Campus::getSortOrder)
                        .orderByAsc(Campus::getId)
        );

        return campuses.stream()
                .map(this::convertToListVO)
                .toList();
    }

    /**
     * 查询校区详情。
     */
    public CampusDetailVO getDetail(Long campusId) {
        Campus campus = requireEnabledCampus(campusId);
        City city = requireEnabledCity(campus.getCityId());

        Long locationCount = locationMapper.selectCount(
                new LambdaQueryWrapper<Location>()
                        .eq(Location::getCampusId, campusId)
                        .eq(Location::getStatus, ENABLED)
        );

        return CampusDetailVO.builder()
                .id(campus.getId())
                .cityId(city.getId())
                .cityName(city.getName())
                .name(campus.getName())
                .shortName(campus.getShortName())
                .address(campus.getAddress())
                .longitude(campus.getLongitude())
                .latitude(campus.getLatitude())
                .coverUrl(campus.getCoverUrl())
                .description(campus.getDescription())
                .locationCount(locationCount)
                .build();
    }

    private City requireEnabledCity(Long cityId) {
        if (cityId == null || cityId <= 0) {
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

        return city;
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

    private CampusListVO convertToListVO(Campus campus) {
        return CampusListVO.builder()
                .id(campus.getId())
                .cityId(campus.getCityId())
                .name(campus.getName())
                .shortName(campus.getShortName())
                .address(campus.getAddress())
                .longitude(campus.getLongitude())
                .latitude(campus.getLatitude())
                .coverUrl(campus.getCoverUrl())
                .build();
    }
}
