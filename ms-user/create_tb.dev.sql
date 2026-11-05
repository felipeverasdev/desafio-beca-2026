CREATE TABLE tb_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,
    cpf VARCHAR(11) NOT NULL,
    phone VARCHAR(13) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ DEFAULT NULL
);

GO

CREATE INDEX idx_users_deleted_at ON tb_users (deleted_at);

CREATE UNIQUE INDEX uk_users_active_email ON tb_users (email) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_users_active_cpf ON tb_users (cpf) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uk_users_active_phone ON tb_users (phone) WHERE deleted_at IS NULL;