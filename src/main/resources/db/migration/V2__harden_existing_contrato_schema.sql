CREATE INDEX IF NOT EXISTS idx_contratos_empresa_id ON contratos(empresa_id);
CREATE INDEX IF NOT EXISTS idx_contratos_status_id ON contratos(status_id);
CREATE INDEX IF NOT EXISTS idx_contratos_data_criacao ON contratos(data_criacao);
CREATE UNIQUE INDEX IF NOT EXISTS uq_contratos_empresa_aberto ON contratos(empresa_id) WHERE status_id <> 3;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM status_contrato WHERE id = 1 AND nome_status = 'Aguardando pagamento')
       OR NOT EXISTS (SELECT 1 FROM status_contrato WHERE id = 2 AND nome_status = 'Ativo')
       OR NOT EXISTS (SELECT 1 FROM status_contrato WHERE id = 3 AND nome_status = 'Inativo') THEN
        RAISE EXCEPTION 'Mapeamento de status_contrato incompatível. Esperado: 1=Aguardando pagamento, 2=Ativo, 3=Inativo.';
    END IF;
END $$;
