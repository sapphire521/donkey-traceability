package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_batch_event")
public class BizBatchEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchNo;
    private String eventType;
    private String orgName;
    private String operator;
    private LocalDateTime eventTime;
    private String summary;
    private String txId;
    private Integer blockNo;
}
