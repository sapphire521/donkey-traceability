package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.*;
import cn.donkeychain.trace.entity.BizAlert;
import cn.donkeychain.trace.mapper.BizAlertMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AlertController {

    private final BizAlertMapper alertMapper;

    @GetMapping("/alerts")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) String level,
                                            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BizAlert> qw = new LambdaQueryWrapper<BizAlert>()
                .orderByDesc(BizAlert::getCreateTime).orderByDesc(BizAlert::getId);
        if (status != null && !status.isBlank()) qw.eq(BizAlert::getStatus, status);
        if (level != null && !level.isBlank()) qw.eq(BizAlert::getLevel, level);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(BizAlert::getContent, keyword).or().like(BizAlert::getTargetId, keyword)
                    .or().like(BizAlert::getAlertType, keyword));
        }
        return Result.ok(Pages.of(alertMapper.selectPage(new Page<>(page, size), qw)));
    }

    @PutMapping("/alerts/{id}/handle")
    public Result<BizAlert> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BizAlert a = alertMapper.selectById(id);
        if (a == null) throw new BizException("预警不存在");
        a.setStatus((String) body.getOrDefault("status", "RESOLVED"));
        a.setHandler((String) body.getOrDefault("handler", "当前用户"));
        a.setHandleNote((String) body.get("handleNote"));
        a.setHandleTime(LocalDateTime.now());
        alertMapper.updateById(a);
        return Result.ok(a);
    }
}
