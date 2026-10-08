package com.example.portfolio.service;

import java.util.List;

// คำนวณมูลค่าตลาด / ต้นทุน / กำไรของแต่ละพอร์ต — แยกจาก PortfolioService (CRUD) ตาม SRP
public interface PortfolioValuationService {

    List<PortfolioSummary> summarize(Long userId);
}
