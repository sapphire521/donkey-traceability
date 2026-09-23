package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_donkey_event")
public class BizDonkeyEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long donkeyId;
    private String earTagId;
    private String eventType;
    private LocalDateTime eventTime;
    private String content;
    private String detailHash;
    private String txId;
    private Integer blockNo;
}
