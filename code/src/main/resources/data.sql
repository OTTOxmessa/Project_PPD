-- ============================================================================
-- ข้อมูลเริ่มต้นสำหรับทดสอบ/เดโม
-- ทุกคำสั่งใช้ ON CONFLICT DO NOTHING หรือ NOT EXISTS จึงรันซ้ำทุกครั้งที่ start แอปได้โดยไม่เกิดข้อมูลซ้ำ
-- ราคาในไฟล์นี้เป็นค่าจำลองเริ่มต้นตอนฐานข้อมูลว่างเท่านั้น
-- หลังแอป start, MarketDataRefreshScheduler จะดึงราคาจริงจาก Yahoo Finance มาแทนที่ (ถ้าเชื่อมต่อได้)
-- บัญชีเดโม: demo@portfolio.com / demo1234
-- ระบบรองรับเฉพาะหุ้นและ ETF ตลาดสหรัฐฯ ราคาทุกตัวเป็น USD
-- ============================================================================

-- ---------- 1) ผู้ใช้เดโม + โปรไฟล์ (ความสัมพันธ์ 1:1) ----------
INSERT INTO users (username, email, password_hash, role, created_at)
VALUES ('demo_investor', 'demo@portfolio.com',
        '$2b$10$zZEjXdfs/lUqp7UH4Z0X3Op2KHvSCU4syeW.kqOaOLG83oxuFv80G', 'USER', now())
ON CONFLICT DO NOTHING;

INSERT INTO user_profiles (user_id, risk_tolerance, investment_goal, phone)
SELECT id, 'MEDIUM', 'ลงทุนระยะยาว เน้นกระจายความเสี่ยง', '0800000000'
FROM users WHERE email = 'demo@portfolio.com'
ON CONFLICT DO NOTHING;

-- ---------- 2) สินทรัพย์ (หุ้นและ ETF ตลาดสหรัฐฯ) ----------
INSERT INTO assets (symbol, name, asset_type, exchange) VALUES
    ('AAPL', 'Apple Inc.',                         'STOCK', 'US'),
    ('MSFT', 'Microsoft',                          'STOCK', 'US'),
    ('NVDA', 'Nvidia',                             'STOCK', 'US'),
    ('JPM',  'JPMorgan Chase',                     'STOCK', 'US'),
    ('KO',   'Coca-Cola Company (The)',            'STOCK', 'US'),
    ('AMZN', 'Amazon',                             'STOCK', 'US'),
    ('TSLA', 'Tesla, Inc.',                        'STOCK', 'US'),
    ('VOO',  'Vanguard S&P 500 ETF',               'ETF',   'US'),
    ('TLT',  'iShares 20+ Year Treasury Bond ETF', 'ETF',   'US')
ON CONFLICT DO NOTHING;

-- ---------- 3) ดัชนีอ้างอิง ----------
INSERT INTO market_indices (index_code, name) VALUES
    ('SPX', 'S&P 500'),
    ('DJI', 'Dow Jones Industrial Average')
ON CONFLICT DO NOTHING;

-- ---------- 4) ราคาย้อนหลังเริ่มต้น (จำลอง, USD) ตั้งแต่ 1 ม.ค. 2026 ถึงวันนี้ ----------
-- base เป็นราคาโดยประมาณช่วงต้นปี 2026 ใช้แค่ตอนดึงราคาจริงไม่ได้ (start แล้วระบบจะดึงจาก Yahoo มาแทน)
-- สูตร: base x (1 + amp x sin((d + phase) / period) + trend x d), d = จำนวนวันนับจาก 1 ม.ค. 2026
-- ใส่เฉพาะสินทรัพย์ที่ยังไม่มีราคาเลย: ถ้านำเข้าราคาจริงจาก Yahoo แล้ว จะไม่เอาข้อมูลจำลองมาปน
INSERT INTO price_history (asset_id, price_date, open, high, low, close, volume)
SELECT asset_id, price_date,
       ROUND((c * 0.998)::numeric, 4),
       ROUND((c * 1.012)::numeric, 4),
       ROUND((c * 0.988)::numeric, 4),
       ROUND(c::numeric, 4),
       1000000
FROM (
    SELECT a.id AS asset_id,
           g.d::date AS price_date,
           p.base * (1 + p.amp * SIN(((g.d::date - DATE '2026-01-01') + p.phase) / p.period)
                       + p.trend * (g.d::date - DATE '2026-01-01')) AS c
    FROM (VALUES
            ('AAPL', 250.00, 0.06,  0, 18.0,  0.0004),
            ('MSFT', 470.00, 0.05, 20, 25.0,  0.0003),
            ('NVDA', 185.00, 0.10, 40, 15.0,  0.0008),
            ('JPM',  315.00, 0.05, 10, 30.0,  0.0003),
            ('KO',    70.00, 0.03,  5, 22.0,  0.0001),
            ('AMZN', 230.00, 0.07, 15, 20.0,  0.0004),
            ('TSLA', 420.00, 0.12, 30, 12.0,  0.0002),
            ('VOO',  630.00, 0.04,  0, 28.0,  0.0003),
            ('TLT',   88.00, 0.02,  0, 40.0, -0.0001)
         ) AS p(symbol, base, amp, phase, period, trend)
    JOIN assets a ON a.symbol = p.symbol
    CROSS JOIN generate_series(DATE '2026-01-01', CURRENT_DATE, INTERVAL '1 day') AS g(d)
    WHERE NOT EXISTS (SELECT 1 FROM price_history x WHERE x.asset_id = a.id)
) AS prices
ON CONFLICT (asset_id, price_date) DO NOTHING;

-- ดัชนีจำลองสำรอง (ถ้าดึง ^GSPC / ^DJI จาก Yahoo ได้ ระบบจะแทนที่ด้วยข้อมูลจริง)
INSERT INTO index_price_history (market_index_id, price_date, close_value)
SELECT m.id,
       g.d::date,
       ROUND((v.base * (1 + 0.04 * SIN((g.d::date - DATE '2026-01-01') / 28.0)
                          + 0.0003 * (g.d::date - DATE '2026-01-01')))::numeric, 4)
FROM (VALUES ('SPX', 6900.00), ('DJI', 48000.00)) AS v(index_code, base)
JOIN market_indices m ON m.index_code = v.index_code
CROSS JOIN generate_series(DATE '2026-01-01', CURRENT_DATE, INTERVAL '1 day') AS g(d)
WHERE NOT EXISTS (SELECT 1 FROM index_price_history x WHERE x.market_index_id = m.id)
ON CONFLICT (market_index_id, price_date) DO NOTHING;

-- ---------- 5) พอร์ตเดโม (ความสัมพันธ์ 1:N กับ users) ----------
INSERT INTO portfolios (user_id, name, base_currency, created_at)
SELECT u.id, 'Demo Portfolio', 'USD', now()
FROM users u
WHERE u.email = 'demo@portfolio.com'
  AND NOT EXISTS (SELECT 1 FROM portfolios p WHERE p.user_id = u.id AND p.name = 'Demo Portfolio');

-- ---------- 6) รายการซื้อย้อนหลัง ณ 5 ม.ค. 2026 ใช้ราคาปิดของวันนั้น ----------
INSERT INTO transactions (portfolio_id, asset_id, type, quantity, price, executed_at)
SELECT p.id, a.id, 'BUY', v.qty, ph.close, TIMESTAMP '2026-01-05 10:00:00'
FROM (VALUES ('AAPL', 20), ('MSFT', 10), ('NVDA', 30), ('JPM', 15),
             ('KO', 50), ('VOO', 10), ('TLT', 40)) AS v(symbol, qty)
JOIN assets a ON a.symbol = v.symbol
JOIN users u ON u.email = 'demo@portfolio.com'
JOIN portfolios p ON p.user_id = u.id AND p.name = 'Demo Portfolio'
JOIN price_history ph ON ph.asset_id = a.id AND ph.price_date = DATE '2026-01-05'
WHERE NOT EXISTS (SELECT 1 FROM transactions t WHERE t.portfolio_id = p.id);

-- ---------- 7) Holdings ที่สอดคล้องกับรายการซื้อข้างบน (N:M portfolio <-> asset) ----------
INSERT INTO holdings (portfolio_id, asset_id, quantity, avg_cost, updated_at)
SELECT t.portfolio_id, t.asset_id, t.quantity, t.price, now()
FROM transactions t
JOIN portfolios p ON p.id = t.portfolio_id AND p.name = 'Demo Portfolio'
JOIN users u ON u.id = p.user_id AND u.email = 'demo@portfolio.com'
WHERE t.type = 'BUY'
ON CONFLICT (portfolio_id, asset_id) DO NOTHING;

-- ---------- 8) สัดส่วนเป้าหมาย (รวม 100%) ----------
INSERT INTO allocation_targets (portfolio_id, asset_id, target_percent)
SELECT p.id, a.id, v.pct
FROM (VALUES ('AAPL', 15.00), ('MSFT', 15.00), ('NVDA', 10.00), ('JPM', 10.00),
             ('KO', 10.00), ('VOO', 25.00), ('TLT', 15.00)) AS v(symbol, pct)
JOIN assets a ON a.symbol = v.symbol
JOIN users u ON u.email = 'demo@portfolio.com'
JOIN portfolios p ON p.user_id = u.id AND p.name = 'Demo Portfolio'
ON CONFLICT (portfolio_id, asset_id) DO NOTHING;

-- ---------- 9) Watchlist ของผู้ใช้เดโม (ตาราง user_watchlist = ความสัมพันธ์ N:M) ----------
INSERT INTO user_watchlist (user_id, asset_id)
SELECT u.id, a.id
FROM users u
JOIN assets a ON a.symbol IN ('AAPL', 'MSFT', 'NVDA', 'AMZN', 'TSLA', 'VOO')
WHERE u.email = 'demo@portfolio.com'
  AND NOT EXISTS (SELECT 1 FROM user_watchlist w WHERE w.user_id = u.id AND w.asset_id = a.id);
