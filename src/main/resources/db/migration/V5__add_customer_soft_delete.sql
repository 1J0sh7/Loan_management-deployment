-- ============================================
-- V5: Add customer soft-delete flag
-- ============================================

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
