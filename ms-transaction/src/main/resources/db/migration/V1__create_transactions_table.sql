CREATE TABLE IF NOT EXISTS tb_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    target_user_id UUID DEFAULT NULL,
    rejection_reason VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON tb_transactions (user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON tb_transactions (created_at);

