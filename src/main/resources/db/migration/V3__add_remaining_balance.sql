-- ============================================
-- V3: Add Remaining Balance
-- Adds remaining_balance column to loan_applications
-- ============================================

ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS remaining_balance DECIMAL(19,2);