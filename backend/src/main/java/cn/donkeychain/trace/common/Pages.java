package cn.donkeychain.trace.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.LinkedHashMap;
import java.util.Map;

public class Pages {
    public static <T> Map<String, Object> of(Page<T> p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", p.getTotal());
        m.put("records", p.getRecords());
        return m;
    }
}
