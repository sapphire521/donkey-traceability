package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.Result;
import cn.donkeychain.trace.entity.*;
import cn.donkeychain.trace.mapper.*;
import cn.donkeychain.trace.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statsService.overview());
    }
}
