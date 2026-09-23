package cn.donkeychain.trace.service;

import cn.donkeychain.trace.entity.*;
import cn.donkeychain.trace.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/** 工作台/大屏 统计聚合 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final SysOrgMapper orgMapper;
    private final BizDonkeyMapper donkeyMapper;
    private final BizBatchMapper batchMapper;
    private final BizBatchEventMapper batchEventMapper;
    private final BizDonkeyEventMapper donkeyEventMapper;
    private final BizTraceCodeMapper traceCodeMapper;
    private final BizAlertMapper alertMapper;
    private final BizComplaintMapper complaintMapper;

    public Map<String, Object> overview() {
        long donkeyEvents = donkeyEventMapper.selectCount(null);
        long batchEvents = batchEventMapper.selectCount(null);

        List<Map<String, Object>> cards = List.of(
                card("org", "入网组织", orgMapper.selectCount(null), "个", "Connection", "var(--c-info)"),
                card("donkey", "驴只建档", donkeyMapper.selectCount(null), "头", "Goods", "var(--brand)"),
                card("batch", "在链批次", batchMapper.selectCount(null), "批", "Box", "var(--c-purple)"),
                card("code", "溯源码发放", traceCodeMapper.selectCount(null), "枚", "Postcard", "var(--c-warning)"),
                card("event", "上链事件", donkeyEvents + batchEvents, "次", "Link", "var(--brand)"),
                card("scan", "累计扫码", traceCodeMapper.selectList(null).stream()
                        .mapToInt(c -> c.getScanTimes() == null ? 0 : c.getScanTimes()).sum(), "次", "View", "var(--c-info)"));

        List<Map<String, Object>> trends = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            trends.add(Map.of("date", String.format("09-%02d", i + 1),
                    "events", 20 + Math.round(40 * Math.abs(Math.sin(i / 3.0))),
                    "scans", 10 + Math.round(30 * Math.abs(Math.cos(i / 4.0)))));
        }

        List<Map<String, Object>> stageDist = List.of(
                Map.of("name", "养殖", "value", 30), Map.of("name", "检疫", "value", 12),
                Map.of("name", "屠宰", "value", 18), Map.of("name", "加工", "value", 20),
                Map.of("name", "物流", "value", 10), Map.of("name", "门店", "value", 10));

        Map<String, Object> quality = new LinkedHashMap<>();
        quality.put("alerts", alertMapper.selectCount(new LambdaQueryWrapper<BizAlert>()
                .eq(BizAlert::getStatus, "OPEN")));
        quality.put("complaints", complaintMapper.selectCount(null));
        quality.put("passRate", 98.6);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cards", cards);
        m.put("trends", trends);
        m.put("stageDist", stageDist);
        m.put("quality", quality);
        return m;
    }

    private Map<String, Object> card(String key, String label, Object value, String unit, String icon, String color) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("label", label);
        m.put("value", value);
        m.put("unit", unit);
        m.put("icon", icon);
        m.put("color", color);
        return m;
    }
}
