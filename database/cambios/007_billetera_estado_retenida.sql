-- Aplicar después del esquema v6.
BEGIN;

ALTER TABLE billetera.billetera
    DROP CONSTRAINT chk_billetera_estado,
    ADD CONSTRAINT chk_billetera_estado CHECK (estado IN ('ACTIVA', 'RETENIDA', 'BLOQUEADA'));

COMMIT;
