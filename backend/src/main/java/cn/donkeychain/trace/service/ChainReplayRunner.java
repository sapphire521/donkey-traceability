package cn.donkeychain.trace.service;

import cn.donkeychain.trace.entity.*;
import cn.donkeychain.trace.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 首次启动时, 若账本为空则将种子业务数据按时间顺序回放上链,
 * 与前端 Mock 版 chain.ts 的回放逻辑等价, 保证账本完整可校验。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainReplayRunner implements ApplicationRunner {

    private final ChainBlockMapper chainBlockMapper;
    private final SysOrgMapper orgMapper;
    private final BizDonkeyMapper donkeyMapper;
    private final BizDonkeyEventMapper donkeyEventMapper;
    private final BizBatchMapper batchMapper;
    private final BizBatchEventMapper batchEventMapper;
    private final BizTransportOrderMapper transportMapper;
    private final BizCertMapper certMapper;
    private final BizInspectionMapper inspectionMapper;
    private final BizTraceCodeMapper traceCodeMapper;
    private final ChainService chain;

    @Override
    public void run(ApplicationArguments args) {
        if (chainBlockMapper.selectCount(null) > 0) {
            log.info("链账本已存在, 跳过种子回放");
            return;
        }
        log.info("账本为空, 开始回放种子业务数据上链...");

        chain.appendTx("GENESIS", map(
                "channel", ChainService.CHANNEL,
                "orderer", "orderer.baoding-donkey.cn:7050",
                "orgs", orgMapper.selectList(null).stream().map(SysOrg::getMspId).distinct().toList(),
                "chaincode", "donkey-trace-cc v1.2"), "OrdererMSP", "驴链平台运营方", ts("2026-01-01 09:00:00"));

        // 组织入网
        for (SysOrg o : orgMapper.selectList(null)) {
            chain.appendTx("JoinNetwork", map("orgCode", o.getOrgCode(), "orgName", o.getOrgName(), "mspId", o.getMspId()),
                    o.getMspId(), o.getOrgName(), o.getCreateTime());
        }
        // 驴只建档 + 事件
        for (BizDonkey d : donkeyMapper.selectList(null)) {
            chain.appendTx("CreateDonkey", map("earTagId", d.getEarTagId(), "breed", d.getBreed(),
                            "orgName", d.getOrgName(), "barnNo", d.getBarnNo()),
                    "OrgFarmMSP", d.getOrgName(), d.getCreateTime(), d.getCreateTxid());
        }
        for (BizDonkeyEvent e : donkeyEventMapper.selectList(null)) {
            String op = "QUARANTINE".equals(e.getEventType()) ? "QuarantineDonkey" : "AddDonkeyEvent";
            var tx = chain.appendTx(op, map("earTagId", e.getEarTagId(), "eventType", e.getEventType(),
                    "content", e.getContent()), "OrgFarmMSP", "", e.getEventTime(), e.getTxId());
            e.setBlockNo(((Number) tx.get("blockNo")).intValue());
            donkeyEventMapper.updateById(e);
        }
        // 批次 + 批次事件
        for (BizBatch b : batchMapper.selectList(null)) {
            chain.appendTx("CreateBatch", map("batchNo", b.getBatchNo(), "batchType", b.getBatchType(),
                            "productName", b.getProductName(), "weightKg", b.getWeightKg(), "orgName", b.getOrgName()),
                    "OrgPlantMSP", b.getOrgName(), b.getCreateTime(), b.getCreateTxid());
        }
        for (BizBatchEvent e : batchEventMapper.selectList(null)) {
            var tx = chain.appendTx("AddBatchEvent", map("batchNo", e.getBatchNo(), "eventType", e.getEventType(),
                    "summary", e.getSummary(), "operator", e.getOperator(), "orgName", e.getOrgName()),
                    "OrgPlantMSP", e.getOrgName(), e.getEventTime(), e.getTxId());
            e.setBlockNo(((Number) tx.get("blockNo")).intValue());
            batchEventMapper.updateById(e);
        }
        // 运输单
        for (BizTransportOrder o : transportMapper.selectList(null)) {
            chain.appendTx("CreateTransport", map("transportNo", o.getTransportNo(), "batchNo", o.getBatchNo(),
                    "vehicleNo", o.getVehicleNo(), "fromOrgName", o.getFromOrgName(), "toOrgName", o.getToOrgName()),
                    "OrgLogisticsMSP", o.getFromOrgName(), o.getCreateTime(), o.getTxId());
        }
        // 证照
        for (BizCert c : certMapper.selectList(null)) {
            chain.appendTx("AddCert", map("certNo", c.getCertNo(), "certType", c.getCertType(),
                    "orgName", c.getOrgName(), "fileHash", c.getFileHash()), "OrgPlatformMSP", c.getOrgName(),
                    c.getIssueDate().atTime(10, 0));
        }
        // 抽检
        for (BizInspection i : inspectionMapper.selectList(null)) {
            chain.appendTx("AddInspection", map("batchNo", i.getBatchNo(), "agency", i.getAgency(),
                    "result", i.getResult(), "orgName", i.getOrgName(), "reportHash", i.getReportHash()),
                    "OrgRegulatorMSP", i.getOrgName(), i.getCreateTime(), i.getTxId());
        }
        // 溯源码绑定
        for (BizTraceCode c : traceCodeMapper.selectList(null)) {
            chain.appendTx("BindTraceCode", map("code", c.getCode(), "batchNo", c.getBatchNo(),
                    "shopOrgName", c.getShopOrgName()), "OrgShopMSP", c.getShopOrgName(), c.getCreateTime(), c.getBindTxid());
        }
        log.info("种子回放完成, 当前区块总数: {}", chainBlockMapper.selectCount(null));
    }

    private LocalDateTime ts(String s) {
        return java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }
}
