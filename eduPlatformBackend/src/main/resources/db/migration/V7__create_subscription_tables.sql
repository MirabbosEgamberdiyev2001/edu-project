-- ============================================
-- V7: SaaS & Monetization - Subscription System
-- SubscriptionPlan, UserSubscription, UsageRecord, Payment
-- ============================================

-- 1. Subscription Plans (tariff definitions)
CREATE TABLE subscription_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    name        JSONB NOT NULL DEFAULT '{}',
    description JSONB DEFAULT '{}',

    plan_type   VARCHAR(20) NOT NULL,
    price_monthly NUMERIC(12,2) NOT NULL DEFAULT 0,
    price_yearly  NUMERIC(12,2),
    currency    VARCHAR(10) NOT NULL DEFAULT 'UZS',

    -- Usage limits
    max_tests_per_day       INTEGER DEFAULT 5,
    max_tests_per_month     INTEGER DEFAULT 150,
    max_groups              INTEGER DEFAULT 3,
    max_students_per_group  INTEGER DEFAULT 30,
    max_exports_per_month   INTEGER DEFAULT 10,
    max_questions_per_import INTEGER DEFAULT 50,

    -- Feature flags
    analytics_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
    export_pdf_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    export_docx_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    proof_visible      BOOLEAN NOT NULL DEFAULT FALSE,
    api_access         BOOLEAN NOT NULL DEFAULT FALSE,
    custom_branding    BOOLEAN NOT NULL DEFAULT FALSE,

    active     BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER,

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted_at TIMESTAMP,

    CONSTRAINT check_plan_type CHECK (plan_type IN ('FREE', 'PREMIUM', 'PRO', 'SCHOOL', 'ENTERPRISE'))
);

CREATE INDEX idx_plan_type ON subscription_plans(plan_type);
CREATE INDEX idx_plan_active ON subscription_plans(active);

-- 2. User Subscriptions (active/past subscriptions per user)
CREATE TABLE user_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL REFERENCES users(id),
    plan_id UUID NOT NULL REFERENCES subscription_plans(id),

    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    start_date TIMESTAMP NOT NULL,
    end_date   TIMESTAMP NOT NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT TRUE,

    cancelled_at TIMESTAMP,
    amount_paid  NUMERIC(12,2),
    assigned_by  UUID REFERENCES users(id),

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT check_sub_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'CANCELLED', 'SUSPENDED'))
);

CREATE INDEX idx_usub_user ON user_subscriptions(user_id);
CREATE INDEX idx_usub_plan ON user_subscriptions(plan_id);
CREATE INDEX idx_usub_status ON user_subscriptions(status);
CREATE INDEX idx_usub_end_date ON user_subscriptions(end_date);

-- 3. Usage Records (daily usage counters)
CREATE TABLE usage_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id    UUID NOT NULL REFERENCES users(id),
    usage_type VARCHAR(30) NOT NULL,
    usage_date DATE NOT NULL,
    count      INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT uk_user_type_date UNIQUE (user_id, usage_type, usage_date),
    CONSTRAINT check_usage_type CHECK (usage_type IN (
        'TEST_GENERATION', 'TEST_ATTEMPT', 'EXPORT_PDF',
        'EXPORT_DOCX', 'GROUP_CREATE', 'QUESTION_IMPORT'
    ))
);

CREATE INDEX idx_usage_user ON usage_records(user_id);
CREATE INDEX idx_usage_type ON usage_records(usage_type);
CREATE INDEX idx_usage_date ON usage_records(usage_date);

-- 4. Payments (transaction history)
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id         UUID NOT NULL REFERENCES users(id),
    subscription_id UUID REFERENCES user_subscriptions(id),

    amount   NUMERIC(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'UZS',

    provider VARCHAR(20) NOT NULL,
    status   VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    external_transaction_id VARCHAR(255),
    provider_order_id       VARCHAR(255),
    callback_data           TEXT,

    paid_at       TIMESTAMP,
    refunded_at   TIMESTAMP,
    failure_reason VARCHAR(500),

    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT check_payment_provider CHECK (provider IN ('PAYME', 'CLICK', 'UZUM')),
    CONSTRAINT check_payment_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED'))
);

CREATE INDEX idx_payment_user ON payments(user_id);
CREATE INDEX idx_payment_subscription ON payments(subscription_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_provider ON payments(provider);
CREATE INDEX idx_payment_external_id ON payments(external_transaction_id);

-- ============================================
-- Seed default subscription plans
-- ============================================

-- Student FREE
INSERT INTO subscription_plans (id, name, description, plan_type, price_monthly, currency,
    max_tests_per_day, max_tests_per_month, max_groups, max_students_per_group,
    max_exports_per_month, max_questions_per_import,
    analytics_enabled, export_pdf_enabled, export_docx_enabled, proof_visible,
    api_access, custom_branding, sort_order)
VALUES (
    gen_random_uuid(),
    '{"uz_latn": "Bepul (Talaba)", "en": "Free (Student)", "ru": "Бесплатный (Студент)", "uz_cyrl": "Бепул (Талаба)"}'::jsonb,
    '{"uz_latn": "Kuniga 5 ta test, asosiy natijalar", "en": "5 tests per day, basic results", "ru": "5 тестов в день, базовые результаты", "uz_cyrl": "Кунига 5 та тест, асосий натижалар"}'::jsonb,
    'FREE', 0, 'UZS',
    5, 150, 0, 0, 0, 0,
    FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, 1
);

-- Student PREMIUM
INSERT INTO subscription_plans (id, name, description, plan_type, price_monthly, price_yearly, currency,
    max_tests_per_day, max_tests_per_month, max_groups, max_students_per_group,
    max_exports_per_month, max_questions_per_import,
    analytics_enabled, export_pdf_enabled, export_docx_enabled, proof_visible,
    api_access, custom_branding, sort_order)
VALUES (
    gen_random_uuid(),
    '{"uz_latn": "Premium (Talaba)", "en": "Premium (Student)", "ru": "Премиум (Студент)", "uz_cyrl": "Премиум (Талаба)"}'::jsonb,
    '{"uz_latn": "Cheksiz testlar, isbot, analitika, PDF", "en": "Unlimited tests, proofs, analytics, PDF", "ru": "Безлимитные тесты, доказательства, аналитика, PDF", "uz_cyrl": "Чексиз тестлар, исбот, аналитика, PDF"}'::jsonb,
    'PREMIUM', 15000, 150000, 'UZS',
    -1, -1, 0, 0, -1, 0,
    TRUE, TRUE, FALSE, TRUE, FALSE, FALSE, 2
);

-- Teacher FREE
INSERT INTO subscription_plans (id, name, description, plan_type, price_monthly, currency,
    max_tests_per_day, max_tests_per_month, max_groups, max_students_per_group,
    max_exports_per_month, max_questions_per_import,
    analytics_enabled, export_pdf_enabled, export_docx_enabled, proof_visible,
    api_access, custom_branding, sort_order)
VALUES (
    gen_random_uuid(),
    '{"uz_latn": "Bepul (O''qituvchi)", "en": "Free (Teacher)", "ru": "Бесплатный (Учитель)", "uz_cyrl": "Бепул (Ўқитувчи)"}'::jsonb,
    '{"uz_latn": "Oyiga 10 ta test, 3 ta guruh", "en": "10 tests per month, 3 groups", "ru": "10 тестов в месяц, 3 группы", "uz_cyrl": "Ойига 10 та тест, 3 та гуруҳ"}'::jsonb,
    'FREE', 0, 'UZS',
    -1, 10, 3, 30, 5, 50,
    FALSE, FALSE, FALSE, FALSE, FALSE, FALSE, 3
);

-- Teacher PRO
INSERT INTO subscription_plans (id, name, description, plan_type, price_monthly, price_yearly, currency,
    max_tests_per_day, max_tests_per_month, max_groups, max_students_per_group,
    max_exports_per_month, max_questions_per_import,
    analytics_enabled, export_pdf_enabled, export_docx_enabled, proof_visible,
    api_access, custom_branding, sort_order)
VALUES (
    gen_random_uuid(),
    '{"uz_latn": "Pro (O''qituvchi)", "en": "Pro (Teacher)", "ru": "Про (Учитель)", "uz_cyrl": "Про (Ўқитувчи)"}'::jsonb,
    '{"uz_latn": "Cheksiz testlar, marketplace, API", "en": "Unlimited tests, marketplace, API", "ru": "Безлимитные тесты, маркетплейс, API", "uz_cyrl": "Чексиз тестлар, маркетплейс, API"}'::jsonb,
    'PRO', 25000, 250000, 'UZS',
    -1, -1, -1, -1, -1, -1,
    TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 4
);

-- School
INSERT INTO subscription_plans (id, name, description, plan_type, price_monthly, price_yearly, currency,
    max_tests_per_day, max_tests_per_month, max_groups, max_students_per_group,
    max_exports_per_month, max_questions_per_import,
    analytics_enabled, export_pdf_enabled, export_docx_enabled, proof_visible,
    api_access, custom_branding, sort_order)
VALUES (
    gen_random_uuid(),
    '{"uz_latn": "Maktab", "en": "School", "ru": "Школа", "uz_cyrl": "Мактаб"}'::jsonb,
    '{"uz_latn": "10 o''qituvchi, analitika, brending", "en": "10 teachers, analytics, branding", "ru": "10 учителей, аналитика, брендинг", "uz_cyrl": "10 ўқитувчи, аналитика, брендинг"}'::jsonb,
    'SCHOOL', 99000, 990000, 'UZS',
    -1, -1, -1, -1, -1, -1,
    TRUE, TRUE, TRUE, TRUE, FALSE, TRUE, 5
);

-- Enterprise
INSERT INTO subscription_plans (id, name, description, plan_type, price_monthly, price_yearly, currency,
    max_tests_per_day, max_tests_per_month, max_groups, max_students_per_group,
    max_exports_per_month, max_questions_per_import,
    analytics_enabled, export_pdf_enabled, export_docx_enabled, proof_visible,
    api_access, custom_branding, sort_order)
VALUES (
    gen_random_uuid(),
    '{"uz_latn": "Korporativ", "en": "Enterprise", "ru": "Корпоративный", "uz_cyrl": "Корпоратив"}'::jsonb,
    '{"uz_latn": "Cheksiz, SLA, maxsus yordam", "en": "Unlimited, SLA, dedicated support", "ru": "Безлимитный, SLA, выделенная поддержка", "uz_cyrl": "Чексиз, SLA, махсус ёрдам"}'::jsonb,
    'ENTERPRISE', 0, 0, 'UZS',
    -1, -1, -1, -1, -1, -1,
    TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 6
);
