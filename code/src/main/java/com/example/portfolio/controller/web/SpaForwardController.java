package com.example.portfolio.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Presentation layer ฝั่งหน้าเว็บ (คู่กับ controller/api ที่เป็น REST)
// ตอน deploy ไฟล์ build ของ React จะอยู่ใน resources/static — URL ของหน้าเว็บทุกหน้า (React Router)
// ต้องได้ index.html กลับไป แล้ว React จะแสดงหน้าที่ตรงกับ URL เอง
// ตอนพัฒนาใช้ Vite ที่พอร์ต 5173 คลาสนี้จึงไม่มีผล
@Controller
public class SpaForwardController {

    @GetMapping({"/", "/login", "/register", "/assets", "/portfolios/{id}"})
    public String forwardToIndex() {
        return "forward:/index.html";
    }
}
