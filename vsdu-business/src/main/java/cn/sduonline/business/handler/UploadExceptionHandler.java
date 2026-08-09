package cn.sduonline.business.handler;

import cn.sduonline.common.exception.BizCode;
import cn.sduonline.common.result.Result;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSizeExceeded() {
        return ResponseEntity.badRequest()
                .body(Result.error(BizCode.BAD_REQUEST, "单张图片不能超过20MB，单次最多上传9张"));
    }
}
