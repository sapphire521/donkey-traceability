package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.*;
import cn.donkeychain.trace.entity.*;
import cn.donkeychain.trace.mapper.*;
import cn.donkeychain.trace.service.ChainService;
import cn.donkeychain.trace.service.StatsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/regulator")
@RequiredArgsConstructor
public class RegulatorController {

    private final BizInspectionMapper inspectionMapper;
    private final BizRecallMapper recallMapper;
    private final BizComplaintMapper complaintMapper;
    private final BizBatchMapper batchMapper;
    private final BizTraceCodeMapper traceCodeMapper;
    private final ChainService chain;
    private final StatsService statsService;
    private final ObjectMapper om;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> m = new LinkedHashMap<>(statsService.overview());
        m.put("regionDist", List.of(
                Map.of("name", "保定", "value", 14), Map.of("name", "石家庄", "value", 6),
                Map.of("name", "沧州", "value", 4), Map.of("name", "衡水", "value", 3),
                Map.of("name", "廊坊", "value", 3)));
        m.put("certExpiring", 3);
        m.put("recallActive", recallMapper.selectCount(
                new LambdaQueryWrapper<BizRecall>().eq(BizRecall::getStatus, "RECALLING")));
        return Result.ok(m);
    }

    @GetMapping("/inspections")
    public Result<Map<String, Object>> inspections(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "20") long size,
                                                   @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BizInspection> qw = new LambdaQueryWrapper<BizInspection>()
                .orderByDesc(BizInspection::getCreateTime).orderByDesc(BizInspection::getId);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(BizInspection::getBatchNo, keyword).or().like(BizInspection::getAgency, keyword)
                    .or().like(BizInspection::getOrgName, keyword));
        }
        return Result.ok(Pages.of(inspectionMapper.selectPage(new Page<>(page, size), qw)));
    }

    @PostMapping("/inspections")
    public Result<BizInspection> addInspection(@RequestBody BizInspection i) {
        i.setId(null);
        i.setCreateTime(LocalDateTime.now());
        inspectionMapper.insert(i);
        chain.appendTx("AddInspection", Map.of("batchNo", i.getBatchNo(), "agency", i.getAgency(),
                "result", i.getResult(), "orgName", i.getOrgName() == null ? "" : i.getOrgName()),
                "OrgRegulatorMSP", i.getOrgName() == null ? "" : i.getOrgName(), i.getCreateTime());
        return Result.ok(i);
    }

    @GetMapping("/recalls")
    public Result<Map<String, Object>> recalls(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "20") long size,
                                               @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BizRecall> qw = new LambdaQueryWrapper<BizRecall>()
                .orderByDesc(BizRecall::getCreateTime).orderByDesc(BizRecall::getId);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(BizRecall::getBatchNo, keyword).or().like(BizRecall::getReason, keyword));
        }
        return Result.ok(Pages.of(recallMapper.selectPage(new Page<>(page, size), qw)));
    }

    @PostMapping("/recalls")
    public Result<BizRecall> createRecall(@RequestBody Map<String, Object> body) {
        String batchNo = (String) body.get("batchNo");
        String reason = (String) body.get("reason");
        BizBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<BizBatch>()
                .eq(BizBatch::getBatchNo, batchNo));

        // 基于批次树自动分析扩散范围: 子批次 + 持有门店 + 关联溯源码
        List<String> downstream = new ArrayList<>();
        List<String> shops = new ArrayList<>();
        if (batch != null) {
            shops.add(batch.getHolderOrgName());
            collectDownstream(batchNo, downstream, shops);
        }
        long codes = traceCodeMapper.selectCount(new LambdaQueryWrapper<BizTraceCode>()
                .eq(BizTraceCode::getBatchNo, batchNo));
        long scanned = traceCodeMapper.selectList(new LambdaQueryWrapper<BizTraceCode>()
                        .eq(BizTraceCode::getBatchNo, batchNo)).stream()
                .mapToInt(c -> c.getScanTimes() == null ? 0 : c.getScanTimes()).sum();

        Map<String, Object> scope = new LinkedHashMap<>();
        scope.put("downstreamBatches", downstream);
        scope.put("shops", shops);
        scope.put("codes", codes);
        scope.put("scanned", scanned);

        BizRecall r = new BizRecall();
        r.setBatchNo(batchNo);
        r.setReason(reason);
        r.setScopeJson(om.valueToTree(scope).toString());
        r.setStatus("RECALLING");
        r.setInitiator("监管员");
        r.setCreateTime(LocalDateTime.now());
        recallMapper.insert(r);
        chain.appendTx("CreateRecall", Map.of("batchNo", batchNo, "reason", reason, "scope", scope),
                "OrgRegulatorMSP", "保定市畜禽产品质量安全监督中心", r.getCreateTime());
        return Result.ok(r);
    }

    private void collectDownstream(String batchNo, List<String> downstream, List<String> shops) {
        List<BizBatch> children = batchMapper.selectList(new LambdaQueryWrapper<BizBatch>()
                .like(BizBatch::getParentNos, "\"" + batchNo + "\""));
        for (BizBatch c : children) {
            downstream.add(c.getBatchNo());
            if (c.getHolderOrgName() != null) shops.add(c.getHolderOrgName());
            collectDownstream(c.getBatchNo(), downstream, shops);
        }
    }

    @GetMapping("/complaints")
    public Result<Map<String, Object>> complaints(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long size,
                                                  @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BizComplaint> qw = new LambdaQueryWrapper<BizComplaint>()
                .orderByDesc(BizComplaint::getCreateTime).orderByDesc(BizComplaint::getId);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(BizComplaint::getCode, keyword).or().like(BizComplaint::getCategory, keyword)
                    .or().like(BizComplaint::getContent, keyword));
        }
        return Result.ok(Pages.of(complaintMapper.selectPage(new Page<>(page, size), qw)));
    }
}
