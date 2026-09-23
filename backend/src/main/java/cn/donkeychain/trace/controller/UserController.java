package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.*;
import cn.donkeychain.trace.entity.SysUser;
import cn.donkeychain.trace.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @GetMapping("/users")
    public Result<Map<String, Object>> list(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long size,
                                            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getRoleCode, keyword));
        }
        Page<SysUser> p = userMapper.selectPage(new Page<>(page, size), qw);
        p.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(Pages.of(p));
    }

    @PostMapping("/users")
    public Result<SysUser> create(@RequestBody Map<String, Object> body) {
        SysUser u = new SysUser();
        u.setOrgId(Long.valueOf(String.valueOf(body.get("orgId"))));
        u.setUsername((String) body.get("username"));
        u.setPassword(encoder.encode((String) body.getOrDefault("password", "123456")));
        u.setRealName((String) body.get("realName"));
        u.setPhone((String) body.get("phone"));
        u.setRoleCode((String) body.get("roleCode"));
        u.setStatus(1);
        userMapper.insert(u);
        u.setPassword(null);
        return Result.ok(u);
    }

    @PutMapping("/users/{id}/status")
    public Result<SysUser> setStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SysUser u = userMapper.selectById(id);
        if (u == null) throw new BizException("用户不存在");
        u.setStatus(Integer.parseInt(String.valueOf(body.get("status"))));
        userMapper.updateById(u);
        u.setPassword(null);
        return Result.ok(u);
    }
}
