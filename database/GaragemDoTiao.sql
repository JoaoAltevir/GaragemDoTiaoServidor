CREATE TABLE user (
    username VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_username_length CHECK (CHAR_LENGTH(username) >= 3),
    CONSTRAINT chk_password_length CHECK (CHAR_LENGTH(password) >= 8)
);

CREATE TABLE session_user (
    token UUID PRIMARY KEY,
    user_username VARCHAR(20) NOT NULL,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_username) REFERENCES Usuario(username) ON DELETE CASCADE
);

CREATE OR REPLACE FUNCTION atualiza_coluna_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuario_updated_at
BEFORE UPDATE ON Usuario
FOR EACH ROW
EXECUTE FUNCTION atualiza_coluna_updated_at();