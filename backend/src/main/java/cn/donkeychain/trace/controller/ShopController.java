package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.*;
import cn.donkeychain.trace.entity.*;
import cn.donkeychain.trace.mapper.*;
import cn.donkeychain.trace.service.ChainService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class ShopController {

    private final BizBatchMapper batchMapper;
    private final BizOutputRecordMapper outputMapper;
    private final BizTraceCodeMapper traceCodeMapper;
    private final ChainService chain;

    @GetMapping("/shop/inventory")
    public Result<List<Map<String, Object>>> inventory() {
        List<BizBatch> inStore = batchMapper.selectList(new LambdaQueryWrapper<BizBatch>()
                .eq(BizBatch::getBatchType, "PRODUCT")
                .eq(BizBatch::getStatus, "IN_STORE")
                .orderByDesc(BizBatch::getProduceDate));
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> list = new ArrayList<>();
        for (BizBatch b : inStore) {
            BigDecimal used = outputMapper.selectList(new LambdaQueryWrapper<BizOutputRecord>()
                            .eq(BizOutputRecord::getBatchNo, b.getBatchNo())).stream()
                    .map(BizOutputRecord::getUsedWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = b.getWeightKg().subtract(used).max(BigDecimal.ZERO);
            long daysLeft = b.getExpireDate() == null ? 0 : ChronoUnit.DAYS.between(today, b.getExpireDate());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("batchNo", b.getBatchNo());
            m.put("productName", b.getProductName());
            m.put("orgName", b.getOrgName());
            m.put("holderOrgName", b.getHolderOrgName());
            m.put("weightKg", b.getWeightKg());
            m.put("remainingKg", remaining.setScale(1, RoundingMode.HALF_UP));
            m.put("produceDate", b.getProduceDate());
            m.put("expireDate", b.getExpireDate());
            m.put("status", b.getStatus());
            m.put("daysLeft", daysLeft);
            list.add(m);
        }
        return Result.ok(list);
    }

    @GetMapping("/shop/outputs")
    public Result<List<BizOutputRecord>> outputs() {
        return Result.ok(outputMapper.selectList(new LambdaQueryWrapper<BizOutputRecord>()
                .orderByDesc(BizOutputRecord::getOutputTime).orderByDesc(BizOutputRecord::getId)));
    }

    @PostMapping("/shop/outputs")
    public Result<BizOutputRecord> createOutput(@RequestBody BizOutputRecord o) {
        o.setId(null);
        o.setCreateTime(LocalDateTime.now());
        if (o.getOutputTime() == null) o.setOutputTime(LocalDateTime.now());
        outputMapper.insert(o);
        return Result.ok(o);
    }

    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final Random rnd = new Random();

    private String genCode() {
        StringBuilder sb = new StringBuilder("DT");
        for (int i = 0; i < 14; i++) sb.append(CODE_CHARS.charAt(rnd.nextInt(CODE_CHARS.length())));
        return sb.toString();
    }

    @PostMapping("/shop/trace-codes")
    public Result<List<BizTraceCode>> createCodes(@RequestBody Map<String, Object> body) {
        int n = Integer.parseInt(String.valueOf(body.getOrDefault("count", "1")));
        String batchNo = (String) body.get("batchNo");
        String shopOrgName = (String) body.get("shopOrgName");
        Long outputId = body.get("outputId") == null ? null : Long.valueOf(String.valueOf(body.get("outputId")));
        List<BizTraceCode> arr = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BizTraceCode c = new BizTraceCode();
            c.setCode(genCode());
            c.setBatchNo(batchNo);
            c.setShopOrgName(shopOrgName);
            c.setOutputId(outputId);
            c.setStatus("BOUND");
            c.setPrintCount(0);
            c.setScanTimes(0);
            c.setCreateTime(LocalDateTime.now());
            var tx = chain.appendTx("BindTraceCode", Map.of("code", c.getCode(), "batchNo", batchNo,
                    "shopOrgName", shopOrgName), "OrgShopMSP", shopOrgName, c.getCreateTime());
            c.setBindTxid((String) tx.get("txId"));
            traceCodeMapper.insert(c);
            arr.add(c);
        }
        return Result.ok(arr);
    }

    @GetMapping("/shop/trace-codes")
    public Result<Map<String, Object>> listCodes(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "20") long size,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String status) {
        LambdaQueryWrapper<BizTraceCode> qw = new LambdaQueryWrapper<BizTraceCode>()
                .orderByDesc(BizTraceCode::getCreateTime).orderByDesc(BizTraceCode::getId);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(BizTraceCode::getCode, keyword).or().like(BizTraceCode::getBatchNo, keyword)
                    .or().like(BizTraceCode::getShopOrgName, keyword));
        }
        if (status != null && !status.isBlank()) qw.eq(BizTraceCode::getStatus, status);
        return Result.ok(Pages.of(traceCodeMapper.selectPage(new Page<>(page, size), qw)));
    }

    @GetMapping("/shop/trace-codes/{id}/qrcode")
    public Result<Map<String, Object>> qrcode(@PathVariable Long id) {
        BizTraceCode c = traceCodeMapper.selectById(id);
        if (c == null) throw new BizException("码不存在");
        return Result.ok(Map.of("code", c.getCode(),
                "url", "https://trace.donkeychain.cn/t/" + c.getCode()));
    }
}
