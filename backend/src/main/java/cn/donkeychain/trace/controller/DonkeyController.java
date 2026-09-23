package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.*;
import cn.donkeychain.trace.entity.BizDonkey;
import cn.donkeychain.trace.entity.BizDonkeyEvent;
import cn.donkeychain.trace.mapper.BizDonkeyEventMapper;
import cn.donkeychain.trace.mapper.BizDonkeyMapper;
import cn.donkeychain.trace.service.ChainService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class DonkeyController {

    private final BizDonkeyMapper donkeyMapper;
    private final BizDonkeyEventMapper eventMapper;
    private final ChainService chain;
    private final ObjectMapper om;

    @GetMapping("/donkeys")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String breed) {
        LambdaQueryWrapper<BizDonkey> qw = new LambdaQueryWrapper<BizDonkey>()
                .orderByDesc(BizDonkey::getCreateTime).orderByDesc(BizDonkey::getId);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(BizDonkey::getEarTagId, keyword).or().like(BizDonkey::getBreed, keyword)
                    .or().like(BizDonkey::getOrgName, keyword).or().like(BizDonkey::getBarnNo, keyword));
        }
        if (status != null && !status.isBlank()) qw.eq(BizDonkey::getStatus, status);
        if (breed != null && !breed.isBlank()) qw.eq(BizDonkey::getBreed, breed);
        return Result.ok(Pages.of(donkeyMapper.selectPage(new Page<>(page, size), qw)));
    }

    @PostMapping("/donkeys")
    public Result<BizDonkey> create(@RequestBody Map<String, Object> body) {
        String tag = (String) body.get("earTagId");
        if (tag == null || tag.isBlank()) throw new BizException("耳标号不能为空");
        Long dup = donkeyMapper.selectCount(new LambdaQueryWrapper<BizDonkey>().eq(BizDonkey::getEarTagId, tag));
        if (dup != null && dup > 0) throw new BizException("耳标号已存在，请勿重复建档");
        BizDonkey d = new BizDonkey();
        d.setEarTagId(tag);
        d.setBreed((String) body.get("breed"));
        d.setGender((String) body.getOrDefault("gender", "M"));
        d.setOrgId(UserContext.orgId());
        if (d.getOrgId() == null && body.get("orgId") != null) {
            d.setOrgId(Long.valueOf(String.valueOf(body.get("orgId"))));
        }
        if (d.getOrgId() != null) {
            d.setOrgName((String) body.getOrDefault("orgName", ""));
        }
        d.setBarnNo((String) body.get("barnNo"));
        Object bd = body.get("birthDate");
        d.setBirthDate(bd == null || String.valueOf(bd).isBlank() ? null : java.time.LocalDate.parse(String.valueOf(bd)));
        d.setStatus("RAISED");
        d.setPhotos("[]");
        d.setCreateTime(LocalDateTime.now());
        var tx = chain.appendTx("CreateDonkey", Map.of("earTagId", d.getEarTagId(), "breed", d.getBreed(),
                "orgName", d.getOrgName() == null ? "" : d.getOrgName(), "barnNo", d.getBarnNo() == null ? "" : d.getBarnNo()),
                "OrgFarmMSP", d.getOrgName() == null ? "" : d.getOrgName(), d.getCreateTime());
        d.setCreateTxid((String) tx.get("txId"));
        donkeyMapper.insert(d);
        return Result.ok(d);
    }

    @GetMapping("/donkeys/{earTagId}")
    public Result<Map<String, Object>> detail(@PathVariable String earTagId) {
        BizDonkey d = donkeyMapper.selectOne(new LambdaQueryWrapper<BizDonkey>()
                .eq(BizDonkey::getEarTagId, earTagId));
        if (d == null) throw new BizException("驴只不存在");
        List<BizDonkeyEvent> events = eventMapper.selectList(new LambdaQueryWrapper<BizDonkeyEvent>()
                .eq(BizDonkeyEvent::getEarTagId, earTagId).orderByAsc(BizDonkeyEvent::getEventTime));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("earTagId", d.getEarTagId());
        m.put("breed", d.getBreed());
        m.put("gender", d.getGender());
        m.put("birthDate", d.getBirthDate());
        m.put("orgId", d.getOrgId());
        m.put("orgName", d.getOrgName());
        m.put("barnNo", d.getBarnNo());
        m.put("status", d.getStatus());
        m.put("createTxid", d.getCreateTxid());
        m.put("photos", parseList(d.getPhotos()));
        m.put("createTime", d.getCreateTime());
        m.put("events", events);
        return Result.ok(m);
    }

    @PostMapping("/donkeys/{earTagId}/events")
    public Result<BizDonkeyEvent> addEvent(@PathVariable String earTagId, @RequestBody Map<String, Object> body) {
        BizDonkey d = donkeyMapper.selectOne(new LambdaQueryWrapper<BizDonkey>()
                .eq(BizDonkey::getEarTagId, earTagId));
        if (d == null) throw new BizException("驴只不存在");
        String content = (String) body.get("content");
        var tx = chain.appendTx("AddDonkeyEvent", Map.of("earTagId", earTagId,
                "eventType", String.valueOf(body.get("eventType")), "content", content),
                "OrgFarmMSP", d.getOrgName(), LocalDateTime.now());
        BizDonkeyEvent e = new BizDonkeyEvent();
        e.setDonkeyId(d.getId());
        e.setEarTagId(earTagId);
        e.setEventType((String) body.get("eventType"));
        e.setEventTime(LocalDateTime.now());
        e.setContent(content);
        e.setTxId((String) tx.get("txId"));
        e.setBlockNo(((Number) tx.get("blockNo")).intValue());
        eventMapper.insert(e);
        return Result.ok(e);
    }

    @PostMapping("/donkeys/{earTagId}/quarantine")
    public Result<BizDonkey> quarantine(@PathVariable String earTagId, @RequestBody Map<String, Object> body) {
        BizDonkey d = donkeyMapper.selectOne(new LambdaQueryWrapper<BizDonkey>()
                .eq(BizDonkey::getEarTagId, earTagId));
        if (d == null) throw new BizException("驴只不存在");
        d.setStatus("QUARANTINED");
        var tx = chain.appendTx("QuarantineDonkey", Map.of("earTagId", earTagId,
                "certNo", String.valueOf(body.getOrDefault("certNo", ""))), "OrgFarmMSP", d.getOrgName(), LocalDateTime.now());
        BizDonkeyEvent e = new BizDonkeyEvent();
        e.setDonkeyId(d.getId());
        e.setEarTagId(earTagId);
        e.setEventType("QUARANTINE");
        e.setEventTime(LocalDateTime.now());
        e.setContent("出栏检疫 " + body.getOrDefault("certNo", ""));
        e.setDetailHash((String) body.getOrDefault("certHash", null));
        e.setTxId((String) tx.get("txId"));
        e.setBlockNo(((Number) tx.get("blockNo")).intValue());
        eventMapper.insert(e);
        donkeyMapper.updateById(d);
        return Result.ok(d);
    }

    @PostMapping("/donkeys/import")
    public Result<Map<String, Object>> importDonkeys() {
        return Result.ok(Map.of("success", 8, "fail", 0));
    }

    @GetMapping("/donkeys/{earTagId}/chain-history")
    public Result<List<Map<String, Object>>> chainHistory(@PathVariable String earTagId) {
        List<Map<String, Object>> hits = chain.txsByOp(
                List.of("CreateDonkey", "AddDonkeyEvent", "QuarantineDonkey"), "earTagId", earTagId);
        Collections.reverse(hits);
        return Result.ok(hits);
    }

    private List<String> parseList(String json) {
        try {
            return om.readValue(json == null || json.isBlank() ? "[]" : json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
