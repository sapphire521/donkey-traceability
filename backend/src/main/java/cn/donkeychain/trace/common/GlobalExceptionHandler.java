package cn.donkeychain.trace.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> biz(BizException e) {
        return Result.fail(50001, e.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> unauthorized(UnauthorizedException e) {
        return Result.fail(40100, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> other(Exception e) {
        log.error("unhandled", e);
        return Result.fail(50000, "服务器内部错误: " + e.getMessage());
    }
}
