DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'ecom_ae') THEN
        CREATE SCHEMA ecom_ae;
    END IF;
END $$;