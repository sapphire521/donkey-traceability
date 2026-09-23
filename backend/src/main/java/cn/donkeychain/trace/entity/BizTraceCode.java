package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_trace_code")
public class BizTraceCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String batchNo;
    private String shopOrgName;
    private Long outputId;
    private String status;
    private String bindTxid;
    private Integer printCount;
    private Integer scanTimes;
    private LocalDateTime createTime;
}
