package com.lanlink.shopping.common;

import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusiness(BusinessException e) {
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public R<Void> handleValid(Exception e) {
        BindException be = (e instanceof BindException) ? (BindException) e : null;
        FieldError fe = (be != null) ? be.getFieldError()
                : ((MethodArgumentNotValidException) e).getBindingResult().getFieldError();
        String msg = fe == null ? "参数校验失败" : fe.getDefaultMessage();
        return R.fail(400, msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public R<Void> handleIllegalArgument(IllegalArgumentException e) {
        return R.fail(400, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception e) {
        e.printStackTrace();
        return R.fail("服务器内部错误: " + e.getMessage());
    }
}
