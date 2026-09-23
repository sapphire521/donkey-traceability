package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.Result;
import cn.donkeychain.trace.entity.SysNotice;
import cn.donkeychain.trace.mapper.SysNoticeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PlatformController {

    private final SysNoticeMapper noticeMapper;

    @GetMapping("/platform/notices")
    public Result<List<SysNotice>> notices() {
        return Result.ok(noticeMapper.selectList(
                new LambdaQueryWrapper<SysNotice>().orderByDesc(SysNotice::getTime)));
    }
}
