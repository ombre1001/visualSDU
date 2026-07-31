package cn.sduonline.business.service;

import cn.sduonline.business.data.po.Campus;
import cn.sduonline.business.data.po.City;
import cn.sduonline.business.data.vo.CityVO;
import cn.sduonline.business.mapper.CampusMapper;
import cn.sduonline.business.mapper.CityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private static final int ENABLED = 1;

    private final CityMapper cityMapper;
    private final CampusMapper campusMapper;

    /**
     * 查询所有启用的城市。
     */
    public List<CityVO> listCities() {
        List<City> cities = cityMapper.selectList(
                new LambdaQueryWrapper<City>()
                        .eq(City::getStatus, ENABLED)
                        .orderByAsc(City::getSortOrder)
                        .orderByAsc(City::getId)
        );

        return cities.stream()
                .map(this::convertToVO)
                .toList();
    }

    private CityVO convertToVO(City city) {
        Long campusCount = campusMapper.selectCount(
                new LambdaQueryWrapper<Campus>()
                        .eq(Campus::getCityId, city.getId())
                        .eq(Campus::getStatus, ENABLED)
        );

        return CityVO.builder()
                .id(city.getId())
                .name(city.getName())
                .code(city.getCode())
                .province(city.getProvince())
                .coverUrl(city.getCoverUrl())
                .description(city.getDescription())
                .campusCount(campusCount)
                .build();
    }
}
