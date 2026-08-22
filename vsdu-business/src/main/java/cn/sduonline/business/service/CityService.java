package cn.sduonline.business.service;

import cn.sduonline.business.data.projection.CitySummaryRow;
import cn.sduonline.business.data.vo.CityVO;
import cn.sduonline.business.mapper.CityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityMapper cityMapper;

    /**
     * 查询所有启用的城市。
     */
    public List<CityVO> listCities() {
        return cityMapper.selectEnabledSummaries().stream()
                .map(this::convertToVO)
                .toList();
    }

    private CityVO convertToVO(CitySummaryRow city) {
        return CityVO.builder()
                .id(city.getId())
                .name(city.getName())
                .code(city.getCode())
                .province(city.getProvince())
                .coverUrl(city.getCoverUrl())
                .description(city.getDescription())
                .campusCount(city.getCampusCount())
                .build();
    }
}
