package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("biz_donkey")
public class BizDonkey {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String earTagId;
    private String breed;
    private String gender;
    private LocalDate birthDate;
    private Long orgId;
    private String orgName;
    private String barnNo;
    private String status;
    private String createTxid;
    private String photos;
    private LocalDateTime createTime;
}
