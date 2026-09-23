package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("biz_transport_order")
public class BizTransportOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String transportNo;
    private String batchNo;
    private Long fromOrgId;
    private String fromOrgName;
    private Long toOrgId;
    private String toOrgName;
    private String vehicleNo;
    private String driverName;
    private String driverPhone;
    private LocalDateTime departTime;
    private LocalDateTime expectArriveTime;
    private LocalDateTime actualArriveTime;
    private String status;
    private String fileUri;
    private String fileHash;
    private String txId;
    private LocalDateTime createTime;
}
