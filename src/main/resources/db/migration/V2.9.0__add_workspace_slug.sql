-- URL-safe slug for multi-tenant routing (/{slug}/{view})
ALTER TABLE workspaces ADD COLUMN IF NOT EXISTS slug VARCHAR(100);

-- Backfill: lowercase name, strip non-alphanumeric characters
UPDATE workspaces
SET slug = LOWER(REGEXP_REPLACE(name, '[^a-zA-Z0-9]', '', 'g'));

-- Replace empty slugs (name was entirely special chars) with fallback
UPDATE workspaces SET slug = 'workspace' WHERE slug = '' OR slug IS NULL;

-- Resolve uniqueness: append id for any duplicate slugs
WITH ranked AS (
    SELECT id, slug,
           ROW_NUMBER() OVER (PARTITION BY slug ORDER BY id) AS rn
    FROM workspaces
)
UPDATE workspaces w
SET slug = r.slug || w.id::TEXT
FROM ranked r
WHERE w.id = r.id AND r.rn > 1;

ALTER TABLE workspaces ALTER COLUMN slug SET NOT NULL;
ALTER TABLE workspaces ADD CONSTRAINT workspaces_slug_unique UNIQUE (slug);
CREATE INDEX IF NOT EXISTS idx_workspaces_slug ON workspaces(slug);
