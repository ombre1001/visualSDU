package cn.sduonline.business.mapper;

import cn.sduonline.business.data.po.ReportOperationLog;
import cn.sduonline.business.data.projection.AdminReportOperationLogRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReportOperationLogMapper extends BaseMapper<ReportOperationLog> {
    List<AdminReportOperationLogRow> selectByReport(
            @Param("reportId") Long reportId,
            @Param("limit") int limit
    );
}
