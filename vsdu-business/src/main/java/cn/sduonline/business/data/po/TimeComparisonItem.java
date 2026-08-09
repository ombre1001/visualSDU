package cn.sduonline.business.data.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("time_comparison_item")
public class TimeComparisonItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long comparisonId;
    private Long mediaId;
    private String label;
    private Integer sortOrder;
}
