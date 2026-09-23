package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_recall")
public class BizRecall {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchNo;
    private String reason;
    private String scopeJson;
    private String status;
    private String initiator;
    private LocalDateTime createTime;
}
