ALTER TABLE tb_users ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER';

INSERT INTO tb_users (id, name, email, password, cpf, phone, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Administrador do Sistema',
    'admin@beca.com',
    '$2a$10$W1Hcf5jXFOYot4cbQF6Lme8UaI5brXeTBPwsaaCzYsLIWbU0q5CL.',
    '00000000000',
    '5511900000000',
    'ROLE_ADMIN',
    NOW(),
    NOW()
)
ON CONFLICT (email) WHERE deleted_at IS NULL DO NOTHING;
