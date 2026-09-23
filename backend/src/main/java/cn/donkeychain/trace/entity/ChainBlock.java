package cn.donkeychain.trace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chain_block")
public class ChainBlock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer blockNo;
    private String prevHash;
    private String hash;
    private LocalDateTime time;
    private Integer txCount;
    private String channel;
    private String txId;
    private String op;
    private String mspId;
    private String orgName;
    private String payload;
}
