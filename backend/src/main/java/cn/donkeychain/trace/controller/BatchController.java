package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.*;
import cn.donkeychain.trace.entity.BizBatch;
import cn.donkeychain.trace.entity.BizBatchEvent;
import cn.donkeychain.trace.entity.SysOrg;
import cn.donkeychain.trace.mapper.BizBatchEventMapper;
import cn.donkeychain.trace.mapper.BizBatchMapper;
import cn.donkeychain.trace.mapper.SysOrgMapper;
import cn.donkeychain.trace.service.ChainService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class BatchController {

    private final BizBatchMapper batchMapper;
    private final BizBatchEventMapper batchEventMapper;
    private final SysOrgMapper orgMapper;
    private final ChainService chain;
    private final ObjectMapper om;

    @GetMapping("/batches")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) String type,
                                            @RequestParam(required = false) String batchType,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword) {
        String t = batchType != null && !batchType.isBlank() ? batchType : type;
        LambdaQueryWrapper<BizBatch> qw = new LambdaQueryWrapper<BizBatch>()
                .orderByDesc(BizBatch::getProduceDate).orderByDesc(BizBatch::getId);
        if (t != null && !t.isBlank()) qw.eq(BizBatch::getBatchType, t);
        if (status != null && !status.isBlank()) qw.eq(BizBatch::getStatus, status);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(BizBatch::getBatchNo, keyword).or().like(BizBatch::getProductName, keyword)
                    .or().like(BizBatch::getHolderOrgName, keyword).or().like(BizBatch::getOrgName, keyword));
        }
        Page<BizBatch> p = batchMapper.selectPage(new Page<>(page, size), qw);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", p.getTotal());
        out.put("records", p.getRecords().stream().map(this::toVo).toList());
        return Result.ok(out);
    }

    /** parentNos / sourceEarTags 以数组形式返回(前端模板对其调用 join) */
    private Map<String, Object> toVo(BizBatch b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("batchNo", b.getBatchNo());
        m.put("batchType", b.getBatchType());
        m.put("parentNos", parseList(b.getParentNos()));
        m.put("sourceEarTags", parseList(b.getSourceEarTags()));
        m.put("orgId", b.getOrgId());
        m.put("orgName", b.getOrgName());
        m.put("holderOrgId", b.getHolderOrgId());
        m.put("holderOrgName", b.getHolderOrgName());
        m.put("productName", b.getProductName());
        m.put("weightKg", b.getWeightKg());
        m.put("produceDate", b.getProduceDate());
        m.put("expireDate", b.getExpireDate());
        m.put("certHash", b.getCertHash());
        m.put("status", b.getStatus());
        m.put("createTxid", b.getCreateTxid());
        m.put("qcReportUri", b.getQcReportUri());
        m.put("createTime", b.getCreateTime());
        return m;
    }

    private List<String> parseList(String json) {
        try {
            return om.readValue(json == null || json.isBlank() ? "[]" : json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @GetMapping("/batches/{batchNo}")
    public Result<Map<String, Object>> detail(@PathVariable String batchNo) {
        BizBatch b = batchMapper.selectOne(new LambdaQueryWrapper<BizBatch>().eq(BizBatch::getBatchNo, batchNo));
        if (b == null) throw new BizException("批次不存在");
        return Result.ok(toVo(b));
    }

    @GetMapping("/batches/{batchNo}/events")
    public Result<List<BizBatchEvent>> events(@PathVariable String batchNo) {
        return Result.ok(batchEventMapper.selectList(new LambdaQueryWrapper<BizBatchEvent>()
                .eq(BizBatchEvent::getBatchNo, batchNo).orderByAsc(BizBatchEvent::getEventTime)));
    }

    @GetMapping("/batches/{batchNo}/chain-history")
    public Result<List<Map<String, Object>>> chainHistory(@PathVariable String batchNo) {
        List<Map<String, Object>> hits = new ArrayList<>(chain.txsByOp(List.of("CreateBatch"), "batchNo", batchNo));
        hits.addAll(chain.txsByOp(List.of("AddBatchEvent"), "batchNo", batchNo));
        hits.addAll(chain.txsByOp(List.of("ReceiveBatch"), "batchNo", batchNo));
        hits.sort(Comparator.comparing(m -> String.valueOf(m.get("time"))));
        return Result.ok(hits);
    }

    @PostMapping("/batches/slaughter")
    public Result<BizBatch> createSlaughter(@RequestBody Map<String, Object> body) {
        return Result.ok(doCreate("SLAUGHTER", body));
    }

    @PostMapping("/batches/process")
    public Result<BizBatch> createProcess(@RequestBody Map<String, Object> body) {
        return Result.ok(doCreate("PROCESS", body));
    }

    @PostMapping("/batches/product")
    public Result<BizBatch> createProduct(@RequestBody Map<String, Object> body) {
        return Result.ok(doCreate("PRODUCT", body));
    }

    private BizBatch doCreate(String type, Map<String, Object> body) {
        BizBatch b = new BizBatch();
        b.setBatchNo((String) body.get("batchNo"));
        b.setBatchType(type);
        b.setParentNos(toJson(listOf(body.get("parentNos"))));
        b.setSourceEarTags(toJson(listOf(body.get("sourceEarTags"))));

        // 归属: 有父批次 -> 沿用父批次组织; 否则用当前用户组织(非加工厂)或第一个加工厂
        List<String> parentNos = listOf(body.get("parentNos"));
        SysOrg org;
        if (!parentNos.isEmpty()) {
            BizBatch parent = batchMapper.selectOne(new LambdaQueryWrapper<BizBatch>()
                    .eq(BizBatch::getBatchNo, parentNos.getFirst()));
            if (parent == null) throw new BizException("父批次不存在: " + parentNos.getFirst());
            org = orgMapper.selectById(parent.getOrgId());
        } else {
            Long orgId = UserContext.orgId();
            org = orgId != null ? orgMapper.selectById(orgId) : null;
            if (org == null || !"PLANT".equals(org.getOrgType())) {
                org = orgMapper.selectOne(new LambdaQueryWrapper<SysOrg>().eq(SysOrg::getOrgType, "PLANT").last("LIMIT 1"));
            }
        }
        b.setOrgId(org.getId());
        b.setOrgName(org.getOrgName());
        b.setHolderOrgId(org.getId());
        b.setHolderOrgName(org.getOrgName());
        if (type.equals("PRODUCT") && body.get("holderOrgId") != null) {
            b.setHolderOrgId(Long.valueOf(String.valueOf(body.get("holderOrgId"))));
            b.setHolderOrgName((String) body.getOrDefault("holderOrgName", b.getHolderOrgName()));
        }
        b.setProductName((String) body.get("productName"));
        b.setWeightKg(body.get("weightKg") == null ? null : new java.math.BigDecimal(String.valueOf(body.get("weightKg"))));
        b.setProduceDate(body.get("produceDate") == null ? LocalDate.now() : LocalDate.parse((String) body.get("produceDate")));
        b.setExpireDate(body.get("expireDate") == null ? null : LocalDate.parse((String) body.get("expireDate")));
        b.setCertHash((String) body.get("certHash"));
        b.setStatus(type.equals("PRODUCT") ? "IN_STORE" : "CREATED");
        b.setCreateTime(LocalDateTime.now());
        var tx = chain.appendTx("CreateBatch", Map.of("batchNo", b.getBatchNo(), "batchType", type,
                "productName", b.getProductName(), "weightKg", b.getWeightKg(), "orgName", b.getOrgName()),
                "OrgPlantMSP", b.getOrgName(), b.getCreateTime());
        b.setCreateTxid((String) tx.get("txId"));
        batchMapper.insert(b);

        BizBatchEvent e = new BizBatchEvent();
        e.setBatchNo(b.getBatchNo());
        e.setEventType("BATCH_CREATE");
        e.setOrgName(b.getOrgName());
        e.setOperator(UserContext.username());
        e.setEventTime(b.getCreateTime());
        e.setSummary("创建" + type + "批次 " + b.getBatchNo());
        e.setTxId((String) tx.get("txId"));
        e.setBlockNo(((Number) tx.get("blockNo")).intValue());
        batchEventMapper.insert(e);
        return b;
    }

    @PostMapping("/batches/expire-scan")
    public Result<Map<String, Object>> expireScan() {
        List<BizBatch> expired = batchMapper.selectList(new LambdaQueryWrapper<BizBatch>()
                .eq(BizBatch::getBatchType, "PRODUCT")
                .isNotNull(BizBatch::getExpireDate)
                .lt(BizBatch::getExpireDate, LocalDate.now())
                .ne(BizBatch::getStatus, "EXPIRED"));
        expired.forEach(b -> {
            b.setStatus("EXPIRED");
            batchMapper.updateById(b);
        });
        return Result.ok(Map.of("scanned", batchMapper.selectCount(null), "marked", expired.size()));
    }

    private List<String> listOf(Object o) {
        if (o instanceof List<?> l) {
            return l.stream().map(String::valueOf).toList();
        }
        if (o instanceof String s && !s.isBlank()) {
            try {
                return om.readValue(s, new TypeReference<List<String>>() {});
            } catch (Exception ignored) {
                return List.of(s);
            }
        }
        return List.of();
    }

    private String toJson(List<String> l) {
        try {
            return om.writeValueAsString(l);
        } catch (Exception e) {
            return "[]";
        }
    }
}
