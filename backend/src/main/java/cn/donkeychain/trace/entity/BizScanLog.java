package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_scan_log")
public class BizScanLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String ip;
    private String region;
    private String ua;
    private LocalDateTime scanTime;
    private Integer riskFlag;
}
