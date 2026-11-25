ALTER TABLE audit_log DROP COLUMN actor_email;
ALTER TABLE audit_log ADD COLUMN actor_id BIGINT;
CREATE INDEX idx_audit_log_action ON audit_log (action);
