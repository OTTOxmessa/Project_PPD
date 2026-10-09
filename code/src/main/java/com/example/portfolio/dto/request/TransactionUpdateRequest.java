package com.example.portfolio.dto.request;

import com.example.portfolio.domain.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

// แก้รายการซื้อขาย: เปลี่ยนได้ทุกอย่างยกเว้นสินทรัพย์ (ถ้าเลือกหุ้นผิด ให้ลบแล้วบันทึกใหม่)
public record TransactionUpdateRequest(
        @NotNull(message = "กรุณาเลือกประเภทรายการ") TransactionType type,
        @NotNull(message = "กรุณากรอกจำนวน")
        @DecimalMin(value = "0", inclusive = false, message = "จำนวนต้องมากกว่า 0") BigDecimal quantity,
        @NotNull(message = "กรุณากรอกราคาต่อหน่วย")
        @DecimalMin(value = "0", inclusive = false, message = "ราคาต่อหน่วยต้องมากกว่า 0") BigDecimal price,
        @PastOrPresent(message = "วันที่ทำรายการต้องไม่เป็นวันในอนาคต") LocalDate executedAt // ไม่ส่ง = คงวันเดิม
) {
}
