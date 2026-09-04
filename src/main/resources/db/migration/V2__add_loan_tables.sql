-- ============================================
-- V2: Loan Tables
-- Creates loan_products, loan_applications, repayments
-- ============================================

CREATE TABLE IF NOT EXISTS loan_products (
                                             id BIGSERIAL PRIMARY KEY,
                                             name VARCHAR(100) NOT NULL,
    description TEXT,
    interest_rate DECIMAL(5,2) NOT NULL,
    term_months INTEGER NOT NULL,
    max_amount DECIMAL(19,2) NOT NULL,
    active BOOLEAN DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS loan_applications (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 customer_id BIGINT NOT NULL REFERENCES customers(id),
    product_id BIGINT NOT NULL REFERENCES loan_products(id),
    amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_date DATE,
    disbursed_date DATE,
    rejection_reason TEXT,
    approved_by VARCHAR(50)
    );

CREATE TABLE IF NOT EXISTS repayments (
                                          id BIGSERIAL PRIMARY KEY,
                                          loan_application_id BIGINT NOT NULL REFERENCES loan_applications(id),
    amount DECIMAL(19,2) NOT NULL,
    due_date DATE NOT NULL,
    paid_date DATE
    );