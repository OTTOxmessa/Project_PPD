package com.example.portfolio.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

// แปลง exception ทุกชนิดเป็น ErrorResponse รูปแบบเดียวกัน พร้อม status code ที่ถูกต้อง
//   400 ข้อมูลไม่ผ่าน validation / รูปแบบผิด   401 login ไม่สำเร็จ   404 ไม่พบข้อมูลหรือ URL
//   405 method ไม่รองรับ                      409 ขัดกับสถานะข้อมูลปัจจุบัน (ซ้ำ, ขายเกิน, ยังถูกอ้างอิงอยู่)
//   500 ข้อผิดพลาดที่ไม่คาดคิด (ไม่เปิดเผยรายละเอียดภายใน)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    // login ผิด (อีเมลไม่มี หรือรหัสผ่านไม่ตรง) — ตอบเหมือนกันทั้งสองกรณี
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex,
                                                                  HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, List.of());
    }

    // @Valid @RequestBody ไม่ผ่าน — บอกทุกฟิลด์ที่ผิดใน details
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "ข้อมูลที่ส่งมาไม่ผ่านการตรวจสอบ", request, details);
    }

    // constraint บน @PathVariable / @RequestParam ไม่ผ่าน เช่น symbol ยาวเกิน 20 ตัวอักษร
    // Spring อาจส่ง error ของ @Valid @RequestBody มาทางนี้ด้วย (ParameterErrors) — กรณีนั้นต้องบอกชื่อฟิลด์
    // เช่น "price: ..." ไม่ใช่ชื่อพารามิเตอร์ของเมธอด "request: ..." ที่ผู้ใช้ไม่รู้ว่าหมายถึงช่องไหน
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException ex,
                                                                   HttpServletRequest request) {
        List<String> details = ex.getParameterValidationResults().stream()
                .flatMap(GlobalExceptionHandler::describe)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "ข้อมูลที่ส่งมาไม่ผ่านการตรวจสอบ", request, details);
    }

    private static Stream<String> describe(ParameterValidationResult result) {
        if (result instanceof ParameterErrors errors) {
            return errors.getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getDefaultMessage());
        }
        String parameter = result.getMethodParameter().getParameterName();
        return result.getResolvableErrors().stream()
                .map(error -> parameter + ": " + error.getDefaultMessage());
    }

    // JSON ผิดรูปแบบ หรือค่า enum/ตัวเลขที่อ่านไม่ได้ เช่น "type":"HOLD"
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "รูปแบบข้อมูลที่ส่งมาไม่ถูกต้อง", request, List.of());
    }

    // path/query ผิดชนิด เช่น /portfolios/abc หรือ ?from=not-a-date
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "ค่าของ '" + ex.getName() + "' ไม่ถูกต้อง", request, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex,
                                                            HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "ต้องระบุพารามิเตอร์ '" + ex.getParameterName() + "'", request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "ไม่พบ URL นี้", request, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "URL นี้ไม่รองรับ " + ex.getMethod(), request, List.of());
    }

    // กฎทางธุรกิจไม่ผ่าน เช่น ขายเกินจำนวนที่ถือ, อีเมลซ้ำ, ลบสินทรัพย์ที่ยังมีคนถือ
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    // ฐานข้อมูลปฏิเสธเพราะ constraint (UNIQUE / FK RESTRICT) — ด่านสุดท้ายกรณีมี request ชนกันพร้อมกัน
    // ไม่ส่งข้อความจากฐานข้อมูลกลับไป เพราะมีชื่อตาราง/คอลัมน์ภายใน
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "ข้อมูลขัดแย้งกับข้อมูลที่มีอยู่ (ซ้ำ หรือยังถูกอ้างอิงอยู่)", request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex,
                                                               HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    // ไม่ส่งรายละเอียดภายใน (stack trace, ข้อความจากฐานข้อมูล) กลับไปให้ client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "เกิดข้อผิดพลาดที่ไม่คาดคิด", request, List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest request,
                                                List<String> details) {
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), details);
        return ResponseEntity.status(status).body(body);
    }
}
