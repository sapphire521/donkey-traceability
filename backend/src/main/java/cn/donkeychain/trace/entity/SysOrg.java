package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_org")
public class SysOrg {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orgCode;
    private String orgName;
    private String orgType;
    private String mspId;
    private String contact;
    private String phone;
    private String region;
    private Integer status;
    private LocalDateTime createTime;
}
