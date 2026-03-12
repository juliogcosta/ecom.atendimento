CREATE TABLE IF NOT EXISTS ecom_ae.aggregate (
  id              UUID     PRIMARY KEY,
  version         INTEGER  NOT NULL,
  aggregate_type  TEXT     NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_aggregate_aggregate_type ON ecom_ae.aggregate (aggregate_type);

CREATE TABLE IF NOT EXISTS ecom_ae.event (
  id              BIGSERIAL  PRIMARY KEY,
  transaction_id  XID8       NOT NULL,
  aggregate_id    UUID       NOT NULL REFERENCES ecom_ae.aggregate (id),
  version         INTEGER    NOT NULL,
  event_type      TEXT       NOT NULL,
  json_data       JSON       NOT NULL,
  UNIQUE (aggregate_id, version)
);

CREATE INDEX IF NOT EXISTS idx_event_transaction_id_id ON ecom_ae.event (transaction_id, id);
CREATE INDEX IF NOT EXISTS idx_event_aggregate_id ON ecom_ae.event (aggregate_id);
CREATE INDEX IF NOT EXISTS idx_event_version ON ecom_ae.event (version);

CREATE TABLE IF NOT EXISTS ecom_ae.aggregate_snapshot (
  aggregate_id  UUID     NOT NULL REFERENCES ecom_ae.aggregate (id),
  version       INTEGER  NOT NULL,
  json_data     JSON     NOT NULL,
  PRIMARY KEY (aggregate_id, version)
);

CREATE INDEX IF NOT EXISTS idX_aggregate_snapshot_aggregate_id ON ecom_ae.aggregate_snapshot (aggregate_id);
CREATE INDEX IF NOT EXISTS idX_aggregate_snapshot_version ON ecom_ae.aggregate_snapshot (version);
                                   
CREATE TABLE IF NOT EXISTS ecom_ae.event_subscription (
  subscription_name    TEXT    PRIMARY KEY,
  last_transaction_id  XID8    NOT NULL,
  last_event_id        BIGINT  NOT NULL
);