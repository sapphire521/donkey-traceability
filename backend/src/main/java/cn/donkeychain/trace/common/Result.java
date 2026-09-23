package cn.donkeychain.trace.common;

import lombok.Data;
import java.util.UUID;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;
    private String traceId;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        r.traceId = "api-" + UUID.randomUUID().toString().substring(0, 8);
        return r;
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        r.data = null;
        r.traceId = "api-" + UUID.randomUUID().toString().substring(0, 8);
        return r;
    }
}
