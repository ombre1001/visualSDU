package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.ReportReasonType;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReportReasonTypeMapper extends BaseMapper<ReportReasonType> {
    List<ReportReasonType> selectEnabled();

    ReportReasonType selectEnabledByCode(@Param("code") String code);
}
