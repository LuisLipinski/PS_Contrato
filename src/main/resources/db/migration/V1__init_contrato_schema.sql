CREATE TABLE status_contrato (
    id BIGSERIAL PRIMARY KEY,
    nome_status VARCHAR(80) NOT NULL UNIQUE,
    descricao_status VARCHAR(255) NOT NULL
);

INSERT INTO status_contrato (id, nome_status, descricao_status) VALUES
    (1, 'Aguardando pagamento', 'Contrato criado e aguardando confirmação de pagamento'),
    (2, 'Ativo', 'Contrato ativo'),
    (3, 'Inativo', 'Contrato inativo');

SELECT setval(pg_get_serial_sequence('status_contrato', 'id'), 3, true);

CREATE TABLE contratos (
    id UUID PRIMARY KEY,
    empresa_id UUID NOT NULL,
    contract_number VARCHAR(32) NOT NULL UNIQUE,
    status_id BIGINT NOT NULL REFERENCES status_contrato(id),
    data_criacao TIMESTAMP NOT NULL,
    data_atualizacao_status TIMESTAMP NULL
);

CREATE INDEX idx_contratos_empresa_id ON contratos(empresa_id);
CREATE INDEX idx_contratos_status_id ON contratos(status_id);
CREATE INDEX idx_contratos_data_criacao ON contratos(data_criacao);
CREATE UNIQUE INDEX uq_contratos_empresa_aberto ON contratos(empresa_id) WHERE status_id <> 3;
