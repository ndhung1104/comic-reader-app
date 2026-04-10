-- =============================================
-- V17: Lock last chapters policy
-- Rule:
--   - Default premium price: 100 coins
--   - Keep first chapter free for short comics
--     * total = 1  -> lock 0
--     * total = 2  -> lock last 1
--     * total = 3  -> lock last 2
--     * total >= 4 -> lock last 3
-- =============================================

WITH ranked AS (
    SELECT c.id,
           c.comic_id,
           ROW_NUMBER() OVER (PARTITION BY c.comic_id ORDER BY c.chapter_number DESC, c.id DESC) AS row_num_desc,
           COUNT(*) OVER (PARTITION BY c.comic_id) AS total_chapters
    FROM chapters c
),
policy AS (
    SELECT r.id,
           CASE
               WHEN r.total_chapters <= 1 THEN FALSE
               WHEN r.total_chapters <= 3 THEN r.row_num_desc <= (r.total_chapters - 1)
               ELSE r.row_num_desc <= 3
           END AS should_be_premium
    FROM ranked r
)
UPDATE chapters c
SET premium = p.should_be_premium,
    price = CASE WHEN p.should_be_premium THEN 100 ELSE 0 END,
    updated_at = CURRENT_TIMESTAMP
FROM policy p
WHERE c.id = p.id;
