# รายงานผลการทดสอบ (Test Report)

ผลการรันชุดทดสอบทั้งหมดของ backend บน GitHub Actions (job `backend` ใน `.github/workflows/ci-cd.yml`) ไฟล์ในโฟลเดอร์นี้คือผลดิบจาก Maven Surefire ที่ CI อัปโหลดเป็น artifact `test-report`

## สรุปผล

| รายการ | ผล |
|---|---|
| จำนวนการทดสอบที่รัน | **257** |
| ผ่าน | **257 (100%)** |
| ไม่ผ่าน (Failures) | 0 |
| ผิดพลาด (Errors) | 0 |
| ข้าม (Skipped) | 0 |
| คลาสทดสอบ | 39 คลาส |
| เมธอดทดสอบ | 241 เมธอด |
| เวลารวม | 12.96 วินาที |
| วันที่รัน | 10 ตุลาคม 2026 |

**ผลการทดสอบ: ผ่านทั้งหมด**

จำนวนการรัน (257) มากกว่าจำนวนเมธอด (241) เพราะมี `@ParameterizedTest` 2 คลาสที่รันเมธอดเดียวซ้ำด้วยข้อมูลหลายชุด:

| คลาส | เมธอด | จำนวนการรัน |
|---|:---:|:---:|
| `UsMarketTest` | 7 | 18 |
| `AlertStateTest` | 9 | 14 |

## สภาพแวดล้อมที่ใช้ทดสอบ

| รายการ | ค่า |
|---|---|
| ระบบ | GitHub Actions `ubuntu-latest` (Linux amd64) |
| Java | 25.0.4.1 (Eclipse Adoptium / Temurin) |
| Build | `sh mvnw -B verify` |
| ฐานข้อมูล | PostgreSQL 15 (service container ของ CI) ใช้เฉพาะ `contextLoads()` |
| เครื่องมือ | JUnit 5, Mockito, AssertJ, Spring MockMvc (standalone) |

## ผลแยกตามกลุ่ม

| กลุ่ม | สิ่งที่ตรวจ | คลาส | การรัน | ผ่าน | เวลา (s) |
|---|---|:---:|:---:|:---:|:---:|
| สถาปัตยกรรม | กฎ Layered Architecture และ SOLID ตรวจจากซอร์สโค้ดจริง | 2 | 17 | 17 | 0.13 |
| Controller (REST API) | URL, Bean Validation, HTTP status, รูปแบบ error ผ่าน MockMvc | 8 | 58 | 58 | 2.77 |
| Service (business logic) | กฎธุรกิจของแต่ละ service โดย mock repository | 15 | 90 | 90 | 1.11 |
| Design Patterns | Strategy, State, Template Method, Command, Chain of Responsibility, StrategyRegistry | 7 | 55 | 55 | 0.72 |
| ข้อมูลตลาด | ขอบเขตตลาดสหรัฐฯ และ Adapter ค้นหาหุ้นจาก Yahoo | 2 | 24 | 24 | 1.51 |
| Security | สร้าง/ตรวจ JWT และคำตอบ 401 | 2 | 6 | 6 | 0.64 |
| Domain และ Mapper | Enum ประเภทธุรกรรม และการแปลง Entity → DTO | 2 | 6 | 6 | 0.01 |
| Integration | `contextLoads()` เปิด Spring ทั้งระบบกับ PostgreSQL จริง | 1 | 1 | 1 | 6.09 |
| **รวม** | | **39** | **257** | **257** | **12.96** |

## ผลรายคลาส

### สถาปัตยกรรม

| คลาสทดสอบ | การรัน | ผล | เวลา (s) |
|---|:---:|:---:|:---:|
| `architecture.LayeredArchitectureTest` | 10 | ผ่าน | 0.060 |
| `architecture.SolidPrinciplesTest` | 7 | ผ่าน | 0.066 |

### Controller

| คลาสทดสอบ | การรัน | ผล | เวลา (s) |
|---|:---:|:---:|:---:|
| `controller.api.AllocationControllerTest` | 3 | ผ่าน | 0.161 |
| `controller.api.AssetControllerTest` | 11 | ผ่าน | 0.301 |
| `controller.api.AuthControllerTest` | 5 | ผ่าน | 0.218 |
| `controller.api.ExchangeRateControllerTest` | 2 | ผ่าน | 0.053 |
| `controller.api.PortfolioControllerTest` | 12 | ผ่าน | 0.222 |
| `controller.api.PriceAlertControllerTest` | 8 | ผ่าน | 1.438 |
| `controller.api.RebalanceControllerTest` | 4 | ผ่าน | 0.099 |
| `controller.api.TransactionControllerTest` | 13 | ผ่าน | 0.278 |

### Service

| คลาสทดสอบ | การรัน | ผล | เวลา (s) |
|---|:---:|:---:|:---:|
| `service.impl.AllocationServiceImplTest` | 10 | ผ่าน | 0.098 |
| `service.impl.AssetMaintenanceServiceImplTest` | 6 | ผ่าน | 0.019 |
| `service.impl.AssetServiceImplTest` | 5 | ผ่าน | 0.079 |
| `service.impl.AuthServiceImplTest` | 6 | ผ่าน | 0.139 |
| `service.impl.ExchangeRateServiceImplTest` | 4 | ผ่าน | 0.054 |
| `service.impl.HoldingServiceImplTest` | 9 | ผ่าน | 0.199 |
| `service.impl.MarketDataRefreshServiceImplTest` | 1 | ผ่าน | 0.005 |
| `service.impl.PerformanceServiceImplTest` | 3 | ผ่าน | 0.036 |
| `service.impl.PortfolioServiceImplTest` | 7 | ผ่าน | 0.021 |
| `service.impl.PriceAlertMonitorServiceImplTest` | 6 | ผ่าน | 0.113 |
| `service.impl.PriceAlertServiceImplTest` | 7 | ผ่าน | 0.033 |
| `service.impl.PriceHistoryServiceImplTest` | 3 | ผ่าน | 0.077 |
| `service.impl.RebalanceServiceImplTest` | 10 | ผ่าน | 0.116 |
| `service.impl.SymbolSearchServiceImplTest` | 4 | ผ่าน | 0.039 |
| `service.impl.TransactionServiceImplTest` | 9 | ผ่าน | 0.078 |

### Design Patterns

| คลาสทดสอบ | Pattern | การรัน | ผล | เวลา (s) |
|---|---|:---:|:---:|:---:|
| `common.StrategyRegistryTest` | Strategy (เลือกจากชื่อ) | 4 | ผ่าน | 0.006 |
| `service.allocation.AllocationStrategyTest` | Strategy | 6 | ผ่าน | 0.022 |
| `service.analysis.SupportResistanceTest` | Strategy | 8 | ผ่าน | 0.069 |
| `service.alert.state.AlertStateTest` | State | 14 | ผ่าน | 0.113 |
| `service.performance.PerformanceReportTest` | Template Method | 5 | ผ่าน | 0.301 |
| `service.rebalance.RebalanceStrategyTest` | Strategy | 8 | ผ่าน | 0.048 |
| `service.rebalance.CommandAndChainTest` | Command, Chain of Responsibility | 10 | ผ่าน | 0.157 |

### ข้อมูลตลาด, Security, Domain และ Integration

| คลาสทดสอบ | การรัน | ผล | เวลา (s) |
|---|:---:|:---:|:---:|
| `service.market.UsMarketTest` | 18 | ผ่าน | 0.105 |
| `service.market.YahooSymbolSearchTest` | 6 | ผ่าน | 1.405 |
| `security.JwtUtilTest` | 5 | ผ่าน | 0.436 |
| `security.RestAuthenticationEntryPointTest` | 1 | ผ่าน | 0.199 |
| `domain.enums.TransactionTypeTest` | 3 | ผ่าน | 0.008 |
| `mapper.HoldingMapperTest` | 3 | ผ่าน | 0.002 |
| `PortfolioSystemApplicationTests` | 1 | ผ่าน | 6.090 |

## ข้อสังเกต

- `contextLoads()` ใช้เวลาเกือบครึ่งของทั้งหมด (6.09 s) เพราะเปิด Spring ทั้งระบบและเชื่อมต่อฐานข้อมูลจริง เทสต์ที่เหลือทั้ง 256 ตัวใช้ mock ไม่ต้องมีฐานข้อมูลหรืออินเทอร์เน็ต รวมกันใช้เวลาไม่ถึง 7 วินาที
- `YahooSymbolSearchTest` ไม่ได้เรียก Yahoo จริง ใช้ mock `RestTemplate` แทน ผลจึงไม่ขึ้นกับว่า Yahoo ใช้งานได้หรือไม่
- เทสต์กลุ่มสถาปัตยกรรมจะล้มทันทีถ้ามีโค้ดข้ามชั้นหรือผิดกฎ SOLID ที่กำหนด กฎการออกแบบจึงถูกตรวจทุกครั้งที่ build ไม่ได้อยู่แค่ในเอกสาร

## ไฟล์ในโฟลเดอร์นี้

| ไฟล์ | เนื้อหา |
|---|---|
| `TEST-<คลาส>.xml` | ผลละเอียดของแต่ละคลาสทดสอบ: ชื่อทุกเมธอด, เวลา, สภาพแวดล้อม (รูปแบบ JUnit XML อ่านได้ด้วยเครื่องมือ CI ทั่วไป) |
| `<คลาส>.txt` | สรุปสั้นของแต่ละคลาส: จำนวนที่รัน, ผ่าน/ไม่ผ่าน, เวลา |

## รันซ้ำเอง

```bash
cd code
docker compose up -d          # PostgreSQL สำหรับ contextLoads()
./mvnw test                   # Windows CMD: mvnw.cmd test
```

ผลจะอยู่ที่ `code/target/surefire-reports/` รายละเอียดว่าแต่ละคลาสทดสอบอะไรบ้างอยู่ใน [`../README.md`](../README.md)
