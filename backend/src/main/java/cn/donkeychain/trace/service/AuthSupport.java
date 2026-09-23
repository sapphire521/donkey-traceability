package cn.donkeychain.trace.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 角色权限与动态菜单(与前端 Mock 版一致) */
@Component
public class AuthSupport {

    public static final Map<String, String> ROLE_NAME = Map.ofEntries(
            Map.entry("PLATFORM_ADMIN", "平台管理员"), Map.entry("FARM_OPERATOR", "养殖员"),
            Map.entry("FARM_MANAGER", "养殖场长"), Map.entry("PLANT_OPERATOR", "加工员"),
            Map.entry("PLANT_QC", "质检员"), Map.entry("LOGISTICS_DRIVER", "司机"),
            Map.entry("LOGISTICS_DISPATCHER", "调度员"), Map.entry("SHOP_KEEPER", "门店店员"),
            Map.entry("REGULATOR_OFFICER", "监管员"), Map.entry("REGULATOR_ADMIN", "监管管理员"),
            Map.entry("CONSUMER", "消费者"));

    public static final Map<String, List<String>> ROLE_PERMS = new LinkedHashMap<>();
    static {
        ROLE_PERMS.put("PLATFORM_ADMIN", List.of("org:manage", "donkey:write", "batch:slaughter", "batch:process",
                "qc:report", "transport:manage", "shop:stock", "code:bind", "cert:manage", "regulator:audit",
                "regulator:recall", "alert:view", "stats:screen", "trace:view", "sys:manage"));
        ROLE_PERMS.put("FARM_OPERATOR", List.of("donkey:write", "cert:manage", "alert:view"));
        ROLE_PERMS.put("FARM_MANAGER", List.of("donkey:write", "cert:manage", "alert:view", "trace:view"));
        ROLE_PERMS.put("PLANT_OPERATOR", List.of("batch:slaughter", "batch:process", "cert:manage", "alert:view", "trace:view"));
        ROLE_PERMS.put("PLANT_QC", List.of("qc:report", "cert:manage", "alert:view", "trace:view"));
        ROLE_PERMS.put("LOGISTICS_DISPATCHER", List.of("transport:manage", "alert:view", "trace:view"));
        ROLE_PERMS.put("LOGISTICS_DRIVER", List.of("transport:manage", "alert:view", "trace:view"));
        ROLE_PERMS.put("SHOP_KEEPER", List.of("shop:stock", "code:bind", "cert:manage", "alert:view", "trace:view"));
        ROLE_PERMS.put("REGULATOR_OFFICER", List.of("regulator:audit", "cert:manage", "alert:view", "stats:screen", "trace:view"));
        ROLE_PERMS.put("REGULATOR_ADMIN", List.of("regulator:audit", "regulator:recall", "cert:manage", "alert:view", "stats:screen", "trace:view"));
        ROLE_PERMS.put("CONSUMER", List.of());
    }

    public List<String> permsOf(String role) {
        return ROLE_PERMS.getOrDefault(role, List.of());
    }

    /** 动态菜单: 按 permission 过滤 */
    public List<Map<String, Object>> menusOf(String role) {
        List<String> perms = permsOf(role);
        return MENU_TEMPLATE.stream()
                .map(n -> filter(n, perms))
                .filter(n -> n != null)
                .map(n -> (Map<String, Object>) n)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> filter(Map<String, Object> n, List<String> perms) {
        String perm = (String) n.get("permission");
        if (perm != null && !perms.contains(perm)) return null;
        List<Map<String, Object>> children = (List<Map<String, Object>>) n.get("children");
        if (children != null) {
            List<Map<String, Object>> kids = children.stream()
                    .map(c -> filter(c, perms)).filter(c -> c != null)
                    .map(c -> (Map<String, Object>) c).toList();
            Map<String, Object> copy = new LinkedHashMap<>(n);
            if (perm != null) {
                copy.put("children", kids.isEmpty() ? null : kids);
                return copy;
            }
            return kids.isEmpty() ? null : copy;
        }
        return new LinkedHashMap<>(n);
    }

    private static final List<Map<String, Object>> MENU_TEMPLATE = List.of(
            menu("/dashboard", "工作台", "Odometer", null, null),
            menu("/platform", "联盟管理", "Connection", "org:manage", List.of(
                    menu("/platform/org", "组织管理", null, null, null),
                    menu("/platform/user", "用户管理", null, null, null),
                    menu("/platform/notice", "联盟公告", null, null, null))),
            menu("/farm", "养殖管理", "Food", "donkey:write", List.of(
                    menu("/farm/donkey", "驴只档案", null, null, null))),
            menu("/plant", "屠宰加工", "Goods", "batch:slaughter", List.of(
                    menu("/plant/slaughter", "屠宰批次", null, null, null),
                    menu("/plant/process", "加工批次", null, null, null),
                    menu("/plant/product", "成品批次", null, null, null),
                    menu("/plant/qc", "出厂检验", null, null, null))),
            menu("/logistics", "冷链物流", "Van", "transport:manage", List.of(
                    menu("/logistics/transport", "运输管理", null, null, null))),
            menu("/shop", "门店管理", "Shop", "shop:stock", List.of(
                    menu("/shop/inventory", "库存管理", null, null, null),
                    menu("/shop/output", "产出记录", null, null, null),
                    menu("/shop/trace-code", "溯源码管理", null, null, null))),
            menu("/trace", "溯源引擎", "Share", "trace:view", List.of(
                    menu("/trace/tree", "批次树可视化", null, null, null),
                    menu("/trace/report", "溯源报告预览", null, null, null),
                    menu("/trace/chain", "区块链账本", null, null, null))),
            menu("/cert", "证照管理", "Postcard", "cert:manage", null),
            menu("/regulator", "监管中心", "Monitor", "regulator:audit", List.of(
                    menu("/regulator/inspection", "抽检登记", null, null, null),
                    menu("/regulator/recall", "召回管理", null, null, null),
                    menu("/regulator/screen", "数据大屏", null, null, null))),
            menu("/alert", "预警中心", "Warning", "alert:view", null),
            menu("/system", "系统管理", "Setting", "sys:manage", List.of(
                    menu("/system/log", "操作日志", null, null, null),
                    menu("/system/config", "参数配置", null, null, null)))
    );

    private static Map<String, Object> menu(String path, String title, String icon, String permission, List<Map<String, Object>> children) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("path", path);
        m.put("title", title);
        if (icon != null) m.put("icon", icon);
        if (permission != null) m.put("permission", permission);
        if (children != null) m.put("children", children);
        return m;
    }
}
