package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.*;
import cn.donkeychain.trace.entity.BizCert;
import cn.donkeychain.trace.mapper.BizCertMapper;
import cn.donkeychain.trace.service.ChainService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CertController {

    private final BizCertMapper certMapper;
    private final ChainService chain;

    @GetMapping("/certs")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BizCert> qw = new LambdaQueryWrapper<BizCert>().orderByAsc(BizCert::getExpireDate);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(BizCert::getCertNo, keyword).or().like(BizCert::getCertType, keyword)
                    .or().like(BizCert::getOrgName, keyword));
        }
        return Result.ok(Pages.of(certMapper.selectPage(new Page<>(page, size), qw)));
    }

    @PostMapping("/certs")
    public Result<BizCert> create(@RequestBody BizCert c) {
        c.setId(null);
        c.setStatus(1);
        c.setAuditStatus("NONE");
        certMapper.insert(c);
        chain.appendTx("AddCert", Map.of("certNo", c.getCertNo(), "certType", c.getCertType(),
                "orgName", c.getOrgName() == null ? "" : c.getOrgName()), "OrgPlatformMSP",
                c.getOrgName() == null ? "" : c.getOrgName(), LocalDateTime.now());
        return Result.ok(c);
    }

    @GetMapping("/certs/expiring")
    public Result<List<BizCert>> expiring() {
        return Result.ok(certMapper.selectList(new LambdaQueryWrapper<BizCert>()
                .le(BizCert::getExpireDate, LocalDate.now().plusMonths(3))));
    }
}
