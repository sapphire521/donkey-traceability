package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.*;
import cn.donkeychain.trace.entity.BizTransportOrder;
import cn.donkeychain.trace.entity.BizTransportRecord;
import cn.donkeychain.trace.mapper.BizTransportOrderMapper;
import cn.donkeychain.trace.mapper.BizTransportRecordMapper;
import cn.donkeychain.trace.service.ChainService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/transport")
@RequiredArgsConstructor
public class TransportController {

    private final BizTransportOrderMapper orderMapper;
    private final BizTransportRecordMapper recordMapper;
    private final ChainService chain;

    @GetMapping("/orders")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BizTransportOrder> qw = new LambdaQueryWrapper<BizTransportOrder>()
                .orderByDesc(BizTransportOrder::getDepartTime).orderByDesc(BizTransportOrder::getId);
        if (status != null && !status.isBlank()) qw.eq(BizTransportOrder::getStatus, status);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(BizTransportOrder::getTransportNo, keyword)
                    .or().like(BizTransportOrder::getBatchNo, keyword)
                    .or().like(BizTransportOrder::getVehicleNo, keyword)
                    .or().like(BizTransportOrder::getToOrgName, keyword));
        }
        return Result.ok(Pages.of(orderMapper.selectPage(new Page<>(page, size), qw)));
    }

    @PostMapping("/orders")
    public Result<BizTransportOrder> create(@RequestBody BizTransportOrder o) {
        o.setId(null);
        o.setStatus("IN_TRANSIT");
        o.setCreateTime(LocalDateTime.now());
        var tx = chain.appendTx("CreateTransport", Map.of("transportNo", o.getTransportNo(),
                "batchNo", o.getBatchNo(), "vehicleNo", o.getVehicleNo(),
                "fromOrgName", o.getFromOrgName(), "toOrgName", o.getToOrgName()),
                "OrgLogisticsMSP", o.getFromOrgName(), o.getCreateTime());
        o.setTxId((String) tx.get("txId"));
        orderMapper.insert(o);
        return Result.ok(o);
    }

    @PostMapping("/orders/{no}/records")
    public Result<Map<String, Object>> addRecords(@PathVariable String no, @RequestBody Object body) {
        BizTransportOrder o = orderMapper.selectOne(new LambdaQueryWrapper<BizTransportOrder>()
                .eq(BizTransportOrder::getTransportNo, no));
        if (o == null) throw new BizException("运输单不存在");
        java.util.List<?> recs = body instanceof List<?> l ? l : List.of(body);
        int added = 0;
        for (Object r : recs) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) r;
            BizTransportRecord rec = new BizTransportRecord();
            rec.setOrderId(o.getId());
            rec.setRecordTime(LocalDateTime.parse((String) m.get("recordTime")
                    .toString().replace(' ', 'T')));
            rec.setTemperature(new BigDecimal(String.valueOf(m.get("temperature"))));
            rec.setHumidity(Integer.parseInt(String.valueOf(m.get("humidity"))));
            double temp = rec.getTemperature().doubleValue();
            rec.setAbnormal(temp < 0 || temp > 4 ? 1 : 0);
            recordMapper.insert(rec);
            added++;
        }
        return Result.ok(Map.of("added", added));
    }

    @PostMapping("/orders/{no}/finish")
    public Result<BizTransportOrder> finish(@PathVariable String no, @RequestBody Map<String, Object> body) {
        BizTransportOrder o = orderMapper.selectOne(new LambdaQueryWrapper<BizTransportOrder>()
                .eq(BizTransportOrder::getTransportNo, no));
        if (o == null) throw new BizException("运输单不存在");
        o.setStatus("RECEIVED");
        o.setFileUri((String) body.get("fileUri"));
        o.setFileHash((String) body.get("fileHash"));
        orderMapper.updateById(o);
        return Result.ok(o);
    }

    @PostMapping("/orders/{no}/receive")
    public Result<BizTransportOrder> receive(@PathVariable String no, @RequestBody Map<String, Object> body) {
        BizTransportOrder o = orderMapper.selectOne(new LambdaQueryWrapper<BizTransportOrder>()
                .eq(BizTransportOrder::getTransportNo, no));
        if (o == null) throw new BizException("运输单不存在");
        boolean rejected = Boolean.parseBoolean(String.valueOf(body.getOrDefault("rejected", "false")));
        o.setStatus(rejected ? "REJECTED" : "RECEIVED");
        o.setActualArriveTime(LocalDateTime.now());
        var tx = chain.appendTx("ReceiveBatch", Map.of("transportNo", no, "batchNo", o.getBatchNo(),
                "rejected", rejected), "OrgShopMSP", o.getToOrgName(), o.getActualArriveTime());
        o.setTxId((String) tx.get("txId"));
        orderMapper.updateById(o);
        return Result.ok(o);
    }
}
