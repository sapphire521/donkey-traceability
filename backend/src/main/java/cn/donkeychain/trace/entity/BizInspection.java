package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_inspection")
public class BizInspection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchNo;
    private String orgName;
    private String agency;
    private String items;
    private String result;
    private String reportUri;
    private String reportHash;
    private String txId;
    private LocalDateTime createTime;
}
