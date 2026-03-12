CREATE OR REPLACE FUNCTION CHANNEL_EVENT_NOTIFY_FCT()
RETURNS TRIGGER AS
  $BODY$
  DECLARE
    aggregate_type  TEXT;
  BEGIN
    SELECT a.aggregate_type INTO aggregate_type FROM ecom_ae.aggregate a WHERE a.id = new.aggregate_id;
    PERFORM pg_notify('channel_event_notify', aggregate_type);
    RETURN NEW;
  END;
  $BODY$
  LANGUAGE PLPGSQL;

CREATE OR REPLACE TRIGGER CHANNEL_EVENT_NOTIFY_TRG
  AFTER INSERT ON ecom_ae.event
  FOR EACH ROW
  EXECUTE PROCEDURE CHANNEL_EVENT_NOTIFY_FCT();
