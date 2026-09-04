-- ============================================
-- V6: Add Audit Trail for Loan Applications
-- ============================================

CREATE TABLE IF NOT EXISTS loan_application_history (
                                                        id BIGSERIAL PRIMARY KEY,
                                                        loan_application_id BIGINT NOT NULL REFERENCES loan_applications(id) ON DELETE CASCADE,
    previous_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by VARCHAR(50) NOT NULL,
    reason TEXT,
    changed_at TIMESTAMP NOT NULL
    );

CREATE INDEX idx_loan_history_loan_id ON loan_application_history(loan_application_id);
CREATE INDEX idx_loan_history_changed_at ON loan_application_history(changed_at);