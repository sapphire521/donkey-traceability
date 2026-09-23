package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.*;
import cn.donkeychain.trace.entity.SysConfig;
import cn.donkeychain.trace.entity.SysOperationLog;
import cn.donkeychain.trace.mapper.SysConfigMapper;
import cn.donkeychain.trace.mapper.SysOperationLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SystemController {

    private final SysOperationLogMapper logMapper;
    private final SysConfigMapper configMapper;

    @GetMapping("/system/logs")
    public Result<Map<String, Object>> logs(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysOperationLog> qw = new LambdaQueryWrapper<SysOperationLog>()
                .orderByDesc(SysOperationLog::getCreateTime).orderByDesc(SysOperationLog::getId);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(SysOperationLog::getModule, keyword).or().like(SysOperationLog::getOperation, keyword)
                    .or().like(SysOperationLog::getUserName, keyword));
        }
        return Result.ok(Pages.of(logMapper.selectPage(new Page<>(page, size), qw)));
    }

    @GetMapping("/system/configs")
    public Result<List<SysConfig>> configs() {
        return Result.ok(configMapper.selectList(new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getId)));
    }

    @PutMapping("/system/configs/{key}")
    public Result<SysConfig> update(@PathVariable String key, @RequestBody Map<String, Object> body) {
        SysConfig c = configMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key));
        if (c == null) throw new BizException("参数不存在");
        c.setConfigValue((String) body.get("configValue"));
        configMapper.updateById(c);
        return Result.ok(c);
    }
}
