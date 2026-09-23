package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_complaint")
public class BizComplaint {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String category;
    private String content;
    private String phone;
    private String status;
    private LocalDateTime createTime;
}
