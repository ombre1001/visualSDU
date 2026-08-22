package cn.sduonline.business.mapper;

import cn.sduonline.business.data.projection.CitySummaryRow;
import cn.sduonline.business.data.po.City;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CityMapper extends BaseMapper<City> {

    List<CitySummaryRow> selectEnabledSummaries();
}
