package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.*;
import cn.donkeychain.trace.entity.SysOrg;
import cn.donkeychain.trace.mapper.SysOrgMapper;
import cn.donkeychain.trace.service.ChainService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OrgController {

    private final SysOrgMapper orgMapper;
    private final ChainService chain;

    @GetMapping("/orgs")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysOrg> qw = new LambdaQueryWrapper<SysOrg>().orderByAsc(SysOrg::getId);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(SysOrg::getOrgName, keyword).or().like(SysOrg::getOrgCode, keyword)
                    .or().like(SysOrg::getContact, keyword).or().like(SysOrg::getRegion, keyword));
        }
        return Result.ok(Pages.of(orgMapper.selectPage(new Page<>(page, size), qw)));
    }

    @PostMapping("/orgs")
    public Result<SysOrg> create(@RequestBody SysOrg org) {
        org.setId(null);
        org.setStatus(1);
        org.setCreateTime(LocalDateTime.now());
        orgMapper.insert(org);
        chain.appendTx("JoinNetwork", Map.of("orgCode", org.getOrgCode(), "orgName", org.getOrgName(),
                "mspId", org.getMspId()), "OrgPlatformMSP", "驴链平台运营方", org.getCreateTime());
        return Result.ok(org);
    }

    @PutMapping("/orgs/{id}/status")
    public Result<SysOrg> setStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SysOrg o = orgMapper.selectById(id);
        if (o == null) throw new BizException("组织不存在");
        o.setStatus(Integer.parseInt(String.valueOf(body.get("status"))));
        orgMapper.updateById(o);
        return Result.ok(o);
    }
}
