CREATE TABLE IF NOT EXISTS call_log (
    id UUID PRIMARY KEY,
    endpoint TEXT NOT NULL,
    http_method TEXT NOT NULL,
    params TEXT,
    status_code INT NOT NULL,
    response_body TEXT,
    error_message TEXT,
    called_at TIMESTAMPTZ NOT NULL
);