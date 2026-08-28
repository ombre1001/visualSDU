package cn.sduonline.business.service;

import cn.sduonline.business.data.po.LocationCategory;
import cn.sduonline.business.data.vo.LocationCategoryOptionVO;
import cn.sduonline.business.mapper.LocationCategoryMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationCategoryService {

    private static final int ENABLED = 1;

    private final LocationCategoryMapper locationCategoryMapper;

    public List<LocationCategoryOptionVO> listEnabledOptions() {
        return locationCategoryMapper.selectList(
                        new LambdaQueryWrapper<LocationCategory>()
                                .eq(LocationCategory::getStatus, ENABLED)
                                .orderByAsc(LocationCategory::getSortOrder)
                                .orderByAsc(LocationCategory::getId)
                ).stream()
                .map(category -> new LocationCategoryOptionVO(category.getCode(), category.getName()))
                .toList();
    }
}
