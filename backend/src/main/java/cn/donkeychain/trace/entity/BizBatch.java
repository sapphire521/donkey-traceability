package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_batch")
public class BizBatch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String batchNo;
    private String batchType;
    private String parentNos;
    private String sourceEarTags;
    private Long orgId;
    private String orgName;
    private Long holderOrgId;
    private String holderOrgName;
    private String productName;
    private BigDecimal weightKg;
    private LocalDate produceDate;
    private LocalDate expireDate;
    private String certHash;
    private String status;
    private String createTxid;
    private String qcReportUri;
    private LocalDateTime createTime;
}
