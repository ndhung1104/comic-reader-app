-- =============================================
-- V16: Top-Up Packages (admin-configurable)
-- =============================================

CREATE TABLE IF NOT EXISTS top_up_packages (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,
    coins       INTEGER         NOT NULL,
    price_label VARCHAR(50)     NOT NULL,
    bonus_label VARCHAR(100)    DEFAULT '',
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order  INTEGER         NOT NULL DEFAULT 0,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_top_up_packages_active ON top_up_packages(active);

-- Seed default packages
INSERT INTO top_up_packages (name, coins, price_label, bonus_label, active, sort_order)
VALUES
    ('Starter',   500,  '$4.99',  '',            true, 1),
    ('Popular',   1000, '$9.99',  '+100 Bonus',  true, 2),
    ('Premium',   2500, '$19.99', '+500 Bonus',  true, 3),
    ('Ultimate',  5000, '$39.99', '+1000 Bonus', true, 4);
