package cn.donkeychain.trace.controller;

import cn.donkeychain.trace.common.*;
import cn.donkeychain.trace.entity.SysOrg;
import cn.donkeychain.trace.entity.SysUser;
import cn.donkeychain.trace.mapper.SysOrgMapper;
import cn.donkeychain.trace.mapper.SysUserMapper;
import cn.donkeychain.trace.service.AuthSupport;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper userMapper;
    private final SysOrgMapper orgMapper;
    private final AuthSupport authSupport;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Data
    public static class LoginReq {
        private String username;
        private String password;
    }

    @PostMapping("/auth/login")
    public Result<Map<String, Object>> login(@RequestBody LoginReq req) {
        SysUser u = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername() == null ? "" : req.getUsername()));
        if (u == null) throw new BizException("账号不存在");
        if (u.getStatus() != null && u.getStatus() != 1) throw new BizException("账号已停用");
        if (req.getPassword() == null || !encoder.matches(req.getPassword(), u.getPassword())) {
            throw new BizException("密码错误");
        }
        SysOrg org = orgMapper.selectById(u.getOrgId());
        u.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(u);

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("userId", u.getId());
        info.put("username", u.getUsername());
        info.put("realName", u.getRealName());
        info.put("orgId", u.getOrgId());
        info.put("orgName", org == null ? "" : org.getOrgName());
        info.put("orgType", org == null ? "" : org.getOrgType());
        info.put("roleCode", u.getRoleCode());
        info.put("roleName", AuthSupport.ROLE_NAME.getOrDefault(u.getRoleCode(), u.getRoleCode()));
        info.put("mspId", org == null ? "" : org.getMspId());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("accessToken", jwtUtil.issue(u.getId(), u.getUsername(), u.getRoleCode(), u.getOrgId()));
        res.put("refreshToken", jwtUtil.issue(u.getId(), u.getUsername(), u.getRoleCode(), u.getOrgId()));
        res.put("user", info);
        res.put("permissions", authSupport.permsOf(u.getRoleCode()));
        res.put("menus", authSupport.menusOf(u.getRoleCode()));
        return Result.ok(res);
    }

    @PostMapping("/auth/refresh")
    public Result<Map<String, Object>> refresh(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : "";
        jwtUtil.parse(token);
        return Result.ok(Map.of("accessToken", token));
    }

    @PostMapping("/auth/logout")
    public Result<Map<String, Object>> logout() {
        return Result.ok(Map.of());
    }

    @GetMapping("/auth/profile")
    public Result<Map<String, Object>> profile() {
        SysUser u = userMapper.selectById(UserContext.userId());
        SysOrg org = u == null ? null : orgMapper.selectById(u.getOrgId());
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("userId", u.getId());
        info.put("username", u.getUsername());
        info.put("realName", u.getRealName());
        info.put("orgId", u.getOrgId());
        info.put("orgName", org == null ? "" : org.getOrgName());
        info.put("orgType", org == null ? "" : org.getOrgType());
        info.put("roleCode", u.getRoleCode());
        info.put("roleName", AuthSupport.ROLE_NAME.getOrDefault(u.getRoleCode(), u.getRoleCode()));
        return Result.ok(info);
    }

    @Data
    public static class PasswordReq {
        private String oldPassword;
        private String newPassword;
    }

    @PutMapping("/auth/password")
    public Result<Map<String, Object>> password(@RequestBody PasswordReq req) {
        SysUser u = userMapper.selectById(UserContext.userId());
        if (u == null || !encoder.matches(req.getOldPassword(), u.getPassword())) {
            throw new BizException("原密码错误");
        }
        u.setPassword(encoder.encode(req.getNewPassword()));
        userMapper.updateById(u);
        return Result.ok(Map.of("ok", true));
    }
}
