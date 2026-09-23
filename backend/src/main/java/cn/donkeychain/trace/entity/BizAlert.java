package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_alert")
public class BizAlert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String alertType;
    private String level;
    private String targetType;
    private String targetId;
    private String content;
    private String status;
    private String handler;
    private String handleNote;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;
}
