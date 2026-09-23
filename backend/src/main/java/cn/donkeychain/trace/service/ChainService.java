package cn.donkeychain.trace.service;

import cn.donkeychain.trace.common.BizException;
import cn.donkeychain.trace.entity.ChainBlock;
import cn.donkeychain.trace.mapper.ChainBlockMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模拟 Fabric 联盟链账本: 一交易一块, SHA-256 链式哈希。
 * 与前端 Mock 版逻辑等价, 迁移到服务端后由数据库持久化。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChainService {

    public static final String CHANNEL = "donkey-channel";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ChainBlockMapper blockMapper;
    private final ObjectMapper objectMapper;

    // ---------- 哈希 ----------
    public String sha256Hex(String str) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(str.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String genesisPrevHash() {
        return "0x" + "0".repeat(24);
    }

    /** 与链上存储一致的交易 JSON (单交易块) */
    public String txsJson(String txId, String op, String mspId, String orgName, LocalDateTime time, Map<String, Object> payload) {
        try {
            Map<String, Object> tx = new LinkedHashMap<>();
            tx.put("txId", txId);
            tx.put("op", op);
            tx.put("mspId", mspId);
            tx.put("orgName", orgName);
            tx.put("time", time.format(FMT));
            tx.put("payload", payload);
            return objectMapper.writeValueAsString(List.of(tx));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String computeHash(int blockNo, String prevHash, LocalDateTime time, String txsJson) {
        return "0x" + sha256Hex(CHANNEL + "|" + blockNo + "|" + prevHash + "|"
                + time.format(FMT) + "|" + txsJson);
    }

    public String newTxId() {
        return "0x" + sha256Hex("tx|" + CHANNEL + "|" + System.nanoTime() + "|" + Thread.currentThread().getId())
                .substring(0, 20);
    }

    // ---------- 追加交易(一交易一块) ----------
    public synchronized Map<String, Object> appendTx(String op, Map<String, Object> payload,
                                                     String mspId, String orgName, LocalDateTime time) {
        return appendTx(op, payload, mspId, orgName, time, null);
    }

    public synchronized Map<String, Object> appendTx(String op, Map<String, Object> payload,
                                                     String mspId, String orgName, LocalDateTime time, String fixedTxId) {
        ChainBlock prev = blockMapper.selectOne(
                new LambdaQueryWrapper<ChainBlock>().orderByDesc(ChainBlock::getBlockNo).last("LIMIT 1"));
        int blockNo = prev == null ? 0 : prev.getBlockNo() + 1;
        String prevHash = prev == null ? genesisPrevHash() : prev.getHash();
        String txId = fixedTxId != null && !fixedTxId.isBlank() ? fixedTxId : newTxId();
        // 截断到秒: DATETIME 列会四舍五入毫秒, 必须与入库值完全一致才能通过哈希重算
        LocalDateTime t = (time != null ? time : LocalDateTime.now()).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        ChainBlock b = new ChainBlock();
        b.setBlockNo(blockNo);
        b.setPrevHash(prevHash);
        b.setTime(t);
        b.setTxCount(1);
        b.setChannel(CHANNEL);
        b.setTxId(txId);
        b.setOp(op);
        b.setMspId(mspId);
        b.setOrgName(orgName);
        try {
            b.setPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String txsJson = txsJson(txId, op, mspId, orgName, t, payload);
        b.setHash(computeHash(blockNo, prevHash, t, txsJson));
        blockMapper.insert(b);
        return Map.of("txId", txId, "blockNo", blockNo, "hash", b.getHash());
    }

    // ---------- 完整性校验 ----------
    public Map<String, Object> verifyChain() {
        List<ChainBlock> blocks = blockMapper.selectList(
                new LambdaQueryWrapper<ChainBlock>().orderByAsc(ChainBlock::getBlockNo));
        String checkedAt = LocalDateTime.now().format(FMT);
        for (int i = 0; i < blocks.size(); i++) {
            ChainBlock b = blocks.get(i);
            String expectPrev = i == 0 ? genesisPrevHash() : blocks.get(i - 1).getHash();
            String txsJson = txsJson(b.getTxId(), b.getOp(), b.getMspId(), b.getOrgName(), b.getTime(),
                    parsePayload(b.getPayload()));
            if (!b.getPrevHash().equals(expectPrev)) {
                return Map.of("valid", false, "blocks", blocks.size(), "brokenAt", b.getBlockNo(),
                        "reason", String.format("区块 #%d 前向哈希与上一块不匹配", b.getBlockNo()), "checkedAt", checkedAt);
            }
            String expectHash = computeHash(b.getBlockNo(), b.getPrevHash(), b.getTime(), txsJson);
            if (!b.getHash().equals(expectHash)) {
                return Map.of("valid", false, "blocks", blocks.size(), "brokenAt", b.getBlockNo(),
                        "reason", String.format("区块 #%d 哈希自校验失败，数据疑似被篡改", b.getBlockNo()), "checkedAt", checkedAt);
            }
        }
        return Map.of("valid", true, "blocks", blocks.size(), "checkedAt", checkedAt,
                "reason", "全链 SHA-256 哈希校验通过，存证完整可信");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    // ---------- 统计 ----------
    public Map<String, Object> chainStats() {
        List<ChainBlock> blocks = blockMapper.selectList(null);
        Map<String, Long> orgDist = new LinkedHashMap<>();
        Map<String, Long> opDist = new LinkedHashMap<>();
        String today = LocalDate();
        long todayTx = 0;
        for (ChainBlock b : blocks) {
            orgDist.merge(b.getOrgName(), 1L, Long::sum);
            opDist.merge(b.getOp(), 1L, Long::sum);
            if (b.getTime() != null && b.getTime().format(FMT).startsWith(today)) todayTx++;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("channel", CHANNEL);
        m.put("totalBlocks", blocks.size());
        m.put("totalTx", blocks.stream().mapToInt(ChainBlock::getTxCount).sum());
        m.put("todayTx", todayTx);
        m.put("orgDist", toDist(orgDist));
        m.put("opDist", toDist(opDist));
        return m;
    }

    private String LocalDate() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private List<Map<String, Object>> toDist(Map<String, Long> dist) {
        return dist.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("value", e.getValue());
                    return m;
                }).collect(Collectors.toList());
    }

    // ---------- 区块分页 ----------
    public List<ChainBlock> latestBlocks(int n) {
        return blockMapper.selectList(new LambdaQueryWrapper<ChainBlock>()
                .orderByDesc(ChainBlock::getBlockNo).last("LIMIT " + n));
    }

    public Map<String, Object> listBlocks(int page, int size, String keyword) {
        List<ChainBlock> list = blockMapper.selectList(
                new LambdaQueryWrapper<ChainBlock>().orderByDesc(ChainBlock::getBlockNo));
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.toLowerCase();
            list = list.stream().filter(b -> (String.valueOf(b.getBlockNo()) + b.getHash() + b.getTxId()
                    + b.getOp() + b.getOrgName() + b.getPayload()).toLowerCase().contains(kw)).toList();
        }
        // payload 还原为对象便于前端展示
        List<Map<String, Object>> records = list.stream().skip((long) (page - 1) * size).limit(size)
                .map(this::toVo).collect(Collectors.toList());
        return Map.of("total", list.size(), "records", records);
    }

    public Map<String, Object> toVo(ChainBlock b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("blockNo", b.getBlockNo());
        m.put("prevHash", b.getPrevHash());
        m.put("hash", b.getHash());
        m.put("time", b.getTime() == null ? null : b.getTime().format(FMT));
        m.put("txCount", b.getTxCount());
        m.put("channel", b.getChannel());
        m.put("payload", parsePayload(b.getPayload()));
        Map<String, Object> tx = new LinkedHashMap<>();
        tx.put("txId", b.getTxId());
        tx.put("op", b.getOp());
        tx.put("mspId", b.getMspId());
        tx.put("orgName", b.getOrgName());
        tx.put("time", b.getTime() == null ? null : b.getTime().format(FMT));
        tx.put("payload", parsePayload(b.getPayload()));
        m.put("txs", List.of(tx));
        return m;
    }

    // ---------- 交易查询 ----------
    public Map<String, Object> findTx(String txId) {
        ChainBlock b = blockMapper.selectOne(new LambdaQueryWrapper<ChainBlock>()
                .eq(ChainBlock::getTxId, txId.toLowerCase().startsWith("0x") ? txId : "0x" + txId));
        if (b == null) {
            throw new BizException("交易不存在");
        }
        Map<String, Object> vo = toVo(b);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> txs = (List<Map<String, Object>>) vo.get("txs");
        Map<String, Object> out = new LinkedHashMap<>(txs.getFirst());
        out.put("blockNo", b.getBlockNo());
        out.put("blockHash", b.getHash());
        out.put("channel", b.getChannel());
        return out;
    }

    /** 按链码方法与 payload 字段值检索交易(用于链上历史) */
    public List<Map<String, Object>> txsByOp(List<String> ops, String payloadField, String value) {
        List<ChainBlock> blocks = blockMapper.selectList(
                new LambdaQueryWrapper<ChainBlock>().in(ChainBlock::getOp, ops));
        List<Map<String, Object>> hits = new ArrayList<>();
        for (ChainBlock b : blocks) {
            Map<String, Object> payload = parsePayload(b.getPayload());
            if (value.equals(String.valueOf(payload.getOrDefault(payloadField, "")))) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("txId", b.getTxId());
                m.put("blockNo", b.getBlockNo());
                m.put("time", b.getTime() == null ? null : b.getTime().format(FMT));
                m.put("op", b.getOp());
                m.put("payload", payload);
                hits.add(m);
            }
        }
        return hits;
    }
}
