-- ============================================
-- V4: Add repayment status
-- Adds status column to repayments table
-- ============================================

ALTER TABLE repayments ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING';