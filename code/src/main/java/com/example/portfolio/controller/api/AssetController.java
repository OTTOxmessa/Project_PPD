package com.example.portfolio.controller.api;

import com.example.portfolio.dto.request.AssetRequest;
import com.example.portfolio.dto.request.EnsureAssetRequest;
import com.example.portfolio.dto.response.AssetResponse;
import com.example.portfolio.dto.response.PricePointResponse;
import com.example.portfolio.dto.response.QuoteResponse;
import com.example.portfolio.mapper.AssetMapper;
import com.example.portfolio.mapper.PriceHistoryMapper;
import com.example.portfolio.mapper.QuoteMapper;
import com.example.portfolio.service.AssetMaintenanceService;
import com.example.portfolio.service.AssetService;
import com.example.portfolio.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// Resource หลัก: สินทรัพย์ (CRUD ครบ) + sub-resource ราคา
@Tag(name = "Assets", description = "สินทรัพย์ (CRUD), ราคาล่าสุด และราคาย้อนหลัง")
@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetMaintenanceService assetMaintenanceService;
    private final QuoteService quoteService;

    @GetMapping
    @Operation(summary = "รายการสินทรัพย์ทั้งหมด")
    public List<AssetResponse> list() {
        return assetService.getAll().stream().map(AssetMapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "ดูสินทรัพย์ (404 ถ้าไม่มี)")
    public AssetResponse get(@PathVariable Long id) {
        return AssetMapper.toResponse(assetService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "เพิ่มสินทรัพย์ (201, symbol ซ้ำ = 409)")
    public AssetResponse create(@Valid @RequestBody AssetRequest request) {
        return AssetMapper.toResponse(assetService.create(AssetMapper.toEntity(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "แก้ชื่อ/ประเภท/ตลาด (เปลี่ยน symbol ไม่ได้ = 409)")
    public AssetResponse update(@PathVariable Long id, @Valid @RequestBody AssetRequest request) {
        return AssetMapper.toResponse(assetMaintenanceService.update(id, AssetMapper.toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "ลบสินทรัพย์ (204, ยังมีพอร์ตถือหรือมีธุรกรรม = 409)")
    public void delete(@PathVariable Long id) {
        assetMaintenanceService.delete(id);
    }

    // ระบุสินทรัพย์ด้วย symbol (natural key) — PUT จึง idempotent: เรียกซ้ำกี่ครั้งก็ได้ asset ตัวเดิม ไม่สร้างซ้ำ
    // ถ้ายังไม่มีจะสร้างจากรายชื่อหุ้นอ้างอิง ใช้ตอนเลือกหุ้นจากช่องค้นหาในหน้าพอร์ต/watchlist
    @PutMapping("/by-symbol/{symbol}")
    @Operation(summary = "หา หรือ สร้างสินทรัพย์จาก symbol (idempotent)")
    public AssetResponse ensureBySymbol(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9.\\-]{1,20}", message = "ต้องเป็นตัวอักษร ตัวเลข . หรือ - ไม่เกิน 20 ตัว")
            String symbol,
            @Valid @RequestBody EnsureAssetRequest request) {
        return AssetMapper.toResponse(assetService.ensure(symbol, request.name(),
                request.assetType(), request.exchange(), request.referencePrice()));
    }

    // ราคาล่าสุด + % เปลี่ยนแปลงรายวัน (หัวกระดานเทรด)
    @GetMapping("/{id}/quote")
    @Operation(summary = "ราคาล่าสุดและ % เปลี่ยนแปลงรายวัน")
    public QuoteResponse quote(@PathVariable Long id) {
        return QuoteMapper.toResponse(quoteService.getQuote(assetService.getById(id)));
    }

    @GetMapping("/{id}/prices")
    @Operation(summary = "ราคาย้อนหลังตามช่วงวันที่")
    public List<PricePointResponse> prices(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return assetService.getPriceHistory(id, from, to).stream()
                .map(PriceHistoryMapper::toResponse)
                .toList();
    }
}
