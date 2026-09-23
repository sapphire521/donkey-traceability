package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("biz_cert")
public class BizCert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orgId;
    private String orgName;
    private String certType;
    private String certNo;
    private LocalDate issueDate;
    private LocalDate expireDate;
    private String fileUri;
    private String fileHash;
    private String auditStatus;
    private Integer status;
}
