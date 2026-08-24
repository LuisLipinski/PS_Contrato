ALTER TABLE contratos ADD COLUMN IF NOT EXISTS onboarding_id UUID;
ALTER TABLE contratos ADD COLUMN IF NOT EXISTS activation_payment_id UUID;
ALTER TABLE contratos ADD COLUMN IF NOT EXISTS data_pagamento_confirmado TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uq_contratos_onboarding_id
    ON contratos(onboarding_id)
    WHERE onboarding_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_contratos_activation_payment_id
    ON contratos(activation_payment_id)
    WHERE activation_payment_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS contrato_numero_sequencia (
    periodo VARCHAR(6) PRIMARY KEY,
    ultimo_valor BIGINT NOT NULL CHECK (ultimo_valor > 0)
);

INSERT INTO contrato_numero_sequencia (periodo, ultimo_valor)
SELECT
    SUBSTRING(contract_number, 1, 6) AS periodo,
    MAX(CAST(SUBSTRING(contract_number, 7, 6) AS BIGINT)) AS ultimo_valor
FROM contratos
WHERE contract_number ~ '^[0-9]{12}$'
GROUP BY SUBSTRING(contract_number, 1, 6)
ON CONFLICT (periodo)
DO UPDATE SET ultimo_valor = GREATEST(
    contrato_numero_sequencia.ultimo_valor,
    EXCLUDED.ultimo_valor
);
