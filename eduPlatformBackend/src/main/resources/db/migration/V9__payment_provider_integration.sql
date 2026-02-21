-- V9: Payment provider integration (Payme, Click, Uzum)

ALTER TABLE payments
  ADD COLUMN plan_id UUID REFERENCES subscription_plans(id),
  ADD COLUMN duration_months INTEGER DEFAULT 1,
  ADD COLUMN payme_transaction_state INTEGER,
  ADD COLUMN payme_create_time BIGINT,
  ADD COLUMN payme_perform_time BIGINT,
  ADD COLUMN payme_cancel_time BIGINT,
  ADD COLUMN payme_reason INTEGER;

-- Idempotency: one provider_order_id per provider
CREATE UNIQUE INDEX uk_payment_provider_order
  ON payments(provider, provider_order_id) WHERE provider_order_id IS NOT NULL;

-- Provider + external ID lookup (for callbacks)
CREATE INDEX idx_payment_provider_ext_id
  ON payments(provider, external_transaction_id) WHERE external_transaction_id IS NOT NULL;

-- GetStatement (Payme) support
CREATE INDEX idx_payment_provider_created ON payments(provider, status, created_at);

-- Add CANCELLED status
ALTER TABLE payments DROP CONSTRAINT IF EXISTS check_payment_status;
ALTER TABLE payments ADD CONSTRAINT check_payment_status
  CHECK (status IN ('PENDING','COMPLETED','FAILED','REFUNDED','CANCELLED'));

-- Payment -> Subscription linkage
ALTER TABLE user_subscriptions ADD COLUMN IF NOT EXISTS payment_id UUID REFERENCES payments(id);
CREATE INDEX IF NOT EXISTS idx_usub_payment ON user_subscriptions(payment_id) WHERE payment_id IS NOT NULL;
