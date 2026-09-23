package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_output_record")
public class BizOutputRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchNo;
    private Long shopOrgId;
    private String shopOrgName;
    private LocalDateTime outputTime;
    private String ovenNo;
    private String chef;
    private Integer qty;
    private BigDecimal usedWeight;
    private LocalDateTime createTime;
}
