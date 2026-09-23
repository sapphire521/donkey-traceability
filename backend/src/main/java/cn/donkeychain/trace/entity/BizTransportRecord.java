package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_transport_record")
public class BizTransportRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private LocalDateTime recordTime;
    private BigDecimal temperature;
    private Integer humidity;
    private Integer abnormal;
}
