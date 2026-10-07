# SOLID Analysis — ระบบจัดการพอร์ตการลงทุน

เอกสารนี้ระบุว่าหลัก SOLID แต่ละข้อปรากฏที่ไฟล์และบรรทัดไหนในโค้ด พร้อมเหตุผล (ใบงานข้อ 4)
เลขบรรทัดสร้างจากซอร์สโค้ดจริง คลิกชื่อไฟล์เพื่อเปิดบรรทัดนั้นบน GitHub ได้

นอกจากเอกสารนี้ กฎสำคัญของแต่ละหลักการยังถูก **ตรวจอัตโนมัติทุกครั้งที่รัน `mvn test`** ด้วย
[`SolidPrinciplesTest`](../code/src/test/java/com/example/portfolio/architecture/SolidPrinciplesTest.java)
และ [`LayeredArchitectureTest`](../code/src/test/java/com/example/portfolio/architecture/LayeredArchitectureTest.java) — ถ้ามีใครเขียนโค้ดผิดหลัก test จะล้มทันที

| หลักการ | กฎที่ใบงานกำหนด | ตรวจอัตโนมัติด้วย |
|---|---|---|
| S | แต่ละ Class มีหน้าที่เดียว ไม่รวม Business + Validation + Persistence | `servicesStaySmall` และการแยก layer ใน `LayeredArchitectureTest` |
| O | เพิ่มฟีเจอร์ด้วยการเพิ่มคลาส ไม่ใช่แก้ if-else | `noSwitchInServiceLayer` |
| L | Subclass ใช้แทน Superclass ได้ ไม่ throw `UnsupportedOperationException` | `noUnsupportedOperations` |
| I | Interface เล็ก แยกตามการใช้งาน | `serviceInterfacesAreSmall` |
| D | Service ขึ้นกับ Interface + Constructor Injection เท่านั้น | `constructorInjectionOnly`, `dependOnAbstractions`, `everyServiceImplementsAnInterface` |

## S — Single Responsibility Principle

แต่ละคลาสมีเหตุผลให้ต้องแก้เพียงเรื่องเดียว และแยก Validation / Business / Persistence ออกจากกันคนละชั้น

### แยก Validation, Business และ Persistence คนละคลาส

การบันทึกธุรกรรมซื้อขาย 1 ครั้ง ผ่านคลาสที่ทำคนละหน้าที่:

| ไฟล์ | บรรทัด | สิ่งที่เห็นในโค้ด | เหตุผล |
|---|---|---|---|
| [`TransactionRequest.java`](../code/src/main/java/com/example/portfolio/dto/request/TransactionRequest.java#L14) | 14 | `@NotNull`, `@DecimalMin`, `@PastOrPresent` | **Validation** — ตรวจรูปแบบข้อมูลที่ DTO ด้วย Bean Validation ก่อนเข้าระบบ |
| [`TransactionServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/TransactionServiceImpl.java#L40) | 40 | `record(...)` บันทึกธุรกรรม + สั่งปรับ holding ใน transaction เดียว | **Business** — กฎทางธุรกิจอยู่ที่ service เท่านั้น |
| [`TransactionRepository.java`](../code/src/main/java/com/example/portfolio/repository/TransactionRepository.java#L11) | 11 | Spring Data JPA interface | **Persistence** — อ่าน/เขียนฐานข้อมูลที่ repository เท่านั้น |
| [`TransactionMapper.java`](../code/src/main/java/com/example/portfolio/mapper/TransactionMapper.java#L11) | 11 | แปลง Entity เป็น Response DTO | **Mapping** — แยกจาก controller และ service |
| [`GlobalExceptionHandler.java`](../code/src/main/java/com/example/portfolio/exception/GlobalExceptionHandler.java#L25) | 25 | แปลง exception เป็น ErrorResponse | **Error handling** — จุดเดียวของทั้งระบบ controller ไม่ต้อง try/catch เอง |

### Service ที่แยกตามหน้าที่

| ไฟล์ | บรรทัด | สิ่งที่เห็นในโค้ด | เหตุผล |
|---|---|---|---|
| [`PortfolioServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/PortfolioServiceImpl.java#L17) | 17 | CRUD พอร์ตอย่างเดียว | การคำนวณมูลค่า/กำไรเป็นอีกเรื่อง จึงแยกไป `PortfolioValuationServiceImpl` |
| [`PortfolioValuationServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/PortfolioValuationServiceImpl.java#L29) | 29 | คำนวณมูลค่าตลาด ต้นทุน กำไรของแต่ละพอร์ต | เปลี่ยนสูตรมูลค่าได้โดยไม่กระทบ CRUD |
| [`AssetServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/AssetServiceImpl.java#L25) | 25 | จัดการข้อมูลสินทรัพย์ (สร้าง/หา/ราคาย้อนหลัง) | ไม่มีโค้ดค้นหา/จัดอันดับปนอยู่แล้ว |
| [`SymbolSearchServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/SymbolSearchServiceImpl.java#L32) | 32 | ค้นหาและจัดอันดับหุ้นสำหรับ auto-complete | แยกออกจาก `AssetServiceImpl` เพราะเปลี่ยนด้วยเหตุผลต่างกัน (กติกาการค้นหา) |
| [`PriceAlertServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/PriceAlertServiceImpl.java#L21) | 21 | สร้าง/ดู/ลบ alert ที่ผู้ใช้ตั้ง | แยกจากการตรวจราคา |
| [`PriceAlertMonitorServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/PriceAlertMonitorServiceImpl.java#L63) | 63 | ตรวจราคาเทียบ alert ทุกตัวแล้วเปลี่ยนสถานะ | งานเบื้องหลังของ scheduler เป็นคนละหน้าที่กับ CRUD |
| [`PriceHistoryServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/PriceHistoryServiceImpl.java#L70) | 70 | นำเข้าราคาของสินทรัพย์หนึ่งตัว (จริงหรือสำรอง) | ไม่ต้องรู้เรื่องการวนทั้งระบบหรือการเว้นจังหวะ |
| [`MarketDataRefreshServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/MarketDataRefreshServiceImpl.java#L41) | 41 | วนอัปเดตทุกสินทรัพย์ เว้นจังหวะกันโดนจำกัด | แยกจากการนำเข้าราคารายตัว |
| [`RebalanceServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/RebalanceServiceImpl.java#L71) | 71 | ประสานงาน: คำนวณ → ตรวจ → ซื้อขาย → บันทึก log | การสร้าง Command และการจัดรูป log แยกไปอีก 2 คลาสด้านล่าง |
| [`RebalanceCommands.java`](../code/src/main/java/com/example/portfolio/service/rebalance/RebalanceCommands.java#L30) | 30 | สร้าง Command จากคำสั่งซื้อขาย | หน้าที่ "สร้าง" แยกจาก "ใช้" |
| [`TradeOrderFormatter.java`](../code/src/main/java/com/example/portfolio/service/rebalance/TradeOrderFormatter.java#L13) | 13 | แปลงคำสั่งเป็น JSON เก็บใน log | รูปแบบ log เปลี่ยนได้โดยไม่แตะ service |
| [`PivotPointCalculator.java`](../code/src/main/java/com/example/portfolio/service/analysis/PivotPointCalculator.java#L22) | 22 | สูตร Pivot Point อย่างเดียว | หนึ่งคลาสต่อหนึ่งสูตร |
| [`SymbolDirectory.java`](../code/src/main/java/com/example/portfolio/service/market/SymbolDirectory.java#L53) | 53 | โหลดรายชื่อหุ้นจากไฟล์ | แหล่งข้อมูลเปลี่ยน (เช่น เป็น API) แก้คลาสนี้คลาสเดียว |

## O — Open/Closed Principle

เพิ่มพฤติกรรมใหม่ด้วยการ **เพิ่มคลาส** แล้วให้ Spring ฉีดเข้ามาเป็น `List` — service เลือกตัวที่ใช้จากชื่อหรือประเภท ไม่มี `switch`/`if-else` ตามประเภทใน service layer

| ไฟล์ | บรรทัด | สิ่งที่เห็นในโค้ด | เหตุผล |
|---|---|---|---|
| [`StrategyRegistry.java`](../code/src/main/java/com/example/portfolio/common/StrategyRegistry.java#L30) | 30 | หา Strategy จากชื่อ (`?method=pivot`) | ใช้ร่วมกัน 3 ฟีเจอร์ แทน switch ใน controller เดิม |
| [`AllocationServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/AllocationServiceImpl.java#L55) | 55 | เลือก `AllocationStrategy` จากชื่อ | เพิ่มวิธีคำนวณสัดส่วนใหม่ = เพิ่มคลาส `@Component` ที่มี `key()` ใหม่ |
| [`RiskBasedAllocationStrategy.java`](../code/src/main/java/com/example/portfolio/service/allocation/RiskBasedAllocationStrategy.java#L20) | 20 | `key()` = "risk" | ตัวอย่าง Strategy ที่เพิ่มได้โดยไม่แก้ service |
| [`SupportResistanceServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/SupportResistanceServiceImpl.java#L29) | 29 | เลือกสูตรแนวรับ-แนวต้าน (pivot / ma) | เพิ่มสูตรใหม่ เช่น Fibonacci = เพิ่มหนึ่งคลาส |
| [`RebalanceServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/RebalanceServiceImpl.java#L72) | 72 | เลือกวิธีรีบาลานซ์ (threshold / calendar) | ไม่ต้องแก้ service เมื่อมีวิธีใหม่ |
| [`PendingState.java`](../code/src/main/java/com/example/portfolio/service/alert/state/PendingState.java#L41) | 41 | เลือก `AlertConditionEvaluator` ตามเงื่อนไขของ alert | แทน `switch (condition)` เดิม — เพิ่มเงื่อนไขใหม่ = เพิ่ม evaluator หนึ่งคลาส |
| [`PriceAlertMonitorServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/PriceAlertMonitorServiceImpl.java#L45) | 45 | เลือก `AlertState` ตามสถานะปัจจุบัน | แทน `if (status == PENDING) ... if (status == TRIGGERED)` เดิม |
| [`HoldingServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/HoldingServiceImpl.java#L53) | 53 | เลือก `HoldingUpdateRule` ตามประเภทธุรกรรม | แทน `switch (type)` เดิม — ซื้อ/ขายอยู่คนละคลาส |
| [`TransactionType.java`](../code/src/main/java/com/example/portfolio/domain/enums/TransactionType.java#L29) | 29 | แต่ละประเภทรู้ผลต่อจำนวนหน่วยของตัวเอง (+1 / −1 / 0) | ใช้แทน switch ใน `BenchmarkComparisonService` และ if ใน `TransactionServiceImpl` |
| [`BenchmarkComparisonService.java`](../code/src/main/java/com/example/portfolio/service/performance/BenchmarkComparisonService.java#L44) | 44 | `t.getType().signedQuantity(...)` | ไม่ต้องไล่เช็คประเภทธุรกรรม |
| [`TransactionServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/TransactionServiceImpl.java#L48) | 48 | `type.affectsHolding()` | แทน `if (type == BUY || type == SELL)` เดิม |
| [`RebalanceCommands.java`](../code/src/main/java/com/example/portfolio/service/rebalance/RebalanceCommands.java#L23) | 23 | ตารางจับคู่ประเภท → Command | แทน `switch (order.type())` เดิม |
| [`AlertSubject.java`](../code/src/main/java/com/example/portfolio/service/alert/AlertSubject.java#L20) | 20 | แจ้ง observer ทุกตัวที่ Spring ฉีดมา | เพิ่มช่องทางแจ้งเตือนใหม่ (SMS, LINE) = เพิ่มคลาส ไม่แก้ไฟล์นี้ |
| [`RebalanceValidationHandler.java`](../code/src/main/java/com/example/portfolio/service/rebalance/RebalanceValidationHandler.java#L13) | 13 | ต่อ handler ตรวจสอบเป็นสาย | เพิ่มเงื่อนไขตรวจใหม่ = เพิ่ม handler ต่อท้าย |
| [`PerformanceReportTemplate.java`](../code/src/main/java/com/example/portfolio/service/performance/PerformanceReportTemplate.java#L12) | 12 | ลำดับขั้นตอนตายตัว (final) | รายงานแบบใหม่ = subclass ใหม่ ไม่แก้ template |

## L — Liskov Substitution Principle

ทุก implementation ใช้แทนกันได้ผ่าน interface/abstract class โดยผู้เรียกไม่ต้องรู้ว่าเป็นตัวไหน และไม่มีตัวไหน throw `UnsupportedOperationException`

| ไฟล์ | บรรทัด | สิ่งที่เห็นในโค้ด | เหตุผล |
|---|---|---|---|
| [`AllocationServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/AllocationServiceImpl.java#L65) | 65 | เรียกผ่าน `AllocationStrategy` เท่านั้น | 3 implementation (target / equal / risk) สลับกันได้ ผลลัพธ์อยู่ในรูปเดียวกันเสมอ |
| [`PriceAlertMonitorServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/PriceAlertMonitorServiceImpl.java#L48) | 48 | เรียก `AlertState.handle()` โดยไม่รู้ว่าเป็นสถานะไหน | `AlertState` เหลือแค่ 2 เมธอดที่ทุกสถานะใช้จริง — เดิม `isConditionMet()` ไม่มีความหมายใน Triggered/Expired จึงตัดออก |
| [`ExpiredState.java`](../code/src/main/java/com/example/portfolio/service/alert/state/ExpiredState.java#L20) | 20 | สถานะสุดท้าย ไม่ทำอะไร แต่ไม่ throw | ผู้เรียกใช้แทนสถานะอื่นได้โดยไม่พัง |
| [`BenchmarkComparisonService.java`](../code/src/main/java/com/example/portfolio/service/performance/BenchmarkComparisonService.java#L25) | 25 | subclass ของ template | `PerformanceServiceImpl` ใช้ผ่านชนิด `PerformanceReportTemplate` ได้เลย |
| [`PerformanceServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/PerformanceServiceImpl.java#L17) | 17 | ขึ้นกับ abstract class | ใส่ subclass ไหนก็ได้ |
| [`PriceHistoryMarketDataProvider.java`](../code/src/main/java/com/example/portfolio/service/market/PriceHistoryMarketDataProvider.java#L21) | 21 | ราคาจากฐานข้อมูล (`@Primary`) | สลับกับ `ExternalMarketDataAdapter` ได้โดย scheduler/service ไม่ต้องแก้ |
| [`ExternalMarketDataAdapter.java`](../code/src/main/java/com/example/portfolio/service/market/adapter/ExternalMarketDataAdapter.java#L15) | 15 | ราคาจาก API ภายนอก | ทำสัญญาเดียวกันครบ |
| [`MinimumHoldingsValidationHandler.java`](../code/src/main/java/com/example/portfolio/service/rebalance/MinimumHoldingsValidationHandler.java#L10) | 10 | handler ในสายตรวจสอบ | ต่อแทนกันได้ทุกตำแหน่งในสาย |
| [`AlertCondition.java`](../code/src/main/java/com/example/portfolio/domain/enums/AlertCondition.java#L7) | 7 | เหลือเฉพาะเงื่อนไขที่ทำงานได้จริง | เดิม `PCT_CHANGE` คืน false เสมอ (สัญญาว่าทำได้แต่ทำไม่ได้) จึงเอาออก |

## I — Interface Segregation Principle

Interface ของ service ทุกตัวมีไม่เกิน 5 เมธอด และแยกตามผู้ใช้ — ผู้เรียกเห็นเฉพาะเมธอดที่ตัวเองใช้

### Interface ที่แยกออกจากกันเพราะผู้ใช้ต่างกัน

| ไฟล์ | บรรทัด | สิ่งที่เห็นในโค้ด | เหตุผล |
|---|---|---|---|
| [`AssetService.java`](../code/src/main/java/com/example/portfolio/service/AssetService.java#L12) | 12 | จัดการข้อมูลสินทรัพย์ (ใช้โดย `AssetController`) | เดิมรวมการค้นหาหุ้นไว้ด้วย |
| [`AssetMaintenanceService.java`](../code/src/main/java/com/example/portfolio/service/AssetMaintenanceService.java#L7) | 7 | แก้/ลบสินทรัพย์ (ใช้โดย `AssetController` เท่านั้น) | ส่วนอื่นของระบบแค่อ่านสินทรัพย์ จึงไม่ต้องเห็นเมธอดแก้/ลบ และ `AssetService` ไม่บวมเกิน 6 เมธอด |
| [`SymbolSearchService.java`](../code/src/main/java/com/example/portfolio/service/SymbolSearchService.java#L10) | 10 | ค้นหาหุ้น (ใช้โดย `SymbolController`) | controller ค้นหาไม่ต้องเห็นเมธอดสร้าง/แก้สินทรัพย์ |
| [`PriceAlertService.java`](../code/src/main/java/com/example/portfolio/service/PriceAlertService.java#L10) | 10 | CRUD alert (ใช้โดย `PriceAlertController`) | controller ไม่เห็นเมธอดของ scheduler |
| [`PriceAlertMonitorService.java`](../code/src/main/java/com/example/portfolio/service/PriceAlertMonitorService.java#L14) | 14 | ตรวจราคา (ใช้โดย `PriceAlertScheduler`) | scheduler ไม่เห็นเมธอด CRUD |
| [`PriceHistoryService.java`](../code/src/main/java/com/example/portfolio/service/PriceHistoryService.java#L12) | 12 | นำเข้าราคารายตัว (ใช้โดย `AssetServiceImpl`) | แยกจากการอัปเดตทั้งระบบ |
| [`MarketDataRefreshService.java`](../code/src/main/java/com/example/portfolio/service/MarketDataRefreshService.java#L9) | 9 | อัปเดตทั้งระบบ (ใช้โดย `MarketDataRefreshScheduler`) | เมธอดเดียว |
| [`AlertState.java`](../code/src/main/java/com/example/portfolio/service/alert/state/AlertState.java#L16) | 16 | 2 เมธอด: `status()`, `handle()` | ตัด `isConditionMet()` ที่บางสถานะไม่ได้ใช้ออก |
| [`AlertObserver.java`](../code/src/main/java/com/example/portfolio/service/alert/AlertObserver.java#L10) | 10 | เมธอดเดียว | ช่องทางแจ้งเตือนใหม่ implement แค่นี้ |
| [`AlertPublisher.java`](../code/src/main/java/com/example/portfolio/service/alert/AlertPublisher.java#L10) | 10 | เมธอดเดียว | `TriggeredState` ต้องการแค่ "ประกาศ" |
| [`RebalanceCommand.java`](../code/src/main/java/com/example/portfolio/service/rebalance/RebalanceCommand.java#L6) | 6 | เมธอดเดียว | Command ทุกตัวทำสัญญาเดียวกัน |
| [`TokenProvider.java`](../code/src/main/java/com/example/portfolio/security/TokenProvider.java#L4) | 4 | 3 เมธอดที่ระบบใช้จริงกับ token | ไม่เปิดรายละเอียดของ JWT |

### ขนาดของ Service interface ทุกตัว

| Interface | จำนวนเมธอด |
|---|---|
| `AllocationService` | 3 |
| `AssetMaintenanceService` | 2 |
| `AssetService` | 5 |
| `AuthService` | 2 |
| `HoldingService` | 2 |
| `MarketDataRefreshService` | 1 |
| `PerformanceService` | 1 |
| `PortfolioService` | 5 |
| `PortfolioValuationService` | 1 |
| `PriceAlertMonitorService` | 2 |
| `PriceAlertService` | 5 |
| `PriceHistoryService` | 3 |
| `QuoteService` | 2 |
| `RebalanceService` | 3 |
| `SupportResistanceService` | 1 |
| `SymbolSearchService` | 1 |
| `TransactionService` | 2 |
| `WatchlistService` | 3 |

## D — Dependency Inversion Principle

Service ขึ้นกับ interface ไม่ใช่คลาสตัวจริง และรับ dependency ผ่าน **constructor เท่านั้น** (ไม่มี `@Autowired` หรือ `@Value` บน field เลยทั้งโปรเจกต์)

### Constructor Injection

| ไฟล์ | บรรทัด | สิ่งที่เห็นในโค้ด | เหตุผล |
|---|---|---|---|
| [`PortfolioController.java`](../code/src/main/java/com/example/portfolio/controller/api/PortfolioController.java#L24) | 24 | Lombok สร้าง constructor จาก field `final` | dependency เปลี่ยนไม่ได้หลังสร้าง และทดสอบด้วย `new` ได้ทันที |
| [`AllocationServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/AllocationServiceImpl.java#L39) | 39 | constructor รับ repository + `List<AllocationStrategy>` | ไม่มี field injection |
| [`JwtUtil.java`](../code/src/main/java/com/example/portfolio/security/JwtUtil.java#L21) | 21 | ค่าตั้งค่ารับผ่าน constructor parameter | เดิมใช้ `@Value` บน field — ตอนนี้ secret ผิดจะรู้ตั้งแต่ start |
| [`YahooFinancePriceSource.java`](../code/src/main/java/com/example/portfolio/service/market/YahooFinancePriceSource.java#L46) | 46 | `range` รับผ่าน constructor | เดิมใช้ `@Value` บน field |
| [`ExternalMarketDataAdapter.java`](../code/src/main/java/com/example/portfolio/service/market/adapter/ExternalMarketDataAdapter.java#L21) | 21 | URL และ API key รับผ่าน constructor | เดิมใช้ `@Value` บน field |
| [`MarketDataRefreshServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/MarketDataRefreshServiceImpl.java#L32) | 32 | ระยะเว้นจังหวะรับผ่าน constructor | test ส่ง 0 มาได้ ไม่ต้องรอจริง |

### ขึ้นกับ Interface ไม่ใช่คลาสตัวจริง

| ไฟล์ | บรรทัด | สิ่งที่เห็นในโค้ด | เหตุผล |
|---|---|---|---|
| [`AllocationController.java`](../code/src/main/java/com/example/portfolio/controller/api/AllocationController.java#L24) | 24 | controller ขึ้นกับ service interface | ไม่รู้จัก `AllocationServiceImpl` หรือคลาส Strategy ตัวจริง |
| [`TransactionServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/TransactionServiceImpl.java#L31) | 31 | service ขึ้นกับ service อื่นผ่าน interface | เปลี่ยน implementation ได้โดยไม่แก้ผู้เรียก |
| [`AuthServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/AuthServiceImpl.java#L20) | 20 | ออก token ผ่าน `TokenProvider` | เปลี่ยนจาก JWT เป็นแบบอื่นได้โดยไม่แก้ service |
| [`JwtAuthenticationFilter.java`](../code/src/main/java/com/example/portfolio/security/JwtAuthenticationFilter.java#L22) | 22 | filter ก็ขึ้นกับ interface เดียวกัน | ไม่ผูกกับ `JwtUtil` |
| [`TriggeredState.java`](../code/src/main/java/com/example/portfolio/service/alert/state/TriggeredState.java#L16) | 16 | State ส่งต่อให้ Observer ผ่าน interface | ไม่ผูกกับ `AlertSubject` |
| [`PriceHistoryServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/PriceHistoryServiceImpl.java#L34) | 34 | แหล่งราคาจริงผ่าน interface (Adapter) | เปลี่ยนจาก Yahoo เป็นเจ้าอื่นได้ |
| [`PriceHistoryServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/PriceHistoryServiceImpl.java#L35) | 35 | ทางสำรองผ่าน interface | ไม่ผูกกับตัวสร้างข้อมูลจำลอง |
| [`AssetServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/AssetServiceImpl.java#L29) | 29 | รายชื่อหุ้นอ้างอิงผ่าน interface | วันหน้าเปลี่ยนเป็น API ได้ |
| [`PriceAlertMonitorServiceImpl.java`](../code/src/main/java/com/example/portfolio/service/impl/PriceAlertMonitorServiceImpl.java#L28) | 28 | ราคาล่าสุดผ่าน interface | สลับ `@Primary` ได้โดยไม่แก้ service |
| [`SecurityConfig.java`](../code/src/main/java/com/example/portfolio/config/SecurityConfig.java#L21) | 21 | จุดประกอบระบบ (composition root) | `config/` เป็นที่เดียวที่รู้จักคลาสตัวจริงเพื่อต่อสายเข้าด้วยกัน — ยกเว้นในกฎของ test |

ทุกคลาสใน `service/impl/` implement interface ใน `service/` (ตรวจด้วย `everyServiceImplementsAnInterface`)

## สรุปสิ่งที่ปรับเพื่อให้ตรงเงื่อนไข

| หลักการ | ก่อนปรับ | หลังปรับ |
|---|---|---|
| S | `AssetServiceImpl` รวมการค้นหาหุ้น, `PriceAlertService` รวม CRUD กับการตรวจราคา, `RebalanceService` สร้าง Command และจัดรูป JSON เอง | แยกเป็น `SymbolSearchServiceImpl`, `PriceAlertMonitorServiceImpl`, `MarketDataRefreshServiceImpl`, `RebalanceCommands`, `TradeOrderFormatter` |
| O | `switch`/`if` ตามประเภท 7 จุด และ controller เลือก Strategy เอง | `StrategyRegistry`, `AlertConditionEvaluator`, `HoldingUpdateRule`, ตาราง State/Command, พฤติกรรมใน `TransactionType` |
| L | `PCT_CHANGE` คืน false เสมอ, `isConditionMet()` ไม่มีความหมายในบางสถานะ | ตัดออกทั้งคู่ ทุก implementation ทำสัญญาได้ครบ |
| I | `AssetService` / `PriceAlertService` / `PriceHistoryService` มีเมธอดที่ผู้เรียกแต่ละรายไม่ใช้ | แยกเป็น interface ตามผู้ใช้ ทุกตัวไม่เกิน 5 เมธอด |
| D | `@Value` บน field 3 คลาส, ขึ้นกับ `JwtUtil` / `AlertSubject` / `SymbolDirectory` / `SyntheticPriceHistoryGenerator` ตรง ๆ | constructor injection ทั้งหมด และขึ้นกับ `TokenProvider` / `AlertPublisher` / `SymbolCatalog` / `PriceHistoryFallback` |

