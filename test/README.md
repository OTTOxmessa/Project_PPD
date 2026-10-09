# การทดสอบ (Testing)

โค้ดทดสอบทั้งหมดอยู่ที่ `code/src/test/java` ตามโครงสร้างมาตรฐานของ Maven ส่วนโฟลเดอร์ `test/` นี้เก็บแผนการทดสอบและรายงานผล (`test/test-report/`)

## เครื่องมือ

| เครื่องมือ | ใช้ทำอะไร |
|---|---|
| JUnit 5 (Jupiter) | โครงของ test, `@Nested`, `@ParameterizedTest` |
| Mockito | mock repository / service เพื่อทดสอบแต่ละคลาสแยกจากฐานข้อมูล |
| AssertJ | ตรวจผลลัพธ์ (`isEqualByComparingTo` สำหรับ `BigDecimal`) |
| Spring MockMvc (standalone) | ทดสอบ REST controller + `GlobalExceptionHandler` + Bean Validation โดยไม่ต้องเปิด Spring context |

ทั้งหมดมากับ `spring-boot-starter-test` ใน `pom.xml` ไม่ต้องเพิ่ม dependency

## วิธีรัน

```bash
cd code
docker compose up -d          # contextLoads() ต้องใช้ PostgreSQL จริง
mvn test                      # รันทุก test
mvn test -Dtest=RebalanceServiceTest      # รันเฉพาะคลาส
```

Unit test ทุกตัวยกเว้น `PortfolioSystemApplicationTests.contextLoads()` ใช้ mock ทั้งหมด ไม่ต้องมีฐานข้อมูลหรืออินเทอร์เน็ต

## สร้างรายงานผล (Test Report)

```bash
cd code
mvn surefire-report:report
```

จากนั้นคัดลอกไฟล์ไปเก็บไว้ที่ `test/test-report/`:
- `code/target/surefire-reports/` — ผลดิบของแต่ละคลาส (`.txt`, `.xml`)
- `code/target/reports/surefire.html` — รายงานแบบหน้าเว็บ (เปิดด้วยเบราว์เซอร์)

## รายการทดสอบ

### Service Layer

| คลาสทดสอบ | สิ่งที่ทดสอบ | จำนวน |
|---|---|---|
| `PortfolioServiceImplTest` | CRUD พอร์ต, ผูกเจ้าของและตั้งสกุลเงินเป็น USD ตอนสร้าง, ผู้ใช้อื่นเข้าถึง/ลบพอร์ตไม่ได้, ส่ง Pageable ต่อ | 7 |
| `HoldingServiceImplTest` | ซื้อครั้งแรก, ถัวเฉลี่ยต้นทุน (100@10 + 100@20 = 200@15), ขายแล้วต้นทุนไม่เปลี่ยน, ขายเกินจำนวน, ซ่อนตัวที่ขายหมด | 6 |
| `TransactionServiceImplTest` | บันทึกพร้อมอัปเดต holding, วันที่ย้อนหลัง, DIVIDEND ไม่แตะ holding, ขายเกินแล้วไม่บันทึก | 5 |
| `AuthServiceImplTest` | เก็บรหัสผ่านแบบ hash, username/email ซ้ำ, login ถูก/ผิด, ไม่พบอีเมลตอบเหมือนรหัสผิด | 6 |
| `AllocationServiceImplTest` | คำนวณ drift (จริง − เป้า), เลือก Strategy จากชื่อ, ชื่อที่ไม่รู้จัก → 400, สินทรัพย์ไม่มีเป้า, upsert เป้าหมาย | 7 |
| `PriceAlertServiceImplTest` | สร้าง alert (บังคับ PENDING), สินทรัพย์ไม่มีอยู่, ดู/ลบ alert ของพอร์ตอื่นไม่ได้, แก้ได้เฉพาะ PENDING | 7 |
| `AssetMaintenanceServiceImplTest` | แก้ชื่อ/ประเภท/ตลาด (NASDAQ เก็บเป็น US), ห้ามเปลี่ยนเป็นตลาดอื่นหรือคริปโต, ห้ามเปลี่ยน symbol, ห้ามลบสินทรัพย์ที่ยังถูกใช้, ลบราคาย้อนหลัง+watchlist ก่อนลบสินทรัพย์ | 6 |
| `AssetServiceImplTest` | เพิ่มหุ้นสหรัฐฯ (ตลาดเก็บเป็น US), ปฏิเสธหุ้นตลาด SET และคริปโต, สร้างจากรายชื่ออ้างอิง, ไม่สร้างซ้ำ | 5 |
| `UsMarketTest` | ขอบเขตตลาดที่รองรับ: ticker สหรัฐฯ (รวม BRK.B), ชื่อตลาด NYSE/NASDAQ, ปฏิเสธ `.BK`/คริปโต/ตลาดอื่น, สกุลเงิน USD, ดัชนี SPX → ^GSPC และ DJI → ^DJI | 7 (18 กรณี) |
| `PriceAlertMonitorServiceImplTest` | ราคาถึงเป้า PENDING→TRIGGERED→NOTIFIED ในรอบเดียว, ยังไม่ถึงเป้า, ไม่แจ้งซ้ำ, ตรวจทุก alert แม้บางตัวพัง, alert เก่าเกินกำหนด → EXPIRED | 6 |
| `SymbolSearchServiceImplTest` | จัดอันดับผลค้นหา รวมหุ้นในระบบกับรายชื่ออ้างอิงไม่ซ้ำ, ได้ครบแล้วไม่ค้น Yahoo, หาไม่เจอค้นต่อจาก Yahoo (RKLB), ผลจาก Yahoo ไม่ซ้ำ, คำค้นว่าง | 4 |
| `YahooSymbolSearchTest` | Adapter ค้นหาหุ้นจาก Yahoo: เก็บหุ้น/ETF ตลาดสหรัฐฯ, ตัด OTC/ตลาดต่างประเทศ/คริปโต/กองทุนรวม/ดัชนี, BRK-B → BRK.B, จำผลคำค้นเดิม, Yahoo ใช้ไม่ได้ → list ว่าง | 6 |
| `MarketDataRefreshServiceImplTest` | นับเฉพาะตัวที่อัปเดตสำเร็จ ตัวที่พังไม่ทำให้ตัวอื่นหยุด | 1 |
| `TransactionTypeTest` | แต่ละประเภทรู้ผลต่อจำนวนหน่วย (ใช้แทน switch) | 3 |
| `PerformanceServiceImplTest` | ตรวจคำขอ (วันที่กลับด้าน, ไม่ระบุดัชนี) ก่อนส่งให้ Template Method | 3 |

### Design Patterns

| คลาสทดสอบ | Pattern | สิ่งที่ทดสอบ | จำนวน |
|---|---|---|---|
| `AllocationStrategyTest` | Strategy | TargetPercentage / EqualWeight / RiskBased ให้ผลต่างกันจากข้อมูลชุดเดียว | 6 |
| `SupportResistanceTest` | Strategy | Pivot Point, Moving Average Band (±2 SD), service เลือก strategy จากชื่อ | 8 |
| `RebalanceStrategyTest` | Strategy | Threshold (เบี่ยง > 5% เท่านั้น) vs Calendar (กลับไปตามเป้าทุกครั้ง) | 6 |
| `RebalanceServiceImplTest` | Strategy + CoR + Command | ขายก่อนซื้อเสมอ, Chain ปฏิเสธพอร์ตว่าง / ไม่มีเป้าหมาย / เป้ารวมไม่ถึง 100%, บันทึก RebalanceLog, ชื่อวิธีไม่รู้จัก, ประวัติแบบแบ่งหน้า | 8 |
| `StrategyRegistryTest` | Strategy (OCP) | หา Strategy จากชื่อ, ชื่อไม่รู้จัก, ชื่อซ้ำ | 4 |
| `CommandAndChainTest` | Command, Chain of Responsibility | Buy/SellCommand, factory สร้าง Command จากตาราง, formatter, chain ส่งต่อ/หยุดเมื่อไม่ผ่าน, chain เต็มสาย 3 ห่วง | 10 |
| `AlertStateTest` | State + Strategy | PENDING → TRIGGERED (6 กรณีขอบ) ผ่าน `AlertConditionEvaluator`, เงื่อนไขที่ไม่มี evaluator, หมดอายุเมื่อรอเกินกำหนด, TRIGGERED → NOTIFIED, EXPIRED ไม่เปลี่ยน | 14 |
| `PriceAlertMonitorServiceImplTest` | Observer | `AlertSubject` แจ้ง observer ทุกตัว เพิ่มช่องทางใหม่ได้โดยไม่แก้โค้ดเดิม | (รวมด้านบน) |
| `PerformanceReportTest` | Template Method | สูตรผลตอบแทน, ชนะ/แพ้ตลาด, มูลค่าเริ่มต้น 0, คำนวณจากธุรกรรมจริง | 5 |

### Security

| คลาสทดสอบ | สิ่งที่ทดสอบ | จำนวน |
|---|---|---|
| `JwtUtilTest` | ออก/ถอด token, secret ผิด, หมดอายุ, ข้อความไม่ใช่ JWT, secret สั้นเกินไป | 5 |
| `RestAuthenticationEntryPointTest` | ไม่มี token → 401 เป็น JSON รูปแบบเดียวกับ `ErrorResponse` | 1 |

### Controller (REST API)

| คลาสทดสอบ | Status code ที่ทดสอบ | จำนวน |
|---|---|---|
| `PortfolioControllerTest` | 200, 201, 204, 400 (validation, สกุลเงินไม่ใช่ USD, JSON ผิดรูปแบบ, id ไม่ใช่ตัวเลข), 404, 500 และ pagination/sorting `?page=1&size=5&sort=name,asc` | 12 |
| `TransactionControllerTest` | 201, 400 (จำนวน 0, วันที่อนาคต), 404 (พอร์ตคนอื่น), 409 (ขายเกิน) | 6 |
| `AuthControllerTest` | 201, 200, 400 (อีเมล/รหัสผ่านผิดรูปแบบ), 401 (รหัสผ่านผิด), 409 (อีเมลซ้ำ) | 5 |
| `AssetControllerTest` | CRUD สินทรัพย์: 200, 201, 204, 400 (ไม่ระบุประเภท), 404 (พร้อม `path`), 409 (symbol ซ้ำ, เปลี่ยน symbol, ลบตัวที่ยังถูกใช้, constraint ฐานข้อมูล), `PUT /assets/by-symbol/{symbol}` | 11 |
| `PriceAlertControllerTest` | CRUD alert: 200, 201, 204, 400 (ราคาเป้า 0), 404 (พอร์ตคนอื่น), 409 (แก้ alert ที่แจ้งเตือนแล้ว) | 8 |
| `RebalanceControllerTest` | `GET /rebalance-plan` 200, `POST /rebalances` 201, method ไม่รู้จัก 400, `GET /rebalances` แบ่งหน้าเรียงล่าสุดก่อน | 4 |

### Architecture

| คลาสทดสอบ | สิ่งที่ทดสอบ | จำนวน |
|---|---|---|
| `SolidPrinciplesTest` | ตรวจกฎ SOLID จากซอร์สโค้ด: service ไม่ยาวเกิน, ไม่มี switch ใน service layer, ไม่มี UnsupportedOperationException, interface ไม่เกิน 6 เมธอด, ไม่มี field injection, dependency เป็น interface, ทุก impl มี interface | 7 |
| `LayeredArchitectureTest` | อ่าน import จริงในซอร์สโค้ดแล้วตรวจกฎ Layered Architecture: controller ไม่เรียก repository/entity, รู้จักแค่ service interface, scheduler ผ่าน service, service ไม่รู้จัก DTO, repository/domain/dto ไม่ข้ามชั้น | 10 |

### Integration

| คลาสทดสอบ | สิ่งที่ทดสอบ | จำนวน |
|---|---|---|
| `PortfolioSystemApplicationTests` | Spring context โหลดได้ครบ (ต้องเปิด PostgreSQL) | 1 |

**รวม 196 test cases**
