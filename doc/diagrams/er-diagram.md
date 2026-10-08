# ER Diagram

ไดอะแกรมนี้วาดตาม [`schema.sql`](../../code/src/main/resources/schema.sql) ซึ่งเป็นต้นฉบับของโครงสร้างตาราง ส่วนคำอธิบายแต่ละคอลัมน์อยู่ใน [`doc/data-dictionary.md`](../data-dictionary.md)

- ระบบมี **13 ตาราง** แบ่งเป็น 12 entity กับตารางเชื่อม `user_watchlist` อีก 1 ตาราง
- **PK** คือ Primary Key, **FK** คือ Foreign Key, **UK** คือคอลัมน์ที่อยู่ใน UNIQUE constraint
- ชนิดตัวเลขในไดอะแกรมย่อไว้ ขนาดจริงดูได้ใน data dictionary เช่น `numeric` ของราคาคือ `NUMERIC(18,4)`

## ความสัมพันธ์

| แบบ | ตาราง | ทำได้อย่างไรในฐานข้อมูล | ฝั่ง JPA |
|---|---|---|---|
| **1:1** | `users` — `user_profiles` | `user_profiles.user_id` เป็น FK และมี UNIQUE (`uq_user_profiles_user_id`) ผู้ใช้หนึ่งคนจึงมีโปรไฟล์ได้แถวเดียว | `@OneToOne` |
| **1:N** | `users` → `portfolios` | `portfolios.user_id` เป็น FK | `@ManyToOne` / `@OneToMany` |
| **1:N** | `portfolios` → `holdings`, `transactions`, `price_alerts`, `allocation_targets`, `rebalance_logs` | ตารางลูกมี FK ชื่อ `portfolio_id` | `@ManyToOne` / `@OneToMany` |
| **1:N** | `assets` → `price_history`, `market_indices` → `index_price_history` | ตารางลูกมี FK และมี UNIQUE (FK, `price_date`) | `@ManyToOne` |
| **N:M** (มีข้อมูลเพิ่ม) | `portfolios` ↔ `assets` ผ่าน `holdings` | `holdings` เก็บจำนวนหน่วยและต้นทุนเฉลี่ยไว้ด้วย จึงเป็น join entity ที่มี PK ของตัวเอง และมี UNIQUE (`portfolio_id`, `asset_id`) | 2 × `@ManyToOne` |
| **N:M** (ล้วน) | `users` ↔ `assets` ผ่าน `user_watchlist` | ตารางเชื่อมมีแค่ 2 FK และใช้ทั้งคู่รวมกันเป็น PK | `@ManyToMany` + `@JoinTable` |

## ไดอะแกรม

ถ้าดูไฟล์นี้ในที่ที่ไม่แสดง Mermaid ให้เปิดภาพ [`er-diagram.png`](er-diagram.png) แทน

```mermaid
erDiagram
    USERS ||--|| USER_PROFILES : "has (1:1)"
    USERS ||--o{ PORTFOLIOS : owns
    USERS ||--o{ USER_WATCHLIST : watches
    ASSETS ||--o{ USER_WATCHLIST : "watched in"
    PORTFOLIOS ||--o{ HOLDINGS : contains
    ASSETS ||--o{ HOLDINGS : "held as"
    PORTFOLIOS ||--o{ TRANSACTIONS : records
    ASSETS ||--o{ TRANSACTIONS : involves
    PORTFOLIOS ||--o{ PRICE_ALERTS : sets
    ASSETS ||--o{ PRICE_ALERTS : monitors
    PORTFOLIOS ||--o{ ALLOCATION_TARGETS : defines
    ASSETS ||--o{ ALLOCATION_TARGETS : targets
    PORTFOLIOS ||--o{ REBALANCE_LOGS : logs
    ASSETS ||--o{ PRICE_HISTORY : "priced by"
    MARKET_INDICES ||--o{ INDEX_PRICE_HISTORY : "valued by"

    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password_hash "BCrypt"
        varchar role "USER or ADMIN"
        timestamp created_at
    }
    USER_PROFILES {
        bigint id PK
        bigint user_id FK, UK "1:1"
        varchar risk_tolerance "LOW MEDIUM HIGH"
        varchar investment_goal
        varchar phone
    }
    PORTFOLIOS {
        bigint id PK
        bigint user_id FK
        varchar name
        varchar base_currency
        timestamp created_at
    }
    ASSETS {
        bigint id PK
        varchar symbol UK
        varchar name
        varchar asset_type
        varchar exchange
        varchar price_source "YAHOO or SYNTHETIC"
    }
    HOLDINGS {
        bigint id PK
        bigint portfolio_id FK, UK
        bigint asset_id FK, UK
        numeric quantity ">= 0"
        numeric avg_cost ">= 0"
        timestamp updated_at
    }
    TRANSACTIONS {
        bigint id PK
        bigint portfolio_id FK
        bigint asset_id FK
        varchar type "BUY SELL DIVIDEND ..."
        numeric quantity "> 0"
        numeric price "> 0"
        timestamp executed_at
    }
    PRICE_ALERTS {
        bigint id PK
        bigint portfolio_id FK
        bigint asset_id FK
        varchar condition "PRICE_ABOVE or PRICE_BELOW"
        numeric target_price "> 0"
        varchar status "PENDING TRIGGERED ..."
        timestamp created_at
    }
    ALLOCATION_TARGETS {
        bigint id PK
        bigint portfolio_id FK, UK
        bigint asset_id FK, UK
        numeric target_percent "0 - 100"
    }
    REBALANCE_LOGS {
        bigint id PK
        bigint portfolio_id FK
        varchar method "THRESHOLD or CALENDAR"
        timestamp triggered_at
        text details
    }
    PRICE_HISTORY {
        bigint id PK
        bigint asset_id FK, UK
        date price_date UK
        numeric open
        numeric high
        numeric low
        numeric close
        bigint volume
    }
    MARKET_INDICES {
        bigint id PK
        varchar index_code UK
        varchar name
    }
    INDEX_PRICE_HISTORY {
        bigint id PK
        bigint market_index_id FK, UK
        date price_date UK
        numeric close_value "> 0"
    }
    USER_WATCHLIST {
        bigint user_id PK, FK
        bigint asset_id PK, FK
    }
```

## เมื่อลบข้อมูลแม่ (ON DELETE)

```mermaid
flowchart LR
    U[users] -- CASCADE --> UP[user_profiles]
    U -- CASCADE --> P[portfolios]
    U -- CASCADE --> W[user_watchlist]
    P -- CASCADE --> H[holdings]
    P -- CASCADE --> T[transactions]
    P -- CASCADE --> PA[price_alerts]
    P -- CASCADE --> AT[allocation_targets]
    P -- CASCADE --> RL[rebalance_logs]
    A[assets] -- RESTRICT --> H
    A -- RESTRICT --> T
    A -- RESTRICT --> PA
    A -- RESTRICT --> AT
    A -- CASCADE --> PH[price_history]
    A -- CASCADE --> W
    MI[market_indices] -- CASCADE --> IPH[index_price_history]
```

- **CASCADE** ใช้กับข้อมูลลูกที่ไม่มีความหมายถ้าไม่มีแม่ เช่น ลบผู้ใช้แล้วพอร์ตและทุกอย่างในพอร์ตต้องหายตาม
- **RESTRICT** ใช้ห้ามลบสินทรัพย์ที่ยังมีคนถือ มีประวัติซื้อขาย มี alert หรือมีเป้าหมายอ้างถึงอยู่ เพราะข้อมูลเหล่านี้เป็นประวัติทางการเงินที่ต้องเก็บไว้

เหตุผลแบบละเอียดอยู่ใน [Data Dictionary — นโยบาย ON DELETE](../data-dictionary.md#นโยบาย-on-delete)
