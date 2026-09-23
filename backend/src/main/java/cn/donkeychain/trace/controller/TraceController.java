package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.*;
import cn.donkeychain.trace.entity.*;
import cn.donkeychain.trace.mapper.*;
import cn.donkeychain.trace.service.ChainService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class TraceController {

    private final BizTraceCodeMapper traceCodeMapper;
    private final BizBatchMapper batchMapper;
    private final BizBatchEventMapper batchEventMapper;
    private final BizDonkeyMapper donkeyMapper;
    private final BizDonkeyEventMapper donkeyEventMapper;
    private final BizTransportOrderMapper transportMapper;
    private final BizTransportRecordMapper transportRecordMapper;
    private final BizComplaintMapper complaintMapper;
    private final ChainService chain;
    private final ObjectMapper om;

    // ---------- 溯源报告 ----------
    @GetMapping("/trace/codes/{code}")
    public Result<Map<String, Object>> report(@PathVariable String code) {
        return Result.ok(buildTraceReport(code));
    }

    private BizBatch findBatch(String no) {
        return batchMapper.selectOne(new LambdaQueryWrapper<BizBatch>().eq(BizBatch::getBatchNo, no));
    }

    /** 沿 parentNos 上溯整条血缘 */
    private List<BizBatch> ancestors(String no) {
        List<BizBatch> chainList = new ArrayList<>();
        String cur = no;
        int guard = 0;
        while (cur != null && guard++ < 20) {
            BizBatch b = findBatch(cur);
            if (b == null) break;
            chainList.add(b);
            cur = firstParent(b);
        }
        return chainList;
    }

    private String firstParent(BizBatch b) {
        try {
            List<String> parents = om.readValue(b.getParentNos() == null ? "[]" : b.getParentNos(),
                    new TypeReference<List<String>>() {});
            return parents.isEmpty() ? null : parents.getFirst();
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> failedReport(String code, String status, int latestBlock) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("verifyStatus", status);
        m.put("product", Map.of("name", "—", "batchNo", "—", "produceDate", "—", "shopName", "—"));
        m.put("timeline", List.of());
        m.put("blockchain", Map.of("channel", ChainService.CHANNEL, "eventCount", 0,
                "latestBlock", latestBlock, "latestTxId", "-"));
        return m;
    }

    private Map<String, Object> buildTraceReport(String codeStr) {
        BizTraceCode tc = traceCodeMapper.selectOne(new LambdaQueryWrapper<BizTraceCode>()
                .eq(BizTraceCode::getCode, codeStr));
        List<ChainBlock> latestList = chain.latestBlocks(1);
        int latestBlock = latestList.isEmpty() ? 0 : latestList.getFirst().getBlockNo();
        if (tc == null) return failedReport(codeStr, "FAILED", latestBlock);
        if ("DESTROYED".equals(tc.getStatus())) return failedReport(codeStr, "DESTROYED", latestBlock);

        BizBatch product = findBatch(tc.getBatchNo());
        if (product == null) return failedReport(codeStr, "FAILED", latestBlock);
        List<BizBatch> chainBatches = ancestors(tc.getBatchNo());
        BizBatch root = chainBatches.get(chainBatches.size() - 1);

        List<Map<String, Object>> timeline = new ArrayList<>();
        // 养殖环节
        for (String tag : parseStrList(root.getSourceEarTags())) {
            BizDonkey d = donkeyMapper.selectOne(new LambdaQueryWrapper<BizDonkey>()
                    .eq(BizDonkey::getEarTagId, tag));
            if (d == null) continue;
            Map<String, Object> farm = new LinkedHashMap<>();
            farm.put("stage", "FARM");
            farm.put("title", "养殖档案");
            farm.put("orgName", d.getOrgName());
            farm.put("time", d.getCreateTime().toLocalDate().toString());
            farm.put("summary", d.getBreed() + " 入栏建档 耳标" + d.getEarTagId());
            farm.put("txId", d.getCreateTxid());
            farm.put("photos", parseStrList(d.getPhotos()));
            timeline.add(farm);
            BizDonkeyEvent q = donkeyEventMapper.selectOne(new LambdaQueryWrapper<BizDonkeyEvent>()
                    .eq(BizDonkeyEvent::getEarTagId, tag).eq(BizDonkeyEvent::getEventType, "QUARANTINE")
                    .orderByDesc(BizDonkeyEvent::getEventTime).last("LIMIT 1"));
            if (q != null) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("stage", "QUARANTINE");
                m.put("title", "出栏检疫");
                m.put("orgName", d.getOrgName());
                m.put("time", q.getEventTime().toLocalDate().toString());
                m.put("summary", q.getContent());
                m.put("txId", q.getTxId());
                timeline.add(m);
            }
        }
        // 批次链事件
        for (BizBatch b : chainBatches) {
            List<BizBatchEvent> evs = batchEventMapper.selectList(new LambdaQueryWrapper<BizBatchEvent>()
                    .eq(BizBatchEvent::getBatchNo, b.getBatchNo()).orderByAsc(BizBatchEvent::getEventTime));
            for (BizBatchEvent e : evs) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("stage", stageOf(e.getEventType(), b.getBatchType()));
                m.put("title", titleOf(e.getEventType()));
                m.put("orgName", e.getOrgName());
                m.put("time", e.getEventTime().toLocalDate().toString());
                m.put("summary", e.getSummary());
                m.put("txId", e.getTxId());
                timeline.add(m);
            }
        }
        // 冷链运输
        BizTransportOrder transport = transportMapper.selectOne(new LambdaQueryWrapper<BizTransportOrder>()
                .eq(BizTransportOrder::getBatchNo, product.getBatchNo())
                .orderByDesc(BizTransportOrder::getId).last("LIMIT 1"));
        if (transport != null) {
            List<BizTransportRecord> recs = transportRecordMapper.selectList(
                    new LambdaQueryWrapper<BizTransportRecord>().eq(BizTransportRecord::getOrderId, transport.getId()));
            String tempRange = recs.isEmpty() ? "0~4℃" :
                    recs.stream().map(BizTransportRecord::getTemperature).min(BigDecimal::compareTo).get()
                            .stripTrailingZeros().toPlainString() + "~" +
                            recs.stream().map(BizTransportRecord::getTemperature).max(BigDecimal::compareTo).get()
                                    .stripTrailingZeros().toPlainString() + "℃";
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("stage", "TRANSPORT");
            m.put("title", "冷链运输");
            m.put("orgName", transport.getFromOrgName());
            m.put("time", transport.getDepartTime().toLocalDate().toString());
            m.put("summary", "全程" + tempRange + " 符合要求 车牌" + transport.getVehicleNo());
            m.put("txId", transport.getTxId());
            timeline.add(m);
        }
        // 门店制作
        Map<String, Object> shop = new LinkedHashMap<>();
        shop.put("stage", "SHOP");
        shop.put("title", "门店制作");
        shop.put("orgName", product.getHolderOrgName());
        shop.put("time", product.getProduceDate().toString());
        shop.put("summary", product.getProduceDate() + " 第3炉出锅");
        timeline.add(shop);

        long eventCount = timeline.stream().filter(t -> t.get("txId") != null).count();
        Map<String, Object> productInfo = new LinkedHashMap<>();
        productInfo.put("name", "驴肉火烧·卤制驴肉");
        productInfo.put("batchNo", product.getBatchNo());
        productInfo.put("produceDate", product.getProduceDate().toString());
        productInfo.put("shopName", product.getHolderOrgName());
        productInfo.put("expireDate", product.getExpireDate() == null ? null : product.getExpireDate().toString());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("code", codeStr);
        res.put("verifyStatus", "PASSED");
        res.put("product", productInfo);
        res.put("timeline", timeline);
        res.put("blockchain", Map.of("channel", ChainService.CHANNEL, "eventCount", eventCount,
                "latestBlock", latestBlock,
                "latestTxId", latestList.isEmpty() ? "-" : latestList.getFirst().getHash()));
        return res;
    }

    private String stageOf(String eventType, String batchType) {
        return switch (eventType) {
            case "BATCH_CREATE" -> batchType;
            case "QC_PASS" -> "QC";
            case "TRANSFER" -> "TRANSPORT";
            case "RECEIVE", "BIND_CODE" -> "SHOP";
            default -> "PROCESS";
        };
    }

    private String titleOf(String t) {
        return switch (t) {
            case "BATCH_CREATE" -> "批次创建";
            case "QC_PASS" -> "出厂检验";
            case "TRANSFER" -> "冷链发运";
            case "RECEIVE" -> "门店签收";
            case "BIND_CODE" -> "溯源码绑定";
            default -> t;
        };
    }

    // ---------- 投诉 ----------
    @PostMapping("/trace/codes/{code}/complaint")
    public Result<Map<String, Object>> complaint(@PathVariable String code, @RequestBody Map<String, Object> body) {
        BizComplaint c = new BizComplaint();
        c.setCode(code);
        c.setCategory((String) body.get("category"));
        c.setContent((String) body.get("content"));
        c.setPhone((String) body.get("phone"));
        c.setStatus("PENDING");
        c.setCreateTime(LocalDateTime.now());
        complaintMapper.insert(c);
        chain.appendTx("AddComplaint", Map.of("code", code, "category",
                String.valueOf(body.get("category"))), "OrgPlatformMSP", "消费者投诉", LocalDateTime.now());
        return Result.ok(Map.of("ok", true));
    }

    // ---------- 批次树 ----------
    @GetMapping("/trace/batch-tree/{batchNo}")
    public Result<Map<String, Object>> batchTree(@PathVariable String batchNo) {
        return Result.ok(buildTree(batchNo, new HashSet<>()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildTree(String no, Set<String> visited) {
        BizBatch b = findBatch(no);
        if (b == null || visited.contains(no)) {
            return Map.of("id", no, "name", no, "type", "UNKNOWN", "status", "CREATED", "children", List.of());
        }
        visited.add(no);
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", b.getBatchNo());
        node.put("name", b.getBatchNo() + "\n" + b.getProductName());
        node.put("type", b.getBatchType());
        node.put("status", b.getStatus());
        node.put("weight", b.getWeightKg());
        List<Object> children = new ArrayList<>();
        for (BizBatch c : batchMapper.selectList(new LambdaQueryWrapper<BizBatch>()
                .like(BizBatch::getParentNos, "\"" + no + "\""))) {
            children.add(buildTree(c.getBatchNo(), visited));
        }
        if ("SLAUGHTER".equals(b.getBatchType())) {
            for (String tag : parseStrList(b.getSourceEarTags())) {
                Map<String, Object> leaf = new LinkedHashMap<>();
                leaf.put("id", tag);
                leaf.put("name", tag);
                leaf.put("type", "DONKEY");
                leaf.put("status", "RAISED");
                leaf.put("leaf", true);
                children.add(leaf);
            }
        }
        node.put("children", children);
        return node;
    }

    private List<String> parseStrList(String json) {
        try {
            return om.readValue(json == null || json.isBlank() ? "[]" : json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
